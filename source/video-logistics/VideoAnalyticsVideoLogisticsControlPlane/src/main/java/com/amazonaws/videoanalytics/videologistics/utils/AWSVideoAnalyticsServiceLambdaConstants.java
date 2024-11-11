package com.amazonaws.videoanalytics.videologistics.utils;

public final class AWSVideoAnalyticsServiceLambdaConstants {
    public static final String REGION_NAME = "REGION";
    public static final String CREDENTIALS_PROVIDER = "CREDENTIALS_PROVIDER";
    public static final String SERVICE_ENDPOINT ="SERVICE_ENDPOINT";
    public static final String HTTP_CLIENT = "HTTP_CLIENT";
    
    public static final String PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY = "statusCode";
    public static final String PROXY_LAMBDA_RESPONSE_BODY_KEY = "body";

    private AWSVideoAnalyticsServiceLambdaConstants() {
        // Private default constructor so that JaCoCo marks utility class as covered
    }
}
