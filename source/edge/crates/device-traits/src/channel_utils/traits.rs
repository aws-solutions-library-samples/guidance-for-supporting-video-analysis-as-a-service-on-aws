use crate::channel_utils::error::ChannelUtilError;
use crate::channel_utils::{IoTMessage, IoTMessageBuilder, IoTReceiver};
use async_trait::async_trait;
use config::Config;
use mockall::automock;
use serde_json::json;

/// Used to separate out service setup for IoT communication
pub trait IoTServiceSetup {
    /// Setup global communication channel for IoT service and inject message builder.  Builder should
    /// be setup so only topic + payload information is needed.
    fn create_global_iot_communication_channel(
        buffer_size: usize,
        iot_message_builder: IoTMessageBuilder,
    ) -> Result<IoTReceiver, ChannelUtilError>;
}
/// Used to separate out service setup for IoT communication
pub trait DeviceInformationSetup {
    /// Set global configurations
    fn create_global_device_information(config: &Config) -> Result<(), ChannelUtilError>;
}
/// Trait to send messages to aws iot.
#[automock]
#[async_trait]
pub trait IoTServiceSender {
    /// Call to publish message to IoT.  Specify topic and payload.
    /// This will block if the channels buffer is full
    async fn send_iot_message(&mut self, message: IoTMessage) -> Result<(), ChannelUtilError>;
    /// Call to publish message to IoT.  Specify topic and payload.
    /// This will return an error if the buffer is full but will not block.
    /// If buffer is full just try again later.
    fn try_send_iot_message(&mut self, message: IoTMessage) -> Result<(), ChannelUtilError>;
    /// Call to publish message to IoT.  Use if publishing message outside of async runtime
    /// Specify topic and payload.  This is blocking if the channels buffer is full.
    fn send_iot_message_blocking(&mut self, message: IoTMessage) -> Result<(), ChannelUtilError>;
    /// This method will build the message from the topic and send the message over the channel.
    /// It will return an error if the buffer is full.
    fn try_build_and_send_iot_message(
        &mut self,
        topic: &str,
        payload: String,
    ) -> Result<(), ChannelUtilError>;
    /// This method will build the message from the topic and send the message over the channel.
    async fn build_and_send_iot_message(
        &mut self,
        topic: &str,
        payload: String,
    ) -> Result<(), ChannelUtilError>;
    /// This method will build the message from the topic and send the message over the channel.
    /// This is blocking and will stop the executing thread.
    fn build_and_send_iot_message_blocking(
        &mut self,
        topic: &str,
        payload: String,
    ) -> Result<(), ChannelUtilError>;
    /// Get the id of the client, required for formatting IoT topics.
    fn get_client_id(&mut self) -> Result<String, ChannelUtilError>;
}

// Trivial implementation required for dependency injection and mocking
impl std::fmt::Debug for Box<dyn IoTServiceSender + Send + Sync> {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let message = json!({});
        write!(f, "{}", message)
    }
}
