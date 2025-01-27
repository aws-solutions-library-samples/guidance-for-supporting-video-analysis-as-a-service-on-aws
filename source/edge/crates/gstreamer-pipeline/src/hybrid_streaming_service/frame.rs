//! Holds video frame information along with methods to convert to and from Gstreamer buffers.
//Needed here to be reused in database layer.
#[allow(unused)]
use crate::util::get_current_time_since_unix_epoch_ns;
use glib::translate::OptionIntoGlib;
use glib::BoolError;
use gstreamer::{Buffer, BufferFlags, BufferRef, ClockTime};
use serde::{Deserialize, Serialize};
use tracing::warn;

/// Frame used by Rust code when pulled from Gstreamer pipelines.
#[derive(Debug, Clone, Deserialize, Serialize, PartialOrd, PartialEq)]
pub struct Frame {
    /// Identifies the frame as a P frame
    pub is_key_frame: bool,
    /// This will save the absolute timestamp.
    /// This may need to be converted for KVS plugin (real-time vs offline)
    pub time_stamp_ns: u64,
    /// The actual frame data
    #[serde(with = "serde_bytes")]
    pub data: Vec<u8>,
    /// Duration of a frame (time between frames 1/FPS)
    pub duration: u64,
    /// Buffer flags used by gstreamer.
    pub buffer_flags: u32,
}

impl Frame {
    /// Create frame from gstreamer Buffer datatype to be used by rust components.
    #[allow(unused)]
    pub fn new_from_gst_buffer(buffer: &BufferRef, base_time: ClockTime) -> Option<Frame> {
        let Ok(map) = buffer.map_readable() else {
            warn!("Error in fetching data from appsink buffer");
            return None;
        };

        // Get time code for frame from buffer (presentation time)
        let Some(buffer_time) = buffer.pts() else {
            warn!("No time-code on frame!");
            return None;
        };

        // https://gstreamer.pages.freedesktop.org/gstreamer-rs/stable/latest/docs/gstreamer/auto/element/trait.ElementExt.html#method.base_time
        // This conditional compilation is temporary for out-of-date KVS gstreamer plugin.
        // For anything equal and below version 3.4.x , we must use relative time.
        // For everything above we can use absolute time.
        #[cfg(not(target_arch = "x86_64"))]
        let time_stamp_ns = get_current_time_since_unix_epoch_ns();
        #[cfg(target_arch = "x86_64")]
        let time_stamp_ns = buffer_time.nseconds();
        // copy frame data to a vector
        let data = map.to_vec();
        let duration = buffer.duration().unwrap_or_default().nseconds();
        let is_key_frame = buffer.flags().contains(BufferFlags::HEADER)
            || !buffer.flags().contains(BufferFlags::DELTA_UNIT);
        let buffer_flags = buffer.flags().bits();

        Some(Frame { is_key_frame, time_stamp_ns, data, duration, buffer_flags })
    }

    /// Get a new buffer from frame object.  Needed when to put data back into gstreamer pipeline.
    pub fn get_buffer_from_frame(&self) -> Result<Buffer, BoolError> {
        //https://code.amazon.com/packages/OpticsCVRCore/blobs/mainline/--/src/com/amazon/psascyclops/gstreamer/elements/MkvFileReaderSrc.java
        //https://github.com/gstreamer-java/gst1-java-core/blob/master/src/org/freedesktop/gstreamer/Buffer.java
        // This only copies the raw frame into the buffer
        let buffer = self.make_buffer_with_deep_copy_from_data()?;
        Ok(buffer)
    }

    pub fn make_buffer_with_deep_copy_from_data(&self) -> Result<Buffer, BoolError> {
        let mut buffer = Buffer::with_size(self.data.len())?;
        // Set values needed by KVS Plugin
        {
            let buffer = buffer.get_mut().expect("Could not get reference to buffer.");

            let flags = BufferFlags::from_bits(self.buffer_flags).expect("Invalid Flags.");

            buffer.set_flags(flags);
            // KVS plugin uses this value to determine frame time
            buffer.set_pts(Some(ClockTime::from_nseconds(self.time_stamp_ns)));
            buffer.set_dts(Some(ClockTime::from_nseconds(self.time_stamp_ns)));
            buffer.set_offset(ClockTime::GLIB_NONE);
            buffer.set_offset_end(ClockTime::GLIB_NONE);
            buffer.set_duration(Some(ClockTime::from_nseconds(self.duration)));
            let mut map = buffer.map_writable()?;
            // Will panic if the lengths do not match
            map.copy_from_slice(self.data.as_slice());
        }
        Ok(buffer)
    }
}
