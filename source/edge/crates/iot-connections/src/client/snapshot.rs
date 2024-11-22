use crate::constants::PRESIGNED_URL_FIELD;
use device_traits::connections::PubSubMessage;
use serde_json::Value;

///Helper class to aid in snapshots.  This is used in the client manager for snapshots.
#[derive(Debug)]
pub struct SnapshotHelper {}

impl SnapshotHelper {
    /// Helper function to check if message is expected
    pub fn received_expected_shadow_message(
        message: &(dyn PubSubMessage + Send + Sync),
        expected_topic: String,
    ) -> Option<String> {
        if !message.get_topic().contains(expected_topic.as_str()) {
            return None;
        }
        let v: Value = serde_json::from_str(message.get_payload()).unwrap_or(Value::default());
        // Check to see if the valid json message has "state"->"desired"->"required_field" document.
        // If document exists this is the message to the device to start livestream session.
        if let Some(state) = v.get("state") {
            let presigned_url = state.get(PRESIGNED_URL_FIELD);
            // Message not received if field does not exist.
            let message = match presigned_url {
                None => return None,
                Some(message) => message.as_str().expect("Unable to decode as string"),
            };
            return Some(message.to_string());
        }
        return None;
    }
}
