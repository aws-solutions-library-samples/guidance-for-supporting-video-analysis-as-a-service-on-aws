use device_traits::connections::PubSubClient;
use device_traits::state::{State, StateManager};
use edge_process::config::get_media_config;
use edge_process::connections::aws_iot::{new_iot_shadow_manager, setup_and_start_iot_event_loop};
use edge_process::constants::{
    BUFFER_SIZE, IS_ENABLED, IS_STATUS_CHANGED, LOG_LEVEL, LOG_SYNC, PROVISION_SHADOW_NAME,
    SNAPSHOT_SHADOW_NAME, SYNC_FREQUENCY,
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
use gstreamer_pipeline::event_ingestion::create_streaming_service;
use iot_connections::client::IotMqttClientManager;
use once_cell::sync::Lazy;
use reqwest::{Client, Proxy};
use serde_json::{json, Value};
use snapshot_client::constants::INTERVAL_BETWEEN_SNAPSHOT_UPDATE;
use std::env;
use std::error::Error;
use std::process::ExitCode;
use std::sync::mpsc::sync_channel;
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::sync::mpsc::channel;
use tokio::time::{sleep, Instant};
use tokio::{select, try_join};
use tracing::{debug, info};

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

    let pub_sub_client_manager =
        IotMqttClientManager::new_iot_connection_manager(configurations.get_config());
    let iot_client: Box<dyn PubSubClient + Send + Sync> =
        pub_sub_client_manager.new_pub_sub_client().await?;

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
            if !is_device_state_create_enable_or_disable() {
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
        device_streaming_config::get_device_streaming_config_instance(http_client);
    streaming_model.set_up_services_uri(ip_address).await?;
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

    let (logger_config_tx, mut logger_config_rx) = channel::<String>(BUFFER_SIZE);
    let (log_tx, log_rx) = channel::<Value>(BUFFER_SIZE);

    static LOG_SYNC_STATUS: Lazy<Arc<Mutex<bool>>> = Lazy::new(|| Arc::new(Mutex::new(true)));
    static LOG_SYNC_LEVEL: Lazy<Arc<Mutex<String>>> =
        Lazy::new(|| Arc::new(Mutex::new("INFO".to_string())));
    static LOG_SYNC_FREQUENCY: Lazy<Arc<Mutex<u64>>> = Lazy::new(|| Arc::new(Mutex::new(300)));

    let _config_join_handle = tokio::spawn(async move {
        loop {
            select! {
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

    let logger_config_tx_clone = logger_config_tx.clone();
    let snapshot_tx_clone = snapshot_tx.clone();

    let _iot_loop_handle = setup_and_start_iot_event_loop(
        &configurations,
        logger_config_tx_clone,
        snapshot_tx_clone,
        pub_sub_client_manager,
        iot_client,
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

    let (_motion_based_streaming_tx, motion_based_streaming_rx) = sync_channel(5);

    let uri_config = stream_uri_config.clone();
    let mut streaming_service = create_streaming_service(uri_config, motion_based_streaming_rx);

    let _gstreamer_pipeline_handle = tokio::spawn(async move {
        //Start pipeline when device is in the correct state.
        loop {
            match StateManager::get_state() {
                //Device has been set to ENABLED
                State::CreateOrEnableSteamingResources => {
                    streaming_service.ensure_start().await;
                }
                //Device has been set to DISABLED
                State::DisableStreamingResources => {
                    streaming_service.ensure_stop().await;
                }
            }
            sleep(Duration::from_millis(250)).await;
        }
    });

    //This will keep process 2 alive until connections task stops.
    try_join!(_iot_loop_handle)?;

    Ok(ExitCode::SUCCESS)
}

fn is_device_state_create_enable_or_disable() -> bool {
    let state = StateManager::get_state();
    state == State::CreateOrEnableSteamingResources || state == State::DisableStreamingResources
}
