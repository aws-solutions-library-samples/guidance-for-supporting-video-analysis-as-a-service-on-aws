use device_traits::connections::{PubSubMessage, QoS as AWSQoS};
use rumqttc::QoS;

/// Mqtt Message struct used by Async Client
/// AWSIoT Does not currently support QoS 2, it is checked by MqttClient
///
#[derive(Debug, Clone)]
pub struct MqttMessage {
    /// Topic the message was sent to or received from.
    pub(crate) topic: String,
    /// Quality of service QoS 0 : At most once , QoS 1 : At least Once, QoS 2 : Exactly Once
    pub(crate) qos: QoS,
    /// Payload, usually JSON
    pub(crate) payload: String,
    /// Inform broker to keep the last message
    pub(crate) retain: bool,
}

impl MqttMessage {
    /// Create new mqtt message.
    pub fn new(topic: String, qos: AWSQoS, payload: String, retain: bool) -> MqttMessage {
        let qos = Self::aws_qos_to_rumqttc_qos(qos);
        MqttMessage { topic, qos, payload, retain }
    }

    /// convert AWS quality of service enum to rumqttc's
    pub(crate) fn aws_qos_to_rumqttc_qos(qos: AWSQoS) -> QoS {
        match qos {
            AWSQoS::AtMostOnce => QoS::AtMostOnce,
            AWSQoS::AtLeastOnce => QoS::AtLeastOnce,
        }
    }
    /// convert rumqttc's qos enum to aws', panic if invalid qos input.
    pub(crate) fn rumqttc_qos_to_aws_qos(qos: QoS) -> AWSQoS {
        match qos {
            QoS::AtMostOnce => AWSQoS::AtMostOnce,
            QoS::AtLeastOnce => AWSQoS::AtLeastOnce,
            QoS::ExactlyOnce => panic!("AWS does not support QoSExactly Once"),
        }
    }
}

impl PubSubMessage for MqttMessage {
    /// Get a reference to the topic of the message.
    fn get_topic(&self) -> &str {
        &self.topic
    }

    /// Get a reference to the message payload
    fn get_payload(&self) -> &String {
        &self.payload
    }

    /// Get a reference to the quality of service
    fn get_qos(&self) -> AWSQoS {
        Self::rumqttc_qos_to_aws_qos(self.qos)
    }

    /// Get whether this message will be retained by the broker.
    fn get_retain(&self) -> bool {
        self.retain
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use device_traits::connections::QoS as AWSQoS;

    const TEST_TOPIC: &str = "Test Topic";
    const TEST_PAYLOAD: &str = "Test Payload";
    const QUALITY_OF_SERVICE: AWSQoS = AWSQoS::AtLeastOnce;
    const RUMQTTC_QUALITY_OF_SERVICE: rumqttc::QoS = rumqttc::QoS::AtLeastOnce;
    const RETAIN: bool = false;

    #[test]
    /// Tests the new function for MQTT messages.
    fn new_mqtt_message() {
        let message = get_test_message();

        assert_eq!(message.topic, TEST_TOPIC.to_string());
        assert_eq!(message.qos, RUMQTTC_QUALITY_OF_SERVICE);
        assert_eq!(message.payload, TEST_PAYLOAD.to_string());
        assert_eq!(message.retain, RETAIN);
    }

    #[test]
    /// Get a reference to the topic the message came from or will be sent on.
    fn pub_sub_message_get_topic() {
        let message = get_test_message();
        assert_eq!(message.get_topic(), TEST_TOPIC.to_string());
    }

    #[test]
    /// Get a reference to the payload.
    fn pub_sub_message_get_payload() {
        let message = get_test_message();
        assert_eq!(message.get_payload(), TEST_PAYLOAD);
    }

    #[test]
    /// Test that get qos works correctly and converts to enum which does not include ExactlyOnce since AWS IoT does not support this.
    fn pub_sub_message_get_qos() {
        let mut message = get_test_message();
        assert_eq!(message.get_qos(), AWSQoS::AtLeastOnce);
        message.qos = rumqttc::QoS::AtMostOnce;
        assert_eq!(message.get_qos(), AWSQoS::AtMostOnce);
    }

    #[test]
    #[should_panic]
    /// This test should panic as the QoS should never be set to ExactlyOnce as this is not supported by AWS IoT.
    fn pub_sub_message_get_qos_invalid() {
        let mut message = get_test_message();
        message.qos = rumqttc::QoS::ExactlyOnce;

        let _impossible_qos_val = message.get_qos();
    }

    #[test]
    /// Test the get_retain api : https://www.hivemq.com/blog/mqtt-essentials-part-8-retained-messages/
    fn pub_sub_get_retain() {
        let message = get_test_message();
        assert_eq!(message.get_retain(), message.retain);
    }

    fn get_test_message() -> MqttMessage {
        MqttMessage::new(
            TEST_TOPIC.to_string(),
            QUALITY_OF_SERVICE,
            TEST_PAYLOAD.to_string(),
            RETAIN,
        )
    }
}
