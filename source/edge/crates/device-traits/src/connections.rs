use crate::command::{Command, CommandStatus};
use crate::state::State;
use async_trait::async_trait;
use mockall::automock;
use serde_derive::{Deserialize, Serialize};
use serde_json::{json, Value};
use std::fmt::Debug;
use std::sync::Arc;

type BoxedMessage = Box<dyn PubSubMessage + Send + Sync>;
/// Type to manage client that can be shared across awaits
pub type AsyncPubSubClient = Box<dyn PubSubClient + Send + Sync>;
/// Type to manage client manager that can be shared across awaits
pub type AsyncIotClientManager = Box<dyn IotClientManager + Send + Sync>;

/// Trait to manage shadow interactions.
/// The struct implementing this trait will take in shadow messages from Cloud.
/// Update the local copy of the shadow in the filesystem.
/// The IotShadow manager should also add outgoing update messages to the channel.
#[automock]
#[async_trait]
pub trait ShadowManager {
    /// Update the reported state in the shadow.
    /// Success means the local state has been updated + a message was passed to the MQTT client.
    /// The cloud may not receive the update until device connects.
    async fn update_reported_state(&mut self, update_doc: Value) -> anyhow::Result<()>;
    /// Update the desired state in the shadow.  This should come from cloud based messages.
    /// Success means the local state has been updated + a message was passed to the MQTT client.
    /// The cloud may not receive the update until device connects.
    async fn update_desired_state(&mut self, update_doc: Value) -> anyhow::Result<()>;

    /// Turn on local storage of Shadow's desired state set by the cloud.  This is used to
    /// restore a device to the last instructed state in the event of a restart while disconnected from the cloud.
    async fn enable_storage(&mut self) -> anyhow::Result<()>;
}

/// Trait to manage IoT connections for edge process
/// This trait can be used to manage certificates + create pub-sub clients + create pub-sub messages
#[automock]
#[async_trait]
pub trait IotClientManager {
    /// Create a new pub_sub_client configured to communicate with AWS IoT
    async fn new_pub_sub_client(&self) -> anyhow::Result<AsyncPubSubClient>;
    /// Create a new pub sub message for IoT communication.
    fn new_pub_sub_message(&self) -> Box<dyn PubSubMessage + Send + Sync>;
    /// Attempt to publish video settings to AWS IoT.  
    async fn publish_video_settings(
        &mut self,
        video_settings: Value,
        factory_mqtt_client: &mut Box<dyn PubSubClient + Send + Sync>,
    ) -> anyhow::Result<()>;
    /// Received logger settings message from cloud
    fn received_logger_settings_message(
        &self,
        msg: &(dyn PubSubMessage + Send + Sync),
    ) -> Option<Value>;
    /// Received snapshot presigned url from cloud
    fn received_snapshot_message(&self, msg: &(dyn PubSubMessage + Send + Sync)) -> Option<String>;
    /// Received message to change the state of the device.
    fn received_state_message(&self, msg: &(dyn PubSubMessage + Send + Sync)) -> Option<State>;
    /// Received video settings message from cloud
    fn received_video_settings_message(
        &self,
        msg: &(dyn PubSubMessage + Send + Sync),
    ) -> Option<Value>;
    /// Attempt to update IoT jobs status.  
    async fn update_command_status(
        &mut self,
        status: CommandStatus,
        job_id: String,
        factory_mqtt_client: &mut Box<dyn PubSubClient + Send + Sync>,
    ) -> anyhow::Result<()>;
    /// Attempt to start next IoT job.  
    async fn start_next_command(
        &mut self,
        factory_mqtt_client: &mut Box<dyn PubSubClient + Send + Sync>,
    ) -> anyhow::Result<Option<Command>>;
    /// Get the next pending job execution for a thing.
    /// https://docs.aws.amazon.com/iot/latest/developerguide/jobs-mqtt-api.html
    /// Accoding to IoT doc, any job executions with status IN_PROGRESS are returned first.
    /// Job executions are returned in the order in which they were created.
    async fn get_next_pending_job_execution(
        &mut self,
        factory_mqtt_client: &mut Box<dyn PubSubClient + Send + Sync>,
    ) -> anyhow::Result<Option<Value>>;
    /// Update in progress job status when device boots up for REBOOT command
    async fn update_in_progress_job_status(
        &mut self,
        factory_mqtt_client: &mut Box<dyn PubSubClient + Send + Sync>,
        next_pending_job_exec: Option<Value>,
    ) -> Result<(), Box<dyn std::error::Error>>;
    /// Received jobs notify message from cloud and should start new job
    fn received_jobs_notify_message(
        &self,
        msg: &(dyn PubSubMessage + Send + Sync),
    ) -> Option<String>;
}

#[async_trait]
/// PubSubClient trait can be implemented by any Client which implements a pubsub format.
pub trait PubSubClient: Debug {
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
