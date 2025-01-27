use super::error::ClientError;

use serde_derive::Deserialize;
use std::fs::read;
use std::time::Duration;

use rumqttc::{Key, LastWill, MqttOptions, Transport};

const MAX_INFLIGHT_MESSAGES: u16 = 512;
// https://docs.aws.amazon.com/general/latest/gr/iot-core.html#message-broker-limits.html
// quota for AWS mqtt is 128 Kilobytes
const MAX_PACKET_SIZE_LIMIT_IN_BYTES: usize = 128 * 1024;

/// the credential resources for the TLS
#[derive(Debug, Clone, Deserialize)]
pub struct TLSCredentialsResources {
    /// the path to the amzn root ca certificate
    ca_path: String,
    /// the path to the device certificate for AWS IoT
    cert_path: String,
    /// the path to the private key used in establishing the connection with AWS IoT
    key_path: String,
}

/// Struct to create AsyncIoTClientSettings from a config file.
// This is required as LastWill from rumqttc crate does not implement Deserialize
#[derive(Debug, Clone, Deserialize)]
pub struct AsyncIoTFromConfig {
    /// the unique id for the current async client
    client_id: String,
    /// where the TLS credential resources are stored
    tls_credentials: TLSCredentialsResources,
    /// the endpoint for the communication with AWS IoT
    aws_iot_endpoint: String,
    /// Duration in seconds
    keep_alive: u64,
    /// the port for the mqtt communication
    mqtt_port: u16,
}

impl AsyncIoTFromConfig {
    /// Generate AsyncIotClientSettings from a config file, last_will does not implement deserialize
    pub fn get_async_iot_settings(&self, last_will: Option<LastWill>) -> AsyncIotClientSettings {
        AsyncIotClientSettings::new(
            self.client_id.to_owned(),
            Some(self.tls_credentials.to_owned()),
            self.aws_iot_endpoint.to_owned(),
            last_will,
            Duration::from_secs(self.keep_alive),
            self.mqtt_port,
        )
    }
}

/// the settings for AWS IoT async client
#[derive(Debug, Clone)]
pub struct AsyncIotClientSettings {
    /// the unique id for the current async client
    client_id: String,
    /// where the TLS credential resources are stored
    tls_credentials: Option<TLSCredentialsResources>,
    /// the endpoint for the communication with AWS IoT
    aws_iot_endpoint: String,
    /// the Last will message in Mqtt
    last_will: Option<LastWill>,
    /// the cadence that client will ping AWS IoT broker
    keep_alive: Duration,
    /// the port for the mqtt communication
    mqtt_port: u16,
}

impl TLSCredentialsResources {
    /// Initialize a new instance of TLS credential resources set
    pub fn new<T>(ca_path: T, cert_path: T, key_path: T) -> Self
    where
        T: Into<String>,
    {
        let ca_path = ca_path.into();
        let cert_path = cert_path.into();
        let key_path = key_path.into();

        TLSCredentialsResources { ca_path, cert_path, key_path }
    }
}

impl AsyncIotClientSettings {
    /// Initialize a new instance of AWS IoT settings
    pub fn new<T>(
        client_id: T,
        tls_credentials: Option<TLSCredentialsResources>,
        aws_iot_endpoint: T,
        last_will: Option<LastWill>,
        keep_alive: Duration,
        mqtt_port: u16,
    ) -> Self
    where
        T: Into<String>,
    {
        let client_id = client_id.into();
        let aws_iot_endpoint = aws_iot_endpoint.into();

        AsyncIotClientSettings {
            client_id,
            tls_credentials,
            aws_iot_endpoint,
            last_will,
            keep_alive,
            mqtt_port,
        }
    }

    /// Generate the mqtt options based on the AWS settings
    pub fn generate_aws_mqtt_options(&self) -> Result<MqttOptions, ClientError> {
        // general set up for mqtt options
        let mut mqtt_options =
            MqttOptions::new(&self.client_id, &self.aws_iot_endpoint, self.mqtt_port);
        // set the ping cadence to aws iot
        mqtt_options.set_keep_alive(self.keep_alive);
        // This represents the maximum number of QoS:AtLeastOnce and QoS:AtMostOnce messages that
        // can be in the process of being transmitted at the same time.
        mqtt_options.set_inflight(MAX_INFLIGHT_MESSAGES);
        // See: https://docs.aws.amazon.com/iot/latest/developerguide/mqtt.html#mqtt-persistent-sessions
        mqtt_options.set_clean_session(false);
        // set the last will for mqtt if any
        if let Some(last_will) = self.last_will.clone() {
            mqtt_options.set_last_will(last_will);
        }
        // Set the transport to aws iot if needed, not needed for integration tests.
        if let Some(creds) = &self.tls_credentials {
            let transport = generate_tls_transport(creds)?;
            mqtt_options.set_transport(transport);
        }
        mqtt_options
            .set_max_packet_size(MAX_PACKET_SIZE_LIMIT_IN_BYTES, MAX_PACKET_SIZE_LIMIT_IN_BYTES);

        Ok(mqtt_options)
    }

    /// setter for the field tls_credentials
    pub fn tls_credentials_mut(&mut self) -> &mut Option<TLSCredentialsResources> {
        &mut self.tls_credentials
    }
}

/// Generate the transport to AWS IoT based on the settings
// GRCOV_STOP_COVERAGE : This just reads from the file system, no need implement a unit test.
fn generate_tls_transport(
    tls_credentials: &TLSCredentialsResources,
) -> Result<Transport, ClientError> {
    let ca = read(tls_credentials.ca_path.clone())?;
    let cert = read(tls_credentials.cert_path.clone())?;
    let key = read(tls_credentials.key_path.clone())?;
    // gotta use tls rsa key when talk with AWS IoT.
    // For more info, please see: <https://docs.aws.amazon.com/iot/latest/developerguide/transport-security.html>
    Ok(Transport::tls(ca, Some((cert, Key::RSA(key))), None))
}
// GRCOV_BEGIN_COVERAGE
#[cfg(test)]
mod tests {

    use super::*;
    const CLIENT_ID: &str = "Fake_Thing_Name";
    const AWS_END_POINT: &str = "amazon.fake.endpoint.com";
    const STUB_PATH: &str = "/fake/path";
    const DELAY_SEC: u64 = 5;
    const MQTT_PORT: u16 = 1234;

    #[test]
    /// Tests generate_aws_mqtt_options().  In the process this also tests
    /// AsyncIotClientSettings and .tls_credentials_mut().
    fn generate_aws_mqtt_options() {
        let last_will = Some(LastWill::new(STUB_PATH, STUB_PATH, rumqttc::QoS::AtLeastOnce, false));
        let mut aws_iot_settings = AsyncIotClientSettings::new(
            CLIENT_ID,
            None,
            AWS_END_POINT,
            last_will,
            tokio::time::Duration::from_secs(DELAY_SEC),
            MQTT_PORT,
        );
        let credentials = aws_iot_settings.tls_credentials_mut();
        assert!(credentials.is_none());

        let mqtt_settings = aws_iot_settings.generate_aws_mqtt_options();

        assert!(mqtt_settings.is_ok());
    }

    #[test]
    ///Test confirms new function creates the correct type, will not compile if incorrect.
    fn tls_credential_resources_new_and_getter() {
        let _credentials: TLSCredentialsResources =
            TLSCredentialsResources::new(STUB_PATH, STUB_PATH, STUB_PATH);
    }
}
