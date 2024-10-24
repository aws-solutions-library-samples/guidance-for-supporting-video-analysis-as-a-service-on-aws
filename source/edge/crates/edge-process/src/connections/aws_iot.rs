// GRCOV_STOP_COVERAGE
// We do not want to include file in code coverage report as unit testing dependency injection
// will not provide any benefit.

use crate::constants::BUFFER_SIZE;
use crate::utils::config::Config;
use crate::utils::config::ConfigImpl;
use device_traits::channel_utils::traits::{DeviceInformationSetup, IoTServiceSetup};
use device_traits::channel_utils::ServiceCommunicationManager;
use device_traits::connections::{AsyncPubSubClient, IotClientManager, PubSubMessageBuilder};
use mqtt_client::builder::MQTTMessageBuilder;
use tokio::select;
use tokio::task::JoinHandle;
use tracing::{debug, error, info};

/// Inject IoT Message builder into top level crate.
pub fn new_pub_sub_message_builder() -> Box<dyn PubSubMessageBuilder + Send + Sync> {
    MQTTMessageBuilder::new_pub_sub_message_builder()
}

/// Setup event loop for communication with aws-iot
pub async fn setup_and_start_iot_event_loop(
    config: &ConfigImpl,
    mut pub_sub_client_manager: Box<dyn IotClientManager + Send + Sync>,
    mut iot_client: AsyncPubSubClient,
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
                    info!("received message :{}", msg_in.get_payload());
                }
            }
        }
    });
    Ok(handle)
}
