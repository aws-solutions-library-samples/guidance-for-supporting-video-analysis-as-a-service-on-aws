//! This module contains helper structs for device configuration
use crate::client::topics::TopicHelper;
use crate::constants::STATE_SHADOW_FIELD;
use device_traits::connections::PubSubClient;
use device_traits::connections::PubSubMessage;
use serde_json::Value;


///Helper class to aid in device configuration.
#[derive(Debug)]
pub struct ConfigurationHelper<'a> {
    mqtt_client: &'a mut Box<dyn PubSubClient + Send + Sync>,
    topic_helper: TopicHelper,
}

impl ConfigurationHelper<'_> {
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