use crate::channel_utils::error::ChannelUtilError;
use crate::channel_utils::{IoTMessageBuilder, IoTReceiver};
use config::Config;

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
