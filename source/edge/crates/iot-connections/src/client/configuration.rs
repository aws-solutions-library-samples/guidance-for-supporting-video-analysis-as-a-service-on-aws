//! This module contains helper structs for device configuration
use crate::client::topics::TopicHelper;
use crate::constants::{REPORTED_SHADOW_FIELD, STATE_SHADOW_FIELD};
use device_traits::connections::PubSubClient;
use device_traits::connections::PubSubMessage;
use mqtt_client::builder::MQTTMessageBuilder;
use serde_json::{json, Value};
use tracing::{debug, info, instrument, warn};

///Helper class to aid in device configuration.
#[derive(Debug)]
pub struct ConfigurationHelper<'a> {
    mqtt_client: &'a mut Box<dyn PubSubClient + Send + Sync>,
    topic_helper: TopicHelper,
}

impl ConfigurationHelper<'_> {
    /// Constructor for Configuration Helper
    pub fn new(
        mqtt_client: &mut Box<dyn PubSubClient + Send + Sync>,
        client_id: String,
        shadow_name: String,
    ) -> ConfigurationHelper {
        // create topic helper in constructor to ensure correct values added.
        let topic_helper = TopicHelper::new(client_id, Some(shadow_name));
        ConfigurationHelper { mqtt_client, topic_helper }
    }

    /// Function to publish reported settings
    #[instrument]
    pub async fn publish_reported_settings(
        &mut self,
        reported_settings: Value,
    ) -> anyhow::Result<()> {
        self.update_shadow(&reported_settings).await?;
        info!("Published reported settings to {} shadow.", self.topic_helper.get_shadow_name());

        Ok(())
    }

    /// Helper function to update shadow
    #[instrument]
    pub(crate) async fn update_shadow(&mut self, message: &Value) -> anyhow::Result<()> {
        //Update the shadow.
        let shadow_accepted_topic = self.topic_helper.get_shadow_update_accepted();
        let shadow_rejected_topic = self.topic_helper.get_shadow_update_rejected();

        'outer: for i in 1..10 {
            self.mqtt_client
                .send(self.create_message_to_update_shadow_reported_state(message))
                .await?;

            'inner: loop {
                let iot_message = self.mqtt_client.recv().await?;
                if iot_message.get_topic().eq(&shadow_rejected_topic) {
                    warn!("Failed to update IoT shadow, change rejected on attempt : {}", i);
                    break 'inner;
                }
                if iot_message.get_topic().eq(&shadow_accepted_topic) {
                    info!("Successfully updated shadow.");
                    break 'outer;
                }
                debug!("Received an unexpected message when updating configuration shadow.");
            }
        }
        Ok(())
    }

    /// Helper function to format messages for MQTT shadows.
    fn create_message_to_update_shadow_reported_state(
        &self,
        message: &Value,
    ) -> Box<dyn PubSubMessage + Send + Sync> {
        let topic = self.topic_helper.get_shadow_update_topic();
        let mut message_builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        message_builder.set_topic(topic.as_str());

        let payload = json!({ STATE_SHADOW_FIELD: { REPORTED_SHADOW_FIELD: message } });
        let payload = serde_json::to_string::<Value>(&payload)
            .expect("Issue formatting Json.  Invalid code change.");
        message_builder.set_payload(payload);
        message_builder.build_box_message()
    }

    /// Helper function to check if message is expected
    pub fn received_expected_shadow_message(
        message: &(dyn PubSubMessage + Send + Sync),
        expected_topic: String,
        required_field: &str,
    ) -> Option<Value> {
        if !message.get_topic().contains(expected_topic.as_str()) {
            return None;
        }
        let v: Value = serde_json::from_str(message.get_payload()).unwrap_or(Value::default());
        // Check to see if the valid json message has "state"->"desired"->"required_field" document.
        // If document exists this is the desired settings
        if let Some(state) = v.get(STATE_SHADOW_FIELD) {
            let settings = state.get(required_field);
            // Message not received if field does not exist.
            let message = match settings {
                None => return None,
                Some(message) => message.clone(),
            };
            //Field should be set to true by cloud. If false or does not exist return false.
            return Some(message);
        }
        None
    }
}
