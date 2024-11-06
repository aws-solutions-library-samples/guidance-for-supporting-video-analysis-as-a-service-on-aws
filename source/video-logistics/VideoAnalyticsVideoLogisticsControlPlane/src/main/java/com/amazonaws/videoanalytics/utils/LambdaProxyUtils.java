package com.amazonaws.videoanalytics.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.videoanalytics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_BODY_KEY;
import static com.amazonaws.videoanalytics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;

public class LambdaProxyUtils {
    // https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-lambda-proxy-integrations.html#api-gateway-simple-proxy-for-lambda-input-format
    public static String parsePathParameter(Map<String, Object> request, String key) {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
        Map<String, String> pathParameters = mapper.convertValue(request.get("pathParameters"), typeRef);
        return pathParameters.get(key);
    }

    public static String parseBody(Map<String, Object> request) {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<String> typeRef = new TypeReference<String>() {};
        return mapper.convertValue(request.get("body"), typeRef);
    }

    // https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-integration-settings-integration-response.html
    public static Map<String, Object> serializeResponse(int statusCode, Object body) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY, statusCode);
        response.put(PROXY_LAMBDA_RESPONSE_BODY_KEY, body);

        return response;
    }
}
