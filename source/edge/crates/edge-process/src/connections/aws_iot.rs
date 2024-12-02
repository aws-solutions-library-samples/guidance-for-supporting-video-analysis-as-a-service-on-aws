// GRCOV_STOP_COVERAGE
// We do not want to include file in code coverage report as unit testing dependency injection
// will not provide any benefit.

use crate::constants::{BUFFER_SIZE, LOG_SYNC, PROVISION_SHADOW_NAME};
use crate::utils::config::Config;
use crate::utils::config::ConfigImpl;
use device_traits::channel_utils::traits::{
    DeviceInformationSetup, IoTServiceSender, IoTServiceSetup,
};
use device_traits::channel_utils::ServiceCommunicationManager;
use device_traits::command::CommandStatus;
use device_traits::connections::{
    AsyncPubSubClient, IotClientManager, PubSubMessageBuilder, ShadowManager,
};
use device_traits::state::{State, StateManager};
use iot_connections::shadow_manager::IotShadowManager;
use mqtt_client::builder::MQTTMessageBuilder;
use serde_json::{json, Value};
use std::borrow::BorrowMut;
use std::path::PathBuf;
use tokio::select;
use tokio::sync::mpsc::Sender;
use tokio::task::JoinHandle;
use tracing::{debug, error, info, instrument, warn};

/// This creates a shadow which automatically sends messages to connections layer to update
/// the cloud based shadow.  The iot communication service is injected into the shadow manager.
#[instrument]
pub async fn new_iot_shadow_manager(
    thing_name: &str,
    shadow_name: Option<String>,
    dir_path: PathBuf,
) -> Box<dyn ShadowManager + Send + Sync> {
    let mut manager = IotShadowManager::new_shadow_manager(
        thing_name,
        shadow_name,
        get_box_iot_sender(),
        new_pub_sub_message_builder(),
        dir_path,
    );
    // Enable integration with filesystem.  We do not want to stop all operations as we can
    // get this information from the cloud as well.
    if let Err(e) = manager.enable_storage().await {
        warn!("Error in enabling storage for shadow manager! : {:?}", e);
    }
    manager
}

/// Inject IoT Message builder into top level crate.
pub fn new_pub_sub_message_builder() -> Box<dyn PubSubMessageBuilder + Send + Sync> {
    MQTTMessageBuilder::new_pub_sub_message_builder()
}

/// Setup event loop for communication with aws-iot
pub async fn setup_and_start_iot_event_loop(
    config: &ConfigImpl,
    logger_config_tx: Sender<String>,
    snapshot_tx: Sender<String>,
    mut pub_sub_client_manager: Box<dyn IotClientManager + Send + Sync>,
    mut iot_client: AsyncPubSubClient,
    command_tx: Sender<Value>,
) -> anyhow::Result<JoinHandle<()>> {
    // Connections startup sequence.
    ServiceCommunicationManager::create_global_device_information(&config.get_config())
        .expect("Failure to setup global device information");
    let mut rx = ServiceCommunicationManager::create_global_iot_communication_channel(
        BUFFER_SIZE,
        new_pub_sub_message_builder(),
    )
    .expect("Unable to create IoT Communication channel.");

    let handle = tokio::spawn(async move {
        let _msgs = false;
        loop {
            let log_sync = std::env::var(LOG_SYNC).unwrap_or("FALSE".to_string()).eq("TRUE");

            // Publish and empty message to applicable shadow to pass delta.
            trigger_shadows();
            loop {
                select! {
                    // Messages to publish to the cloud.
                    Some(msg_out) = rx.recv() => {
                        // Should assume errors are recoverable and log them.  IoT Client should panic if errors are unrecoverable
                        match iot_client.send(msg_out).await {
                            Ok(_) => {
                                debug!("Message published to AWS IoT.");
                            },
                            Err(e) => {
                                error!("Error sending message to AWS IoT : {:?}",e);
                            }
                        }
                    },
                    // Messages from IoT
                    Ok(msg_in) = iot_client.recv() => {
                        // System state change message received.
                        if let Some(new_state) = pub_sub_client_manager.received_state_message(msg_in.as_ref()) {
                            match new_state {
                                //Device has been set to ENABLED
                                State::CreateOrEnableSteamingResources => {
                                    StateManager::set_state(State::CreateOrEnableSteamingResources);
                                },
                                //Device has been set to DISABLED
                                State::DisableStreamingResources => {
                                    StateManager::set_state(State::DisableStreamingResources);
                                },
                            }
                        }

                        if let Some(message) = pub_sub_client_manager.received_logger_settings_message(msg_in.as_ref()) {
                            if log_sync {
                                info!("Logger settings message received {:?}", message);
                                let _ = logger_config_tx.send(message.to_string()).await;
                            }
                        }

                        if let Some(message) = pub_sub_client_manager.received_snapshot_message(msg_in.as_ref()) {
                            info!("Snapshot presigned url received {:?}", message);
                            let _ = snapshot_tx.send(message).await;
                        };

                        if let Some(job_id) = pub_sub_client_manager.received_jobs_notify_message(msg_in.as_ref()) {
                            info!("New IoT Job message received.");
                            let res = pub_sub_client_manager.start_next_command(iot_client.borrow_mut()).await.unwrap_or(None);

                            if let Some(command) = res {
                                let _ = command_tx.send(json!({"job_id": job_id, "command": command.as_str()})).await;
                            } else {
                                // Set command status to failed if a command is unrecognized
                                warn!("Unrecognized command");
                                let _ = pub_sub_client_manager.update_command_status(CommandStatus::Failed, job_id, iot_client.borrow_mut()).await;
                            }

                        }

                        info!("received message :{}", msg_in.get_payload());
                    }

                }
            }
        }
    });
    Ok(handle)
}

fn get_box_iot_sender() -> Box<dyn IoTServiceSender + Send + Sync> {
    Box::<ServiceCommunicationManager>::default()
}

/// Publish an empty message to provision and videoEncoder so the delta is returned to Process 2.
fn trigger_shadows() {
    let mut iot_sender = Box::<ServiceCommunicationManager>::default();

    let payload = json!({"state":{}});

    let shadows = &[PROVISION_SHADOW_NAME];
    for shadow in shadows {
        let topic = match iot_sender.get_client_id() {
            Ok(id) => {
                format!("$aws/things/{}/shadow/name/{}/update", id, shadow)
            }
            Err(e) => {
                panic!("{:?}", e);
            }
        };
        if let Err(e) =
            iot_sender.try_build_and_send_iot_message(topic.as_str(), payload.to_string())
        {
            panic!("{:?}", e);
        }
    }
}

/// Publish a message to IoT Jobs to update job status.
pub fn update_command_status(status: CommandStatus, job_id: String) {
    let mut iot_sender = Box::<ServiceCommunicationManager>::default();

    let payload = json!({"status": status.as_str()});

    let topic = match iot_sender.get_client_id() {
        Ok(client_id) => {
            format!("$aws/things/{}/jobs/{}/update", client_id, job_id)
        }
        Err(e) => {
            panic!("{:?}", e);
        }
    };
    // attempt to send mqtt message to update command status max 5 times
    for i in 1..5 {
        if let Err(e) =
            iot_sender.try_build_and_send_iot_message(topic.as_str(), payload.to_string())
        {
            error!("Failed to update command status on attempt {}: {:?}", i, e);
        } else {
            break;
        }
    }
}
