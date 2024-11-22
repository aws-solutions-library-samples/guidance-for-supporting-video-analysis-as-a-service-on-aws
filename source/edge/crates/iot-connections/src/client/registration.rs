use crate::client::topics::TopicHelper;
use crate::constants::PROVISION_SHADOW_NAME;
use device_traits::connections::{PubSubClient, PubSubMessage};
use serde_json::Value;
use std::path::PathBuf;

///Helper class to aid in device state management.
#[derive(Debug)]
pub struct RegistrationHelper {
    factory_mqtt_client: Box<dyn PubSubClient + Send + Sync>,
    dir_path: PathBuf,
    topic_helper: TopicHelper,
}

impl RegistrationHelper {
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
}
