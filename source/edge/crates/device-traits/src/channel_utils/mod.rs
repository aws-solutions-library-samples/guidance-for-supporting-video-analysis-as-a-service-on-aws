//! This module holds utilities to organize tokio channels
//! By using static thread-safe objects we can make it easier for the various services in edge process to send messages to each other.
use crate::channel_utils::global_settings::Configurations;
use crate::channel_utils::traits::{DeviceInformationSetup, IoTServiceSender, IoTServiceSetup};
use crate::{
    channel_utils::error::ChannelUtilError,
    connections::{PubSubMessage, PubSubMessageBuilder},
};
use async_trait::async_trait;
use config::Config;
use once_cell::sync::Lazy;
use std::sync::Mutex;
use tokio::sync::mpsc::error::TrySendError;
use tokio::sync::mpsc::{channel, Receiver, Sender};

/// Module holds errors associated with channel_utils
pub mod error;
/// Module to hold config and settings objects for all edge process services
pub mod global_settings;
/// Module which holds traits related to setting up communication channels.
pub mod traits;

pub(crate) type IoTReceiver = Receiver<Box<dyn PubSubMessage + Send + Sync>>;
pub(crate) type IoTSender = Sender<Box<dyn PubSubMessage + Send + Sync>>;
pub(crate) type IoTMessageBuilder = Box<dyn PubSubMessageBuilder + Send + Sync>;
pub(crate) type IoTMessage = Box<dyn PubSubMessage + Send + Sync>;

// Global static to ensure full system can access the channels including C dependencies.
static COMMUNICATION_CHANNELS: Lazy<Mutex<GlobalData>> =
    Lazy::new(|| Mutex::new(GlobalData::default()));

#[derive(Debug, Default)]
struct GlobalData {
    pub iot_publish_channel: Option<IoTSender>,
    pub configurations: Option<Configurations>,
    pub iot_message_builder: Option<IoTMessageBuilder>,
}

/// ServiceCommunicationManager struct is used to send messages between Services within edge process
/// it follows a lazy loader approach so will only access global values when channel between services is created.
#[derive(Debug, Default)]
pub struct ServiceCommunicationManager {
    iot_publish_channel: Option<IoTSender>,
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

    /// Get tokio channel from global resource, will return an error if the resource is not created.
    fn get_iot_channel_from_global() -> Result<Option<IoTSender>, ChannelUtilError> {
        let mut mut_guard =
            COMMUNICATION_CHANNELS.lock().expect("COMMUNICATION_CHANNELS Mutex Poisoned");
        // Get a copy of the channel if it exists
        match mut_guard.iot_publish_channel.as_mut() {
            None => Err(ChannelUtilError::CommunicationNotCreated),
            Some(e) => Ok(Some(e.clone())),
        }
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
        if self.iot_publish_channel.is_none() {
            self.iot_publish_channel = Self::get_iot_channel_from_global()?;
        }

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

#[async_trait]
impl IoTServiceSender for ServiceCommunicationManager {
    /// Call to publish message to IoT.  Specify topic and payload.
    /// This will block if the channels buffer is full
    async fn send_iot_message(&mut self, message: IoTMessage) -> Result<(), ChannelUtilError> {
        // If empty get from global, avoid doing this every time to improve performance
        if self.iot_publish_channel.is_none() {
            self.iot_publish_channel = Self::get_iot_channel_from_global()?;
        }

        if let Some(ref_to_channel) = self.iot_publish_channel.as_ref() {
            ref_to_channel
                .send(message)
                .await
                .map_err(|e| ChannelUtilError::SendError(format!("{:?}", e)))?;
        }

        Ok(())
    }
    /// Call to publish message to IoT.  Specify topic and payload.
    /// This will return an error if the buffer is full but will not block.
    /// If buffer is full just try again later.
    fn try_send_iot_message(&mut self, message: IoTMessage) -> Result<(), ChannelUtilError> {
        // If empty get from global, avoid doing this every time to improve performance
        if self.iot_publish_channel.is_none() {
            self.iot_publish_channel = Self::get_iot_channel_from_global()?;
        }
        // At this point channel is always defined. So panic otherwise.
        // Also panic if channel closes as this should never happen.
        // If buffer is full just try again later.
        if let Some(ref_to_channel) = self.iot_publish_channel.as_ref() {
            match ref_to_channel.try_send(message) {
                Ok(_) => Ok(()),
                Err(TrySendError::Full(_e)) => Err(ChannelUtilError::BufferFullError),
                Err(TrySendError::Closed(e)) => {
                    panic!("IoT Communication channel closed unexpectedly. :{:?}", e)
                }
            }
        } else {
            panic!("IoT communication channel is in an undefined state.")
        }
    }
    /// Call to publish message to IoT.  Use if publishing message outside of async runtime
    /// Specify topic and payload.  This is blocking if the channels buffer is full.
    fn send_iot_message_blocking(&mut self, message: IoTMessage) -> Result<(), ChannelUtilError> {
        // If empty get from global, avoid doing this every time to improve performance
        if self.iot_publish_channel.is_none() {
            self.iot_publish_channel = Self::get_iot_channel_from_global()?;
        }

        if let Some(ref_to_channel) = self.iot_publish_channel.as_ref() {
            ref_to_channel
                .blocking_send(message)
                .map_err(|e| ChannelUtilError::SendError(format!("{:?}", e)))?;
        }

        Ok(())
    }

    /// This method will build the message from the topic and send the message over the channel
    fn try_build_and_send_iot_message(
        &mut self,
        topic: &str,
        payload: String,
    ) -> Result<(), ChannelUtilError> {
        // Only activates global settings once iot message builder is guaranteed.
        self.ensure_iot_setup_from_global()?;

        let built_message = self.build_message_from_builder(topic, payload)?;

        self.try_send_iot_message(built_message)?;

        Ok(())
    }
    /// This method will build the message from the topic and send the message over the channel.
    async fn build_and_send_iot_message(
        &mut self,
        topic: &str,
        payload: String,
    ) -> Result<(), ChannelUtilError> {
        // Only activates global settings once iot message builder is guaranteed.
        self.ensure_iot_setup_from_global()?;

        let built_message = self.build_message_from_builder(topic, payload)?;

        self.send_iot_message(built_message).await?;

        Ok(())
    }
    /// This method will build the message from the topic and send the message over the channel.
    /// This is blocking and will stop the executing thread.
    fn build_and_send_iot_message_blocking(
        &mut self,
        topic: &str,
        payload: String,
    ) -> Result<(), ChannelUtilError> {
        self.ensure_iot_setup_from_global()?;

        let built_message = self.build_message_from_builder(topic, payload)?;

        self.send_iot_message_blocking(built_message)?;

        Ok(())
    }
    fn get_client_id(&mut self) -> Result<String, ChannelUtilError> {
        self.ensure_configurations_setup_from_global()?;

        if let Some(configurations) = self.configurations.as_mut() {
            return Ok(configurations.client_id.clone());
        }
        // Should never reach this point
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

        if global_resources.iot_publish_channel.is_some() {
            return Err(ChannelUtilError::ResourceAlreadyExists);
        }

        if global_resources.iot_message_builder.is_some() {
            return Err(ChannelUtilError::ResourceAlreadyExists);
        }

        // Create tokio channel, add to global resource, return receive channel
        let (tx, rx) = channel(buffer_size);

        // Assign resources
        global_resources.iot_publish_channel = Some(tx);
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

    const BUFFER_SIZE: usize = 3;
    const TEST_STRING: &str = "TEST_STRING";

    // All tests associated with iot service messaging here.
    // Required due to the use of mutable static data
    #[tokio::test]
    async fn iot_service_communication_tests() {
        // Test failures if IoT service not yet created.
        assert_eq!(
            setup_and_send_iot_service_message().await,
            Err(ChannelUtilError::CommunicationNotCreated)
        );
        assert_eq!(
            setup_and_send_iot_blocking_message(),
            Err(ChannelUtilError::CommunicationNotCreated)
        );
        assert_eq!(
            setup_and_try_send_iot_message(),
            Err(ChannelUtilError::CommunicationNotCreated)
        );
        // Test successes after channel is created.
        let mut iot_rx = create_and_test_iot_service_channel(BUFFER_SIZE);
        // Tests related to normal sending and receiving
        test_iot_send_and_receive_from_multiple_threads(&mut iot_rx, BUFFER_SIZE).await;
        // Tests related to full buffer for try_send
        test_try_send_iot_messages_full_buffer(BUFFER_SIZE);
    }

    #[test]
    fn test_client_info() {
        let mut service_communication_manager = ServiceCommunicationManager::default();
        assert_eq!(
            service_communication_manager.get_client_id(),
            Err(ChannelUtilError::CommunicationNotCreated)
        );
        create_and_test_client_info_in_global();
        assert_eq!(service_communication_manager.get_client_id(), Ok(TEST_STRING.to_string()));
    }

    async fn setup_and_send_iot_service_message() -> Result<(), ChannelUtilError> {
        // Setup mock message + manager.
        let mock_pub_sub_message = Box::new(MockPubSubMessage::new());
        let mut service_communication_manager = ServiceCommunicationManager::default();
        // Send message to iot service.
        service_communication_manager.send_iot_message(mock_pub_sub_message).await
    }

    fn setup_and_send_iot_blocking_message() -> Result<(), ChannelUtilError> {
        // Setup mock message + manager.
        let mock_pub_sub_message = Box::new(MockPubSubMessage::new());
        let mut service_communication_manager = ServiceCommunicationManager::default();
        // Send message to iot service.
        service_communication_manager.send_iot_message_blocking(mock_pub_sub_message)
    }

    fn setup_and_try_send_iot_message() -> Result<(), ChannelUtilError> {
        // Setup mock message + manager.
        let mock_pub_sub_message = Box::new(MockPubSubMessage::new());
        let mut service_communication_manager = ServiceCommunicationManager::default();
        // Send message to iot service.
        service_communication_manager.try_send_iot_message(mock_pub_sub_message)
    }

    async fn test_iot_send_and_receive_from_multiple_threads(
        iot_rx: &mut IoTReceiver,
        buffer_size: usize,
    ) {
        // Send messages in different threads + tasks does not matter.
        tokio::spawn(async {
            assert!(setup_and_send_iot_service_message().await.is_ok());
        });
        tokio::task::spawn_blocking(move || {
            assert!(setup_and_try_send_iot_message().is_ok());
        });
        std::thread::spawn(move || {
            assert!(setup_and_send_iot_blocking_message().is_ok());
        });

        let mut count = 0;
        //Collect all messages, sometimes channels return None, so need to account for this.
        while let Some(_result) = iot_rx.recv().await {
            count += 1;
            if count == buffer_size {
                break;
            }
        }
        assert_eq!(count, buffer_size);
    }

    fn test_try_send_iot_messages_full_buffer(buffer_size: usize) {
        // Test that send fails due to full buffer.
        for _ in 0..buffer_size {
            let _ = setup_and_try_send_iot_message();
        }
        assert_eq!(setup_and_try_send_iot_message(), Err(ChannelUtilError::BufferFullError));
    }

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
