use serde::{Deserialize, Serialize};
use std::collections::{BTreeMap};
use std::sync::atomic::{AtomicBool, AtomicI32, Ordering};
use std::sync::mpsc::{SyncSender, TrySendError};
use std::sync::{Arc, Mutex};
use tracing::{debug, error, trace};
use crate::constants::{MAX_FRAGMENTS_NO_MOTION};
use crate::hybrid_streaming_service::frame::Frame;
use crate::util::convert_ns_to_ms;

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
    tail_fragment_counter: Arc<AtomicI32>,
}

impl FragmentManager {

    pub fn new(
        realtime_tx: SyncSender<Arc<Frame>>,
        fragment_max: u64,
    ) -> Self {
        FragmentManager {
            fragment_map: Arc::new(Mutex::default()),
            fragment_max: Arc::new(fragment_max),
            realtime_tx,
            forward_frames: Arc::new(AtomicBool::new(true)),
            // -MAX_FRAGMENTS_NO_MOTION: discard excess fragments
            tail_fragment_counter: Arc::new(AtomicI32::new(
                MAX_FRAGMENTS_NO_MOTION.try_into().unwrap_or(2) * -1,
            )),
        }
    }

    pub fn add_frame(&mut self, frame: Arc<Frame>) {
        self.add_frame_for_continuous_streaming(frame);
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
}