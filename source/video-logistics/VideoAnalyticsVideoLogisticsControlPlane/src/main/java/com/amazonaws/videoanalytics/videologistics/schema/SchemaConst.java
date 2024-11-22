package com.amazonaws.videoanalytics.videologistics.schema;

public class SchemaConst {
    //    Common Schema Attributes
    public static final String CREATED_AT = "CreatedAt";
    public static final String JOB_ID = "JobId";
    public static final String JOB_STATUS = "JobStatus";
    public static final String DEVICE_ID = "DeviceId";
    public static final String LAST_UPDATED = "LastUpdated";
    public static final String CUSTOMER_ACCOUNT_ID = "CustomerAccountId";
    public static final String WORKFLOW_NAME = "WorkflowName";
    public static final String JOB_COMPLETED_AT = "JobCompletedAt";
    public static final String LAST_UPDATED_AT = "LastUpdatedAt";
    public static final String FAILURE_REASON = "FailureReason";
    // Register Device Job Attributes
    public static final String VL_REGISTER_DEVICE_JOB_TABLE_NAME =
            "VLRegisterDeviceJobTable";
    public static final String KVS_STREAM = "KvsStream";
    public static final String PLAYBACK_SIGNALING_CHANNEL_ARN = "PlaybackSignalingChannelARN";
    public static final String LIVE_STREAM_SIGNALING_CHANNEL_ARN = "LiveStreamSignalingChannelARN";
    //    Model Schema Attributes
    public static final String MODEL_SCHEMA_TABLE_NAME =
            "ModelSchemaTable";
    public static final String MODEL_SCHEMA_ID = "ModelSchemaId";
    public static final String MODEL_SCHEMA_NAME = "ModelSchemaName";
    public static final String MODEL_SCHEMA_VERSION = "ModelSchemaVersion";
    public static final String SCHEMA = "Schema";
    public static final String NAME = "Name";
    public static final String TYPE = "Type";
    //    Playback Session Attributes
    public static final String PLAYBACK_SESSION_TABLE_NAME =
            "PlaybackSessionTable";
    //    Livestream Session Attributes
    public static final String LIVESTREAM_SESSION_TABLE_NAME =
            "LivestreamSessionTable";
    public static final String SESSION_ID = "SessionId";
    public static final String SESSION_STATUS = "SessionStatus";
    public static final String PEER_CONNECTION_STATUS = "PeerConnectionStatus";
    public static final String HLS_URL = "HLSUrl";
    public static final String CONNECTED_AT = "ConnectedAt";
    public static final String SIGNALING_CHANNEL_URL = "SignalingChannelUrl";
    public static final String STREAM_LOG_ARN = "StreamLogArn";
    public static final String TEMPORARY_IS_WEBRTC_CONNECTION = "TemporaryIsWebRTCConnection";
    public static final String ERROR_CODE = "ErrorCode";
    public static final String ERROR_MESSAGE = "ErrorMessage";
    public static final String START_TIME = "StartTime";
    public static final String END_TIME = "EndTime";
    public static final String STREAM_SOURCE = "StreamSource";
    public static final String PASSWORD = "Password";
    public static final String USERNAME = "Username";
    public static final String TTL = "TTL";
    public static final String URIS = "Uris";
    public static final String EXPIRATION_TIME = "ExpirationTime";
    public static final String ICE_SERVER = "IceServer";
    public static final String CLIENT_ID = "ClientId";
    public static final String STREAM_SESSION_TYPE = "StreamSessionType";
    public static final String IS_LIVESTREAM = "IsLivestream";
    public static final String SOURCE = "Source";
    public static final String ENCRYPTED_FAS_CREDENTIALS = "EncryptedFASCredentials";
    public static final String FAS_ACCESS_KEY = "AccessKey";
    public static final String FAS_SECRET_KEY = "SecretKey";
    public static final String FAS_SECURITY_TOKEN = "SecurityToken";


    // VIDEO_TIMELINE attributes
    public static final String RAW_VIDEO_TIMELINE_TABLE_NAME =
            "RawVideoTimelineTable";
    public static final String VIDEO_TIMELINE_TABLE_NAME =
            "VideoTimelineTable";

    public static final String RAW_VIDEO_TIMELINE_PARTITION_KEY = "CustomerId_DeviceId";
    public static final String VIDEO_TIMELINE_PARTITION_KEY = "CustomerId_DeviceId_TimeUnit";
    public static final String LOCATION = "Location";

    public static final String TIMESTAMP = "Timestamp";
    public static final String UNIT_TIMESTAMP = "UnitTimestamp";
    public static final String DENSITY_IN_MILLIS = "DensityInMillis";
    public static final String CLOUD_DENSITY_IN_MILLIS = "CloudDensityInMillis";
    public static final String DEVICE_DENSITY_IN_MILLIS = "DeviceDensityInMillis";
    public static final String TIME_INCREMENT_UNIT = "TimeIncrementUnit";
    public static final String DURATION_IN_MILLIS = "DurationInMillis";
    public static final String EXPIRATION_TIMESTAMP = "ExpirationTimestamp";
    public static final String RAW_PARTITION_KEY = "rawPartitionKey";
    public static final String RAW_SORT_KEY = "rawSortKey";
    public static final long SECONDS_BUCKET_DURATION = 5L;
    public static final long UNIT_BUCKET_DURATION = 1L;
    public static final long SECONDS_BUCKET_DURATION_MILLIS = SECONDS_BUCKET_DURATION * 1000L;
    public static final long MILLIS_CONVERSION_UNIT = 1000L;
    public static final String OPEN_SEARCH_PIT_TABLE_NAME = "OpenSearchPitTable";
    public static final String FORWARDING_RULES_TABLE_NAME = "ForwardingRulesTable";
    public static final String OPEN_SEARCH_PIT_PARTITION_KEY = "CustomerAccountIdModelName";
    public static final String OPEN_SEARCH_PIT_SORT_KEY = "Endpoint";
    public static final String OPEN_SEARCH_PIT_ID = "PitId";
    public static final String LAMBDA_SCHEDULED_TIME = "LambdaScheduledTime";
    public static final String FORWARDING_RULES_NAME = "ForwardingRulesName";
    public static final String FORWARDED_DATA_TYPES = "ForwardedDataTypes";
    public static final String SELECTORS = "Selectors";
    public static final String DESTINATIONS = "Destinations";

    // Video Export Attributes
    public static final String VIDEO_EXPORT_JOB_PARTITION_KEY = "CustomerAccountId_DeviceId";
    public static final String EXPORTED_S3_PATH = "ExportedS3Path";
    public static final String LAST_EXPORTED_TIME = "LastExportedTimestamp";
    public static final String EXPORT_FILE_START_TIME = "ExportFileStartTime";
    public static final String EXPORT_FILE_END_TIME = "ExportFileEndTime";
    public static final String SEGMENT_JOB_STATUS = "SegmentJobStatus";
    public static final String VIDEO_EXPORT_RESULTS_JOB_STATUS_GSI_NAME = "JobIDStatus-Index";
    public static final String VIDEO_EXPORT_RESULTS_JOB_END_TIME_GSI_NAME = "JobIDEndTime-Index";
    public static final String VIDEO_EXPORT_DEVICE_JOBS_GSI_NAME = "DeviceJobStatus-Index";
    public static final String TOTAL_SEGMENTS = "TotalSegments";
    public static final String SEGMENT_NUMBER = "SegmentNumber";
    public static final String VIDEO_EXPORT_JOB_TABLE_NAME = "VideoExportJobTable";
    public static final String VIDEO_EXPORT_RESULT_TABLE_NAME = "VideoExportResultTable";
    public static final String VIDEO_EXPORT_JOB_CUSTOMER_JOB_ID_GSI_NAME = "AccountIDJobID-Index";


    private SchemaConst() {
        // restrict instantiation
    }
}