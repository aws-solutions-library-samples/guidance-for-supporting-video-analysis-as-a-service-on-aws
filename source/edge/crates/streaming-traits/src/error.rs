use thiserror::Error;
use tracing::error;

/// Error that can be thrown by Video Streaming Component.
#[derive(Debug, Error)]
pub enum VideoStreamingError {
    /// Invalid Request Error.
    #[error("Invalid Request")]
    InvalidRequestError,

    /// Gstreamer Error.
    #[error("Error while running Gstreamer video pipeline - {0}")]
    GstreamerError(String),

    /// Any other runtime error.
    #[error("Error encountered while running Video Streaming component")]
    OtherError(#[from] std::fmt::Error),

    /// MutexGuard Lock Error on shared resource.
    #[error("Error while acquiring lock on - {0}")]
    MutexGuardLockError(String),

    /// MutexGuard Wait Error on shared resource.
    #[error("Error while waiting for notification on lock for - {0}")]
    MutexGuardWaitError(String),

    /// Error when formatting the sd card
    #[error("The requested storage configuration for sd card is not formatted")]
    SDCardFormattingRequiredError,

    /// Error when sd card is not attached to camera
    #[error("The requested storage configuration for sd card does not exist")]
    NoSDCardAvailableError,

    /// SQLite Error while processing video for offline ingestion.
    #[error("Error encountered while processing video for offline ingestion - {0}")]
    OfflineIngestionSQLiteError(String),

    /// DiskUsage Error.
    #[error("Error encountered while calculating disk usage")]
    DiskUsageError(#[from] std::io::Error),
}

/// Error that can be thrown by Metadata Streaming Component.
#[derive(Debug, Error)]
pub enum MetadataStreamingError {
    /// Metadata Processing Error
    #[error("Error in post-processing AI event")]
    AIEventPostProcessingError,
}
