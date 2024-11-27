// Grace period which is added to the current time to determine whether the extracted credentials are still valid
pub const GRACE_PERIOD: u64 = 6 * 60 * 60; // 6 hours ago in Duration

pub const X_AMZ_DATE: &str = "x-amz-date";

pub const X_AMZ_SECURITY_TOKEN: &str = "x-amz-security-token";
