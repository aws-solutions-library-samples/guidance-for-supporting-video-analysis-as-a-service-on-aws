//! This crate will store all traits used by the device client for streaming.
//! Used to facilitate dependency injection of the Models + Controllers.

#![warn(missing_docs, missing_debug_implementations, rust_2018_idioms)]

use async_trait::async_trait;
use mockall::automock;
use std::error::Error;

/// Module contains different errors thrown by VideoStreaming components.
pub mod error;

/// Struct for StreamUri configurations
#[derive(Debug, Clone)]
pub struct StreamUriConfiguration {
    /// RTSP Stream uri.
    pub rtsp_uri: String,
    /// Onvif username.
    pub username: String,
    /// Onvif password.
    pub password: String,
}

/// Config object for Streaming Service Configurations
#[derive(Debug, Clone)]
pub struct StreamingServiceConfigurations {
    /// RTSP Stream uri.
    pub rtsp_uri: String,
    /// Onvif username.
    pub username: String,
    /// Onvif password.
    pub password: String,
    /// IOT Credential Endpoint.
    pub iot_endpoint: String,
    /// X.509 Certificate Path on filesystem.
    pub iot_cert_path: String,
    /// Private Key Path on filesystem.
    pub private_key_path: String,
    /// Role Alias that will enable IoT to assume role with the certificate.
    pub role_alias: String,
    /// AWS Region in which resource is created in AWS Account.
    pub region: String,
    /// CA Certification path on filesystem.
    pub ca_cert_path: String,
    /// Stream name: Should be iot-thing name
    pub stream_name: String,
    /// region device points at
    pub aws_region: String,
}

/// This trait is responsible to get stream URI for video from device.
#[automock]
#[async_trait]
pub trait VideoStreamConsumer {
    /// set up services uri
    async fn set_up_services_uri(&mut self, ip_address: String) -> Result<(), Box<dyn Error>>;

    /// GetStreamUri information related
    async fn get_stream_uri(&mut self) -> Result<String, Box<dyn Error>>;

    /// Formats RTSP url taken from GetStreamUri
    async fn get_rtsp_url(
        &mut self,
        username: String,
        password: String,
    ) -> Result<StreamUriConfiguration, Box<dyn Error>>;

    /// Set new credentials for onvif.
    async fn bootstrap(&mut self, username: String, password: String)
        -> Result<(), Box<dyn Error>>;
}
