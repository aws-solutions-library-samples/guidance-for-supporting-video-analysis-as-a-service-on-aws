use crate::constants::{
    FRAGMENTS_AFTER_MOTION_STOP, MAX_FRAGMENTS_NO_MOTION, MOTION_BASED_STREAMING,
};
#[cfg(feature = "sd-card-catchup")]
use crate::data_storage::video_storage::FileMetadataStorage;
use crate::hybrid_streaming_service::frame::Frame;
use crate::util::convert_ns_to_ms;
use serde::{Deserialize, Serialize};
use std::collections::{BTreeMap, Bound::Included};
use std::sync::atomic::{AtomicBool, AtomicI32, Ordering};
use std::sync::mpsc::{SyncSender, TrySendError};
use std::sync::{Arc, Mutex};
use tracing::{debug, error, info, trace, warn};

/// Allows for passing Fragments without unnecessary mem-copies.
pub(crate) type FrameList = Vec<Arc<Frame>>;
/// Fragment Time-code, used as a key for fragment map
pub(crate) type FragmentTimeCodeMs = u64;

/// Video fragment information extracted from gstreamer pipeline
#[allow(unused)]
#[derive(Debug, Clone, Serialize, Deserialize, PartialOrd, PartialEq)]
pub(crate) struct VideoFragmentInformation {
    /// Key frame timestamp (Round off?)
    pub start_of_fragment_timestamp: u64,
    /// Vector of frames
    pub frame_list: FrameList,
    /// Duration of fragment
    pub duration: u64,
}

impl VideoFragmentInformation {
    // Must insert a valid keyframe or will return None
    pub fn new_with_frame(key_frame: Arc<Frame>) -> Option<VideoFragmentInformation> {
        // Guard, fragments must begin with a keyframe.
        if !key_frame.is_key_frame {
            error!("Fragment requires valid keyframe.");
            return None;
        }
        let start_of_fragment_timestamp = key_frame.time_stamp_ns;
        let duration = key_frame.duration;
        let frame_list = vec![key_frame];

        Some(VideoFragmentInformation { start_of_fragment_timestamp, frame_list, duration })
    }

    pub fn add_frame(&mut self, frame: Arc<Frame>) {
        if frame.is_key_frame {
            error!("Attempted to add multiple key frames to fragment.");
            return;
        }
        // Add frame to fragment and update the duration.
        self.duration = frame.time_stamp_ns - self.start_of_fragment_timestamp;
        self.frame_list.push(frame);
    }

    /// Checks if fragment is valid, used to check for data corruption or serialization errors.
    #[cfg(feature = "sd-card-catchup")]
    pub fn is_valid_fragment(&self) -> bool {
        // Must begin with a key frame, will be false if first frame is not key or
        let starts_with_keyframe =
            self.frame_list.first().map(|frame| frame.is_key_frame).unwrap_or(false);

        // Confirm timestamps are increasing.
        let mut previous_fragment = 0_u64;
        let mut is_ascending = true;
        for frame in self.frame_list.iter() {
            is_ascending = is_ascending && (frame.time_stamp_ns > previous_fragment);
            previous_fragment = frame.time_stamp_ns;
        }

        // Test distance between timestamps?
        starts_with_keyframe && is_ascending
    }
}

/// Forwarding service added frames into this struct.  It will store in memory and either
/// delete a fragment in the event it persists in the cloud or if the fragment_max is hit
/// and the fragment is not marked for forwarding.
/// This struct is thread safe as it will be shared between multiple threads.
/// Clones are effectively shallow copies
#[derive(Debug, Clone)]
#[cfg(not(feature = "sd-card-catchup"))]
pub(crate) struct FragmentManager {
    fragment_map: Arc<Mutex<BTreeMap<FragmentTimeCodeMs, VideoFragmentInformation>>>,
    fragment_max: Arc<u64>,
    realtime_tx: SyncSender<Arc<Frame>>,
    forward_frames: Arc<AtomicBool>,
    motion_detected: Arc<AtomicBool>,
    tail_fragment_counter: Arc<AtomicI32>,
    is_motion_based_streaming: Arc<bool>,
}

// Alternate struct with database_client for SD card catchup
#[derive(Debug, Clone)]
#[cfg(feature = "sd-card-catchup")]
pub(crate) struct FragmentManager {
    fragment_map: Arc<Mutex<BTreeMap<FragmentTimeCodeMs, VideoFragmentInformation>>>,
    fragment_max: Arc<u64>,
    realtime_tx: SyncSender<Arc<Frame>>,
    forward_frames: Arc<AtomicBool>,
    motion_detected: Arc<AtomicBool>,
    tail_fragment_counter: Arc<AtomicI32>,
    is_motion_based_streaming: Arc<bool>,
    database_client: Arc<Mutex<FileMetadataStorage>>,
}

impl FragmentManager {
    #[cfg(not(feature = "sd-card-catchup"))]
    pub fn new(realtime_tx: SyncSender<Arc<Frame>>, fragment_max: u64) -> Self {
        let is_motion_based_streaming =
            std::env::var(MOTION_BASED_STREAMING).unwrap_or("TRUE".to_string()).eq("TRUE");
        FragmentManager {
            fragment_map: Arc::new(Mutex::default()),
            fragment_max: Arc::new(fragment_max),
            realtime_tx,
            forward_frames: Arc::new(AtomicBool::new(true)),
            motion_detected: Arc::new(AtomicBool::new(false)),
            // -MAX_FRAGMENTS_NO_MOTION: discard excess fragments
            tail_fragment_counter: Arc::new(AtomicI32::new(
                MAX_FRAGMENTS_NO_MOTION.try_into().unwrap_or(2) * -1,
            )),
            is_motion_based_streaming: Arc::new(is_motion_based_streaming),
        }
    }

    #[cfg(feature = "sd-card-catchup")]
    pub fn new(
        realtime_tx: SyncSender<Arc<Frame>>,
        fragment_max: u64,
        database_client: Arc<Mutex<FileMetadataStorage>>,
    ) -> Self {
        let is_motion_based_streaming =
            std::env::var(MOTION_BASED_STREAMING).unwrap_or("TRUE".to_string()).eq("TRUE");
        FragmentManager {
            fragment_map: Arc::new(Mutex::default()),
            fragment_max: Arc::new(fragment_max),
            realtime_tx,
            forward_frames: Arc::new(AtomicBool::new(true)),
            motion_detected: Arc::new(AtomicBool::new(false)),
            // -MAX_FRAGMENTS_NO_MOTION: discard excess fragments
            tail_fragment_counter: Arc::new(AtomicI32::new(
                MAX_FRAGMENTS_NO_MOTION.try_into().unwrap_or(2) * -1,
            )),
            is_motion_based_streaming: Arc::new(is_motion_based_streaming),
            database_client,
        }
    }

    pub fn add_frame(&mut self, frame: Arc<Frame>) {
        if *self.is_motion_based_streaming {
            self.add_frame_for_motion_based(frame);
        } else {
            self.add_frame_for_continuous_streaming(frame);
        }
    }

    fn add_frame_for_motion_based(&mut self, frame: Arc<Frame>) {
        // create new fragment in the map
        if frame.is_key_frame {
            self.create_and_insert_fragment(frame.clone());
            // If no motion detected within last 3 frames, discard excess over 2 fragments
            if !self.motion_detected.load(Ordering::Relaxed) {
                // If tail fragment counter has been initialized (not negative),
                // decrement when key frame is encountered and
                // store excess frames on SD card similar to when motion is detected.
                // Otherwise, only keep latest 2 fragments.
                // However, we cannot immediately discard the excess fragments in case internet is disconnected/slow.
                // Therefore, we should continue to store excess over 2 fragments 2 times.
                let counter = self.tail_fragment_counter.load(Ordering::Relaxed);
                if counter > 0 {
                    self.tail_fragment_counter.store(counter - 1, Ordering::Relaxed);

                    // logic to forward frames is set in delete_or_store
                    self.delete_or_store_excess_fragments(None);
                } else if counter > MAX_FRAGMENTS_NO_MOTION.try_into().unwrap_or(2) * -1 {
                    self.tail_fragment_counter.store(counter - 1, Ordering::Relaxed);

                    // may need to delete more than 1 excess fragment when it is the first key frame after the 3 tail fragments
                    loop {
                        match self.delete_or_store_excess_fragments(Some(MAX_FRAGMENTS_NO_MOTION)) {
                            true => continue,
                            false => break,
                        }
                    }
                } else {
                    self.remove_excess_fragment_from_map(Some(MAX_FRAGMENTS_NO_MOTION));
                }
            } else {
                // logic to forward frames is set in delete_or_store
                self.delete_or_store_excess_fragments(None);
            }
        } else {
            self.insert_frame_into_latest_fragment(frame.clone());
        }

        // Whether to send a frame is a bit tricky as we need to send frames starting with a key frame.
        // To accomplish that a flag is set at the start of a fragment.  If the fragment manager is full
        // then internet connection is spotty and we do not want to keep pushing to the pipeline.
        // Only send realtime frame if motion has been detected or is within 2 fragments/key frames from when motion stopped
        if self.forward_frames.load(Ordering::Relaxed) {
            if self.motion_detected.load(Ordering::Relaxed)
                || self.tail_fragment_counter.load(Ordering::Relaxed) > 0
            {
                debug!("Motion has been detected within the last {} fragments. Sending frame to realtime.", FRAGMENTS_AFTER_MOTION_STOP);
                self.try_send_realtime_frame(frame);
            } else {
                debug!("Motion has not been detected within the last {} fragments. Not sending frame to realtime.", FRAGMENTS_AFTER_MOTION_STOP);
            }
        }
    }

    fn add_frame_for_continuous_streaming(&mut self, frame: Arc<Frame>) {
        // create new fragment in the map
        if frame.is_key_frame {
            self.create_and_insert_fragment(frame.clone());
            // logic to forward frames is set in delete_or_store
            self.delete_or_store_excess_fragments(None);
        } else {
            self.insert_frame_into_latest_fragment(frame.clone());
        }

        // Whether to send a frame is a bit tricky as we need to send frames starting with a key frame.
        // To accomplish that a flag is set at the start of a fragment.  If the fragment manager is full
        // then internet connection is spotty and we do not want to keep pushing to the pipeline.
        if self.forward_frames.load(Ordering::Relaxed) {
            self.try_send_realtime_frame(frame);
        }
    }

    fn create_and_insert_fragment(&mut self, key_frame: Arc<Frame>) {
        // Kvs callback will be in ms.
        let timestamp_in_ms = convert_ns_to_ms(key_frame.time_stamp_ns);
        let fragment =
            VideoFragmentInformation::new_with_frame(key_frame).expect("Non-keyframe inserted.");
        let mut map_lock = self.fragment_map.lock().expect("Fragment manager poisoned.");
        map_lock.insert(timestamp_in_ms, fragment);
    }

    // If the number of fragments is greater than the max, then drop it (no sd-card-catchup)
    #[cfg(not(feature = "sd-card-catchup"))]
    fn delete_or_store_excess_fragments(&mut self, fragment_max_opt: Option<u64>) -> bool {
        // If there is an excess fragment that means cloud ingest is not keeping up so stop publishing fragments.
        // If no excess fragment exists then start/continue publishing fragments
        let Some(_excess_fragment) = self.remove_excess_fragment_from_map(fragment_max_opt) else {
            self.forward_frames.store(true, Ordering::Relaxed);
            return false;
        };
        true
    }

    // If the number of fragments is greater than the max, then store in DB (sd-card-catchup).
    #[cfg(feature = "sd-card-catchup")]
    fn delete_or_store_excess_fragments(&mut self, fragment_max_opt: Option<u64>) -> bool {
        // If there is an excess fragment that means cloud ingest is not keeping up so stop publishing fragments.
        // If no excess fragment exists then start/continue publishing fragments
        let Some(excess_fragment) = self.remove_excess_fragment_from_map(fragment_max_opt) else {
            self.forward_frames.store(true, Ordering::Relaxed);
            return false;
        };
        let mut database_lock = self.database_client.lock().expect("Database client poisoned.");
        match database_lock.save_fragment(&excess_fragment) {
            Ok(_) => {
                debug!("Fragment stored in database.");
            }
            Err(e) => {
                error!("Error storing fragment in database : {:?}", e);
            }
        };
        true
    }

    // Returns None if no excess fragments. Pops earliest fragment.
    // Uses fragment_max from constructor if no fragment_max_opt is passed in
    fn remove_excess_fragment_from_map(
        &mut self,
        fragment_max_opt: Option<u64>,
    ) -> Option<VideoFragmentInformation> {
        let mut map_lock = self.fragment_map.lock().expect("Fragment manager poisoned.");
        let number_of_fragments = map_lock.len() as u64;
        match fragment_max_opt {
            Some(fragment_max) => {
                if number_of_fragments.le(&fragment_max) {
                    return None;
                }
            }
            None => {
                if number_of_fragments.le(self.fragment_max.as_ref()) {
                    return None;
                }
            }
        }
        map_lock.pop_first().map(|(_time_code, fragment)| fragment)
    }

    // delete a fragment that is persisted in KVS. Fragment may have been naturally phased out.
    pub(crate) fn remove_fragment(
        &mut self,
        kvs_ack_timestamp_in_ms: u64,
    ) -> Option<VideoFragmentInformation> {
        let mut map_lock = self.fragment_map.lock().expect("Map poisoned");

        // Pull map key for fragment.
        let key = {
            // get range since rounding is used.
            let mut range = map_lock.range((
                Included(kvs_ack_timestamp_in_ms - 1),
                Included(kvs_ack_timestamp_in_ms + 1),
            ));

            // Pull first key from range.
            let Some((time_in_ms, _fragment)) = range.next() else {
                return None;
            };
            *time_in_ms
        };

        map_lock.remove(&key)
    }

    /// If no fragment in map just log error.  No way to recover.
    fn insert_frame_into_latest_fragment(&mut self, frame: Arc<Frame>) {
        let mut map_lock = self.fragment_map.lock().expect("Fragment manager poisoned.");
        // get most recent fragment timestamp
        let last_key = match map_lock.last_key_value() {
            None => {
                error!("Non-keyframe inserted into fragment manager with no existing fragments.");
                return;
            }
            Some(entry) => *entry.0,
        };
        // Append frame to end of the fragment.
        let video_fragment_ref = map_lock.get_mut(&last_key).expect("Value must exist.");
        video_fragment_ref.add_frame(frame);
    }

    /// If buffer is full just store.
    fn try_send_realtime_frame(&mut self, frame: Arc<Frame>) {
        match self.realtime_tx.try_send(frame) {
            Ok(_) => {
                trace!("Frame sent to realtime kvs client.");
            }
            // If internet connection slow or disconnected this buffer can fill up
            // This is normal behavior in the real world.
            Err(TrySendError::Full(_)) => {
                debug!("Buffer to realtime kvs client.");
            }
            // If channel breaks it means the Stream service is being shutdown
            // or an unrecoverable error has occurred.
            Err(TrySendError::Disconnected(_)) => {
                panic!("KVS Realtime channel disconnected. Stopping Thread");
            }
        }
    }

    /// Helper function to get all frames in fragment_map when motion is detected
    fn get_frames_from_map(&mut self) -> Vec<Arc<Frame>> {
        let mut frame_list: Vec<Arc<Frame>> = vec![];
        let map_lock = self.fragment_map.lock().expect("Fragment manager poisoned.");
        let number_of_fragments = map_lock.len() as u64;
        if number_of_fragments > MAX_FRAGMENTS_NO_MOTION {
            warn!("There are more than {} fragments in the fragment manager when motion has been detected", MAX_FRAGMENTS_NO_MOTION);
        }
        for (_timestamp, fragment) in map_lock.iter() {
            frame_list.append(fragment.to_owned().frame_list.as_mut());
        }
        frame_list
    }

    /// Function to handle motion detection signal appropriately
    pub fn handle_motion_detection(&mut self, motion_detected: bool) {
        // If motion start is detected, send frames from fragments in fragment_map to realtime pipeline
        // There should only be 2 fragments in the fragment_map at this point
        if motion_detected && self.forward_frames.load(Ordering::Relaxed) {
            info!("Motion start has been detected. Attempting to send frames from last {} fragments to realtime", MAX_FRAGMENTS_NO_MOTION);
            let frame_list: Vec<Arc<Frame>> = self.get_frames_from_map();
            for frame in frame_list {
                self.try_send_realtime_frame(frame);
            }
        }
        // If motion stop is detected, set tail_fragment_counter to 3
        else {
            info!(
                "Motion stop has been detected. Setting tail_fragment_counter to {}",
                FRAGMENTS_AFTER_MOTION_STOP
            );
            self.tail_fragment_counter.store(FRAGMENTS_AFTER_MOTION_STOP, Ordering::Relaxed);
        }

        self.motion_detected.store(motion_detected, Ordering::Relaxed);
    }
}

#[cfg(not(feature = "sd-card-catchup"))]
#[cfg(test)]
mod tests {
    use super::*;
    use crate::constants::{FRAME_BUFFER_SIZE, MAX_FRAGMENTS};
    use crate::util::FragmentTimeInNs;
    use std::sync::mpsc::{sync_channel, Receiver};

    const DEFAULT_DURATION: u64 = 100;
    const START_OF_FRAGMENT: FragmentTimeInNs = 100;

    #[tokio::test]
    async fn verify_fragment_manager() {
        let (mut fragment_manager, realtime_rx) = create_fragment_manager();

        // create test_fragments
        let test_fragments = create_fragments(MAX_FRAGMENTS + 1);

        // add fragments
        for fragment in test_fragments {
            for frame in fragment.to_owned().frame_list {
                fragment_manager.add_frame(frame);
            }
        }

        // assert data sent to realtime channel
        assert!(realtime_rx.try_recv().is_ok());
    }

    #[tokio::test]
    async fn verify_fragment_manager_continuous() {
        let (mut fragment_manager, realtime_rx) = create_fragment_manager();

        // create test_fragments
        let test_fragments = create_fragments(MAX_FRAGMENTS + 1);

        // add fragments
        for fragment in test_fragments {
            for frame in fragment.to_owned().frame_list {
                fragment_manager.add_frame_for_continuous_streaming(frame);
            }
        }

        // assert data sent to realtime channel
        assert!(realtime_rx.try_recv().is_ok());

        // assert fragment_map len does not go above MAX_FRAGMENTS
        let map_lock = fragment_manager.fragment_map.lock().expect("Fragment manager poisoned.");
        let number_of_fragments = map_lock.len() as u64;
        assert!(number_of_fragments <= MAX_FRAGMENTS);
    }

    #[tokio::test]
    async fn verify_fragment_manager_no_motion() {
        let (mut fragment_manager, realtime_rx) = create_fragment_manager();

        // create test_fragments for entire test
        let test_fragments = create_fragments(MAX_FRAGMENTS_NO_MOTION + 1);

        // add 3 fragments before motion start is detected (2 is max fragments when there is no motion)
        for i in 0..(MAX_FRAGMENTS_NO_MOTION.try_into().unwrap_or(2) + 1) {
            for frame in test_fragments.get(i).unwrap().to_owned().frame_list {
                fragment_manager.add_frame_for_motion_based(frame);
            }
        }

        // assert data NOT sent to realtime channel
        assert!(realtime_rx.try_recv().is_err());

        // assert fragment_map len does not go above MAX_FRAGMENTS_NO_MOTION
        let map_lock = fragment_manager.fragment_map.lock().expect("Fragment manager poisoned.");
        let number_of_fragments = map_lock.len() as u64;
        assert!(number_of_fragments <= MAX_FRAGMENTS_NO_MOTION);
    }

    #[tokio::test]
    async fn verify_fragment_manager_motion_start() {
        let (mut fragment_manager, realtime_rx) = create_fragment_manager();

        // create test_fragments for entire test
        let test_fragments = create_fragments(MAX_FRAGMENTS_NO_MOTION + MAX_FRAGMENTS + 1);

        // add 2 fragments before motion start is detected
        for i in 0..MAX_FRAGMENTS_NO_MOTION.try_into().unwrap_or(2) {
            for frame in test_fragments.get(i).unwrap().to_owned().frame_list {
                fragment_manager.add_frame_for_motion_based(frame);
            }
        }

        // motion start
        fragment_manager.handle_motion_detection(true);

        // assert data sent to realtime channel (2 frames before motion) and flag is set
        assert!(realtime_rx.try_recv().is_ok());
        assert!(fragment_manager.motion_detected.load(Ordering::Relaxed));

        // add 6 fragments after motion start is detected (5 is max fragments when there is motion)
        for i in 0..(MAX_FRAGMENTS.try_into().unwrap_or(5) + 1) {
            for frame in test_fragments
                .get(i + MAX_FRAGMENTS_NO_MOTION.try_into().unwrap_or(2))
                .unwrap()
                .to_owned()
                .frame_list
            {
                fragment_manager.add_frame_for_motion_based(frame);
            }
        }

        // assert data sent to realtime channel
        assert!(realtime_rx.try_recv().is_ok());

        // assert fragment_map len does not go above MAX_FRAGMENTS
        let map_lock = fragment_manager.fragment_map.lock().expect("Fragment manager poisoned.");
        let number_of_fragments = map_lock.len() as u64;
        assert!(number_of_fragments <= MAX_FRAGMENTS);
    }

    #[tokio::test]
    async fn verify_fragment_manager_motion_stop() {
        let (mut fragment_manager, realtime_rx) = create_fragment_manager();

        // create test_fragments for entire test
        let test_fragments = create_fragments(
            FRAGMENTS_AFTER_MOTION_STOP.try_into().unwrap_or(3) + MAX_FRAGMENTS + 1,
        );

        // motion stop
        fragment_manager.handle_motion_detection(false);

        // assert flag and tail counter are set
        assert!(!fragment_manager.motion_detected.load(Ordering::Relaxed));
        assert_eq!(
            fragment_manager.tail_fragment_counter.load(Ordering::Relaxed),
            FRAGMENTS_AFTER_MOTION_STOP
        );

        // add 3 tail fragments after motion stop is detected
        for i in 0..FRAGMENTS_AFTER_MOTION_STOP.try_into().unwrap_or(3) {
            for frame in test_fragments.get(i).unwrap().to_owned().frame_list {
                fragment_manager.add_frame_for_motion_based(frame);
            }
        }

        // assert data sent to realtime channel and tail counter is 0 after adding 3 more fragments after motion stop
        assert!(realtime_rx.try_recv().is_ok());
        assert_eq!(fragment_manager.tail_fragment_counter.load(Ordering::Relaxed), 0);

        // add 6 more fragments after 3 tails fragments
        for i in 0..(MAX_FRAGMENTS.try_into().unwrap_or(5) + 1) {
            for frame in test_fragments
                .get(i + FRAGMENTS_AFTER_MOTION_STOP.try_into().unwrap_or(3))
                .unwrap()
                .to_owned()
                .frame_list
            {
                fragment_manager.add_frame_for_motion_based(frame);
            }
        }

        // tail counter is -2 after adding at least 2 fragments after 3 tail fragments
        assert_eq!(
            fragment_manager.tail_fragment_counter.load(Ordering::Relaxed),
            MAX_FRAGMENTS_NO_MOTION.try_into().unwrap_or(2) * -1
        );

        // assert fragment_map len does not go above MAX_FRAGMENTS_NO_MOTION
        let map_lock = fragment_manager.fragment_map.lock().expect("Fragment manager poisoned.");
        let number_of_fragments = map_lock.len() as u64;
        assert!(number_of_fragments <= MAX_FRAGMENTS_NO_MOTION);
    }

    fn create_fragment_manager() -> (FragmentManager, Receiver<Arc<Frame>>) {
        let (realtime_tx, realtime_rx) = sync_channel(FRAME_BUFFER_SIZE);
        (FragmentManager::new(realtime_tx, MAX_FRAGMENTS), realtime_rx)
    }

    fn create_fragments(number_of_fragments: u64) -> Vec<VideoFragmentInformation> {
        let mut fragments = Vec::new();
        for i in 0..number_of_fragments {
            let start = START_OF_FRAGMENT + DEFAULT_DURATION * i;
            let fragment = VideoFragmentInformation {
                start_of_fragment_timestamp: start,
                frame_list: create_frames(start),
                duration: 4_u64,
            };
            fragments.push(fragment);
        }
        fragments
    }

    fn create_frames(start: u64) -> Vec<Arc<Frame>> {
        let data: Vec<u8> = (0..255).collect();
        let mut frames: Vec<Arc<Frame>> = Vec::new();

        for i in 0..30 {
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
}
