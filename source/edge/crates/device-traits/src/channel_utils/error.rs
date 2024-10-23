use thiserror::Error;
/// Error types for ChannelUtility
#[derive(Error, Debug, PartialEq)]
pub enum ChannelUtilError {
    /// Error when resource could not be locked
    #[error("Failed to lock global resource.")]
    CouldNotLock,
    /// Error when MPSC resource already exists.
    #[error("Communication channel already exists")]
    ResourceAlreadyExists,
    /// Error when MPSC resource has not been created yet.
    #[error("Communication not established yet")]
    CommunicationNotCreated,
    /// Send error occurred in underlying channel
    #[error("Send Error in underlying channel : `{0}`")]
    SendError(String),
    /// Channel is full, this should never result in a panic.
    #[error("Channel is full, wait till it is emptied.")]
    BufferFullError,
    /// Channel is closed Error.
    #[error("Unknown Error, Channel utility is in an undefined state.")]
    ChannelClosed,
    /// Error when util is in a bad state.
    #[error("Unknown Error, Channel utility is in an undefined state.")]
    UnknownError,
}
