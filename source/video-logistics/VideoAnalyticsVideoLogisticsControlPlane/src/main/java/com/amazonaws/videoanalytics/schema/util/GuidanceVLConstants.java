package com.amazonaws.videoanalytics.schema.util;

public class GuidanceVLConstants {
    public static final String SERVICE_NAME = "AWSFathomVLControlPlane";
    // this name is for the metrics factory. Theoretically it can be any name. Setting it to package name
    // helps with filtering metrics at a package level.
    public static final String METRICS_FACTORY_SERVICE_NAME = "AWSFathomVLControlPlane";
    public static final String DEVICE_ID = "12345";
    public static final int CONNECTION_TIMEOUT = 200000;
    public static final String CLIENT_ID = "12345";
    public static final String TIMESTAMP = "Fri Feb 17 16:26:01 UTC 2023";
    public static final String DEVICE_GROUP_ID = "11111";
    public static final String JOB_ID = "12345";
    public static final String MODEL_SCHEMA_NAME = "PersonDetection";
    public static final String MODEL_SCHEMA_ID = "PersonDetection:1.1";
    public static final String OPENSEARCH_SERVICE_NAME = "es";
    public static final String REGION_NAME = "RegionName";
    public static final String FAS_DECRYPTION_HELPER = "FasDecryptionHelper";
    public static final String CUSTOMER_CREDENTIALS_PROVIDER = "CustomerCredentialsProvider";
    public static final String SERVICE_ACCOUNT_ID = "AWS_ACCOUNT";
    public static final String AIRPORT_CODE = "airportCode";
    public static final String STAGE_NAME = "Stage";
    public static final String FAS_ENCRYPTION_KEY_ARN = "FasEncryptionKeyArn";
    public static final String FORWARDING_RULES_Q_ARN = "ForwardingRulesQArn";
    public static final String OPENSEARCH_INTERCEPTOR_NAME = "OpenSearchInterceptor";
    public static final String TIMESTAMP_KEY = "timestamp";
    public static final int TWELVE_HOURS = 12 * 60 * 60;
    public static final long MAX_MEDIA_PLAYLIST_FRAGMENTS = 5000;
    public static final int MILLIS_TO_HOURS = 60 * 60 * 1000;
    public static final int DATA_RETENTION_TIME_PERIOD_IN_HOURS = 2160;
    public static final String NEW_LINE_DELIMITER = "\n";
    public static final String SERVICE_ENDPOINT = "ServiceEndpoint";
    public static final String X_AMZ_ENCRYPTED_FAS_CREDENTIALS = "X-Amz-EncryptedFasCredentials";
    public static final String X_AMZ_ACCOUNT_ID_HEADER = "X-Amz-AccountId";
    public static final String DEV = "Dev";
    public static final String ALPHA = "Alpha";
    public static final String BETA = "Beta";
    public static final String GAMMA = "Gamma";
    public static final String PROD = "Prod";
    public static final String STREAMING_PEER_CONNECTIONS = "StreamingPeerConnections";
    public static final String CLIENT_ID_NAME = "ClientId";
    public static final String STATE = "State";
    public static final String ICE_SERVER_LIST = "IceServerList";
    public static final String LIVE_STREAM_SIGNALING_CHANNEL = "%s-LiveStreamSignalingChannel";
    public static final String PLAYBACK_SIGNALING_CHANNEL = "%s-PlaybackSignalingChannel";
    // default is set to 4s (4000ms) to account for the fragment size configured for VHT cameras
    public static final Long TIMELINE_DURATION_IN_MILLIS = 4000L;

    public static final Integer HOURS_IN_DAY = 24;
    public static final String UPLOAD_BUCKET_FORMAT = "fathom-%s"; // fathom-<region>
    public static final String INFERENCE = "inference";
    public static final int MAX_INFERENCES_PER_AGGREGATION_BUCKET = 1000;
    public static final String FAILURE_REASON = "failureReason";
    public static final String ERROR_MESSAGE = "errorMessage";

    // Hardcoded placeholder for TTL duration for video on KVS in seconds = 3 months in seconds
    public static final Long KVS_TTL_DURATION = 3L * 30 * 24 * 60 * 60;
    // Max fragment duration allowed by KVS is 20s. This helps us determine how far to look back in time to get accurate
    // detailed timeline information.
    public static final Long MAX_KVS_FRAGMENT_DURATION_BUFFER = 20*1000L;

    public static final long CONTENT_LENGTH = 3000;
    public static final String EXECUTOR_SERVICE_NAME = "ExecutorService";
    public static final String SDK_ASYNC_HTTP_CLIENT = "SdkAsyncHttpClient";

    public static final String FORWARDING_RULES_BUCKET_NAME = "fathom-fwd-rules";
    public static final String FWD_RULES_ACCESS_LOGS_BUCKET_NAME = "fathom-fwd-rules-access-logs";
    public static final String FWD_RULES_ACCESS_LOG_BUCKET_PREFIX = "%s/logs";
    public static final String FWD_RULES_EVENT_NOTIFICATION_ID = "forwarding-rules-event-notification-%s";
    public static final String FWD_RULES_DATA_FILE_NAME = "data";
    public static final String FWD_RULES_IDENTITY_FILE_NAME = "identity";
    public static final String VIDEO_EXPORT_STATE_MACHINE_ARN = "VIDEO_EXPORT_STATE_MACHINE_ARN";

    public static final String FATHOM_DATASET_MANAGEMENT_CLIENT = "DatasetManagementClient";
    public static final String FATHOM_DATASET_CONTROL_PLANE_ENDPOINT = "FathomDatasetControlPlaneEndpoint";
}
