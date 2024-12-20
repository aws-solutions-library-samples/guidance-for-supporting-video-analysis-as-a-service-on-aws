use thiserror::Error;
use tracing::error;

// This is added as linter was giving warning for 'all variants have the same postfix: `Error`'.
// Recommendation was to use full path which we are already using.
#[allow(clippy::enum_variant_names)]
/// Error that can be thrown by Video Streaming Component.
#[derive(Debug, Error)]
pub enum DatabaseError {
    /// SQLite Error.
    #[error("Error encountered when calling SQLite Database")]
    FileMetadataStorageError(rusqlite::Error),
    /// FileSystem Error.
    #[error("Error encountered while persisting video frame file on local storage")]
    FileSystemStorageError(#[from] std::io::Error),
    /// Serialization Error
    #[error("Error while serializing or deserializing fragment")]
    SerializationError(Box<bincode::ErrorKind>),
    #[error("Database corrupted")]
    FileMetadataStorageDatabaseCorrupted,
    #[error("FS Extra error")]
    FileSystemReadError(#[from] fs_extra::error::Error),
}
