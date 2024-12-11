package com.amazonaws.videoanalytics.videologistics.utils;

public final class AWSVideoAnalyticsServiceLambdaConstants {
    public static final String REGION_NAME = "REGION";
    public static final String ACCOUNT_ID = "ACCOUNT_ID";
    public static final String CREDENTIALS_PROVIDER = "CREDENTIALS_PROVIDER";
    public static final String SERVICE_ENDPOINT ="SERVICE_ENDPOINT";
    public static final String HTTP_CLIENT = "HTTP_CLIENT";
    
    // body key for request and response
    public static final String PROXY_LAMBDA_BODY_KEY = "body";
    public static final String PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY = "pathParameters";
    public static final String PROXY_LAMBDA_REQUEST_SESSION_ID_PATH_PARAMETER_KEY = "sessionId";
    public static final String PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY = "statusCode";

    public static final String LIVE_STREAM_SIGNALING_CHANNEL = "%s-LiveStreamSignalingChannel";
    public static final String PLAYBACK_SIGNALING_CHANNEL = "%s-PlaybackSignalingChannel";

    public static final int MILLIS_TO_HOURS = 60 * 60 * 1000;
    public static final int TWELVE_HOURS = 12 * 60 * 60;
    public static final long MAX_MEDIA_PLAYLIST_FRAGMENTS = 5000;

    // "video-analytics-image-upload-bucket-%accountId-%region"
    public static final String UPLOAD_BUCKET_FORMAT = "video-analytics-image-upload-bucket-%s-%s"; 

    public static final String NEW_LINE_DELIMITER = "\n";
    public static final int DATA_RETENTION_TIME_PERIOD_IN_HOURS = 2160;
    
    public static final int CONNECTION_TIMEOUT = 200000;

    public static final String OPENSEARCH_INTERCEPTOR_NAME = "OpenSearchInterceptor";

    public static final String OPENSEARCH_SIGNER_NAME = "esSigner";

    public static final String OPENSEARCH_SERVICE_NAME = "es";


    private AWSVideoAnalyticsServiceLambdaConstants() {
        // Private default constructor so that JaCoCo marks utility class as covered
    }
}
