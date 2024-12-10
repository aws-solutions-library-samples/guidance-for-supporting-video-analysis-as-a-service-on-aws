use crate::client::topics::TopicHelper;
use crate::constants::PROVISION_SHADOW_NAME;
use device_traits::connections::{PubSubClient, PubSubMessage};
use mqtt_client::builder::MQTTMessageBuilder;
use serde_json::{json, Value};
use tracing::{debug, info, instrument, warn};

///Helper class to aid in device state management.
#[derive(Debug)]
pub struct RegistrationHelper<'a> {
    mqtt_client: &'a mut Box<dyn PubSubClient + Send + Sync>,
    topic_helper: TopicHelper,
}

impl RegistrationHelper<'_> {
    /// Constructor for Registration Helper
    pub fn new(
        mqtt_client: &mut Box<dyn PubSubClient + Send + Sync>,
        client_id: String,
    ) -> RegistrationHelper {
        // create topic helper in constructor to ensure correct values added.
        let topic_helper = TopicHelper::new(client_id, Some(PROVISION_SHADOW_NAME.to_owned()));
        RegistrationHelper { mqtt_client, topic_helper }
    }

    pub fn received_expected_shadow_message(
        message: &(dyn PubSubMessage + Send + Sync),
        expected_topic: String,
        required_field: &str,
    ) -> bool {
        if !message.get_topic().contains(expected_topic.as_str()) {
            return false;
        }
        let v: Value = serde_json::from_str(message.get_payload()).unwrap_or(Value::default());
        // Check to see if the valid json message has "state"->"desired"->"required_field" document.
        if let Some(state) = v.get("state") {
            let field_exists = state.get(required_field);
            // Message not received if field does not exist.
            let message = match field_exists {
                None => return false,
                Some(message) => message,
            };
            //Field should be set to true by cloud. If false or does not exist return false.
            return message.as_bool().unwrap_or(false);
        }
        false
    }

    /// Publish device info
    #[instrument]
    pub async fn publish_device_info(&mut self, device_info: Value) -> anyhow::Result<()> {
        self.update_provision_shadow(&device_info).await?;
        info!("Published device info to provision shadow");

        Ok(())
    }

    #[instrument]
    pub(crate) async fn update_provision_shadow(&mut self, message: &Value) -> anyhow::Result<()> {
        // Update the shadow.
        let shadow_accepted_topic = self.topic_helper.get_shadow_update_accepted();
        let shadow_rejected_topic = self.topic_helper.get_shadow_update_rejected();

        'outer: for i in 1..10 {
            self.mqtt_client
                .send(self.create_message_to_update_provision_shadow_reported_state(message))
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
                debug!("Received an unexpected message when updating provision shadow.");
            }
        }
        Ok(())
    }

    // Helper function to format messages for MQTT shadows.
    fn create_message_to_update_provision_shadow_reported_state(
        &self,
        message: &Value,
    ) -> Box<dyn PubSubMessage + Send + Sync> {
        let mut message_builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        let topic = self.topic_helper.get_shadow_update_topic();
        message_builder.set_topic(topic.as_str());

        let payload = json!({"state":{"reported":message}});
        let payload = serde_json::to_string::<Value>(&payload)
            .expect("Issue formatting Json.  Invalid code change.");
        message_builder.set_payload(payload);
        message_builder.build_box_message()
    }
}
