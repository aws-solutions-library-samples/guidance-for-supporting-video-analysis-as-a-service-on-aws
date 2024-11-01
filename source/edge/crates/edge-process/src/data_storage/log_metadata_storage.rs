use crate::constants::LOG_DB_FILE_NAME;
use crate::data_storage::error::DatabaseError;
use crate::utils::logger_setup::construct_current_log_name;
use rusqlite::{params, Connection};
use tracing::info;

// GRCOV_STOP_COVERAGE
// We don't want to add test cases for LogMetadataStorage as it will delete the existing data
// before inserting a new data

/// Struct to persist log metadata on disk
#[derive(Debug)]
pub struct LogMetadataStorage {
    /// SQLite DB file path.
    #[allow(unused)]
    pub db_path: String,
    /// Connection instance.
    pub db_connection: Connection,
}

impl LogMetadataStorage {
    /// Instantiate LogMetadataStorage.
    pub fn new_connection(db_dir_path: String) -> Result<LogMetadataStorage, DatabaseError> {
        let db_path = db_dir_path.to_owned() + "/" + LOG_DB_FILE_NAME;

        let connection = match Self::attempt_to_create_db_connection(db_path.as_str()) {
            Ok(connection) => {
                info!("Successfully established a database connection.");
                connection
            }
            Err(e) => {
                panic!("Error occurred when establishing database connection : {:?}", e);
            }
        };

        Ok(LogMetadataStorage { db_path, db_connection: connection })
    }

    fn attempt_to_create_db_connection(db_path: &str) -> Result<Connection, DatabaseError> {
        let connection =
            Connection::open(db_path).map_err(DatabaseError::FileMetadataStorageError)?;
        connection
            .execute_batch(
                "
              PRAGMA journal_mode = OFF;
              PRAGMA synchronous = FULL;
              PRAGMA locking_mode = EXCLUSIVE;
              PRAGMA temp_store = MEMORY;",
            )
            .map_err(DatabaseError::FileMetadataStorageError)?;

        connection
            .execute(
                "CREATE TABLE IF NOT EXISTS log_metadata (file_name TEXT PRIMARY KEY, position INTEGER)",
                [],
            )
            .map_err(DatabaseError::FileMetadataStorageError)?;

        Ok(connection)
    }

    /// Save log metadata to database.
    pub fn save_log_metadata(
        &mut self,
        log_file_name: &String,
        position: u64,
    ) -> Result<(), DatabaseError> {
        let transaction =
            self.db_connection.transaction().map_err(DatabaseError::FileMetadataStorageError)?;

        let _ = transaction.execute("DELETE FROM log_metadata", []);

        let _ = transaction.execute(
            "INSERT INTO log_metadata (file_name, position) VALUES (?, ?)",
            params![log_file_name, position],
        );

        transaction.commit().map_err(DatabaseError::FileMetadataStorageError)?;
        Ok(())
    }

    /// get log metadata from database.
    pub fn get_log_metadata(&mut self) -> Result<(String, u64), DatabaseError> {
        let query = "SELECT file_name, position FROM log_metadata";
        // Execute the query and fetch the log metadata.
        let mut statement =
            self.db_connection.prepare(query).map_err(DatabaseError::FileMetadataStorageError)?;

        let mut rows = statement.query([]).map_err(DatabaseError::FileMetadataStorageError)?;

        let mut start_file_name = construct_current_log_name();
        let mut start_position = 0;
        while let Some(row) = rows.next().map_err(DatabaseError::FileMetadataStorageError)? {
            start_file_name = row.get(0).map_err(DatabaseError::FileMetadataStorageError)?;
            start_position = row.get(1).map_err(DatabaseError::FileMetadataStorageError)?;
        }
        Ok((start_file_name, start_position))
    }
}