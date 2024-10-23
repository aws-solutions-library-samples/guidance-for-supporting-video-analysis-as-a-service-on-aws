/// Errors that MQTT Client may throw.  Integrated with Tracing Crate.
/*
Credits to the following docs as some functiosn in this file derived from there:
1. https://docs.rs/rumqttc/latest/rumqttc/index.html
2. https://docs.rs/aws-iot-device-sdk-rust/latest/aws_iot_device_sdk_rust/
*/
use thiserror::Error;
use tracing::error;

use rumqttc::{ClientError as RumqttcClientError, ConnectionError};

/// Unified error kinds that can be thrown by async client
#[derive(Debug, Error)]
pub enum ClientError {
    /// Run into issues when reading files
    #[error("Cannot read the file")]
    IOError(#[from] std::io::Error),
    /// Fail to convert between the data types
    #[error("Cannot succeed in converting the data types")]
    BytesToStringConversionError(#[from] std::str::Utf8Error),
    /// Failed to connect to AWS IoT
    #[error("Failed to connect to AWS IoT")]
    AWSConnectionError(#[from] ConnectionError),
    /// When use specify the QoS type that AWS IoT currently doesn't support
    #[error(
        "Currently AWS IoT doesn't support QoS2. For more info, \
    please check: https://docs.aws.amazon.com/iot/latest/developerguide/mqtt.html"
    )]
    AWSIoTQoSError,
    /// Error encountered on a subscribing calls.
    #[error("Failed to subscribe to one of the given topics.")]
    AWSIoTSubscribeError,
    /// Error encountered while attempting to connect to the broker.
    #[error("Failed to connect to the MQTT broker.")]
    AWSIotConnectError,
    /// The errors thrown by rumqttc client
    #[error("rumqttc client error: {0}")]
    AsyncClientError(#[from] RumqttcClientError),
    /// Failed to parse the flatbuffer from the bytes
    #[error("Cannot parse flatbuffer from the bytes. Error: {0:?}")]
    FlatbufferParsingError(String),
    /// Invalid message for publish, eg payload is too large.
    #[error("MQTT Message is not valid for publish to AWS IoT.")]
    InvalidMessageForPublish(String),
}
