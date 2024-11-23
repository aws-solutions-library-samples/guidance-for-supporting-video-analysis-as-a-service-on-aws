use crate::client::configuration::ConfigurationHelper;
use crate::client::registration::RegistrationHelper;
use crate::client::snapshot::SnapshotHelper;
use crate::client::topics::TopicHelper;
use crate::constants::{
    DISABLED_FIELD_FROM_CLOUD, ENABLED_FIELD_FROM_CLOUD, LOGGER_SETTINGS_FIELD,
    PROVISION_SHADOW_NAME, SNAPSHOT_SHADOW_NAME,
};
use async_trait::async_trait;
use config::Config;
use device_traits::connections::{
    AsyncIotClientManager, AsyncPubSubClient, IotClientManager, PubSubMessage, QoS,
};
use device_traits::state::State;
use mqtt_client::{
    client::MqttClient,
    client_settings::{AsyncIotClientSettings, TLSCredentialsResources},
    message::MqttMessage,
};
use rumqttc::LastWill;
use serde_derive::Deserialize;
use serde_json::Value;
use std::{path::PathBuf, time::Duration};
use tracing::{info, instrument};

mod configuration;
mod registration;
mod snapshot;
///Helper methods for working with IoT topics.
pub mod topics;

/// Struct implements IotConnectionManager for MQTT with IoT
#[derive(Debug, Clone, Deserialize)]
pub struct IotMqttClientManager {
    pub(crate) ca_cert_path: PathBuf,
    pub(crate) pem_cert_path: PathBuf,
    pub(crate) key_path: PathBuf,
    pub(crate) dir_path: PathBuf,
    /// The unique id for the current async client
    pub(crate) client_id: String,
    /// The endpoint for the communication with AWS IoT
    pub(crate) aws_iot_endpoint: String,
    /// The Message published to broker in the event of ungraceful disconnect.
    /// ManagerLastWill struct used as dependency struct does not implement Deserialize
    pub(crate) last_will: Option<ManagerLastWill>,
    /// The cadence that client will ping AWS IoT broker
    pub(crate) keep_alive_milli_sec: u64,
    /// The port for the mqtt communication
    pub(crate) mqtt_port: u16,
}

#[async_trait]
impl IotClientManager for IotMqttClientManager {
    /// Create a new mqtt client using customer certs and configurations
    #[instrument]
    async fn new_pub_sub_client(&self) -> anyhow::Result<AsyncPubSubClient> {
        let mut mqtt_client = self.setup_mqtt_client(self.get_tls_credentials()).await?;
        info!("MQTT client created");

        // subscribe to delta, accepted, and rejected shadow topics for all named shadow
        let shadows = &[PROVISION_SHADOW_NAME];

        for shadow in shadows {
            let topic_helper =
                TopicHelper::new(self.client_id.to_owned(), Some(shadow.to_string()));

            let shadow_accepted_topic = topic_helper.get_shadow_update_accepted();
            let shadow_rejected_topic = topic_helper.get_shadow_update_rejected();
            let shadow_delta_topic = topic_helper.get_shadow_update_delta_topic();

            mqtt_client.subscribe(shadow_accepted_topic.as_str(), QoS::AtLeastOnce).await?;
            mqtt_client.subscribe(shadow_rejected_topic.as_str(), QoS::AtLeastOnce).await?;
            mqtt_client.subscribe(shadow_delta_topic.as_str(), QoS::AtLeastOnce).await?;
        }

        Ok(mqtt_client)
    }

    /// Helper function to get a new pub_sub message
    #[instrument]
    fn new_pub_sub_message(&self) -> Box<dyn PubSubMessage + Send + Sync> {
        let topic = "".to_string();
        let qos = QoS::AtLeastOnce;
        let payload = "".to_string();
        let retain = false;

        Box::new(MqttMessage::new(topic, qos, payload, retain))
    }

    /// Checks if message is logger settings message
    fn received_logger_settings_message(
        &self,
        msg: &(dyn PubSubMessage + Send + Sync),
    ) -> Option<Value> {
        let topic_helper =
            TopicHelper::new(self.client_id.to_string(), Some(PROVISION_SHADOW_NAME.to_string()));
        ConfigurationHelper::received_expected_shadow_message(
            msg,
            topic_helper.get_shadow_update_delta_topic(),
            LOGGER_SETTINGS_FIELD,
        )
    }

    /// Checks if message is snapshot presigned url message
    fn received_snapshot_message(&self, msg: &(dyn PubSubMessage + Send + Sync)) -> Option<String> {
        let topic_helper =
            TopicHelper::new(self.client_id.to_string(), Some(SNAPSHOT_SHADOW_NAME.to_string()));
        SnapshotHelper::received_expected_shadow_message(
            msg,
            topic_helper.get_shadow_update_delta_topic(),
        )
    }

    /// Received a message to update the State of the device
    fn received_state_message(&self, msg: &(dyn PubSubMessage + Send + Sync)) -> Option<State> {
        let topic_helper =
            TopicHelper::new(self.client_id.to_string(), Some(PROVISION_SHADOW_NAME.to_string()));

        if RegistrationHelper::received_expected_shadow_message(
            msg,
            topic_helper.get_shadow_update_delta_topic(),
            ENABLED_FIELD_FROM_CLOUD,
        ) {
            return Some(State::CreateOrEnableSteamingResources);
        }

        if RegistrationHelper::received_expected_shadow_message(
            msg,
            topic_helper.get_shadow_update_delta_topic(),
            DISABLED_FIELD_FROM_CLOUD,
        ) {
            return Some(State::DisableStreamingResources);
        }

        None
    }
}

impl IotMqttClientManager {
    /// This will panic if IoTConnectionsManager cannot be created.
    #[instrument]
    pub fn new_iot_connection_manager(config: Config) -> AsyncIotClientManager {
        Box::new(
            config
                .try_deserialize::<IotMqttClientManager>()
                .expect("Config file provided is of improper form."),
        )
    }

    #[instrument]
    fn get_tls_credentials(&self) -> Option<TLSCredentialsResources> {
        Some(TLSCredentialsResources::new(
            self.ca_cert_path.to_str()?,
            self.pem_cert_path.to_str()?,
            self.key_path.to_str()?,
        ))
    }

    fn get_mqtt_keep_alive_duration(&self) -> Duration {
        Duration::from_millis(self.keep_alive_milli_sec)
    }
    /// Settings for mqtt client only change by the certificates used.
    #[instrument]
    fn get_async_mqtt_settings(
        &self,
        tls_credentials: Option<TLSCredentialsResources>,
    ) -> AsyncIotClientSettings {
        let last_will = if self.last_will.is_some() {
            Some(self.last_will.as_ref().unwrap().get_mqtt_last_will())
        } else {
            None
        };
        AsyncIotClientSettings::new(
            self.client_id.to_owned(),
            tls_credentials,
            self.aws_iot_endpoint.to_owned(),
            last_will,
            self.get_mqtt_keep_alive_duration(),
            self.mqtt_port,
        )
    }
    #[instrument]
    async fn setup_mqtt_client(
        &self,
        tls_credentials: Option<TLSCredentialsResources>,
    ) -> anyhow::Result<AsyncPubSubClient> {
        let _async_mqtt_settings = self.get_async_mqtt_settings(tls_credentials);
        Ok(Box::new(MqttClient::new(&_async_mqtt_settings).await?))
    }
}

/// The Mqtt dependency we use does not include deserialize on the LastWill Message
/// This is required to read the setting in from a config file.
/// It can be removed if rumqttc's LastWill structure implements Deserialize in the future.
#[derive(Debug, Deserialize, Clone)]
pub(crate) struct ManagerLastWill {
    pub(crate) topic: String,
    pub(crate) payload: String,
    pub(crate) qos: QoS,
    pub(crate) retain: bool,
}

impl ManagerLastWill {
    pub fn get_mqtt_last_will(&self) -> LastWill {
        let qos = match self.qos {
            QoS::AtMostOnce => rumqttc::QoS::AtMostOnce,
            QoS::AtLeastOnce => rumqttc::QoS::AtLeastOnce,
        };
        LastWill::new(self.topic.to_owned(), self.payload.to_owned(), qos, self.retain)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::constants::STATE_SHADOW_FIELD;
    use mqtt_client::builder::MQTTMessageBuilder;
    use serde_json::json;
    const TEST_STR: &str = "Content of str does not matter.";
    const AWS_QOS: QoS = QoS::AtLeastOnce;
    const MQTT_QOS: rumqttc::QoS = rumqttc::QoS::AtLeastOnce;
    const RETAIN: bool = false;
    const PATH: &str = "/home/test/dir";
    const CLIENT_ID: &str = "client-id";
    const MQTT_PORT: u16 = 100;
    const KEEP_ALIVE_MS: u64 = 6000;

    /// This test can be removed if rumqttc LastWill implements in the future Deserialize
    #[test]
    fn manager_last_will() {
        let last_will = get_last_will();
        let last_will: LastWill = last_will.get_mqtt_last_will();
        assert_eq!(last_will.topic, TEST_STR.to_string());
        assert_eq!(last_will.message, TEST_STR.to_string());
        assert_eq!(last_will.qos, MQTT_QOS);
        assert_eq!(last_will.retain, RETAIN);
    }
    ///Test helper function which generates AsyncIotClientSettings.
    ///tls_credentials are set to None or method will read from the filesystem.
    /// Confirm that settings can generate valid rumqttc settings.
    #[test]
    fn async_settings_test() {
        let mqtt_client_manager = get_mqtt_client_manager();
        let result = mqtt_client_manager.get_async_mqtt_settings(None);
        assert!(result.generate_aws_mqtt_options().is_ok());
    }

    /// Confirm helper can setup a valid mqtt client
    #[tokio::test]
    async fn setup_mqtt_client_helper_test() {
        let mqtt_client_manager = get_mqtt_client_manager();
        let mqtt_client = mqtt_client_manager.setup_mqtt_client(None).await;
        assert!(mqtt_client.is_ok());
    }

    ///Test to make sure the new_pub_sub_message() method returns an object of the correct type.
    #[test]
    fn get_pub_sub_message_test() {
        let mqtt_client_manager = get_mqtt_client_manager();
        let _message: Box<dyn PubSubMessage + Send + Sync> =
            mqtt_client_manager.new_pub_sub_message();
    }

    // Confirm that helper can create valid tls_credentials
    #[test]
    fn get_tls_credentials_test() {
        let mqtt_client_manager = get_mqtt_client_manager();
        let tls_credentials = mqtt_client_manager.get_tls_credentials();
        assert!(tls_credentials.is_some());
    }

    #[test]
    fn received_logger_settings_message_test() {
        let mqtt_client_manager = get_mqtt_client_manager();

        let topic_helper =
            TopicHelper::new(CLIENT_ID.to_string(), Some(PROVISION_SHADOW_NAME.to_string()));

        let mut message_builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        message_builder.set_topic(topic_helper.get_shadow_update_delta_topic().as_str());

        let settings = json!({"isEnabled": true, "syncFrequency": 300, "logLevel": "INFO"});
        let payload = json!({ STATE_SHADOW_FIELD: { LOGGER_SETTINGS_FIELD: settings } });
        let payload = serde_json::to_string::<Value>(&payload)
            .expect("Issue formatting Json.  Invalid code change.");
        message_builder.set_payload(payload);
        let message = message_builder.build_box_message();

        let received_settings =
            mqtt_client_manager.received_logger_settings_message(message.as_ref());

        assert_eq!(received_settings.unwrap(), settings);
    }

    /// Helper function to generate last will for tests.
    fn get_last_will() -> ManagerLastWill {
        ManagerLastWill {
            topic: TEST_STR.to_string(),
            payload: TEST_STR.to_string(),
            qos: AWS_QOS,
            retain: RETAIN,
        }
    }
    /// Create mqtt client manager for unit tests.
    fn get_mqtt_client_manager() -> IotMqttClientManager {
        let path = PathBuf::default().join(PATH);
        let last_will = Some(get_last_will());
        IotMqttClientManager {
            ca_cert_path: path.to_owned(),
            pem_cert_path: path.to_owned(),
            key_path: path.to_owned(),
            dir_path: path,
            client_id: CLIENT_ID.to_string(),
            aws_iot_endpoint: CLIENT_ID.to_string(),
            last_will,
            keep_alive_milli_sec: KEEP_ALIVE_MS,
            mqtt_port: MQTT_PORT,
        }
    }
}
