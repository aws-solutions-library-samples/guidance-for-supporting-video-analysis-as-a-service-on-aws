///Buffer size of tokio channels.
pub const BUFFER_SIZE: usize = 10000;

///Shadow for device capabilities
pub const PROVISION_SHADOW_NAME: &str = "provision";
///Shadow for snapshots
pub const SNAPSHOT_SHADOW_NAME: &str = "snapshot";
pub const IS_STATUS_CHANGED: &str = "isStatusChanged";
pub const LOG_SYNC: &str = "LOG_SYNC";
pub const IS_ENABLED: &str = "isEnabled";
pub const LOG_LEVEL: &str = "logLevel";
pub const SYNC_FREQUENCY: &str = "syncFrequency";
pub const LOG_DB_FILE_NAME: &str = "log_metadata.db";
pub const FILE_NAME_KEY: &str = "file_name";
pub const POSITION_KEY: &str = "position";
/// Prefix for log files date + times are added by tracing crate
pub const LOG_FILE_PREFIX: &str = "client.log";
