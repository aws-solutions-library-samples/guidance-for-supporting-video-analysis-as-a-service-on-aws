use thiserror::Error;
use tokio::sync::mpsc::error::SendError;
use tracing::error;

#[derive(Debug, Error)]
pub enum WebrtcClientError {
    /// IO Error.
    #[error("IO error")]
    IOError(#[from] std::io::Error),

    /// Error from Sender.
    #[error("Sender error")]
    SendError(SendError<std::string::String>),

    /// Error from webrtc-rs library.
    #[error("Error encountered while running Video Streaming component")]
    WebrtcError(#[from] webrtc::Error),

    /// Any other runtime error.
    #[error("Error encountered while running Video Streaming component")]
    OtherError(#[from] std::fmt::Error),

    /// Error from signaling client
    #[error("Signaling error")]
    SignalingError(String),

    /// Error connecting to RTSP
    #[error("RTSP error")]
    RtspError(String),

    /// Maximum number of connections
    #[error("Maximum connections reached")]
    MaximumConnectionsError(String),

    /// Error parsing message from client
    #[error("Parsing error")]
    ParsingError(String),
}
