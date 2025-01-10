use crate::data_storage::constants::{
    DB_FILE_NAME, DB_JOURNAL_NAME, FRAGMENT_BASE_NAME, MAX_TIME_FOR_DB, QUERY_SIZE_FOR_DELETE,
};
use crate::data_storage::error::DatabaseError;
use crate::hybrid_streaming_service::fragment::VideoFragmentInformation;
use crate::util::{FragmentDurationInNs, FragmentTimeInNs};
use bincode::{deserialize, serialize};
use fs_extra::dir::get_size;
use rusqlite::{params, Connection};
use std::collections::vec_deque::VecDeque;
use std::fs::{read, remove_file, write, DirEntry};
use tracing::{debug, error, info};

/// Struct to persist frame metadata on Local Storage.
#[derive(Debug)]
pub(crate) struct FileMetadataStorage {
    /// SQLite DB file path.
    #[allow(unused)]
    pub(crate) db_path: String,
    pub(crate) fragment_directory: String,
    /// Connection instance.
    pub(crate) db_connection: Connection,
    /// Maximum storage of disk space, in Bytes
    pub(crate) local_storage_disk_usage_max_bytes: u64,
    /// Current usage in Bytes
    pub(crate) current_storage_disk_usage_bytes: u64,
}

impl FileMetadataStorage {
    /// Instantiate FileMetadataStorage.  Storage path is the path to the directory where DB + fragments will be stored.
    pub(crate) fn new_connection(
        storage_path: &str,
        local_storage_disk_usage_max_in_mb: u64,
        db_path: Option<String>,
    ) -> Result<FileMetadataStorage, DatabaseError> {
        // SQLite database can be set separately or default to the storage path.
        let db_dir_path = db_path.unwrap_or(storage_path.to_string());
        let db_path = db_dir_path.to_owned() + "/" + DB_FILE_NAME;
        let db_journal_path = db_dir_path + "/" + DB_JOURNAL_NAME;

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

                    // recreate + restore from fragments.  None means will not test db, assume good since fresh.
                    let mut connection =
                        Self::attempt_to_create_db_connection(db_path.as_str(), None)?;
                    // Restore database from fragment files.
                    Self::restore_database_from_fragments(storage_path, &mut connection)?;
                    connection
                }
            };

        //https://crates.io/crates/fs_extra (Pretty simple helper, recursively queries the FS)
        let current_storage_disk_usage_bytes =
            get_size(storage_path).map_err(DatabaseError::FileSystemReadError)?;
        // Convert from MB to bytes.
        let local_storage_disk_usage_max_bytes = local_storage_disk_usage_max_in_mb * 1_000_000_u64;

        Ok(FileMetadataStorage {
            db_path,
            fragment_directory: storage_path.to_owned(),
            db_connection: connection,
            local_storage_disk_usage_max_bytes,
            current_storage_disk_usage_bytes,
        })
    }

    fn restore_database_from_fragments(
        storage_path: &str,
        db_connection: &mut Connection,
    ) -> Result<(), DatabaseError> {
        info!("Restoring database from fragments.");

        let files =
            std::fs::read_dir(storage_path).map_err(DatabaseError::FileSystemStorageError)?;
        // Iterate over the files in the directory.  Pull metadata from the file name.
        for file_result in files {
            let dir_entry = file_result.map_err(DatabaseError::FileSystemStorageError)?;
            // We ignore all directory types other than fragment files.
            if let Some((fragment_start_timestamp, fragment_duration)) =
                Self::get_metadata_from_result(dir_entry)
            {
                // Insert metadata from file into database.
                db_connection
                    .execute(
                        "INSERT INTO metadata (timestamp, duration) VALUES (?, ?)",
                        params![fragment_start_timestamp, fragment_duration],
                    )
                    .map_err(DatabaseError::FileMetadataStorageError)?;
            }
        }
        Ok(())
    }

    fn get_metadata_from_result(
        dir_entry: DirEntry,
    ) -> Option<(FragmentTimeInNs, FragmentDurationInNs)> {
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

        // Format of fragment files {FRAGMENT_BASE_NAME}_{start_time}_{duration}
        if !file_name.contains(FRAGMENT_BASE_NAME) {
            return None;
        };
        // Confirmed file name.  So pull timestamp + duration from it.
        Self::pull_data_from_fragment_filename(file_name)
    }

    fn pull_data_from_fragment_filename(
        fragment_file_name: &str,
    ) -> Option<(FragmentTimeInNs, FragmentDurationInNs)> {
        let mut parts: VecDeque<&str> = fragment_file_name.split('_').collect();
        // Should have exactly 3 parts
        // First part should be FRAGMENT_BASE_NAME
        let Some(first_part) = parts.pop_front() else {
            return None;
        };
        // Check to confirm expected pattern, Should have exactly 2 parts after popping one.
        if !first_part.eq(FRAGMENT_BASE_NAME) && parts.len().eq(&2) {
            error!("Invalid fragment file detected.");
            return None;
        }
        // Pull &str from vector.
        let Some(str_time_stamp_in_ns) = parts.pop_front() else {
            return None;
        };
        let Some(str_duration_in_ns) = parts.pop_front() else {
            return None;
        };
        // Parse to correct data-types
        let Ok(time_stamp_in_ns) = str_time_stamp_in_ns.parse::<FragmentTimeInNs>() else {
            return None;
        };
        let Ok(duration_in_ns) = str_duration_in_ns.parse::<FragmentDurationInNs>() else {
            return None;
        };

        Some((time_stamp_in_ns, duration_in_ns))
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

        connection.execute(
            "CREATE TABLE IF NOT EXISTS metadata (timestamp INTEGER PRIMARY KEY, duration INTEGER)",
            [],
        ).map_err(DatabaseError::FileMetadataStorageError)?;

        // Confirm healthy database connection if path is set.
        if let Some(storage_path) = storage_path {
            Self::test_database_health(storage_path, &connection)?;
        };
        Ok(connection)
    }

    /// Save fragment to database.  Copies the content of the fragment.
    pub(crate) fn save_fragment(
        &mut self,
        fragment: &VideoFragmentInformation,
    ) -> Result<usize, DatabaseError> {
        let fragment_duration: FragmentDurationInNs = fragment.duration;
        let fragment_start_timestamp: FragmentTimeInNs = fragment.start_of_fragment_timestamp;

        let serialized_fragment =
            serialize(&fragment).map_err(DatabaseError::SerializationError)?;

        self.write_serialized_fragment_to_fs(
            fragment_start_timestamp,
            fragment_duration,
            serialized_fragment,
        )?;

        let result = self
            .db_connection
            .execute(
                "INSERT INTO metadata (timestamp, duration) VALUES (?, ?)",
                params![fragment_start_timestamp, fragment_duration],
            )
            .map_err(DatabaseError::FileMetadataStorageError)?;

        // Delete fragments if too much data is stored.
        self.ensure_local_storage_within_limits()?;

        Ok(result)
    }

    /// This will delete fragments from and including start_time to and not including end_time
    pub(crate) fn delete_fragment(
        &mut self,
        start_time: FragmentTimeInNs,
        duration: FragmentDurationInNs,
    ) -> Result<usize, DatabaseError> {
        // Delete fragment from FS.
        self.delete_fragment_from_fs(start_time, duration)?;
        // Delete information from DB.
        let result = self
            .db_connection
            .execute("DELETE from metadata WHERE timestamp == ?", params![start_time])
            .map_err(DatabaseError::FileMetadataStorageError)?;

        Ok(result)
    }

    /// Fetch fragments from local storage in range of (start_time, end_time) limit by n and order by timestamp.
    pub(crate) fn query_n_fragments_in_time_range(
        &mut self,
        start_time: FragmentTimeInNs,
        end_time: FragmentTimeInNs,
        limit: u64,
    ) -> Result<VecDeque<VideoFragmentInformation>, DatabaseError> {
        // Query database for metadata of existing fragments.
        let metadata =
            self.query_metadata_for_n_fragments_in_time_range(start_time, end_time, limit)?;
        // We can create vector with known capacity.
        let mut fragments: VecDeque<VideoFragmentInformation> =
            VecDeque::with_capacity(metadata.len());

        for (start_time_in_ns, duration_in_ns) in metadata.iter() {
            let serialized_fragment =
                self.read_serialized_fragment_from_fs(*start_time_in_ns, *duration_in_ns)?;
            // Very unlikely to fail.  Can only happen if write occurs during ungraceful shutdown. Delete fragment
            // from database + log data loss.
            let deserialized_fragment_result =
                deserialize::<VideoFragmentInformation>(&serialized_fragment)
                    .map_err(DatabaseError::SerializationError);

            let Ok(deserialized_fragment) = deserialized_fragment_result else {
                error!("Fragment corrupted at timestamp : {}", start_time_in_ns);
                self.delete_fragment(*start_time_in_ns, *duration_in_ns)?;
                continue;
            };
            fragments.push_back(deserialized_fragment);
        }
        Ok(fragments)
    }

    /// Fetch timeline metadata for fragments in storage.  This is used for timeline generation.
    /// Will return sorted fragment data in ascending order (earliest to latest)
    pub(crate) fn query_metadata_for_n_fragments_in_time_range(
        &self,
        start_time: FragmentTimeInNs,
        end_time: FragmentTimeInNs,
        limit: u64,
    ) -> Result<Vec<(FragmentTimeInNs, FragmentDurationInNs)>, DatabaseError> {
        let query =
            "SELECT timestamp, duration FROM metadata WHERE timestamp >= ? AND timestamp < ? ORDER BY timestamp LIMIT ?";

        // Execute the query and fetch the fragments matching timestamp.
        let mut statement =
            self.db_connection.prepare(query).map_err(DatabaseError::FileMetadataStorageError)?;

        let mut rows = statement
            .query(params![start_time, end_time, limit])
            .map_err(DatabaseError::FileMetadataStorageError)?;

        let mut metadata = Vec::new();
        while let Some(row) = rows.next().map_err(DatabaseError::FileMetadataStorageError)? {
            let start_time = row.get(0).map_err(DatabaseError::FileMetadataStorageError)?;
            let duration = row.get(1).map_err(DatabaseError::FileMetadataStorageError)?;
            metadata.push((start_time, duration));
        }
        Ok(metadata)
    }
    /// Saves fragment to FS.  If successful returns the path to the file for storage in DB.
    fn write_serialized_fragment_to_fs(
        &mut self,
        start_time: FragmentTimeInNs,
        duration: FragmentDurationInNs,
        fragment: Vec<u8>,
    ) -> Result<String, std::io::Error> {
        let fragment_size = fragment.len() as u64;
        let fragment_path = self.get_fragment_full_path(start_time, duration);
        write(fragment_path.as_str(), fragment)?;
        // Update current usage after write succeeds.
        self.current_storage_disk_usage_bytes += fragment_size;

        Ok(fragment_path)
    }

    fn read_serialized_fragment_from_fs(
        &self,
        start_time: FragmentTimeInNs,
        duration: FragmentDurationInNs,
    ) -> Result<Vec<u8>, std::io::Error> {
        let fragment_path = self.get_fragment_full_path(start_time, duration);
        read(fragment_path)
    }

    fn delete_fragment_from_fs(
        &mut self,
        start_time: FragmentTimeInNs,
        duration: FragmentDurationInNs,
    ) -> Result<(), DatabaseError> {
        let fragment_path = self.get_fragment_full_path(start_time, duration);
        // Update delete + update current disk use.
        let fragment_size = get_size(&fragment_path).map_err(DatabaseError::FileSystemReadError)?;
        remove_file(fragment_path).map_err(DatabaseError::FileSystemStorageError)?;
        self.current_storage_disk_usage_bytes -= fragment_size;

        Ok(())
    }

    fn get_fragment_full_path(
        &self,
        start_time: FragmentTimeInNs,
        duration: FragmentDurationInNs,
    ) -> String {
        let directory = self.fragment_directory.as_str();
        Self::create_path_from_fragment_directory(directory, start_time, duration)
    }
    #[inline]
    fn create_path_from_fragment_directory(
        directory: &str,
        start_time: FragmentTimeInNs,
        duration: FragmentDurationInNs,
    ) -> String {
        // pulled out for testing.
        format!("{directory}/{FRAGMENT_BASE_NAME}_{start_time}_{duration}")
    }

    fn test_database_health(
        storage_path: &str,
        connection: &Connection,
    ) -> Result<(), DatabaseError> {
        // If fragments exist but database query is empty throw error.
        let query =
            "SELECT timestamp, duration FROM metadata WHERE timestamp >= ? AND timestamp < ? ORDER BY timestamp LIMIT ?";

        // Execute the query and fetch the fragments matching timestamp.
        let mut statement =
            connection.prepare(query).map_err(DatabaseError::FileMetadataStorageError)?;

        let mut rows = statement
            .query(params![0_u64, MAX_TIME_FOR_DB, 1])
            .map_err(DatabaseError::FileMetadataStorageError)?;

        // If we get a row then the database is healthy.
        if rows.next().map_err(DatabaseError::FileMetadataStorageError)?.is_some() {
            return Ok(());
        }

        // If empty we need to check if there are fragments, if none we assume health.
        let files =
            std::fs::read_dir(storage_path).map_err(DatabaseError::FileSystemStorageError)?;

        // Iterate over the files in the directory.  Pull metadata from the file name.
        for file_result in files {
            let dir_entry = file_result.map_err(DatabaseError::FileSystemStorageError)?;
            if let Some((_, _)) = Self::get_metadata_from_result(dir_entry) {
                return Err(DatabaseError::FileMetadataStorageDatabaseCorrupted);
            }
        }

        Ok(())
    }
    // If current usage is over the limit.  Delete until within bounds.
    fn ensure_local_storage_within_limits(&mut self) -> Result<(), DatabaseError> {
        // Do nothing if data is within limits.
        if self.data_within_limits() {
            return Ok(());
        }
        // Query database for N fragments, Fragments should be of similar size but not exact.  So may need to delete multiple.
        let oldest_fragments_metadata = self.query_metadata_for_n_fragments_in_time_range(
            0,
            MAX_TIME_FOR_DB,
            QUERY_SIZE_FOR_DELETE,
        )?;
        // NOTE: Can optimize by reading FS metadata and batch deleting.
        for (fragment_time, fragment_duration) in oldest_fragments_metadata.iter() {
            self.delete_fragment(*fragment_time, *fragment_duration)?;
            if self.data_within_limits() {
                return Ok(());
            }
        }
        // Recursive call.  Future fragments may be small due to cloud forwarding rules.
        debug!("Recursive call on ensure_local_storage_within_limits, check query size");
        self.ensure_local_storage_within_limits()
    }

    fn data_within_limits(&self) -> bool {
        // If max is greater than or equal then data is within limits
        self.local_storage_disk_usage_max_bytes.ge(&self.current_storage_disk_usage_bytes)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::data_storage::constants::MAX_TIME_FOR_DB;
    use crate::hybrid_streaming_service::frame::Frame;
    use std::fs::{create_dir, remove_dir_all};
    use std::sync::Arc;

    const START_OF_FRAGMENT: FragmentTimeInNs = 100;
    const CORRUPTED_FRAGMENT_UINT: u64 = 123456789;
    const TEST_NUMBER_OF_FRAGMENTS: u64 = 5;
    const LOCAL_STORAGE_MAX: u64 = 1000000;
    const SMALL_LOCAL_STORAGE: u64 = 1;
    const DEFAULT_DURATION: u64 = 100;
    const CIRCULAR_BUFFER_FRAGMENTS_COUNT: u64 = 100;

    #[test]
    fn test_file_storage() {
        // Create a unique directory for the test, use test method name.
        let directory_for_tests = TestDirectoryHelper::get_test_directory_path("test_file_storage");
        // create database connection
        let mut file_metadata_storage = FileMetadataStorage::new_connection(
            directory_for_tests.get_dir_path().as_str(),
            LOCAL_STORAGE_MAX,
            None,
        )
        .unwrap();

        let test_fragments = create_fragments(TEST_NUMBER_OF_FRAGMENTS, None);

        // Save fragment
        for fragment in test_fragments.iter() {
            file_metadata_storage.save_fragment(fragment).expect("Error in saving fragment");
        }
        // Query for fragment
        let fragments = file_metadata_storage
            .query_n_fragments_in_time_range(
                START_OF_FRAGMENT,
                MAX_TIME_FOR_DB,
                TEST_NUMBER_OF_FRAGMENTS,
            )
            .expect("Error in search query!");

        // Fragments should be sorted from earliest to latest (how test_fragments were generated)
        // All fragments should be in the query.
        assert_eq!(test_fragments, fragments);

        // Delete all fragments
        let metadata = file_metadata_storage
            .query_metadata_for_n_fragments_in_time_range(
                START_OF_FRAGMENT,
                MAX_TIME_FOR_DB,
                TEST_NUMBER_OF_FRAGMENTS,
            )
            .expect("Error in search query!");

        for (start_time, duration) in metadata.iter() {
            file_metadata_storage
                .delete_fragment(*start_time, *duration)
                .expect("Failed to delete.");
        }

        // Confirm fragments have been deleted, will return an empty vector.
        let fragments = file_metadata_storage
            .query_n_fragments_in_time_range(
                START_OF_FRAGMENT,
                MAX_TIME_FOR_DB,
                TEST_NUMBER_OF_FRAGMENTS,
            )
            .expect("Error in search query!");
        assert!(fragments.is_empty());
    }
    #[test]
    fn test_database_recovery() {
        // Create a unique directory for the test, use test method name.
        let directory_for_tests =
            TestDirectoryHelper::get_test_directory_path("test_database_recovery");
        // Create fragments for test + store them directly in the database.
        let test_fragments = create_fragments(TEST_NUMBER_OF_FRAGMENTS, None);
        // Save fragments to test directory.
        serialize_and_store_fragments_for_test(directory_for_tests.get_dir_path(), &test_fragments);
        // Store a corrupted fragments.  This will force the database to handle the case of a corrupted fragment.
        // Save simulated corrupted fragment.  (Just string this will not deserialize)
        let corrupted_fragment_path = FileMetadataStorage::create_path_from_fragment_directory(
            directory_for_tests.get_dir_path().as_str(),
            CORRUPTED_FRAGMENT_UINT,
            CORRUPTED_FRAGMENT_UINT,
        );
        // Create fragment file without using SQLite database.
        write(corrupted_fragment_path.as_str(), "Just a string, content does not matter.").unwrap();

        // Create an invalid database file. The class should be able to recover from this.
        let db_path = directory_for_tests.get_dir_path() + "/" + DB_FILE_NAME;
        write(db_path, "Invalid Database!").unwrap();
        // Create database client, should be able to use database as normal after this.
        let mut file_metadata_storage = FileMetadataStorage::new_connection(
            directory_for_tests.get_dir_path().as_str(),
            LOCAL_STORAGE_MAX,
            Some(directory_for_tests.get_dir_path()),
        )
        .unwrap();
        // Should be able to query for fragments.
        let fragments = file_metadata_storage
            .query_n_fragments_in_time_range(
                START_OF_FRAGMENT,
                MAX_TIME_FOR_DB,
                TEST_NUMBER_OF_FRAGMENTS + 1,
            )
            .expect("Error in search query!");

        assert!(!fragments.is_empty());
        assert_eq!(test_fragments, fragments);
    }

    /// Test circular buffer feature.  We should be able to push fragments into the database and the oldest
    /// fragments should be deleted as needed.
    #[test]
    fn test_circular_buffer() {
        // Create a unique directory for the test, use test method name.
        let directory_for_tests =
            TestDirectoryHelper::get_test_directory_path("test_circular_buffer");
        // Create fragments for test + store them directly in the database.
        let mut test_fragments = create_fragments(CIRCULAR_BUFFER_FRAGMENTS_COUNT, None);

        // create db with low memory utilization.  Push too many into the db and confirm they are deleting.
        // create database connection
        let mut file_metadata_storage = FileMetadataStorage::new_connection(
            directory_for_tests.get_dir_path().as_str(),
            LOCAL_STORAGE_MAX,
            None,
        )
        .unwrap();
        // Push data to database.  Make sure it is over the threshold of 1 Mb set by this test.
        for fragment in test_fragments.iter() {
            let _ = file_metadata_storage.save_fragment(fragment).unwrap();
        }

        // Query for all fragments in the database.  Should exactly match the last fragments we inserted.
        // This checks that the deletes are eliminating the oldest fragments.
        let mut fragments = file_metadata_storage
            .query_n_fragments_in_time_range(0, MAX_TIME_FOR_DB, CIRCULAR_BUFFER_FRAGMENTS_COUNT)
            .expect("Error in search query!");

        assert_eq!(fragments.pop_front(), test_fragments.pop_front());
    }

    fn create_fragments(
        number_of_fragments: u64,
        fragment_duration: Option<u64>,
    ) -> VecDeque<VideoFragmentInformation> {
        let mut fragments = VecDeque::new();
        let fragment_duration = fragment_duration.unwrap_or(DEFAULT_DURATION);
        for i in 0..number_of_fragments {
            let start = START_OF_FRAGMENT + fragment_duration * i;
            let fragment = VideoFragmentInformation {
                start_of_fragment_timestamp: start,
                frame_list: create_frames(start),
                duration: 4_u64,
            };
            fragments.push_back(fragment);
        }
        fragments
    }

    fn serialize_and_store_fragments_for_test(
        directory_for_test: String,
        fragments: &VecDeque<VideoFragmentInformation>,
    ) {
        for fragment in fragments.iter() {
            let fragment_path = FileMetadataStorage::create_path_from_fragment_directory(
                directory_for_test.as_str(),
                fragment.start_of_fragment_timestamp,
                fragment.duration,
            );

            // Serialize fragment to byte array.
            let serialized_fragment =
                serialize(&fragment).map_err(DatabaseError::SerializationError).unwrap();

            // Create fragment file without using SQLite database.
            write(fragment_path.as_str(), serialized_fragment).unwrap();
        }
    }

    fn create_frames(start: u64) -> Vec<Arc<Frame>> {
        let data: Vec<u8> = (0..255).collect();
        let mut frames: Vec<Arc<Frame>> = Vec::new();

        for i in 0..60 {
            let frame = Frame {
                is_key_frame: i == 0,
                time_stamp_ns: start + i,
                data: data.clone(),
                duration: 0,
                buffer_flags: 1_u32,
            };
            frames.push(Arc::new(frame));
        }
        frames
    }
    /// Add a unique directory name per test. This will create the directory within the CARGO_MANIFEST_DIR/src/data_storage
    /// directory to store database + fragments. for the test.  Note re-using directory names between tests will cause failures.
    /// When this drops it will automatically delete the directory.
    struct TestDirectoryHelper {
        full_path_test_directory: String,
    }

    impl TestDirectoryHelper {
        fn get_test_directory_path(test_directory: &str) -> Self {
            // create a directory
            let full_path_test_directory =
                env!("CARGO_MANIFEST_DIR").to_string() + "/src/data_storage/" + test_directory;
            create_dir(full_path_test_directory.clone()).unwrap();
            TestDirectoryHelper { full_path_test_directory }
        }

        fn get_dir_path(&self) -> String {
            self.full_path_test_directory.clone()
        }
    }
    /// This will cleanup file when Database object is dropped for unit tests.  This is in the tests module so this
    /// will not be implemented in production software.  This will get called even if tests fail.
    impl Drop for TestDirectoryHelper {
        fn drop(&mut self) {
            // Delete directory made for test and all internal files.
            // Same as rm -r <directory_path>
            remove_dir_all(self.full_path_test_directory.clone()).unwrap();
        }
    }
}
