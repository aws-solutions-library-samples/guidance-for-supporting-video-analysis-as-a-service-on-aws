use thiserror::Error;
/// error types for Client
#[derive(Error, Debug)]
pub enum ClientError {
    /// error from http client
    #[error("Failed to send http request")]
    HttpClientError(#[from] reqwest::Error),
}
