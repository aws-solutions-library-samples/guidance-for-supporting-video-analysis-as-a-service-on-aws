use std::sync::Arc;

use crate::message::MqttMessage;
use device_traits::connections::{PubSubMessage, PubSubMessageBuilder, QoS};

///Message builder for MQTTMessages implements PubSubMessageBuilder Trait.
#[derive(Debug, Default, Clone, Eq, PartialEq)]
pub struct MQTTMessageBuilder {
    /// Topic the message was sent to or received from.
    pub(crate) topic: String,
    /// Quality of service QoS 0 : At most once , QoS 1 : At least Once
    pub(crate) qos: QoS,
    /// Payload, usually JSON
    pub(crate) payload: String,
    /// Inform broker to keep the last message
    pub(crate) retain: bool,
}

impl PubSubMessageBuilder for MQTTMessageBuilder {
    /// Get the topic used in the message.
    fn get_topic(&self) -> &str {
        &self.topic
    }
    /// Set the value of the topic of the message.
    fn set_topic(&mut self, topic: &str) {
        self.topic = topic.to_owned();
    }
    /// Get a reference to the message payload
    fn get_payload(&self) -> &String {
        &self.payload
    }
    /// Set the payload of a message
    fn set_payload(&mut self, payload: String) {
        self.payload = payload
    }
    /// Get a reference to the quality of service
    fn get_qos(&self) -> QoS {
        self.qos
    }
    /// Set the quality of serive of a message
    fn set_qos(&mut self, qos: QoS) {
        self.qos = qos;
    }
    /// Get the retain value of the message
    fn get_retain(&self) -> bool {
        self.retain
    }
    /// Set the retain value of the message
    fn set_retain(&mut self, retain: bool) {
        self.retain = retain
    }
    /// Get a the message in a Box.
    fn build_box_message(&self) -> Box<dyn PubSubMessage + Send + Sync> {
        Box::new(MqttMessage::new(
            self.topic.to_owned(),
            self.qos,
            self.payload.to_owned(),
            self.retain,
        ))
    }
    /// Get the message in a Arc
    fn build_arc_message(&self) -> Arc<dyn PubSubMessage + Send + Sync> {
        Arc::new(MqttMessage::new(
            self.topic.to_owned(),
            self.qos,
            self.payload.to_owned(),
            self.retain,
        ))
    }

    /// Make a copy of builder.  Used to support injection
    fn clone_builder(&self) -> Box<dyn PubSubMessageBuilder + Send + Sync> {
        Box::new(self.clone())
    }
}

impl MQTTMessageBuilder {
    /// Return the MQTTMessage builder as the PubSubMessageBuilder trait for dependency injection
    pub fn new_pub_sub_message_builder() -> Box<dyn PubSubMessageBuilder + Send + Sync> {
        Box::<MQTTMessageBuilder>::default()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    const TEST_TOPIC: &str = r"$aws/reserved/topic";
    const TEST_PAYLOAD: &str = r"Just data.";
    const TEST_QOS: QoS = QoS::AtLeastOnce;
    const TEST_RETAIN: bool = false;

    /// Test the getter + setter for topic
    #[test]
    fn message_builder_topic() {
        let mut pub_sub_builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        pub_sub_builder.set_topic(TEST_TOPIC);
        assert_eq!(pub_sub_builder.get_topic(), TEST_TOPIC);
    }
    /// Test the getter + setter for payload
    #[test]
    fn message_builder_payload() {
        let mut pub_sub_builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        pub_sub_builder.set_payload(TEST_PAYLOAD.to_owned());
        assert_eq!(pub_sub_builder.get_payload(), TEST_PAYLOAD);
    }

    /// Test the getter + setter for quality of service
    #[test]
    fn message_builder_qos() {
        let mut pub_sub_builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        pub_sub_builder.set_qos(QoS::AtLeastOnce);
        assert_eq!(pub_sub_builder.get_qos(), QoS::AtLeastOnce);
        pub_sub_builder.set_qos(QoS::AtMostOnce);
        assert_eq!(pub_sub_builder.get_qos(), QoS::AtMostOnce);
    }
    /// Test the getter + setter for retain value
    #[test]
    fn message_builder_retain() {
        let mut pub_sub_builder = MQTTMessageBuilder::new_pub_sub_message_builder();

        pub_sub_builder.set_retain(true);
        assert!(pub_sub_builder.get_retain());

        pub_sub_builder.set_retain(false);
        assert!(!pub_sub_builder.get_retain());
    }
    /// Test generation of pub_sub_message in a Box
    #[test]
    fn message_builder_boxed_pub_sub_message() {
        let builder = setup_builder();
        let message: Box<dyn PubSubMessage + Send + Sync> = builder.build_box_message();
        assert_eq!(message.get_topic(), TEST_TOPIC);
        assert_eq!(message.get_payload(), TEST_PAYLOAD);
        assert_eq!(message.get_qos(), TEST_QOS);
        assert_eq!(message.get_retain(), TEST_RETAIN);
    }
    /// Test generation of pub_sub_messsage in a Arc.  Used for passing messages in tokio channels
    #[test]
    fn message_builder_arc_pub_sub_message() {
        let builder = setup_builder();
        let message: Arc<dyn PubSubMessage + Send + Sync> = builder.build_arc_message();
        assert_eq!(message.get_topic(), TEST_TOPIC);
        assert_eq!(message.get_payload(), TEST_PAYLOAD);
        assert_eq!(message.get_qos(), TEST_QOS);
        assert_eq!(message.get_retain(), TEST_RETAIN);
    }

    #[test]
    fn clone_builder() {
        let builder = setup_builder();
        let _copy = builder.clone_builder();
    }
    /// Setup message builder for message generation tests.
    fn setup_builder() -> Box<dyn PubSubMessageBuilder> {
        let mut pub_sub_builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        pub_sub_builder.set_retain(TEST_RETAIN);
        pub_sub_builder.set_qos(TEST_QOS);
        pub_sub_builder.set_payload(TEST_PAYLOAD.to_owned());
        pub_sub_builder.set_topic(TEST_TOPIC);
        pub_sub_builder
    }
}
