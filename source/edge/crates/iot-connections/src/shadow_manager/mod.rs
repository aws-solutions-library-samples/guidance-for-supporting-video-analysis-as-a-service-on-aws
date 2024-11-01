use crate::constants;
use anyhow::anyhow;
use async_trait::async_trait;
use device_traits::channel_utils::traits::IoTServiceSender;
use device_traits::{
    connections::{PubSubMessage, PubSubMessageBuilder, QoS, ShadowManager},
    merge::Merge,
};
use serde_json::{json, Value};
use std::path::PathBuf;
use tokio::fs::{File, OpenOptions};
use tokio::io::{AsyncReadExt, AsyncSeekExt, AsyncWriteExt, SeekFrom};
use tracing::{debug, instrument, warn};
// Comment out until merge from live succeeds.
// use json_value_merge::Merge;

// TODO:
// 1. Schema validation (Use dynamic json parsing for now.  This is not the final decision)
// 2. Pass messages to the control logic.
// Links to AWS IoT Shadow SDK for guidance : https://github.com/aws/aws-iot-device-sdk-python-v2/blob/main/awsiot/iotshadow.py

/// Shadow manager struct this struct implements the IotShadowManager trait
/// and is responsible for syncing the local shadow on the edge with the shadow
/// in the cloud.
#[derive(Debug)]
pub struct IotShadowManager {
    // Name of IoT Thing the shadow is for, used to generate topics.
    pub(crate) thing_name: String,
    // Name of the shadow, optional as classic shadows do not have a name.
    // If no name is provided this will assume a classic shadow.
    pub(crate) shadow_name: Option<String>,
    // Quality of service of MQTT messages + subscriptions.
    pub(crate) quality_of_service: QoS,
    // Used to send messages to be published by MQTT client.
    pub(crate) iot_channel: Box<dyn IoTServiceSender + Send + Sync>,
    // Used to hold local shadow document.
    pub(crate) reported_state: Value,
    // Holds the desired state and is updated by cloud messages.
    pub(crate) desired_state: Value,
    // Holds message builder for sending messages to publish.
    pub_sub_message_builder: Box<dyn PubSubMessageBuilder + Send + Sync>,
    //TODO: Client tokens help associate a request with a response in the cloud
    //pub(crate) client_token
    //TODO: Versioning
    //pub(crate) version:u64
    /// Path to configuration directory, state information is saved here.
    dir_path: PathBuf,
    /// Hold async file where shadow's desired state is stored.
    /// Shadows are limited to 30kB so this file will always be small.
    local_storage: Option<File>,
}

#[async_trait]
impl ShadowManager for IotShadowManager {
    /// Update the local reported state.  This will update the shadow with the local shadow in the Filesystem and pass
    /// message to the mpsc channel which will pass the message to the pub_sub client in the connections layer.
    /// This will only update the shadow's reported state as the desired state should be updated by edge process
    /// https://docs.aws.amazon.com/iot/latest/developerguide/device-shadow-document.html
    #[instrument]
    async fn update_reported_state(&mut self, update_doc: Value) -> anyhow::Result<()> {
        // TODO: Add json validation from a schema.  https://crates.io/crates/jsonschema
        // Return an error if an invalid doc is entered

        // TODO: Fix merge trait. Commented due to issues with handling arrays for VEC options
        // self.reported_state.merge(&update_doc);
        // debug!("New reported state added to shadow: {}", self.reported_state);
        //TODO: Generate MQTT message + send to channel
        let pub_sub_message = self.build_message_for_shadow_update(update_doc)?;

        self.iot_channel.send_iot_message(pub_sub_message).await?;
        Ok(())
    }

    /// This function receives messages from the cloud and updates the local.
    /// This method saves the information to the file-system so the device can
    /// restore settings even when restarted and disconnected from the cloud.
    #[instrument]
    async fn update_desired_state(&mut self, update_doc: Value) -> anyhow::Result<()> {
        //TODO: Add schema validation
        self.desired_state.merge(&update_doc);
        debug!("New desired state added to shadow: {}", self.desired_state);

        //TODO: Call or send messages to control components.

        // Writing to the filesystem is not a critical error.  Log and continue.
        if let Err(e) = self.write_to_local().await {
            // TODO: Consider other recovery steps here.  (Eg delete + reopen file)
            warn!("Could not write shadow's desired state to file : {:?}", e);
        }
        Ok(())
    }

    /// Get the list of topics the MQTT client must subscribe to receive shadow messages from the cloud.
    fn get_shadow_topics(&self) -> Vec<(String, QoS)> {
        let mut output = Vec::new();
        let prefix = self.get_shadow_topic_prefix();

        for topic_suffix in constants::SHADOW_TOPICS_TO_SUBSCRIBE {
            output.push((format!("{prefix}/{topic_suffix}"), self.quality_of_service))
        }
        output
    }

    /// Enable the storage of the desired state in the filesystem.  If device restarts while
    /// not connected to the cloud this is used to recover normal operations.
    async fn enable_storage(&mut self) -> anyhow::Result<()> {
        // Open or create a file, this will return an error if permissions are
        // not set correctly by customer.  Currently overwrites old values.
        let file = OpenOptions::new()
            .read(true)
            .write(true)
            .create(true)
            .open(self.get_storage_file_path())
            .await?;
        // Set file within struct, will automatically close when ShadowManager is dropped.
        self.local_storage = Some(file);

        // Restore desired state from local storage.
        self.restore_desired_from_local().await?;

        Ok(())
    }
}

impl IotShadowManager {
    /// Create a new shadow manager
    pub fn new(
        thing_name: &str,
        shadow_name: Option<String>,
        iot_channel: Box<dyn IoTServiceSender + Send + Sync>,
        pub_sub_message_builder: Box<dyn PubSubMessageBuilder + Send + Sync>,
        dir_path: PathBuf,
    ) -> IotShadowManager {
        // QoS is set to AtLeastOnce by the constant as we want at least this level of message quality.
        IotShadowManager {
            thing_name: thing_name.to_string(),
            shadow_name,
            quality_of_service: constants::QUALITY_OF_SERVICE,
            iot_channel,
            reported_state: Value::default(),
            desired_state: Value::default(),
            pub_sub_message_builder,
            dir_path,
            local_storage: None,
        }
    }
    /// Create a new shadow manager as the IoTShadowManager Trait
    pub fn new_shadow_manager(
        thing_name: &str,
        shadow_name: Option<String>,
        iot_channel: Box<dyn IoTServiceSender + Send + Sync>,
        pub_sub_message_builder: Box<dyn PubSubMessageBuilder + Send + Sync>,
        dir_path: PathBuf,
    ) -> Box<dyn ShadowManager + Send + Sync> {
        Box::new(IotShadowManager::new(
            thing_name,
            shadow_name,
            iot_channel,
            pub_sub_message_builder,
            dir_path,
        ))
    }
    /// Generate topic prefix for shadow related topics
    fn get_shadow_topic_prefix(&self) -> String {
        let mut topic_prefix = format!(r"$aws/things/{}/shadow", self.thing_name);
        if let Some(name) = &self.shadow_name {
            topic_prefix.push_str(format!("/name/{name}").as_str());
        }
        topic_prefix
    }
    /// Generate shadow update json structure.
    fn build_message_for_shadow_update(
        &mut self,
        update: serde_json::Value,
    ) -> anyhow::Result<Box<dyn PubSubMessage + Send + Sync>> {
        let topic =
            format!("{}/{}", self.get_shadow_topic_prefix(), constants::UPDATE_SHADOW_TOPIC_SUFFIX);

        //TODO: Add more flushed out JSON handling methods + checks
        let mut update_shadow_payload = json!({"state":{"reported":{}}});
        update_shadow_payload["state"]["reported"] = update;
        let payload: String = update_shadow_payload.to_string();

        //Create a message.  Will be passed through a channel to be published by connections layer.
        Ok(self.build_pub_sub_message(&topic, payload))
    }
    /// Helper function to build pub_sub_messages for struct.
    /// Retain + quality of service should be fixed.
    fn build_pub_sub_message(
        &mut self,
        topic: &str,
        payload: String,
    ) -> Box<dyn PubSubMessage + Send + Sync> {
        self.pub_sub_message_builder.set_topic(topic);
        self.pub_sub_message_builder.set_retain(constants::RETAIN);
        self.pub_sub_message_builder.set_qos(constants::QUALITY_OF_SERVICE);
        self.pub_sub_message_builder.set_payload(payload);
        self.pub_sub_message_builder.build_box_message()
    }

    // Helper function to get full path to the local file associated with the shadow.
    fn get_storage_file_path(&mut self) -> PathBuf {
        let shadow_file_name = format!(
            "{}_{}.json",
            self.thing_name,
            self.shadow_name.to_owned().unwrap_or("default".to_owned())
        );
        self.dir_path.join(shadow_file_name)
    }
    // Sync with the local filesystem if enabled.
    async fn write_to_local(&mut self) -> anyhow::Result<()> {
        // if storage is enabled this will write the de
        if let Some(storage_file) = self.local_storage.as_mut() {
            Self::clear_local(storage_file).await?;
            storage_file.write_all(self.desired_state.to_string().as_bytes()).await?;
            storage_file.sync_all().await?;
        }
        Ok(())
    }
    //Clear contents of an open file.  This is a fast operation, File size set to zero and cursor for
    //the open file is set to the beginning.
    async fn clear_local(file: &mut File) -> anyhow::Result<()> {
        file.set_len(0).await?;
        file.seek(SeekFrom::Start(0)).await?;
        Ok(())
    }
    // Restore desired state from local storage if enabled.
    async fn restore_desired_from_local(&mut self) -> anyhow::Result<()> {
        if let Some(storage_file) = self.local_storage.as_mut() {
            Self::storage_file_invalid(storage_file).await?;
            let mut buffer = String::new();
            storage_file.read_to_string(&mut buffer).await?;
            //TODO: Add schema validation check here before restoring JSON object to shadow manager
            self.desired_state = serde_json::from_str(&buffer)?;
        }
        Ok(())
    }
    // Return an error if desired file is too large,
    // if so we do not want to load it into system memory.
    async fn storage_file_invalid(file: &mut File) -> anyhow::Result<()> {
        if file.metadata().await?.len() > constants::MAX_FILE_SIZE_BYTES {
            return Err(anyhow!("Shadow's storage file is too large to load into system memory."));
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {

    use super::*;
    use device_traits::channel_utils::traits::MockIoTServiceSender;
    use device_traits::connections::ShadowManager;
    use mqtt_client::builder::MQTTMessageBuilder;

    const THING_NAME: &str = "ThingName";
    const SHADOW_NAME: &str = "ShadowName";
    const BUFFER_LEN: usize = 10;
    const CLASSIC_SHADOW_TOPIC_PREFIX: &str = r"$aws/things/ThingName/shadow";
    const NAMED_SHADOW_TOPIC_PREFIX: &str = r"$aws/things/ThingName/shadow/name/ShadowName";

    const GET_ACCEPTED_TOPIC_CLASSIC: &str = r"$aws/things/ThingName/shadow/get/accepted";
    const GET_REJECTED_TOPIC_CLASSIC: &str = r"$aws/things/ThingName/shadow/get/rejected";
    const UPDATE_DELTA_CLASSIC: &str = r"$aws/things/ThingName/shadow/update/delta";
    const UPDATE_ACCEPTED_CLASSIC: &str = r"$aws/things/ThingName/shadow/update/accepted";
    const UPDATE_REJECTED_CLASSIC: &str = r"$aws/things/ThingName/shadow/update/rejected";

    const GET_ACCEPTED_TOPIC_NAMED: &str =
        r"$aws/things/ThingName/shadow/name/ShadowName/get/accepted";
    const GET_REJECTED_TOPIC_NAMED: &str =
        r"$aws/things/ThingName/shadow/name/ShadowName/get/rejected";
    const UPDATE_DELTA_NAMED: &str = r"$aws/things/ThingName/shadow/name/ShadowName/update/delta";
    const UPDATE_ACCEPTED_NAMED: &str =
        r"$aws/things/ThingName/shadow/name/ShadowName/update/accepted";
    const UPDATE_REJECTED_NAMED: &str =
        r"$aws/things/ThingName/shadow/name/ShadowName/update/rejected";
    const UPDATE_NAMED: &str = r"$aws/things/ThingName/shadow/name/ShadowName/update";

    /// Test constructor for shadow manager trait, create a named + unnamed shadow
    #[tokio::test]
    async fn new_shadow_manager() {
        let _classic_shadow: Box<dyn ShadowManager> =
            get_boxed_shadow_manager(THING_NAME, None, None);
        let _named_shadow: Box<dyn ShadowManager> =
            get_boxed_shadow_manager(THING_NAME, None, None);
    }
    /// Test constructor for IoT shadow manager struct
    #[tokio::test]
    async fn new_iot_shadow_manager() {
        let classic_shadow = get_iot_shadow_manager(THING_NAME, None, None);
        assert_eq!(classic_shadow.shadow_name, None);

        let named_shadow = get_iot_shadow_manager(THING_NAME, Some(SHADOW_NAME.to_owned()), None);
        assert_eq!(named_shadow.shadow_name, Some(SHADOW_NAME.to_string()));
    }
    ///Confirm the shadow manager is creating the correct topic prefixes.
    #[tokio::test]
    async fn get_shadow_topic_prefix_named_shadow() {
        let named_shadow = get_iot_shadow_manager(THING_NAME, Some(SHADOW_NAME.to_owned()), None);

        assert_eq!(named_shadow.get_shadow_topic_prefix(), NAMED_SHADOW_TOPIC_PREFIX);
    }
    ///Confirm the shadow manager is creating the correct topic prefixes.
    #[tokio::test]
    async fn get_shadow_topic_prefix_classic_shadow() {
        let classic_shadow = get_iot_shadow_manager(THING_NAME, None, None);

        assert_eq!(classic_shadow.get_shadow_topic_prefix(), CLASSIC_SHADOW_TOPIC_PREFIX);
    }
    /// Test the generation of the list of topics the mqtt client must subscribe to utilize shadows.
    #[tokio::test]
    async fn get_shadow_topics_classic_shadow_test() {
        let classic_shadow = get_boxed_shadow_manager(THING_NAME, None, None);
        let sub_list = classic_shadow.get_shadow_topics();
        topic_list_helper(
            sub_list,
            GET_ACCEPTED_TOPIC_CLASSIC.to_owned(),
            GET_REJECTED_TOPIC_CLASSIC.to_owned(),
            UPDATE_DELTA_CLASSIC.to_owned(),
            UPDATE_ACCEPTED_CLASSIC.to_owned(),
            UPDATE_REJECTED_CLASSIC.to_owned(),
        )
    }
    /// Test the generation of the list of topics the mqtt client must subscribe to utilize shadows.
    #[tokio::test]
    async fn get_shadow_topics_named_shadow_test() {
        let named_shadow = get_boxed_shadow_manager(THING_NAME, Some(SHADOW_NAME.to_owned()), None);
        let sub_list = named_shadow.get_shadow_topics();
        topic_list_helper(
            sub_list,
            GET_ACCEPTED_TOPIC_NAMED.to_owned(),
            GET_REJECTED_TOPIC_NAMED.to_owned(),
            UPDATE_DELTA_NAMED.to_owned(),
            UPDATE_ACCEPTED_NAMED.to_owned(),
            UPDATE_REJECTED_NAMED.to_owned(),
        )
    }
    ///Test message format of shadow update helper function.
    #[tokio::test]
    async fn build_message_for_shadow_update() {
        let mut named_shadow =
            get_iot_shadow_manager(THING_NAME, Some(SHADOW_NAME.to_owned()), None);
        let input_value = json!({"status":"enabled","connected":true});
        let correct_output =
            json!({"state":{"reported":{"status":"enabled","connected":true}}});
        named_shadow.reported_state = input_value.clone();

        let message = named_shadow.build_message_for_shadow_update(input_value).unwrap();
        assert_eq!(message.get_topic(), UPDATE_NAMED);
        assert_eq!(message.get_qos(), constants::QUALITY_OF_SERVICE);
        assert_eq!(message.get_retain(), constants::RETAIN);
        assert_eq!(message.get_payload(), &correct_output.to_string());
    }

    /// Test that shadow updates the local state by merging in the doc.
    #[tokio::test]
    async fn update_desired_state_test_simple() {
        let mut named_shadow =
            get_iot_shadow_manager(THING_NAME, Some(SHADOW_NAME.to_owned()), None);

        named_shadow.desired_state = json!({"status":"enabled"});

        let input_value = json!({"connected":true});
        let correct_after_merge = json!({"status":"enabled","connected":true});
        let result = named_shadow.update_desired_state(input_value).await;
        assert!(result.is_ok());
        assert_eq!(named_shadow.desired_state.to_string(), correct_after_merge.to_string());

        let input_value = json!({"status":"enabled"});
        let correct_after_merge = json!({"status":"enabled","connected":true});

        let result = named_shadow.update_desired_state(input_value).await;
        assert!(result.is_ok());
        assert_eq!(named_shadow.desired_state.to_string(), correct_after_merge.to_string());
    }
    /// Test that shadow updates the local state by merging in the doc and send message over the channel
    #[tokio::test]
    async fn update_reported_state_test() {
        // Update and make sure the message is sent on the channel
        let correct_payload =
            json!({"state":{"reported":{"status":"enabled","connected":"true"}}});
        let mut mock_sender = Box::new(MockIoTServiceSender::new());
        let _ = mock_sender
            .expect_send_iot_message()
            .withf(move |x| x.get_payload().to_string().eq(&correct_payload.clone().to_string()))
            .returning(|_| Ok(()));

        let mut named_shadow =
            get_iot_shadow_manager(THING_NAME, Some(SHADOW_NAME.to_owned()), Some(mock_sender));

        let new_device_state = json!({"status":"enabled","connected":"true"});
        named_shadow.update_reported_state(new_device_state.clone()).await.unwrap();
    }

    //Simple helper to ensure list has all required topics to subscribe too.
    fn topic_list_helper(
        topic_list: Vec<(String, QoS)>,
        get_accepted_topic: String,
        get_rejected_topic: String,
        update_delta_topic: String,
        update_accepted_topic: String,
        update_rejected_topic: String,
    ) {
        assert_eq!(topic_list.len(), 5);
        assert!(topic_list.contains(&(get_accepted_topic, constants::QUALITY_OF_SERVICE)));
        assert!(topic_list.contains(&(get_rejected_topic, constants::QUALITY_OF_SERVICE)));
        assert!(topic_list.contains(&(update_delta_topic, constants::QUALITY_OF_SERVICE)));
        assert!(topic_list.contains(&(update_accepted_topic, constants::QUALITY_OF_SERVICE)));
        assert!(topic_list.contains(&(update_rejected_topic, constants::QUALITY_OF_SERVICE)));
    }
    /// Helper function to create IoTShadowManagers for tests
    fn get_iot_shadow_manager(
        thing_name: &str,
        shadow_name: Option<String>,
        mock_sender: Option<Box<MockIoTServiceSender>>,
    ) -> IotShadowManager {
        let builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        IotShadowManager::new(
            thing_name,
            shadow_name,
            mock_sender.unwrap_or(Box::default()),
            builder,
            PathBuf::default(),
        )
    }
    /// Helper function to create ShadowManager trait
    fn get_boxed_shadow_manager(
        thing_name: &str,
        shadow_name: Option<String>,
        mock_sender: Option<Box<MockIoTServiceSender>>,
    ) -> Box<dyn ShadowManager> {
        let builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        IotShadowManager::new_shadow_manager(
            thing_name,
            shadow_name,
            mock_sender.unwrap_or(Box::default()),
            builder,
            PathBuf::default(),
        )
    }
}