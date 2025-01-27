//! This crate will store all traits used by the device client for streaming.
//! Used to facilitate dependency injection of the Models + Controllers.

#![warn(missing_docs, missing_debug_implementations, rust_2018_idioms)]

use async_trait::async_trait;
use mockall::automock;
use serde_json::Value;
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
#[cfg(not(feature = "sd-card-catchup"))]
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

/// Alternate struct with fields for SD card catchup
#[derive(Debug, Clone)]
#[cfg(feature = "sd-card-catchup")]
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
    /// local storage path for sd card
    pub local_storage_path: String,
    /// optional db_path. Otherwise will just store SQLite DB in local_storage_path
    pub db_path: Option<String>,
    /// local storage disk usage for sd card
    pub local_storage_disk_usage: u64,
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

/// This trait is responsible to create streaming pipeline to get streamed information from device source and
/// send it to correct sink in required format. This trait is also responsible to start and
/// stop streaming pipeline.
#[automock]
#[async_trait]
pub trait StreamingPipeline {
    /// Builds video streaming pipeline.
    async fn create_pipeline(
        &self,
        stream_uri_config: StreamUriConfiguration,
    ) -> Result<(), Box<dyn Error>>;
    /// Ensure video streaming pipeline to consume video data from device is started.
    async fn ensure_start_pipeline(&mut self);
    /// Ensure video streaming pipeline is stopped.
    async fn ensure_stop_pipeline(&mut self);
}

/// This trait is responsible to do any pre-processing / post-processing on AI events before ingesting it to cloud.
#[automock]
pub trait EventProcessor {
    /// Post-process AI events for transformations.
    fn post_process_event(&self, event: String) -> Result<String, Box<dyn Error>>;
    /// Returns processed event if motion detection event
    fn get_motion_based_event(&self, event: String) -> Result<Option<Value>, Box<dyn Error>>;
}
