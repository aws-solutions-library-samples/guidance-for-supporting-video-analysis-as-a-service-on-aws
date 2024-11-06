package com.amazonaws.videoanalytics.devicemanagement.utils;

public final class AWSVideoAnalyticsServiceLambdaConstants {
    public static final String REGION_NAME = "REGION";
    public static final String CREDENTIALS_PROVIDER = "CREDENTIALS_PROVIDER";
    public static final String SERVICE_ENDPOINT ="SERVICE_ENDPOINT";
    public static final String HTTP_CLIENT = "HTTP_CLIENT";

    // body key for request and response
    public static final String PROXY_LAMBDA_BODY_KEY = "body";
    public static final String PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY = "pathParameters";
    public static final String PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY = "deviceId";
    public static final String PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY = "statusCode";

    public static final String DEV = "Dev";
    public static final String ALPHA = "Alpha";
    public static final String BETA = "Beta";
    public static final String GAMMA = "Gamma";
    public static final String PROD = "Prod";
    public static final String DEVICE_CAP_KEY = "deviceCapabilities";
    public static final String DEVICE_STATE_KEY = "deviceState";
    public static final String DEVICE_METADATA_KEY = "deviceMetadata";
    public static final String SD_CARD_KEY = "sdCard";
    public static final String MANUFACTURER_KEY = "manufacturer";
    public static final String MODEL_KEY = "model";
    public static final String DEVICE_NAME_KEY = "device_id";
    public static final String AI_CHIP_SET_KEY = "ai_chip_set";
    public static final String AI_MODEL_VERSION_KEY = "ai_model_version";
    public static final String AI_SDK_VERSION_KEY = "ai_sdk_version";
    public static final String MAC_KEY = "serial_number";
    public static final String FIRMWARE_VERSION_KEY = "firmware_version";
    public static final String RECORDING = "recording";
    public static final String SDK_VERSION_KEY = "sdk_version";
    public static final String PRIVATE_IP_KEY = "private_ip";
    public static final String PUBLIC_IP_KEY = "public_ip";
    public static final String CREATED_AT_KEY = "createdAt";
    // ISO 8601 date and time with offset: 2023-11-09T11:43:43.000Z, 2023-11-09T11:43:43.000−07:00, 
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    public static final String SHADOW_STATE_KEY = "state";
    public static final String SHADOW_METADATA_KEY = "metadata";
    public static final String SHADOW_REPORTED_KEY = "reported";
    public static final String SHADOW_DESIRED_KEY = "desired";

    public static final String VIDEO_SETTINGS = "videoSettings";
    public static final String PROFILE_ID = "profileId";
    public static final String AI_SETTINGS = "aiSettings";
    public static final String STREAMING_SETTINGS = "streamingSettings";
    public static final String IMAGING_SETTINGS = "imagingSettings";

    public static final String IOT_FLEET_INDEXING_INDEX_AWS_THINGS = "AWS_Things";
    public static final String IOT_FLEET_INDEXING_CONNECTIVITY = "connectivity.connected:";
    public static final String IOT_FLEET_INDEXING_THING_NAME = "thingName:";

    public static final int MAX_THING_GROUP_RESULT_COUNT = 20;

    private AWSVideoAnalyticsServiceLambdaConstants() {
        // Private default constructor so that JaCoCo marks utility class as covered
    }
}
