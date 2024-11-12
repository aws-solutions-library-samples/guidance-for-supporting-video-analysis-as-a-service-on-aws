package com.amazonaws.videoanalytics.videologistics.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_BODY_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;

public class LambdaProxyUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
        if (request == null || request.get("body") == null) {
            return null;
        }
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

    public static <T> T parseRequestBody(Map<String, Object> request, Class<T> valueType) {
        String body = parseBody(request);
        if (body == null) {
            throw new RuntimeException("Failed to parse request body: body is null");
        }
        try {
            return MAPPER.readValue(body, valueType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse request body", e);
        }
    }
}
