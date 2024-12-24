use crate::constants::DESTINATION_FOR_TIMELINE_CLOUD;
#[cfg(feature = "sd-card-catchup")]
use crate::constants::DESTINATION_FOR_TIMELINE_DEVICE;
use device_traits::channel_utils::error::ChannelUtilError;
use device_traits::channel_utils::{traits::IoTServiceSender, ServiceCommunicationManager};
use serde_json::json;
#[cfg(feature = "sd-card-catchup")]
use serde_json::Value;
use std::time::{SystemTime, UNIX_EPOCH};
#[cfg(feature = "sd-card-catchup")]
use tracing::error;

// Types used for readability
pub(crate) type FragmentTimeInNs = u64;
pub(crate) type FragmentDurationInNs = u64;

/// Get current time - in nanoseconds since linux epoch:
/// https://github.com/awslabs/amazon-kinesis-video-streams-pic/blob/532178bbd4d2e6e6511fa8ffa62a15dba58c02f0/src/utils/src/Time.c#LL47C5-L47C66
/// This method will break when ns since epoch exceeds 18_446_744_073_709_551_615 in 531 years
pub fn get_current_time_since_unix_epoch_ns() -> u64 {
    let current_time = SystemTime::now();
    let since_the_epoch = current_time.duration_since(UNIX_EPOCH).expect("Time went backwards");
    since_the_epoch.as_nanos() as u64
}

/// Convert ns to ms with integer division.  (What KVS Gstreamer plugin does.)
pub(crate) fn convert_ns_to_ms(time_ns: u64) -> u64 {
    time_ns / 1_000_000_u64
}

#[derive(Debug)]
pub(crate) struct IoTMessageUtil {
    /// Used to send MQTT messages to cloud.
    pub(crate) iot_message_sender: Box<dyn IoTServiceSender + Send + Sync>,
    timeline_topic: Option<String>,
}

impl IoTMessageUtil {
    pub(crate) fn new() -> Self {
        IoTMessageUtil {
            iot_message_sender: Box::<ServiceCommunicationManager>::default(),
            timeline_topic: None,
        }
    }
    pub(crate) fn try_send_timeline_cloud(
        &mut self,
        fragment_time_stamp_in_ns: FragmentTimeInNs,
        fragment_duration_in_ns: FragmentDurationInNs,
    ) -> Result<(), ChannelUtilError> {
        //Format topic and payload.
        let topic = self.get_timeline_topic()?;
        let fragment_time_stamp_in_ms = convert_ns_to_ms(fragment_time_stamp_in_ns);
        let fragment_duration_in_ms = convert_ns_to_ms(fragment_duration_in_ns);
        let payload = Self::get_timeline_generation_payload_for_cloud(
            fragment_time_stamp_in_ms,
            fragment_duration_in_ms,
        );

        self.iot_message_sender.try_build_and_send_iot_message(&topic, payload)
    }

    /// Format + try and send message to IoT with a batch of fragments which are stored on the device.
    #[cfg(feature = "sd-card-catchup")]
    pub(crate) fn try_send_timeline_device(
        &mut self,
        metadata_of_timestamps: Vec<(FragmentTimeInNs, FragmentDurationInNs)>,
    ) -> Result<(), ChannelUtilError> {
        //Format topic and payload.
        let topic = self.get_timeline_topic()?;

        let payload = Self::get_timeline_generation_payload_for_device(metadata_of_timestamps);

        // We limit the size of the vector to be well under this quota.  This check is used to identify future bugs.
        if payload.len() > 128000 {
            error!("Payload too large, will not publish to IoT.")
        }

        self.iot_message_sender.try_build_and_send_iot_message(&topic, payload)
    }

    // Format of payload for timeline in cloud.
    fn get_timeline_generation_payload_for_cloud(
        fragment_time_stamp: u64,
        fragment_duration: u64,
    ) -> String {
        let fragments =
            json!([{"timestamp": fragment_time_stamp, "duration": fragment_duration}]).to_string();
        json!({"location":DESTINATION_FOR_TIMELINE_CLOUD,"timestamps":fragments}).to_string()
    }

    /// Get payload for IoT Message.  Message should not be above 128kB IoT quota.
    /// https://docs.aws.amazon.com/general/latest/gr/iot-core.html#limits_iot
    #[cfg(feature = "sd-card-catchup")]
    fn get_timeline_generation_payload_for_device(
        metadata_of_timestamps: Vec<(FragmentTimeInNs, FragmentDurationInNs)>,
    ) -> String {
        // Create String
        let new_vec: Vec<Value> = Vec::new();
        let mut json_array = Value::Array(new_vec);

        // Format array of timestamps + durations for message to IoT.
        for (time_stamp_in_ns, duration_in_ns) in metadata_of_timestamps.iter() {
            // Get a mut reference to the json array.
            if let Some(json_array) = json_array.as_array_mut() {
                // MS convert
                let time_in_ms = convert_ns_to_ms(*time_stamp_in_ns);
                let duration_in_ms = convert_ns_to_ms(*duration_in_ns);
                json_array.push(json!({"timestamp": time_in_ms, "duration": duration_in_ms}))
            }
        }
        json!({"location":DESTINATION_FOR_TIMELINE_DEVICE,"timestamps":json_array.to_string()})
            .to_string()
    }

    fn get_timeline_topic(&mut self) -> Result<String, ChannelUtilError> {
        // Only runs the first time a message is sent.
        if self.timeline_topic.is_none() {
            let client_id = self.iot_message_sender.get_client_id()?;
            self.timeline_topic = Some(format!("videoanalytics/{}/timeline", client_id));
        }

        Ok(self.timeline_topic.to_owned().expect("This must be valid."))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use device_traits::channel_utils::traits::MockIoTServiceSender;
    const CLIENT_ID: &str = "test_client_id";
    const ALT_CLIENT_ID: &str = "alt_client_id";
    const DURATION_IN_NS: u64 = 1_000_000_000;
    const DURATION_IN_MS: u64 = 1000;
    const UNIX_EPOCH_IN_NS: u64 = 1703804521234567891;
    const UNIX_EPOCH_IN_MS: u64 = 1703804521234;

    /// Tests to confirm topic initialized correctly.
    #[test]
    fn test_timeline_topic_first_time() {
        let mut mock_sender = Box::<MockIoTServiceSender>::default();
        mock_sender.expect_get_client_id().returning(|| Ok(CLIENT_ID.to_string()));
        let mut manager = IoTMessageUtil { iot_message_sender: mock_sender, timeline_topic: None };
        let topic = manager.get_timeline_topic().unwrap();
        assert_eq!(format!("videoanalytics/{}/timeline", CLIENT_ID), topic);
    }

    /// Tests to confirm topic is reused properly after initialization
    #[test]
    fn test_timeline_topic_second_time() {
        let mut manager = IoTMessageUtil {
            iot_message_sender: Box::<MockIoTServiceSender>::default(),
            timeline_topic: Some(format!("videoanalytics/{}/timeline", ALT_CLIENT_ID)),
        };
        let topic = manager.get_timeline_topic().unwrap();
        assert_eq!(format!("videoanalytics/{}/timeline", ALT_CLIENT_ID), topic);
    }

    #[test]
    fn test_try_send_timeline_cloud() {
        let mut mock_sender = Box::<MockIoTServiceSender>::default();

        mock_sender.expect_get_client_id().returning(|| Ok(CLIENT_ID.to_string()));
        mock_sender.expect_try_build_and_send_iot_message().returning(|topic, payload| {
            let fragments =
                json!([{"timestamp": UNIX_EPOCH_IN_MS, "duration": DURATION_IN_MS}]).to_string();
            let expected_payload =
                json!({"location":DESTINATION_FOR_TIMELINE_CLOUD,"timestamps":fragments})
                    .to_string();
            assert_eq!(topic, format!("videoanalytics/{}/timeline", CLIENT_ID));
            assert_eq!(payload, expected_payload);
            Ok(())
        });
        let mut manager = IoTMessageUtil { iot_message_sender: mock_sender, timeline_topic: None };
        assert!(manager.try_send_timeline_cloud(UNIX_EPOCH_IN_NS, DURATION_IN_NS).is_ok());
    }

    #[cfg(feature = "sd-card-catchup")]
    #[test]
    fn test_try_send_timeline_device() {
        let mut mock_sender = Box::<MockIoTServiceSender>::default();

        let test_metadata = vec![
            (UNIX_EPOCH_IN_NS, DURATION_IN_NS),
            (UNIX_EPOCH_IN_NS, DURATION_IN_NS),
            (UNIX_EPOCH_IN_NS, DURATION_IN_NS),
        ];

        mock_sender.expect_get_client_id().returning(|| Ok(CLIENT_ID.to_string()));
        mock_sender.expect_try_build_and_send_iot_message().returning(|topic, payload| {
            let fragments = json!([{"timestamp": UNIX_EPOCH_IN_MS, "duration": DURATION_IN_MS},
                {"timestamp": UNIX_EPOCH_IN_MS, "duration": DURATION_IN_MS},
                {"timestamp": UNIX_EPOCH_IN_MS, "duration": DURATION_IN_MS}])
            .to_string();

            let expected_payload =
                json!({"location": DESTINATION_FOR_TIMELINE_DEVICE,"timestamps":fragments})
                    .to_string();

            assert_eq!(topic, format!("videoanalytics/{}/timeline", CLIENT_ID));
            assert_eq!(payload, expected_payload);
            Ok(())
        });
        let mut manager = IoTMessageUtil { iot_message_sender: mock_sender, timeline_topic: None };
        assert!(manager.try_send_timeline_device(test_metadata).is_ok());
    }

    #[test]
    fn test_int_division() {
        assert_eq!(convert_ns_to_ms(UNIX_EPOCH_IN_NS), UNIX_EPOCH_IN_MS);
    }
}
