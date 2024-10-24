use async_trait::async_trait;
use mockall::automock;
use serde_derive::{Deserialize, Serialize};
use serde_json::json;
use std::sync::Arc;
type BoxedMessage = Box<dyn PubSubMessage + Send + Sync>;
/// Type to manage client that can be shared across awaits
pub type AsyncPubSubClient = Box<dyn PubSubClient + Send + Sync>;
/// Type to manage client manager that can be shared across awaits
pub type AsyncIotClientManager = Box<dyn IotClientManager + Send + Sync>;

/// Trait to manage IoT connections for edge process
/// This trait can be used to manage certificates + create pub-sub clients + create pub-sub messages
#[automock]
#[async_trait]
pub trait IotClientManager {
    /// Create a new pub_sub_client configured to communicate with AWS IoT
    async fn new_pub_sub_client(&self) -> anyhow::Result<AsyncPubSubClient>;
    /// Create a new pub sub message for IoT communication.
    fn new_pub_sub_message(&self) -> Box<dyn PubSubMessage + Send + Sync>;
}

#[async_trait]
/// PubSubClient trait can be implemented by any Client which implements a pubsub format.
pub trait PubSubClient {
    /// subscribe to the given mqtt topic given the qos
    async fn subscribe(&mut self, topic: &str, qos: QoS) -> anyhow::Result<()>;
    /// subscribe to the given mqtt topic
    async fn unsubscribe(&mut self, topic: &str) -> anyhow::Result<()>;
    /// Events are generated from incoming messages.  This accesses the next message on the event queue.
    async fn recv(&mut self) -> anyhow::Result<BoxedMessage>;
    /// publish the mqtt message
    async fn send(&self, message: BoxedMessage) -> anyhow::Result<()>;
}

/// PubSubMessage builder trait.
#[automock]
pub trait PubSubMessageBuilder {
    /// Get the topic used in the builder.
    fn get_topic(&self) -> &str;
    /// Set the value of the topic of the builder.
    fn set_topic(&mut self, topic: &str);
    /// Get a reference to the builder payload
    fn get_payload(&self) -> &String;
    /// Set the payload of a builder
    fn set_payload(&mut self, payload: String);
    /// Get a reference to the quality of service
    fn get_qos(&self) -> QoS;
    /// Set the quality of  of a builder
    fn set_qos(&mut self, qos: QoS);
    /// Get the retain value of the builder
    fn get_retain(&self) -> bool;
    /// Set the retain value of the builder
    fn set_retain(&mut self, retain: bool);
    /// Get a the message in a Box.
    fn build_box_message(&self) -> Box<dyn PubSubMessage + Send + Sync>;
    /// Get a the message in a Arc, can be sent through tokio channels
    fn build_arc_message(&self) -> Arc<dyn PubSubMessage + Send + Sync>;
    /// Get a clone of PubSubMessageBuilder
    fn clone_builder(&self) -> Box<dyn PubSubMessageBuilder + Send + Sync>;
}
//Manual implementation required.
impl std::fmt::Debug for Box<dyn PubSubMessageBuilder + Send + Sync> {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let message = json!({
        "topic": self.get_topic(),
        "payload": self.get_payload(),
        "qos":self.get_qos(),
        "retain":self.get_retain()
        });
        write!(f, "{}", message)
    }
}

//Manual implementation required.
impl std::fmt::Debug for dyn PubSubMessage + Send + Sync {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let message = json!({
        "topic": self.get_topic(),
        "payload": self.get_payload(),
        "qos":self.get_qos(),
        "retain":self.get_retain()
        });
        write!(f, "{}", message)
    }
}

//TODO: Convert Payload from String to flat buffer
//TODO: Do we want to follow a builder pattern for PubSubMessages?
/// This trait is implemented to use PubSubClient
#[automock]
pub trait PubSubMessage {
    /// Get the topic the message was received on or sent too.
    fn get_topic(&self) -> &str;
    /// Get a reference to the message payload
    fn get_payload(&self) -> &String;
    /// Set the payload of a message
    fn get_qos(&self) -> QoS;
    /// Set the quality of service of a message
    fn get_retain(&self) -> bool;
}

/// Quality of service, We only allow AtMostOnce and AtLeastOnce as this is all AWS IoT supports.
/// https://docs.aws.amazon.com/iot/latest/developerguide/mqtt.html#mqtt-qos
#[repr(u8)]
#[derive(Default, Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Serialize, Deserialize)]
pub enum QoS {
    /// This service level guarantees a best-effort delivery.
    /// There is no guarantee of delivery.
    AtMostOnce = 0,
    /// Guarantees that a message is delivered at least one time to the receiver.
    /// May be sent more than once.
    #[default]
    AtLeastOnce = 1,
}
