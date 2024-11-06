package com.amazonaws.videoanalytics.devicemanagement.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_BODY_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;

public class LambdaProxyUtils {
    private LambdaProxyUtils() {
        // Private default constructor so that JaCoCo marks utility class as covered
    }

    // https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-lambda-proxy-integrations.html#api-gateway-simple-proxy-for-lambda-input-format
    public static String parsePathParameter(Map<String, Object> request, String key) {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
        Map<String, String> pathParameters = mapper.convertValue(request.get(PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY), typeRef);
        return pathParameters.get(key);
    }

    public static String parseBody(Map<String, Object> request) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(request.get(PROXY_LAMBDA_BODY_KEY), String.class);
    }

    // https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-integration-settings-integration-response.html
    public static Map<String, Object> serializeResponse(int statusCode, String body) {
        Map<String, Object> response = new HashMap<>();
        response.put(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY, statusCode);
        response.put(PROXY_LAMBDA_BODY_KEY, body);

        return response;
    }
}
