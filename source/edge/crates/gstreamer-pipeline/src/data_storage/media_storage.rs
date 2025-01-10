use crate::data_storage::constants::{
    MAX_TIME_FOR_DB, MEDIA_DB_FILE_NAME, MEDIA_DB_JOURNAL_NAME, MEDIA_FILE_NAME,
    QUERY_SIZE_FOR_DELETE,
};
use crate::data_storage::error::DatabaseError;
use bincode::{deserialize, serialize};
use event_processor::constants::{INFERENCE_KEY, TIMESTAMP_KEY};
use fs_extra::dir::get_size;
use rusqlite::{params, Connection, Rows};
use serde_json::Value;
use std::collections::vec_deque::VecDeque;
use std::fs::{read_to_string, remove_file, write, DirEntry};
use tracing::{debug, error, info};

/// Struct to persist media metadata on disk
#[derive(Debug)]
pub(crate) struct MediaMetadataStorage {
    /// SQLite DB file path.
    #[allow(unused)]
    pub(crate) db_path: String,
    pub(crate) directory: String,
    /// Connection instance.
    pub(crate) db_connection: Connection,
    /// Maximum storage of disk space, in Bytes
    pub(crate) local_storage_disk_usage_max_bytes: u64,
    /// Current usage in Bytes
    pub(crate) current_storage_disk_usage_bytes: u64,
}

impl MediaMetadataStorage {
    /// Instantiate MediaMetadataStorage.  Storage path is the path to the directory where DB + metadata will be stored.
    pub(crate) fn new_connection(
        storage_path: &str,
        local_storage_disk_usage_max_in_mb: u64,
        db_path: Option<String>,
    ) -> Result<MediaMetadataStorage, DatabaseError> {
        // SQLite database can be set separately owa default to the storage path.
        let db_dir_path = db_path.unwrap_or(storage_path.to_string());
        let db_path = db_dir_path.to_owned() + "/" + MEDIA_DB_FILE_NAME;
        let db_journal_path = db_dir_path + "/" + MEDIA_DB_JOURNAL_NAME;

        let connection =
            match Self::attempt_to_create_db_connection(db_path.as_str(), Some(storage_path)) {
                Ok(connection) => {
                    info!("Successfully established a database connection.");
                    connection
                }
                Err(e) => {
                    // Log error
                    error!("Error occurred when establishing database connection : {:?}", e);
                    // delete corrupted database + journal if it exists.  It will exist if this is running so error thrown.
                    remove_file(db_path.as_str()).map_err(DatabaseError::FileSystemStorageError)?;
                    // Will error if journal does not exist.  Which should be logged, but is not required.
                    if let Err(e) = remove_file(db_journal_path.as_str()) {
                        error!("Error deleting journal : {:?}", e);
                    }

                    // recreate + restore from media.  None means will not test db, assume good since fresh.
                    let mut connection =
                        Self::attempt_to_create_db_connection(db_path.as_str(), None)?;
                    // Restore database from media files.
                    Self::restore_database_from_media(storage_path, &mut connection)?;
                    connection
                }
            };

        //https://crates.io/crates/fs_extra (Pretty simple helper, recursively queries the FS)
        let current_storage_disk_usage_bytes =
            get_size(storage_path).map_err(DatabaseError::FileSystemReadError)?;
        // Convert from MB to bytes.
        let local_storage_disk_usage_max_bytes = local_storage_disk_usage_max_in_mb * 1_000_000_u64;

        Ok(MediaMetadataStorage {
            db_path,
            directory: storage_path.to_owned(),
            db_connection: connection,
            local_storage_disk_usage_max_bytes,
            current_storage_disk_usage_bytes,
        })
    }

    fn get_metadata_from_result(dir_entry: DirEntry) -> Option<(u64)> {
        // If cannot pull metadata.
        let Ok(meta_data) = dir_entry.metadata() else {
            return None;
        };
        // If it is not a file then we do not care, we only want to find fragments.
        if !meta_data.is_file() {
            return None;
        }

        let file_name_os = dir_entry.file_name();
        let Some(file_name) = file_name_os.to_str() else {
            return None;
        };

        // Format of media files {MEDIA_FILE_NAME}_{start_time}
        if !file_name.contains(MEDIA_FILE_NAME) {
            return None;
        };
        // Confirmed file name.  So pull timestamp + duration from it.
        Self::pull_data_from_media_filename(file_name)
    }

    fn pull_data_from_media_filename(media_file_name: &str) -> Option<(u64)> {
        let mut parts: VecDeque<&str> = media_file_name.split('_').collect();
        // Should have exactly 2 parts
        // First part should be MEDIA_FILE_NAME
        let Some(first_part) = parts.pop_front() else {
            return None;
        };
        // Check to confirm expected pattern, Should have exactly 2 parts after popping one.
        if !first_part.eq(MEDIA_FILE_NAME) && parts.len().eq(&1) {
            error!("Invalid media file detected.");
            return None;
        }
        // Pull &str from vector.
        let Some(str_time_stamp_in_ns) = parts.pop_front() else {
            return None;
        };
        // Parse to correct data-types
        let Ok(time_stamp_in_ns) = str_time_stamp_in_ns.parse::<u64>() else {
            return None;
        };
        Some(time_stamp_in_ns)
    }

    fn attempt_to_create_db_connection(
        db_path: &str,
        storage_path: Option<&str>,
    ) -> Result<Connection, DatabaseError> {
        let connection =
            Connection::open(db_path).map_err(DatabaseError::FileMetadataStorageError)?;
        connection
            .execute_batch(
                "
              PRAGMA journal_mode = OFF;
              PRAGMA synchronous = FULL;
              PRAGMA cache_size = 1000000;
              PRAGMA locking_mode = EXCLUSIVE;
              PRAGMA temp_store = MEMORY;",
            )
            .map_err(DatabaseError::FileMetadataStorageError)?;

        connection
            .execute("CREATE TABLE IF NOT EXISTS media (timestamp INTEGER PRIMARY KEY)", [])
            .map_err(DatabaseError::FileMetadataStorageError)?;

        // Confirm healthy database connection if path is set.
        if let Some(storage_path) = storage_path {
            Self::test_database_health(storage_path, &connection)?;
        };

        Ok(connection)
    }

    /// Save media to database.  Copies the content of the media file.
    /// TODO: eventually, we will want to package media into a zip file instead of a string
    pub(crate) fn save_media(&mut self, media: String) -> Result<usize, DatabaseError> {
        let mut media_as_json: Value = serde_json::from_str(&media.clone()).unwrap();
        let media_start_time = media_as_json[TIMESTAMP_KEY].as_u64().unwrap();

        self.write_serialized_media_to_fs(media_start_time, media)?;

        let result = self
            .db_connection
            .execute("INSERT INTO media (timestamp) VALUES (?)", params![media_start_time])
            .map_err(DatabaseError::FileMetadataStorageError)?;

        // Delete fragments if too much data is stored.
        self.ensure_local_storage_within_limits()?;
        Ok(result)
    }

    pub(crate) fn query_media_in_time_range(
        &mut self,
        start_time: u64,
        end_time: u64,
        limit: usize,
    ) -> Result<Vec<String>, DatabaseError> {
        // Query database for metadata of existing media.
        let start_times =
            self.query_for_n_media_start_times_in_time_range(start_time, end_time, limit)?;
        let mut medias = Vec::new();
        for start_time in start_times.iter() {
            medias.push(self.read_serialized_media_from_fs(*start_time)?);
        }
        Ok(medias)
    }

    pub(crate) fn query_for_n_media_start_times_in_time_range(
        &self,
        start_time: u64,
        end_time: u64,
        limit: usize,
    ) -> Result<Vec<u64>, DatabaseError> {
        let query =
            "SELECT timestamp FROM media WHERE timestamp >= ? AND timestamp < ? ORDER BY timestamp LIMIT ?";
        // Execute the query and fetch the media matching timestamp.
        let mut statement =
            self.db_connection.prepare(query).map_err(DatabaseError::FileMetadataStorageError)?;

        let mut rows = statement
            .query(params![start_time, end_time, limit])
            .map_err(DatabaseError::FileMetadataStorageError)?;

        let mut start_times = Vec::new();
        while let Some(row) = rows.next().map_err(DatabaseError::FileMetadataStorageError)? {
            let start_time = row.get(0).map_err(DatabaseError::FileMetadataStorageError)?;
            start_times.push(start_time);
        }
        Ok(start_times)
    }

    fn read_serialized_media_from_fs(&self, start_time: u64) -> std::io::Result<String> {
        let media_path = self.get_media_full_path(start_time);
        read_to_string(media_path)
    }

    /// Saves media to FS.  If successful returns the path to the file for storage in DB.
    fn write_serialized_media_to_fs(
        &mut self,
        start_time: u64,
        data: String,
    ) -> Result<String, std::io::Error> {
        let media_size = data.len() as u64;
        let media_path = self.get_media_full_path(start_time);
        write(media_path.as_str(), data)?;
        // Update current usage after write succeeds.
        self.current_storage_disk_usage_bytes += media_size;

        Ok(media_path)
    }

    fn restore_database_from_media(
        storage_path: &str,
        db_connection: &mut Connection,
    ) -> Result<(), DatabaseError> {
        info!("Restoring database from media.");

        let files =
            std::fs::read_dir(storage_path).map_err(DatabaseError::FileSystemStorageError)?;
        // Iterate over the files in the directory.  Pull metadata from the file name.
        for file_result in files {
            let dir_entry = file_result.map_err(DatabaseError::FileSystemStorageError)?;
            // We ignore all directory types other than fragment files.
            if let Some((start_timestamp)) = Self::get_metadata_from_result(dir_entry) {
                // Insert metadata from file into database.
                db_connection
                    .execute("INSERT INTO media (timestamp) VALUES (?)", params![start_timestamp])
                    .map_err(DatabaseError::FileMetadataStorageError)?;
            }
        }
        Ok(())
    }

    /// This will delete media from and including start_time
    pub(crate) fn delete_media(&mut self, start_time: u64) -> Result<usize, DatabaseError> {
        // Delete fragment from FS.
        self.delete_media_from_fs(start_time)?;
        // Delete information from DB.
        let result = self
            .db_connection
            .execute("DELETE from media WHERE timestamp == ?", params![start_time])
            .map_err(DatabaseError::FileMetadataStorageError)?;

        Ok(result)
    }

    fn delete_media_from_fs(&mut self, start_time: u64) -> Result<(), DatabaseError> {
        let media_path = self.get_media_full_path(start_time);
        // Update delete + update current disk use.
        let media_size = get_size(&media_path).map_err(DatabaseError::FileSystemReadError)?;
        remove_file(media_path).map_err(DatabaseError::FileSystemStorageError)?;
        self.current_storage_disk_usage_bytes -= media_size;

        Ok(())
    }

    fn get_media_full_path(&self, start_time: u64) -> String {
        let directory = self.directory.as_str();
        Self::create_media_from_fragment_directory(directory, start_time)
    }
    #[inline]
    fn create_media_from_fragment_directory(directory: &str, start_time: u64) -> String {
        // pulled out for testing.
        format!("{directory}/{MEDIA_FILE_NAME}_{start_time}")
    }

    fn test_database_health(
        storage_path: &str,
        connection: &Connection,
    ) -> Result<(), DatabaseError> {
        // Temporary solution. If media exist but database query is empty throw error.
        let query =
            "SELECT timestamp FROM media WHERE timestamp >= ? AND timestamp < ? ORDER BY timestamp LIMIT ?";

        // Execute the query and fetch the media matching timestamp.
        let mut statement =
            connection.prepare(query).map_err(DatabaseError::FileMetadataStorageError)?;

        let mut rows = statement
            .query(params![0_u64, MAX_TIME_FOR_DB, 5])
            .map_err(DatabaseError::FileMetadataStorageError)?;
        // If we get a row then the database is healthy.
        if rows.next().map_err(DatabaseError::FileMetadataStorageError)?.is_some() {
            return Ok(());
        }

        // If empty we need to check if there are media, if none we assume health.
        let files =
            std::fs::read_dir(storage_path).map_err(DatabaseError::FileSystemStorageError)?;

        // Iterate over the files in the directory.  Pull metadata from the file name.
        for file_result in files {
            let dir_entry = file_result.map_err(DatabaseError::FileSystemStorageError)?;
            if let Some(_) = Self::get_metadata_from_result(dir_entry) {
                return Err(DatabaseError::FileMetadataStorageDatabaseCorrupted);
            }
        }

        Ok(())
    }
    // // If current usage is over the limit.  Delete until within bounds.
    fn ensure_local_storage_within_limits(&mut self) -> Result<(), DatabaseError> {
        // Do nothing if data is within limits.
        if self.data_within_limits() {
            return Ok(());
        }
        // Query database for N fragments, Fragments should be of similar size but not exact.  So may need to delete multiple.
        let mut media_start_times = self.query_for_n_media_start_times_in_time_range(
            0,
            MAX_TIME_FOR_DB,
            QUERY_SIZE_FOR_DELETE as usize,
        )?;

        for media_start_time in media_start_times.iter() {
            self.delete_media(*media_start_time)?;
            if self.data_within_limits() {
                return Ok(());
            }
        }
        debug!("Recursive call on ensure_local_storage_within_limits, check query size");
        self.ensure_local_storage_within_limits()
    }

    fn data_within_limits(&self) -> bool {
        // If max is greater than or equal then data is within limits
        self.local_storage_disk_usage_max_bytes.ge(&self.current_storage_disk_usage_bytes)
    }
}
