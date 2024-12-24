/// Gstreamer plugin responsible to make a connection to an RTSP server and read the data.
pub const RTSP_SRC: &str = "rtspsrc";
/// Gstreamer plugin responsible to extract H264 video from RTP packets (RFC 3984).
pub const RTP_DEPAY: &str = "rtph264depay";
/// Gstreamer plugin responsible to parse H.264 streams.
pub const H264_PARSE: &str = "h264parse";
/// Gstreamer plugin responsible for application to get a handle on the GStreamer data in a pipeline.
pub const APP_SINK: &str = "appsink";
/// Max number of buffers appsink configured to have.
pub const APP_SINK_BUFFER_SIZE: u32 = 1;
/// KVS Plugin for Gstreamer
pub(crate) const KVS_SINK: &str = "kvssink";
/// KVS Realtime Setting for plugin
pub const KVS_REALTIME: &str = "0";
/// KVS Gstreamer plugin name for realtime ingestion.
pub(crate) const KVS_REALTIME_SINK: &str = "kvssinkrealtime";
/// Default frame rate
pub(crate) const DEFAULT_FRAME_RATE: u32 = 15;
/// Default buffer duration for kvs realtime client in seconds.
pub(crate) const DEFAULT_BUFFER_DURATION: u32 = 60;
/// Buffer for frames in the system. Start with 20 seconds of video. 30 is max FPS on some cameras.
pub(crate) const FRAME_BUFFER_SIZE: usize = 20 * 30;
pub(crate) const DEFAULT_KVS_PLUGIN_BUFFER_SIZE: u32 = 10_u32;
/// In MBytes per second not mbps.
pub(crate) const DEFAULT_KVS_PLUGIN_EXPECTED_BANDWIDTH: u32 = 125000_u32;
/// Max fragments in memory, used for forwarding service.
pub(crate) const MAX_FRAGMENTS: u64 = 5;
/// Max fragments in memory when motion is not detected
pub(crate) const MAX_FRAGMENTS_NO_MOTION: u64 = 2;
/// Max fragments to send after motion
pub(crate) const FRAGMENTS_AFTER_MOTION_STOP: i32 = 3;
/// GStreamer plugin responsible to filter stream based on caps.
pub const CAPSFILTER: &str = "capsfilter";
/// Start tag of onvif metadata stream.
pub const METADATA_STREAM_START_TAG: &str = "MetadataStream";
/// Motion based streaming env var
pub const MOTION_BASED_STREAMING: &str = "MOTION_BASED_STREAMING";
/// Destination field for timeline fragments location.
pub(crate) const DESTINATION_FOR_TIMELINE_CLOUD: &str = "CLOUD";

/// Catchup sleep time in seconds
#[cfg(feature = "sd-card-catchup")]
pub(crate) const CATCHUP_SLEEP_MS: u64 = 100;
#[cfg(feature = "sd-card-catchup")]
pub(crate) const CATCHUP_KVS_SLEEP_MS: u64 = 20;
/// Override the MAX_FRAGMENTS for catchup.  
#[cfg(feature = "sd-card-catchup")]
pub(crate) const CATCHUP_BUFFER_SIZE: &str = "CATCHUP_BUFFER_SIZE";
/// Developer tool. Disable streaming to cloud.  Just store data on disk.
#[cfg(feature = "sd-card-catchup")]
pub(crate) const ROUTE_VIDEO_SD: &str = "ROUTE_VIDEO_SD";
/// KVS Offline Setting for plugin
#[cfg(feature = "sd-card-catchup")]
pub const KVS_OFFLINE: &str = "2";
/// Destination field for timeline fragments location.
#[cfg(feature = "sd-card-catchup")]
pub(crate) const DESTINATION_FOR_TIMELINE_DEVICE: &str = "DEVICE";
/// Sleep time for device storage timeline generation messages.
#[cfg(feature = "sd-card-catchup")]
pub(crate) const TIMELINE_SLEEP_IN_SEC: u64 = 1;
/// Max Metadata for Device Timeline Messages
#[cfg(feature = "sd-card-catchup")]
pub(crate) const MAX_TIMELINE_ENTRIES: u64 = 500;
