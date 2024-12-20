// GRCOV_STOP_COVERAGE
//! This service will hold the logic to read fragments from database + send them to the offline pipeline
//! When fragments are successfully published to KVS this service will delete them.

use crate::constants::MAX_FRAGMENTS;
#[cfg(feature = "sd-card-catchup")]
use crate::constants::{
    CATCHUP_BUFFER_SIZE, CATCHUP_KVS_SLEEP_MS, CATCHUP_SLEEP_MS, ROUTE_VIDEO_SD,
};
use crate::data_storage::constants::MAX_TIME_FOR_DB;
use crate::data_storage::error::DatabaseError;
use crate::data_storage::video_storage::FileMetadataStorage;
use crate::hybrid_streaming_service::fragment::VideoFragmentInformation;
use crate::hybrid_streaming_service::frame::Frame;
use crate::hybrid_streaming_service::kvs_callbacks::fragment_ack::{
    get_kvs_fragment_rx_channel_for_offline, OfflineTimeStampMs,
};
use crate::hybrid_streaming_service::message_service::GST_CAPS_FOR_KVS;
use crate::util::convert_ns_to_ms;
use crossbeam_channel::{Receiver, RecvTimeoutError};
use std::collections::{BTreeMap, VecDeque};
use std::env;
use std::ops::Bound::Included;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::mpsc::{SyncSender, TrySendError};
use std::sync::{Arc, Mutex};
use std::thread::JoinHandle;
use std::time::Duration;
use tracing::{debug, error, info};

/// Holds the logic to periodically query the database + push stored fragments to the offline client.
#[derive(Debug)]
pub(crate) struct CatchupVideoService {
    // Main event loop, handle held in struct to be checked by watchdog thread.
    _event_loop: JoinHandle<()>,
    //
    _callback_loop: JoinHandle<()>,
    // Database client
    _database_client: Arc<Mutex<FileMetadataStorage>>,
    // Used to cancel threads when struct is dropped.
    cancellation_token: Arc<AtomicBool>,
    _catchup_fragment_metadata_manager: CatchupFragmentMetadataManager,
}

impl CatchupVideoService {
    pub fn new(
        offline_tx: SyncSender<Arc<Frame>>,
        database_client: Arc<Mutex<FileMetadataStorage>>,
    ) -> CatchupVideoService {
        let max_number_of_fragments_in_memory: u64 =
            env::var(CATCHUP_BUFFER_SIZE).unwrap_or_default().parse().unwrap_or(MAX_FRAGMENTS);

        let cancellation_token = Arc::new(AtomicBool::new(false));
        let offline_fragment_ack_rx = get_kvs_fragment_rx_channel_for_offline();
        let mut _catchup_fragment_metadata_manager =
            CatchupFragmentMetadataManager::new(max_number_of_fragments_in_memory);
        let _event_loop = Self::setup_event_loop(
            offline_tx,
            database_client.clone(),
            cancellation_token.clone(),
            _catchup_fragment_metadata_manager.clone(),
        );
        let _callback_loop = Self::setup_callback_loop(
            database_client.clone(),
            cancellation_token.clone(),
            offline_fragment_ack_rx,
            _catchup_fragment_metadata_manager.clone(),
        );

        CatchupVideoService {
            _event_loop,
            _callback_loop,
            cancellation_token,
            _database_client: database_client,
            _catchup_fragment_metadata_manager,
        }
    }
    /// This thread handles kvs offline callbacks for fragments which persist in the cloud.
    fn setup_callback_loop(
        database_client: Arc<Mutex<FileMetadataStorage>>,
        cancellation_token: Arc<AtomicBool>,
        callback_rx: Receiver<OfflineTimeStampMs>,
        mut catchup_fragment_metadata_manager: CatchupFragmentMetadataManager,
    ) -> JoinHandle<()> {
        std::thread::spawn(move || {
            while !cancellation_token.load(Ordering::Relaxed) {
                // time-out used so loop checks cancellation token.
                let ack_time_in_ms = match callback_rx.recv_timeout(Duration::from_secs(1)) {
                    Ok(ack_time_in_ms) => ack_time_in_ms,
                    Err(RecvTimeoutError::Timeout) => {
                        debug!("Offline callback loop timeout");
                        continue;
                    }
                    Err(RecvTimeoutError::Disconnected) => {
                        error!("Offline client callback channel closed unexpectedly.");
                        break;
                    }
                };

                info!("Catchup successfully persisted fragment is timestamp : {}.", ack_time_in_ms);
                // This would mean a double callback for a fragment which can happen.
                let Some((fragment_time_in_ns, fragment_duration)) =
                    catchup_fragment_metadata_manager.pull_fragment_data_from_map(ack_time_in_ms)
                else {
                    info!(
                        "Could not find fragment in callback manager time in ms : {}",
                        ack_time_in_ms
                    );
                    continue;
                };

                info!("Persisted fragment time in ns {}", fragment_time_in_ns);

                // Delete from database
                if let Err(e) = Self::delete_fragment(
                    database_client.clone(),
                    fragment_time_in_ns,
                    fragment_duration,
                ) {
                    error!("Failed to delete fragment from database! : {:?}", e);
                }
            }
        })
    }

    fn setup_event_loop(
        offline_tx: SyncSender<Arc<Frame>>,
        database_client: Arc<Mutex<FileMetadataStorage>>,
        cancellation_token: Arc<AtomicBool>,
        mut catchup_fragment_metadata_manager: CatchupFragmentMetadataManager,
    ) -> JoinHandle<()> {
        std::thread::spawn(move || {
            // Dev Feature to not attempt to push data to the cloud.,
            // frames will not publish offline client as if internet not connected.
            let no_streaming_flag =
                std::env::var(ROUTE_VIDEO_SD).unwrap_or("FALSE".to_string()).eq("TRUE");

            let mut last_gotten_fragment_timestamp: u64 = 0;
            while !cancellation_token.load(Ordering::Relaxed) {
                // Block until pipelines are setup
                if !Self::caps_arc_set() {
                    debug!("Wait for Caps to be set in pipelines.");
                    std::thread::sleep(Duration::from_millis(CATCHUP_SLEEP_MS));
                    continue;
                }
                // Used to limit the number of fragments in memory.
                let allowed_num_of_fragments_in_memory =
                    catchup_fragment_metadata_manager.get_empty_slots();

                if allowed_num_of_fragments_in_memory.eq(&0) {
                    debug!("Fragment data manager full.");
                    std::thread::sleep(Duration::from_millis(CATCHUP_SLEEP_MS));
                    continue;
                }

                let mut fragments = match Self::query_db_for_fragments(
                    database_client.clone(),
                    0_u64,
                    allowed_num_of_fragments_in_memory,
                ) {
                    Ok(fragments) => fragments,
                    Err(e) => {
                        error!("Database error on query for catchup service : {:?}", e);
                        continue;
                    }
                };

                debug!("Number of fragments retrieved {:?}", fragments.len());

                // If there are no fragments sleep to let more be written to database.
                if fragments.is_empty() {
                    // If no remaining fragments in the database then sleep and wait for more data.
                    info!("No fragments in the database, sleeping for {}", CATCHUP_SLEEP_MS);
                    std::thread::sleep(Duration::from_millis(CATCHUP_SLEEP_MS));
                    continue;
                }

                // Block sending (Dev Mode), put here so this thread will act as if disconnected from the internet.
                if no_streaming_flag {
                    info!("Dev Mode: Streaming disabled do not publish fragments to KVS.");
                    std::thread::sleep(Duration::from_millis(CATCHUP_SLEEP_MS));
                    continue;
                }

                // We don't want to keep sending the same fragments over to the KVS offline plugin
                if !fragments.is_empty() {
                    if last_gotten_fragment_timestamp
                        .eq(&fragments.get(0).unwrap().start_of_fragment_timestamp)
                    {
                        std::thread::sleep(Duration::from_millis(CATCHUP_SLEEP_MS));
                        continue;
                    }
                    last_gotten_fragment_timestamp =
                        fragments.get(0).unwrap().start_of_fragment_timestamp;
                }

                while let Some(fragment) = fragments.pop_front() {
                    // Handle the recoverable errors in the method, panic if channel is closed.
                    if !fragment.is_valid_fragment() {
                        error!("Invalid Fragment found!");
                        continue;
                    }
                    info!("Sending fragment to offline pipeline.");
                    Self::try_send_offline_fragment_and_store_metadata(
                        &offline_tx,
                        fragment,
                        &mut catchup_fragment_metadata_manager,
                    );
                }
            }
        })
    }

    //  By performing locks in a quick function we avoid deadlocks from occurring.
    fn query_db_for_fragments(
        database_client: Arc<Mutex<FileMetadataStorage>>,
        start_time: u64,
        limit: u64,
    ) -> Result<VecDeque<VideoFragmentInformation>, DatabaseError> {
        let mut database_lock = database_client.lock().expect("Database client is poisoned!");
        database_lock.query_n_fragments_in_time_range(start_time, MAX_TIME_FOR_DB, limit)
    }

    fn delete_fragment(
        database_client: Arc<Mutex<FileMetadataStorage>>,
        start_time: u64,
        duration: u64,
    ) -> Result<usize, DatabaseError> {
        let mut database_lock = database_client.lock().expect("Database client is poisoned!");
        info!("Deleted fragment from offline database");
        database_lock.delete_fragment(start_time, duration)
    }

    fn try_send_offline_fragment_and_store_metadata(
        offline_tx: &SyncSender<Arc<Frame>>,
        fragment: VideoFragmentInformation,
        catchup_fragment_metadata_manager: &mut CatchupFragmentMetadataManager,
    ) {
        let time_stamp_in_ns = fragment.start_of_fragment_timestamp;
        let duration_in_ns = fragment.duration;
        for frame in fragment.frame_list {
            match offline_tx.try_send(frame) {
                Ok(_) => {
                    info!("Frame send to offline kvs client.");
                    std::thread::sleep(Duration::from_millis(CATCHUP_KVS_SLEEP_MS));
                    catchup_fragment_metadata_manager
                        .store_fragment_data(time_stamp_in_ns, duration_in_ns)
                }
                // If internet connection slow or disconnected this buffer can fill up
                // This is normal behavior in the real world.
                Err(TrySendError::Full(_)) => {
                    error!("Buffer to offline kvs client is unexpectedly full.");
                }
                // If channel breaks it means the Stream service is being shutdown
                // or an unrecoverable error has occurred.
                Err(TrySendError::Disconnected(_)) => {
                    panic!("KVS offline channel disconnected. Stopping Thread");
                }
            }
        }
    }

    fn caps_arc_set() -> bool {
        // RWLock only blocks if it is being written too.
        let lock = GST_CAPS_FOR_KVS.read().expect("Caps are poisoned.");
        lock.is_some()
    }
}

/// Threads must check this token in their loops.
impl Drop for CatchupVideoService {
    fn drop(&mut self) {
        self.cancellation_token.store(true, Ordering::Relaxed);
    }
}
/// Thread-safe manager between catchup + callback threads.
#[derive(Debug, Clone)]
struct CatchupFragmentMetadataManager {
    metadata_map: Arc<Mutex<BTreeMap<u64, (u64, u64)>>>,
    max_fragments: Arc<u64>,
}

impl CatchupFragmentMetadataManager {
    /// Create a new Fragment manager.  This object is thread safe.
    fn new(max_fragments: u64) -> CatchupFragmentMetadataManager {
        let metadata_map = Arc::new(Mutex::default());
        let max_fragments = Arc::new(max_fragments);
        CatchupFragmentMetadataManager { metadata_map, max_fragments }
    }
    /// Tells how many empty slots are available to be published to KVS.
    /// This limits the amount of data in system memory.
    fn get_empty_slots(&self) -> u64 {
        let max_fragments = self.get_max_fragments();
        let map_size = self.get_size_of_fragment_map();
        let empty_slots = max_fragments - map_size;

        if empty_slots.gt(&max_fragments) {
            error!("Cannot have more empty slots than max_fragments.");
        }
        empty_slots
    }

    fn get_size_of_fragment_map(&self) -> u64 {
        self.metadata_map.lock().expect("Map poisoned.").len() as u64
    }

    fn get_max_fragments(&self) -> u64 {
        *self.max_fragments.as_ref()
    }
    /// Store fragment data to KVS offline pipeline.  time_stamp is in nanoseconds.
    /// create key in ms which is the fragment ack from KVS.
    fn store_fragment_data(&mut self, time_stamp: u64, duration: u64) {
        let key_in_ms = convert_ns_to_ms(time_stamp);
        let mut map_lock = self.metadata_map.lock().expect("Poisoned Mutex");
        map_lock.insert(key_in_ms, (time_stamp, duration));
    }

    /// KVS fragment timestamp is in milli seconds, internal systems use nana seconds.
    /// This will pull the fragment within +/- 1 millisecond.  This should only ever pull
    /// one fragment will log error if pulls multiple
    fn pull_fragment_data_from_map(&mut self, kvs_timestamp: u64) -> Option<(u64, u64)> {
        let mut map_lock = self.metadata_map.lock().expect("Map poisoned");

        // Pull key
        let key = {
            let mut range =
                map_lock.range((Included(kvs_timestamp - 1), Included(kvs_timestamp + 1)));

            // Pull first key from range.
            let key = match range.next() {
                None => return None,
                Some(val) => *val.0,
            };
            // this should never happen.
            if range.next().is_some() {
                error!("Multiple fragments with same millisecond key found.");
            }
            key
        };

        map_lock.remove(&key)
    }
}
