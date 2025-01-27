// GRCOV_STOP_COVERAGE
//! This service will hold the logic to read media from database and send them to cloud

use crate::constants::{
    CATCHUP_BUFFER_SIZE, CATCHUP_SLEEP_MS, MAXIMUM_MEDIA_BUFFER_SIZE, ROUTE_VIDEO_SD,
};
use crate::data_storage::constants::MAX_TIME_FOR_DB;
use crate::data_storage::error::DatabaseError;
use crate::data_storage::media_storage::MediaMetadataStorage;
use event_processor::constants::TIMESTAMP_KEY;
use serde_json::Value;
use std::env;
use std::error::Error;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex};
use std::thread::JoinHandle;
use std::time::Duration;
use tracing::{debug, error};
use video_analytics_client::video_analytics_client::VideoAnalyticsClient;

/// Holds the logic to periodically query the database + push stored media to cloud.
#[derive(Debug)]
pub(crate) struct CatchupService {
    // Main event loop, handle held in struct to be checked by watchdog thread.
    _event_loop: JoinHandle<()>,
    // Database client
    _database_client: Arc<Mutex<MediaMetadataStorage>>,
    // Used to cancel threads when struct is dropped.
    cancellation_token: Arc<AtomicBool>,
}

impl CatchupService {
    pub async fn new(
        database_client: Arc<Mutex<MediaMetadataStorage>>,
        client_id: String,
    ) -> CatchupService {
        let max_number_of_fragments_in_memory: usize = env::var(CATCHUP_BUFFER_SIZE)
            .unwrap_or_default()
            .parse()
            .unwrap_or(MAXIMUM_MEDIA_BUFFER_SIZE);

        // Separate client for catchup, as we want this thread to send messages through another client
        let video_analytics_client = VideoAnalyticsClient::from_conf().await;

        let cancellation_token = Arc::new(AtomicBool::new(true));
        let _event_loop = Self::setup_event_loop(
            database_client.clone(),
            video_analytics_client,
            cancellation_token.clone(),
            max_number_of_fragments_in_memory,
            client_id,
        );
        CatchupService { _event_loop, cancellation_token, _database_client: database_client }
    }

    fn setup_event_loop(
        database_client: Arc<Mutex<MediaMetadataStorage>>,
        mut video_analytics_client: VideoAnalyticsClient,
        cancellation_token: Arc<AtomicBool>,
        max_number_of_fragments_in_memory: usize,
        client_id: String,
    ) -> JoinHandle<()> {
        // Need to have this thread inside of the tokio runtime since we have a tokio async function
        let rt = tokio::runtime::Runtime::new().unwrap();
        std::thread::spawn(move || {
            // Dev Feature to not attempt to push data to the cloud.,
            // frames will not publish offline client as if internet not connected.
            let no_streaming_flag =
                std::env::var(ROUTE_VIDEO_SD).unwrap_or("FALSE".to_string()).eq("TRUE");

            let mut video_analytics_client = video_analytics_client;
            while !cancellation_token.load(Ordering::Relaxed) {
                let mut media = match Self::query_db_for_media(
                    database_client.clone(),
                    0_u64,
                    max_number_of_fragments_in_memory,
                ) {
                    Ok(media) => media,
                    Err(e) => {
                        error!("Database error on query for catchup service : {:?}", e);
                        continue;
                    }
                };

                // If there are no media sleep to let more be written to database.
                if media.is_empty() {
                    // If no remaining media in the database then sleep and wait for more data.
                    debug!("No media in the database, sleeping for {}", CATCHUP_SLEEP_MS);
                    std::thread::sleep(Duration::from_millis(CATCHUP_SLEEP_MS));
                    continue;
                }

                // Block sending (Dev Mode), put here so this thread will act as if disconnected from the internet.
                if no_streaming_flag {
                    debug!("Dev Mode: Streaming disabled do not send to cloud.");
                    std::thread::sleep(Duration::from_millis(CATCHUP_SLEEP_MS));
                    continue;
                }

                rt.block_on(async {
                    while let Some(media) = media.pop() {
                        // Adding so we don't throttle
                        std::thread::sleep(Duration::from_millis(50));
                        let media_as_json: Value = serde_json::from_str(&*media.clone())
                            .expect("Unable to convert media to json");
                        match Self::try_send_media_to_cloud(
                            &mut video_analytics_client.to_owned(),
                            client_id.clone(),
                            media.clone(),
                        )
                        .await
                        {
                            Ok(_) => {
                                debug!("Sent data to cloud.");
                                let _ = Self::delete_media(
                                    database_client.clone(),
                                    media_as_json[TIMESTAMP_KEY]
                                        .as_u64()
                                        .expect("Unable to convert timestamp"),
                                );
                            }
                            Err(e) => {
                                std::thread::sleep(Duration::from_millis(CATCHUP_SLEEP_MS));
                            }
                        }
                    }
                });
            }
        })
    }

    //  By performing locks in a quick function we avoid deadlocks from occurring.
    fn query_db_for_media(
        database_client: Arc<Mutex<MediaMetadataStorage>>,
        start_time: u64,
        limit: usize,
    ) -> Result<Vec<String>, DatabaseError> {
        let mut database_lock = database_client.lock().expect("Database client is poisoned!");
        database_lock.query_media_in_time_range(start_time, MAX_TIME_FOR_DB, limit)
    }

    fn delete_media(
        database_client: Arc<Mutex<MediaMetadataStorage>>,
        start_time: u64,
    ) -> Result<usize, DatabaseError> {
        let mut database_lock = database_client.lock().expect("Database client is poisoned!");
        database_lock.delete_media(start_time)
    }
    async fn try_send_media_to_cloud(
        video_analytics_client: &mut VideoAnalyticsClient,
        client_id: String,
        media: String,
    ) -> Result<(), Box<dyn Error>> {
        return video_analytics_client.import_media_object(client_id, media.into_bytes()).await;
    }
}

/// Threads must check this token in their loops.
impl Drop for CatchupService {
    fn drop(&mut self) {
        self.cancellation_token.store(false, Ordering::Relaxed);
    }
}
