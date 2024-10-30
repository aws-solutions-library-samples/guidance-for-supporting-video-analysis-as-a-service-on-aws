package com.amazonaws.videoanalytics.devicemanagement.exceptions;

public class VideoAnalyticsExceptionMessage {
    public static final String INVALID_INPUT_EXCEPTION = "Malformed input, please fix the input.";
    public static final String RESOURCE_ALREADY_EXISTS = "Video Analytics resource you are trying to create " +
            "already exists. Please fix the input.";
    public static final String INTERNAL_SERVER_EXCEPTION = "Service unavailable, internal server exception.";
    public static final String UNAUTHORIZED_EXCEPTION = "You are not authorized to perform this operation." ;
    public static final String THROTTLING_EXCEPTION = "The rate exceeds the limit.";
    public static final String RESOURCE_NOT_FOUND = "Video Analytics resource you are trying to get does not exist.";
    public static final String LIMIT_EXCEEDED_EXCEPTION = "A limit has been exceeded.";
    public static final String DELETE_CONFLICT_EXCEPTION = "Resources attached to a policy has not been fully cleaned up. " +
            "Therefore, Video Analytics Device Management fails to delete the device";
    public static final String IOT_SERVICE_RETRYABLE = "Retryable IoT error, retrying.";
    public static final String JSON_PROCESSING_EXCEPTION = "JSON processing exception thrown, fix JSON content.";
    
    private VideoAnalyticsExceptionMessage() {
        // Private default constructor so that JaCoCo marks utility class as covered
    }
}
