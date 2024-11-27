use std::time::{SystemTime, UNIX_EPOCH};

/// Get current time - in nanoseconds since linux epoch:
/// https://github.com/awslabs/amazon-kinesis-video-streams-pic/blob/532178bbd4d2e6e6511fa8ffa62a15dba58c02f0/src/utils/src/Time.c#LL47C5-L47C66
/// This method will break when ns since epoch exceeds 18_446_744_073_709_551_615 in 531 years
pub fn get_current_time_since_unix_epoch_ns() -> u64 {
    let current_time = SystemTime::now();
    let since_the_epoch = current_time.duration_since(UNIX_EPOCH).expect("Time went backwards");
    since_the_epoch.as_nanos() as u64
}

/// Convert ns to ms with integer division.  (What KVS Gstreamer plugin does.)
pub(crate) fn convert_ns_to_ms(time_ns: u64) -> u64 {
    time_ns / 1_000_000_u64
}