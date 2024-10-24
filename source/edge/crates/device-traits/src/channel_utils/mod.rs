//! This module holds utilities to organize tokio channels
//! By using static thread-safe objects we can make it easier for the various services in edge process to send messages to each other.
use crate::channel_utils::global_settings::Configurations;
use crate::channel_utils::traits::{DeviceInformationSetup, IoTServiceSetup};
use crate::{
    channel_utils::error::ChannelUtilError,
    connections::{PubSubMessage, PubSubMessageBuilder},
};
use config::Config;
use once_cell::sync::Lazy;
use std::sync::Mutex;
use tokio::sync::mpsc::{channel, Receiver};

/// Module holds errors associated with channel_utils
pub mod error;
/// Module to hold config and settings objects for all edge process services
pub mod global_settings;
/// Module which holds traits related to setting up communication channels.
pub mod traits;

pub(crate) type IoTReceiver = Receiver<Box<dyn PubSubMessage + Send + Sync>>;
pub(crate) type IoTMessageBuilder = Box<dyn PubSubMessageBuilder + Send + Sync>;
pub(crate) type IoTMessage = Box<dyn PubSubMessage + Send + Sync>;

// Global static to ensure full system can access the channels including C dependencies.
static COMMUNICATION_CHANNELS: Lazy<Mutex<GlobalData>> =
    Lazy::new(|| Mutex::new(GlobalData::default()));

#[derive(Debug, Default)]
struct GlobalData {
    pub configurations: Option<Configurations>,
    pub iot_message_builder: Option<IoTMessageBuilder>,
}

/// ServiceCommunicationManager struct is used to send messages between Services within edge process
/// it follows a lazy loader approach so will only access global values when channel between services is created.
#[derive(Debug, Default)]
pub struct ServiceCommunicationManager {
    configurations: Option<Configurations>,
    iot_message_builder: Option<IoTMessageBuilder>,
}

impl ServiceCommunicationManager {
    /// Get configurations for edge Process
    pub fn get_configurations(&mut self) -> Result<&Configurations, ChannelUtilError> {
        if self.configurations.is_none() {
            self.configurations = Self::get_configurations_from_global()?;
        }
        // Error should never be thrown here
        self.configurations.as_ref().ok_or(ChannelUtilError::UnknownError)
    }

    fn get_configurations_from_global() -> Result<Option<Configurations>, ChannelUtilError> {
        let mut mut_guard =
            COMMUNICATION_CHANNELS.lock().expect("COMMUNICATION_CHANNELS Mutex Poisoned");
        // Get a copy of the channel if it exists
        match mut_guard.configurations.as_mut() {
            None => Err(ChannelUtilError::CommunicationNotCreated),
            Some(configurations) => Ok(Some(configurations.clone())),
        }
    }

    fn get_iot_message_builder_from_global() -> Result<Option<IoTMessageBuilder>, ChannelUtilError>
    {
        let mut mut_guard =
            COMMUNICATION_CHANNELS.lock().expect("COMMUNICATION_CHANNELS Mutex Poisoned");
        // Get a copy of the channel if it exists
        match mut_guard.iot_message_builder.as_mut() {
            None => Err(ChannelUtilError::CommunicationNotCreated),
            Some(builder) => Ok(Some(builder.clone_builder())),
        }
    }

    fn ensure_iot_setup_from_global(&mut self) -> Result<(), ChannelUtilError> {
        if self.iot_message_builder.is_none() {
            self.iot_message_builder = Self::get_iot_message_builder_from_global()?;
        }
        Ok(())
    }

    fn ensure_configurations_setup_from_global(&mut self) -> Result<(), ChannelUtilError> {
        if self.configurations.is_none() {
            self.configurations = Self::get_configurations_from_global()?;
        }
        Ok(())
    }

    fn build_message_from_builder(
        &mut self,
        topic: &str,
        payload: String,
    ) -> Result<IoTMessage, ChannelUtilError> {
        self.ensure_iot_setup_from_global()?;

        if let Some(ref_to_builder) = self.iot_message_builder.as_mut() {
            ref_to_builder.set_topic(topic);
            ref_to_builder.set_payload(payload);
            return Ok(ref_to_builder.build_box_message());
        }
        // This should never run.
        Err(ChannelUtilError::UnknownError)
    }
}

//Trait used to separate out logic so only the IoTService sets up the channel.
impl IoTServiceSetup for ServiceCommunicationManager {
    /// Setup communication channel.
    fn create_global_iot_communication_channel(
        buffer_size: usize,
        iot_message_builder: IoTMessageBuilder,
    ) -> Result<IoTReceiver, ChannelUtilError> {
        let mut global_resources =
            COMMUNICATION_CHANNELS.lock().expect("COMMUNICATION_CHANNELS Mutex Poisoned");

        if global_resources.iot_message_builder.is_some() {
            return Err(ChannelUtilError::ResourceAlreadyExists);
        }

        // Create tokio channel, add to global resource, return receive channel
        let (_tx, rx) = channel(buffer_size);

        // Assign resources
        global_resources.iot_message_builder = Some(iot_message_builder);
        // Return channel to IoT Service
        Ok(rx)
    }
}

impl DeviceInformationSetup for ServiceCommunicationManager {
    fn create_global_device_information(config: &Config) -> Result<(), ChannelUtilError> {
        let mut mutex_guard =
            COMMUNICATION_CHANNELS.lock().expect("COMMUNICATION_CHANNELS Mutex Poisoned");

        if mutex_guard.configurations.is_some() {
            return Err(ChannelUtilError::ResourceAlreadyExists);
        }

        let configurations = config
            .clone()
            .try_deserialize()
            .expect("Could not generate global client info from config.");

        mutex_guard.configurations = Some(configurations);
        Ok(())
    }
}

// Since tests are performed in the same process the global values are shared.  To account for this
// Tests will be separated into service tests.
#[cfg(test)]
mod tests {
    use super::*;
    use crate::connections::MockPubSubMessage;
    use crate::connections::MockPubSubMessageBuilder;
    const TEST_STRING: &str = "TEST_STRING";

    /// Test creation of Iot service communication channel, return for testing
    fn create_and_test_iot_service_channel(buffer_size: usize) -> IoTReceiver {
        let mock_builder = Box::<MockPubSubMessageBuilder>::default();
        let result = ServiceCommunicationManager::create_global_iot_communication_channel(
            buffer_size,
            mock_builder,
        );
        assert!(result.is_ok());
        let mock_builder = Box::<MockPubSubMessageBuilder>::default();
        let should_err = ServiceCommunicationManager::create_global_iot_communication_channel(
            buffer_size,
            mock_builder,
        );
        assert_eq!(should_err.unwrap_err(), ChannelUtilError::ResourceAlreadyExists);
        result.unwrap()
    }

    fn create_and_test_client_info_in_global() {
        // Manually create a config for testing, unwraps used as this will not fail
        let config = Config::builder()
            .set_default("client_id", TEST_STRING.to_string())
            .unwrap()
            .set_default("ca_path", TEST_STRING.to_string())
            .unwrap()
            .set_default("cert_path", TEST_STRING.to_string())
            .unwrap()
            .set_default("credential_endpoint", TEST_STRING.to_string())
            .unwrap()
            .set_default("key_path", TEST_STRING.to_string())
            .unwrap()
            .set_default("role_aliases", TEST_STRING.to_string())
            .unwrap()
            .set_default("aws_region", TEST_STRING.to_string())
            .unwrap()
            .set_default("dir", TEST_STRING.to_string())
            .unwrap()
            .build()
            .unwrap();

        let should_succeed = ServiceCommunicationManager::create_global_device_information(&config);
        assert!(should_succeed.is_ok());
        let should_err = ServiceCommunicationManager::create_global_device_information(&config);
        assert_eq!(should_err, Err(ChannelUtilError::ResourceAlreadyExists));

        // Confirm that other components can get the generated config object which is deserialize from the input config.
        let mut get_configs = ServiceCommunicationManager::default();
        assert!(get_configs.get_configurations().is_ok())
    }
}
