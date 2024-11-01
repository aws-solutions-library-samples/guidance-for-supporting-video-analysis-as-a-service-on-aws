use thiserror::Error;
use tracing::error;

#[allow(clippy::enum_variant_names)]
#[derive(Debug, Error)]
pub enum DatabaseError {
    /// SQLite Error.
    #[error("Error encountered when calling SQLite Database")]
    FileMetadataStorageError(rusqlite::Error),
}