use crate::soap::serialization_error;
use http_client::error::ClientError;
use thiserror::Error;

/// error types for Onvifclient
#[derive(Error, Debug)]
pub enum OnvifClientError {
    /// error from http client
    #[error("Reqwest error")]
    ReqwestError(#[from] reqwest::Error),

    /// error from http client
    #[error("Failed to send http request")]
    HttpClientError(#[from] ClientError),

    /// error from http_auth when handling digest header
    #[error("Failed to generate digest")]
    DigestError,

    /// error when onvif server return with unexpected http status
    #[error("Unexpected Http status code")]
    UnexpectedHttpStatusError,

    /// error when calling onvif GetServices
    #[error("Failed to get services")]
    OnvifGetServicesError,

    /// error when calling onvif GetServiceCapabilities
    #[error("The given ONVIF service doesn't exist in VideoAnalytics service")]
    OnvifServiceDoesntExist,

    /// error from xml <-> Rust struct serialization/deserialization
    #[error("Failed to serialize/deserialize")]
    SerializationError(#[from] serialization_error::SerializationError),

    /// error when onvif credential has issue
    #[error("Credential is incorrect")]
    CredentialError,

    /// error from Rust struct <-> json serialization/deserialization
    #[error("Failed to serialize/deserialize between Rust struct and json")]
    SerDeError(#[from] serde_json::Error),

    /// a generic error type
    #[error("Other error")]
    Others,

    /// error when GetProfiles API provides empty profile_token
    #[error("Failed to provide profile_token from get profiles")]
    OnvifGetProfilesError,

    /// error when rtsp url is not expected in the format
    #[error("The RTSP url returned is not expected in the format")]
    OnvifRtspUrlError,

    /// error when GetVideoConfigurations API provides empty Vec<VideoEncoder2Configuration>
    #[error("Failed to get video encoder configuration(s)")]
    OnvifGetVideoEncoderConfigurationsError,

    /// error when GetVideoAnalyticsConfigurations API provides empty Vec<VideoAnalyticsConfiguration>
    #[error("Failed to get video analytics configuration(s)")]
    OnvifGetVideoAnalyticsConfigurationsError,

    /// error when GetVideoAnalyticsConfigurations API provides empty Vec<VideoSourceConfiguration>
    #[error("Failed to get video source configuration(s)")]
    OnvifGetVideoSourceConfigurationsError,

    /// error when GetStorageConfigurations returns "The requested storage configuration does not exist"
    #[error("No SD card")]
    OnvifNoCardError,

    /// error when GetStorageConfigurations returns "The requested storage is on error"
    #[error("SD card not mounted")]
    OnvifCardNotMountedError,

    /// error when GetStorageConfigurations returns "The requested storage is not formatted"
    #[error("SD card is not formatted")]
    OnvifCardNotFormattedError,

    /// error when GetStorageConfigurations returns "The requested storage is on readonly mode"
    #[error("SD card is in readonly mode")]
    OnvifCardError,
}
