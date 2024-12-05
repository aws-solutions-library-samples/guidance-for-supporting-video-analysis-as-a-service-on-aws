use device_traits::connections::QoS;

// Currently quality of service is set to AtLeastOnce which is likely what we will use for iot.
pub(crate) const QUALITY_OF_SERVICE: QoS = QoS::AtLeastOnce;
pub(crate) const RETAIN: bool = false;
pub(crate) const UPDATE_SHADOW_TOPIC_SUFFIX: &str = "update";
// Max allowable size for local shadow.  If longer than this we do not load it into system
// memory, we just log that this occurred.
pub(crate) const MAX_FILE_SIZE_BYTES: u64 = 30 * 1024;
pub(crate) const PROVISION_SHADOW_NAME: &str = "provision";
pub(crate) const LOGGER_SETTINGS_FIELD: &str = "loggerSettings";
pub(crate) const STATE_SHADOW_FIELD: &str = "state";
pub(crate) const REPORTED_SHADOW_FIELD: &str = "reported";
pub(crate) const ENABLED_FIELD_FROM_CLOUD: &str = "enabled";
pub(crate) const DISABLED_FIELD_FROM_CLOUD: &str = "disabled";
pub(crate) const SNAPSHOT_SHADOW_NAME: &str = "snapshot";
pub(crate) const PRESIGNED_URL_FIELD: &str = "presignedUrl";
pub(crate) const VIDEO_ENCODER_SHADOW_NAME: &str = "videoEncoder";
pub(crate) const VIDEO_SETTINGS_FIELD: &str = "videoSettings";
///IoT Jobs execution
pub const JOB_EXECUTION_FIELD: &str = "execution";
///IoT Jobs jobId
pub(crate) const JOB_ID_FIELD: &str = "jobId";
