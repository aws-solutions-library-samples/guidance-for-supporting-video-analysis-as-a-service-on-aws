//! Messaging tools for communicating between the pipelines.
//! This does not include frame data. Use of static data is required to interact with C style callbacks.
use gstreamer::Caps;
use std::sync::RwLock;

// Sends CAPS information for APP Srcs from RTSP pipeline.
pub(crate) static GST_CAPS_FOR_KVS: RwLock<Option<Caps>> = RwLock::new(None);
