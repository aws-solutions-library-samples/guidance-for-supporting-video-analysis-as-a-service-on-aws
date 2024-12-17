use device_traits::connections::{PubSubClient, ShadowManager};
use device_traits::state::{State, StateManager};
use edge_process::config::get_media_config;
use edge_process::constants::{
    BUFFER_SIZE, IS_ENABLED, IS_STATUS_CHANGED, LOG_LEVEL, LOG_SYNC, PROVISION_SHADOW_NAME,
    SNAPSHOT_SHADOW_NAME, SYNC_FREQUENCY, VIDEO_ENCODER_SHADOW_NAME,
};
use edge_process::device_streaming_config;
use edge_process::device_thumbnail::{
    get_device_snapshot_config_instance, get_device_snapshot_instance,
};
use edge_process::log_sync::log_sync::setup_and_start_log_sync_loop;
use edge_process::utils::{
    args::get_cli_args,
    config::{Config, ConfigImpl},
    logger_setup::init_tracing,
};
use gstreamer_pipeline::event_ingestion::{create_streaming_service, initiate_event_ingestion};

use device_traits::command::{Command, CommandStatus};
use device_traits::DeviceStateModel;
use edge_process::connections::aws_iot::{
    new_iot_shadow_manager, setup_and_start_iot_event_loop, update_command_status,
};
use edge_process::device_state::get_device_model;
use futures_util::stream::StreamExt;
use iot_connections::client::IotMqttClientManager;
use kvs_client::client::KinesisVideoStreamClient;
use once_cell::sync::Lazy;
use reqwest::{Client, Proxy};
use serde_json::{json, Value};
use snapshot_client::constants::INTERVAL_BETWEEN_SNAPSHOT_UPDATE;
use std::borrow::BorrowMut;
use std::env;
use std::error::Error;
use std::process::ExitCode;
use std::str::FromStr;
use std::sync::mpsc::sync_channel;
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::sync::mpsc::channel;
use tokio::time::{sleep, Instant};
use tokio::{select, try_join};
use tracing::{debug, error, info, warn};
use ws_discovery_client::client::ws_discovery_client::DiscoveryBuilder;
use webrtc_client::constants::{CLIENT_ID, STATE};
use webrtc_client::signaling_client::WebrtcSignalingClient;
use webrtc_client::state_machine::WebrtcStateMachine;

#[tokio::main]
async fn main() -> Result<ExitCode, Box<dyn Error>> {
    let cli_args = get_cli_args();

    let config_path = cli_args.settings_path.expect("No config path entered!");

    // Get settings from file
    let configurations = ConfigImpl::new(&config_path.clone()).await?;

    let settings = configurations.get_settings();

    let dir = settings.get_dir().await.expect("Invalid Directory Entered");

    let local_log_file_path = dir.to_owned().join("logs");
    let dir_path = dir.to_owned().display().to_string();

    //File Writer collects tracing logs, Returns a guard which must exist for the lifetime of the program.
    let _log_file_guard = init_tracing(&settings).await;

    let mut http_client = Client::new();

    // If we are tunneling IP, the remote endpoint is getting reached
    // We need to set up a correct proxy for reqwest to use
    // this is used for local development and testing purposes
    if let Ok(localhost_endpoint) = env::var("LOCALHOST_ENDPOINT") {
        http_client = Client::builder().proxy(Proxy::http(localhost_endpoint)?).build()?;
    }

    let ip_address = env::var("LOCALHOST_ENDPOINT").unwrap_or("127.0.0.1".to_string());

    let log_sync = env::var(LOG_SYNC).unwrap_or("FALSE".to_string()).eq("TRUE");

    let mut pub_sub_client_manager =
        IotMqttClientManager::new_iot_connection_manager(configurations.get_config());
    let mut iot_client: Box<dyn PubSubClient + Send + Sync> =
        pub_sub_client_manager.new_pub_sub_client().await?;

    // IP address discovery feature
    if cfg!(feature = "ip-discovery") {
        let devices = DiscoveryBuilder::default().run().await.unwrap().collect::<Vec<_>>().await;

        debug!("Devices found: {:?}", devices);
        // There should only be 1 device found since we are using unicast
        if devices.len() > 1 {
            error!("More than 1 device IP received");
        }
        if let Some(host) =
            devices.first().and_then(|device| device.urls.first()).and_then(|url| url.host())
        {
            info!("Device IP found: {}", host.to_string());
        }
    }

    // Reboot command feature
    if cfg!(feature = "command") {
        // Get the next pending IoT job that's in progress when edge process starts up.
        // For remote operations such as reboot device, there wll be pending job execution that needs to be mark completed when edge process boots up.
        let next_pending_job_exec = pub_sub_client_manager
            .get_next_pending_job_execution(iot_client.borrow_mut())
            .await
            .unwrap_or(Some(json! {{}}));
        // Remote operations that trigger device to restart. For example: REBOOT.
        // Only those those job types are required to be check when device boot up
        let _ = pub_sub_client_manager
            .update_in_progress_job_status(iot_client.borrow_mut(), next_pending_job_exec)
            .await;
    }

    let mut iot_shadow_client_video_config = new_iot_shadow_manager(
        settings.get_client_id(),
        Some(VIDEO_ENCODER_SHADOW_NAME.to_string()),
        dir.to_owned(),
    )
    .await;

    let mut iot_shadow_client_provision_for_log = new_iot_shadow_manager(
        settings.get_client_id(),
        Some(PROVISION_SHADOW_NAME.to_string()),
        dir.to_owned(),
    )
    .await;

    let mut iot_shadow_client_for_snapshot = new_iot_shadow_manager(
        settings.get_client_id(),
        Some(SNAPSHOT_SHADOW_NAME.to_string()),
        dir.to_owned(),
    )
    .await;

    // Video encoder configuration feature
    if cfg!(feature = "config") {
        let mut device_model =
            setup_device_model(http_client.clone(), config_path.clone(), ip_address.clone())
                .await?;
        // Get video settings from Process 1
        let video_settings =
            device_model.get_video_encoder_configurations().await.unwrap_or(json!({}));
        pub_sub_client_manager
            .publish_video_settings(video_settings, iot_client.borrow_mut())
            .await
            .expect("Failed to publish video settings at start up");
    }

    let config_media_path = get_media_config(&config_path.clone()).await?;

    let mut thumbnail_model = get_device_snapshot_config_instance(http_client.clone());

    thumbnail_model.set_up_services_uri(ip_address.clone()).await?;
    thumbnail_model
        .bootstrap(
            config_media_path.onvif_account_name.clone(),
            config_media_path.onvif_password.clone(),
        )
        .await?;

    let (snapshot_tx, mut snapshot_rx) = channel::<String>(BUFFER_SIZE);
    // Snapshot logic
    let mut snapshot_client = get_device_snapshot_instance(
        http_client.clone(),
        config_media_path.snapshot_username,
        config_media_path.snapshot_password,
    );

    let _snapshot_join_handle = tokio::spawn(async move {
        let interval = sleep(Duration::from_millis(1));
        tokio::pin!(interval);
        loop {
            if !is_device_state_create_or_enable() {
                sleep(Duration::from_millis(250)).await;
                continue;
            }
            select! {
                () = &mut interval => {
                    let snapshot_uri = thumbnail_model
                        .get_snapshot_uri()
                        .await
                        // if snapshot uri is empty, try_send_request_presigned_url_to_iot will return error and it will be logged
                        .unwrap_or_default();
                    let _ = snapshot_client.try_send_request_presigned_url_to_iot(snapshot_uri).await;
                    interval.as_mut().reset(Instant::now() + Duration::from_secs(INTERVAL_BETWEEN_SNAPSHOT_UPDATE));
                }
                Some(presigned_url) = snapshot_rx.recv() => {
                    snapshot_client.upload_snapshot_to_presigned_url(presigned_url.clone())
                        .await
                        .expect("Unable to upload snapshot");
                    let update_doc = json!({"presignedUrl": presigned_url.clone()});
                    let _res = iot_shadow_client_for_snapshot.as_mut().update_reported_state(update_doc.to_owned()).await;
                }
            }
        }
    });

    let mut streaming_model =
        device_streaming_config::get_device_streaming_config_instance(http_client.clone());
    streaming_model.set_up_services_uri(ip_address.clone()).await?;
    streaming_model
        .bootstrap(
            config_media_path.onvif_account_name.clone(),
            config_media_path.onvif_password.clone(),
        )
        .await?;
    let _get_stream_response = streaming_model.get_stream_uri().await?;
    let stream_uri_config = streaming_model
        .get_rtsp_url(
            config_media_path.onvif_account_name.clone(),
            config_media_path.onvif_password.clone(),
        )
        .await?;
    
    // Livestream
    let (streaming_signal_tx, mut streaming_signal_rx) = channel::<String>(BUFFER_SIZE);
    let mut webrtc_state_machine = WebrtcStateMachine::new(
        streaming_signal_tx.clone(),
        stream_uri_config.rtsp_uri.clone(),
        config_media_path.onvif_account_name.clone(),
        config_media_path.onvif_password.clone(),
        config_media_path.aws_region.clone(),
    )
    .expect("Unable to create webrtc state machine");

    // This is used by P2P to send and receive P2P connection state with cloud.
    // using unnamed classic shadow to communicate P2P connections from cloud to edge
    let mut iot_classic_shadow_client: Box<dyn ShadowManager + Send + Sync> =
        new_iot_shadow_manager(settings.get_client_id(), None, dir.to_owned()).await;

    let (peer_connection_tx, mut peer_connection_rx) = channel::<String>(1000);

    let _peer_streaming_join_handle = tokio::spawn(async move {
        clear_peer_to_peer_content_from_shadow_blocking(iot_classic_shadow_client.as_mut()).await;
        loop {
            // Guard to prevent new peer-to-peer streaming unless device is enabled.
            if !is_device_state_create_or_enable() {
                sleep(Duration::from_millis(250)).await;
                continue;
            }
            select! {
                // Handle connection from the cloud
                Some(message) = peer_connection_rx.recv() => {
                    let Ok(message) = serde_json::from_str(&message) else {
                        error!("Error in converting peer connection from string to json");
                        return;
                    };

                    debug!("received connection signal from the cloud");
                    let _res = webrtc_state_machine
                        .handle_state_update(message).await;
                }

                // Temporary solution for clearing out shadows
                Some(msg_in) = streaming_signal_rx.recv() => {

                    info!("received message");
                    // Re instantiate box inside of move
                    let mut iot_classic_shadow_client = new_iot_shadow_manager(
                        settings.get_client_id(),
                        None,
                        dir.to_owned(),
                    )
                    .await;

                    let Ok(message) = serde_json::from_str::<Value>(&msg_in) else {
                        error!("Error in converting streaming signal message from string to json");
                        return;
                    };
                    let client_id = &message[CLIENT_ID];
                    let status = &message[STATE];
                    if status == "Failed" || status == "Disconnected" {
                        let Ok(clear_streaming_peer_connections_shadow_entry)  = serde_json::from_str::<Value>(&format!(
                            "{{
                                \"StreamingPeerConnections\": {{
                                    {client_id}: null
                                }}
                            }}"
                        )) else {
                            error!("Error in converting peer connection from string to json");
                            return;
                        };
                        info!("Clearing state");
                        let _res = iot_classic_shadow_client.update_desired_state_from_device(clear_streaming_peer_connections_shadow_entry.clone()).await;

                        let res = iot_classic_shadow_client.update_reported_state(clear_streaming_peer_connections_shadow_entry.clone()).await;
                        let _res = webrtc_state_machine.handle_state_update(message.clone()).await;
                    }
                }
            }
        }
    });
    let peer_connection_tx_clone = peer_connection_tx.clone();

    let (video_config_tx, mut video_config_rx) = channel::<Value>(BUFFER_SIZE);
    let (logger_config_tx, mut logger_config_rx) = channel::<String>(BUFFER_SIZE);
    let (log_tx, log_rx) = channel::<Value>(BUFFER_SIZE);

    static LOG_SYNC_STATUS: Lazy<Arc<Mutex<bool>>> = Lazy::new(|| Arc::new(Mutex::new(true)));
    static LOG_SYNC_LEVEL: Lazy<Arc<Mutex<String>>> =
        Lazy::new(|| Arc::new(Mutex::new("INFO".to_string())));
    static LOG_SYNC_FREQUENCY: Lazy<Arc<Mutex<u64>>> = Lazy::new(|| Arc::new(Mutex::new(300)));

    let mut config_onvif_client =
        setup_device_model(http_client.clone(), config_path.clone(), ip_address.clone()).await?;
    let _config_join_handle = tokio::spawn(async move {
        loop {
            select! {
                Some(video_settings) = video_config_rx.recv() => {
                    // Video encoder configuration feature
                    if cfg!(feature = "config") {
                        /* expected structure of video_settings
                            {
                                "vec1": {
                                    "codec": "H264",
                                    "bitRateType": "VBR",
                                    "frameRateLimit": 15,
                                    "resolution": "1920x1080",
                                    "bitRateLimit": 1024,
                                    "gopLength": 60
                                }
                            }
                        */
                        let video_settings_object = video_settings.as_object().unwrap();
                        for (key, value) in video_settings_object.iter() {
                            info!("key: {}, value: {}", key, value);
                            let _res = config_onvif_client.set_video_encoder_configuration(key.to_owned(), value.to_owned()).await;
                        }
                        let video_settings_res = config_onvif_client.get_video_encoder_configurations().await;
                        match video_settings_res {
                            Ok(video_settings) => iot_shadow_client_video_config.update_reported_state(video_settings).await.expect("Failed to update configuration shadow"),
                            Err(_) => error!("Could not retrieve video settings after configuration"),
                        }
                    } else {
                        info!("Video encoder configurations is not enabled. No-op.");
                    }
                },
                Some(logger_settings) = logger_config_rx.recv() => {
                    /* expected structure of logger_settings
                        {
                            "isEnabled": true,
                            "syncFrequency": 300,
                            "logLevel": "INFO"
                        }
                    */
                    if log_sync {
                        // LOG SYNC env variable is set to true, code has received new log settings from cloud
                        // if we are unable to parse those settings, code should panic and stop
                        let logger_settings_value: Value = serde_json::from_str(logger_settings.as_str()).expect("received invalid JSON for log sync settings");
                        let logger_settings_object = logger_settings_value.as_object().expect("unable to parse log sync JSON as a map");
                        info!("Logger settings: {:?}", logger_settings_value);

                        let mut data = json!({});
                        data[IS_STATUS_CHANGED] = json!(false);

                        if !logger_settings_object.is_empty() {
                            let mut global_sync_status = LOG_SYNC_STATUS.lock().expect("Global Logger Sync Setting is Poisoned");
                            let mut global_sync_frequency = LOG_SYNC_FREQUENCY.lock().expect("Global Logger Frequency Setting is Poisoned");
                            let mut global_sync_level = LOG_SYNC_LEVEL.lock().expect("Global Logger Level Setting is Poisoned");
                            if logger_settings_object.contains_key(IS_ENABLED) {
                                let value = logger_settings_object.get(IS_ENABLED);
                                *global_sync_status = value.expect("Can not get log sync status").as_bool().unwrap_or(true);
                                data[IS_STATUS_CHANGED] = json!(true);
                            }
                            if logger_settings_object.contains_key(SYNC_FREQUENCY) {
                                let value = logger_settings_object.get(SYNC_FREQUENCY);
                                *global_sync_frequency = value.expect("Can not get sync frequency").as_u64().unwrap_or(300);
                            }
                            if logger_settings_object.contains_key(LOG_LEVEL) {
                                let value = logger_settings_object.get(LOG_LEVEL);
                                *global_sync_level = value.expect("Can not get log level").as_str().unwrap_or("INFO").to_string();
                            }
                            let sync_status = *global_sync_status;
                            data[IS_ENABLED] = json!(sync_status);
                            let sync_frequency = *global_sync_frequency;
                            data[SYNC_FREQUENCY] = json!(sync_frequency);
                            let sync_level = (*global_sync_level).clone();
                            data[LOG_LEVEL] = json!(sync_level);
                        }

                        let _ = log_tx.send(data.clone()).await;

                        let report_logger_settings = json!({"loggerSettings": logger_settings_value});
                        while let Err(_e) = iot_shadow_client_provision_for_log
                            .update_reported_state(report_logger_settings.to_owned())
                            .await
                        {
                            debug!("Failed to update logger configuration in provision shadow");
                            sleep(Duration::from_millis(100)).await;
                        }
                    }
                }
            }
        }
    });
    let video_config_tx_clone = video_config_tx.clone();
    let logger_config_tx_clone = logger_config_tx.clone();
    let snapshot_tx_clone = snapshot_tx.clone();

    let mut command_onvif_client =
        setup_device_model(http_client.clone(), config_path.clone(), ip_address.clone()).await?;

    let (command_tx, mut command_rx) = channel::<Value>(BUFFER_SIZE);
    let _command_join_handle = tokio::spawn(async move {
        loop {
            select! {
                Some(command_info) = command_rx.recv() => {
                    /* expected structure of command_info
                        {
                            "job_id": <job-id>,
                            "command": Command enum,
                        }
                    */
                    let command_type = command_info
                        .get("command")
                        .and_then(|command| command.as_str())
                        .and_then(|command_str| Command::from_str(command_str).ok())
                        .unwrap_or(Command::Unknown);

                    let job_id_str = command_info
                        .get("job_id")
                        .and_then(|job_id| job_id.as_str())
                        .unwrap_or("no job_id in the payload.");

                    // Reboot command feature
                    if cfg!(feature = "command") {
                        match command_type {
                            Command::Reboot => {
                                info!("Trying to reboot device");

                                let res = command_onvif_client.reboot_device().await;
                                if res.is_err() {
                                    error!("Error rebooting device: {:?}", res);
                                    update_command_status(CommandStatus::Failed, job_id_str.to_string());
                                } else {
                                    info!("Initiated reboot");
                                    // do not update status to SUCCEEDED until after edge-process binary is restarted
                                }
                            },
                            Command::Unknown => {
                                warn!("Unrecognized command");
                            },
                        }
                    } else {
                        info!("Reboot command is not enabled. Marking job as failed.");
                        update_command_status(CommandStatus::Failed, job_id_str.to_string());
                    }
                }
            }
        }
    });
    let command_tx_clone = command_tx.clone();

    let _iot_loop_handle = setup_and_start_iot_event_loop(
        &configurations,
        logger_config_tx_clone,
        snapshot_tx_clone,
        video_config_tx_clone,
        pub_sub_client_manager,
        iot_client,
        command_tx_clone,
        peer_connection_tx_clone,
    )
    .await?;

    let _log_loop_handle =
        setup_and_start_log_sync_loop(dir_path, local_log_file_path, log_rx).await?;

    loop {
        match StateManager::get_state() {
            State::CreateOrEnableSteamingResources => {
                info!("Device is enabled creating streaming resources.");
                break;
            }
            _ => {
                debug!("Device not enabled. Blocking creation streaming resources.");
            }
        }
        sleep(Duration::from_millis(250)).await;
    }

    let (motion_based_streaming_tx, motion_based_streaming_rx) = sync_channel(5);
    let uri_config = stream_uri_config.clone();
    let mut streaming_service = create_streaming_service(uri_config);

    // Livestream
    // ServiceCommunicationManager is established in setup_and_start_iot_event_loop().
    // Therefore, KinesisVideoStreamClient that depends on ServiceCommunicationManager can't be instantiated prior to that function
    let kvs_client = KinesisVideoStreamClient::from_conf().await?;

    let _webrtc_signaling_client = WebrtcSignalingClient::new(
        kvs_client.ice_server_configs.to_owned(),
        stream_uri_config.rtsp_uri.clone(),
        config_media_path.aws_region.clone(),
        config_media_path.onvif_account_name.clone(),
        config_media_path.onvif_password.clone(),
    )
    .await?;

    let _gstreamer_pipeline_handle = tokio::spawn(async move {
        // Start pipeline when device is in the correct state.
        loop {
            match StateManager::get_state() {
                // Device has been set to ENABLED
                State::CreateOrEnableSteamingResources => {
                    streaming_service.ensure_start().await;
                }
                // Device has been set to DISABLED
                State::DisableStreamingResources => {
                    streaming_service.ensure_stop().await;
                }
            }
            sleep(Duration::from_millis(250)).await;
        }
    });

    // Setup AI metadata pipeline + control threads.
    let (_thread_ai_event_handle, _task_ai_pipeline_control_handle) =
        initiate_event_ingestion(stream_uri_config.clone(), motion_based_streaming_tx).await?;

    // This will keep process 2 alive until connections task stops.
    try_join!(_iot_loop_handle)?;

    Ok(ExitCode::SUCCESS)
}

fn is_device_state_create_or_enable() -> bool {
    let state = StateManager::get_state();
    state == State::CreateOrEnableSteamingResources
}

async fn setup_device_model(
    http_client: Client,
    config_path: String,
    ip_address: String,
) -> Result<Box<dyn DeviceStateModel + Send + Sync>, Box<dyn Error>> {
    let mut model = get_device_model(http_client);

    // bootstrap the model layer before retrieving any information from the device
    model.bootstrap(config_path.as_str(), ip_address).await?;
    Ok(model)
}

async fn clear_peer_to_peer_content_from_shadow_blocking(
    peer_to_peer_shadow_manager: &mut (dyn ShadowManager + Send + Sync),
) {
    let clear_streaming_peer_connections_shadow_entry = json!({ "StreamingPeerConnections": null });
    // IoT service can take a little while to start up so retry until successful.
    while let Err(_e) = peer_to_peer_shadow_manager
        .update_reported_state(clear_streaming_peer_connections_shadow_entry.to_owned())
        .await
    {
        debug!("Iot classic shadow failed to update, retrying.");
        sleep(Duration::from_millis(100)).await;
    }

    while let Err(_e) = peer_to_peer_shadow_manager
        .update_desired_state_from_device(clear_streaming_peer_connections_shadow_entry.to_owned())
        .await
    {
        debug!("Iot classic shadow failed to update, retrying.");
        sleep(Duration::from_millis(100)).await;
    }
}
