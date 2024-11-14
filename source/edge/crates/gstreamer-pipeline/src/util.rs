use std::time::{SystemTime, UNIX_EPOCH};

/// Get current time - in nanoseconds since linux epoch:
/// https://tiny.amazon.com/gljzg1ze/githawslamazblob5321srcutil
/// This method will break when ns since epoch exceeds 18_446_744_073_709_551_615 in 531 years
pub fn get_current_time_since_unix_epoch_ns() -> u64 {
    let current_time = SystemTime::now();
    let since_the_epoch = current_time.duration_since(UNIX_EPOCH).expect("Time went backwards");
    since_the_epoch.as_nanos() as u64
}
