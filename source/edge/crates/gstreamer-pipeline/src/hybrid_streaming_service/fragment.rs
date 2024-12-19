use crate::constants::{
    FRAGMENTS_AFTER_MOTION_STOP, MAX_FRAGMENTS_NO_MOTION, MOTION_BASED_STREAMING,
};
use crate::hybrid_streaming_service::frame::Frame;
use crate::util::convert_ns_to_ms;
use serde::{Deserialize, Serialize};
use std::collections::BTreeMap;
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
}

/// Forwarding service added frames into this struct.  It will store in memory and either
/// delete a fragment in the event it persists in the cloud or if the fragment_max is hit
/// and the fragment is not marked for forwarding.
/// This struct is thread safe as it will be shared between multiple threads.
/// Clones are effectively shallow copies
#[derive(Debug, Clone)]
pub(crate) struct FragmentManager {
    fragment_map: Arc<Mutex<BTreeMap<FragmentTimeCodeMs, VideoFragmentInformation>>>,
    fragment_max: Arc<u64>,
    realtime_tx: SyncSender<Arc<Frame>>,
    forward_frames: Arc<AtomicBool>,
    motion_detected: Arc<AtomicBool>,
    tail_fragment_counter: Arc<AtomicI32>,
    is_motion_based_streaming: Arc<bool>,
}

impl FragmentManager {
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
                    self.delete_excess_fragments(None);
                } else if counter > MAX_FRAGMENTS_NO_MOTION.try_into().unwrap_or(2) * -1 {
                    self.tail_fragment_counter.store(counter - 1, Ordering::Relaxed);

                    // may need to delete more than 1 excess fragment when it is the first key frame after the 3 tail fragments
                    loop {
                        match self.delete_excess_fragments(Some(MAX_FRAGMENTS_NO_MOTION)) {
                            true => continue,
                            false => break,
                        }
                    }
                } else {
                    self.remove_excess_fragment_from_map(Some(MAX_FRAGMENTS_NO_MOTION));
                }
            } else {
                // logic to forward frames is set in delete_or_store
                self.delete_excess_fragments(None);
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
            self.delete_excess_fragments(None);
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

    // If the number of fragments is greater than the max, then drop it
    fn delete_excess_fragments(&mut self, fragment_max_opt: Option<u64>) -> bool {
        // If there is an excess fragment that means cloud ingest is not keeping up so stop publishing fragments.
        // If no excess fragment exists then start/continue publishing fragments
        let Some(_excess_fragment) = self.remove_excess_fragment_from_map(fragment_max_opt) else {
            self.forward_frames.store(true, Ordering::Relaxed);
            return false;
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
