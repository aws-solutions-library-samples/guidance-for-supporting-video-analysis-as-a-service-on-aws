// GRCOV_STOP_COVERAGE
//! This module holds the logic for timeline generation for fragments that are on the device in local
//! storage but have not made it to the cloud.

use crate::data_storage::constants::MAX_TIME_FOR_DB;
use crate::data_storage::error::DatabaseError;
use crate::data_storage::video_storage::FileMetadataStorage;
use crate::util::{FragmentDurationInNs, FragmentTimeInNs, IoTMessageUtil};
use std::sync::{Arc, Mutex};
use tracing::{debug, error};

pub(crate) struct DeviceTimelineGenerationTool {
    database_client: Arc<Mutex<FileMetadataStorage>>,
    /// Maximum number of fragment metadata that will be published at once.
    max_fragments_per_payload: u64,
    /// Used to message IoT with timeline information.
    iot_message_util: IoTMessageUtil,
    /// The last fragment published.  Used to generate successive DB queries.
    last_fragment_time_published_in_ns: u64,
}

impl DeviceTimelineGenerationTool {
    /// Create DeviceTimelineTool.
    pub(crate) fn new(
        database_client: Arc<Mutex<FileMetadataStorage>>,
        iot_message_util: IoTMessageUtil,
        max_fragments_per_payload: u64,
    ) -> Self {
        // Start at unix epoch.
        let last_fragment_time_published_in_ns = 0;
        DeviceTimelineGenerationTool {
            database_client,
            iot_message_util,
            max_fragments_per_payload,
            last_fragment_time_published_in_ns,
        }
    }

    /// Function to be called by catchup services KVS callback.
    /// This will let our tool know there is data to be streamed in the database and that
    /// there is an active internet connection.  This will panic if IoT connection closes.
    pub(crate) fn ensure_update_timeline_in_cloud(&mut self) {
        // Query DB for fragments.  Returns an ordered list of fragment metadata.
        let fragment_metadata_list = match self.query_database_for_times() {
            Ok(list) => list,
            Err(e) => {
                error!("Database error on query for device timeline : {:?}", e);
                return;
            }
        };
        // Get last time in vector, if doesnt exist no fragments so return.
        let Some((fragment_time, _duration)) = fragment_metadata_list.last() else {
            debug!("No fragments in database.");
            return;
        };
        // Copy the time, so we can update future queries if we pass message to IoT Service.
        let fragment_time = *fragment_time;

        // Send IoT Message
        if let Err(e) = self.iot_message_util.try_send_timeline_device(fragment_metadata_list) {
            error!("Failed to send timeline message! : {:?}", e);
            return;
        }

        // Update last time so that we do not query these fragments again, only when IoT channel accepts the message.
        // The +1 nanosecond is to ensure the last fragment is not double published.
        self.last_fragment_time_published_in_ns = fragment_time + 1;
    }

    // Helper to deal with locks on the database client.
    fn query_database_for_times(
        &mut self,
    ) -> Result<Vec<(FragmentTimeInNs, FragmentDurationInNs)>, DatabaseError> {
        let locked_client = self.database_client.lock().expect("Database client poisoned.");
        locked_client.query_metadata_for_n_fragments_in_time_range(
            self.last_fragment_time_published_in_ns,
            MAX_TIME_FOR_DB,
            self.max_fragments_per_payload,
        )
    }
}
