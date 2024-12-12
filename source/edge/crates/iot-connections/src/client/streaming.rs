//! This module contains helper structs for streaming
use device_traits::connections::PubSubMessage;
use serde_json::Value;

///Helper class to aid in streaming.  This is used in the client manager for streaming.
#[derive(Debug)]
pub struct StreamingHelper {}

impl StreamingHelper {
    /// Helper function to check if message is expected
    pub fn received_expected_shadow_message(
        message: &(dyn PubSubMessage + Send + Sync),
        expected_topic: String,
        required_field: &str,
    ) -> Option<String> {
        if !message.get_topic().contains(expected_topic.as_str()) {
            return None;
        }
        let v: Value = serde_json::from_str(message.get_payload()).unwrap_or(Value::default());

        // Check to see if the valid json message has "state"->"desired"->"required_field" document.
        // If document exists this is the message to the device to start livestream session.
        if let Some(state) = v.get("state") {
            let streaming_peer_connections =
                state.get(required_field).expect("Unable to retrieve streaming field");
            if let Some(obj) = streaming_peer_connections.as_object() {
                let results = Vec::from_iter(obj.keys());
                if results.len() > 0 {
                    return Some(obj[results[0]].to_string());
                }
            }
        }
        return None;
    }
}
