use thiserror::Error;
use tracing::error;

#[allow(clippy::enum_variant_names)]
/// Error that can be thrown by Video Streaming Component.
#[derive(Debug, Error)]
pub enum IoTClientError {
    #[error("Error encountered while trying to fetch credentials")]
    CredentialsError(String),
    #[error("Error encountered while calling the client")]
    ClientError(String),
}
