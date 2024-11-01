use thiserror::Error;
use tracing::error;

// This is added as linter was giving warning for 'all variants have the same postfix: `Error`'.
// Recommendation was to use full path which we are already using.
#[allow(clippy::enum_variant_names)]
/// Error that can be thrown by Video Streaming Component.
#[derive(Debug, Error)]
pub enum KVSClientError {
    #[error("Error encountered while calling the client")]
    ClientError(String),
}
