package com.amazonaws.videoanalytics.devicemanagement.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_BODY_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;

public class LambdaProxyUtils {
    public static String parsePathParameter(Map<String, Object> request, String key) {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
        Map<String, String> pathParameters = mapper.convertValue(request.get("pathParameters"), typeRef);
        return pathParameters.get(key);
    }

    // https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-integration-settings-integration-response.html
    public static Map<String, Object> serializeResponse(int statusCode, String body) {
        Map<String, Object> response = new HashMap();
        response.put(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY, statusCode);
        response.put(PROXY_LAMBDA_RESPONSE_BODY_KEY, body);

        return response;
    }
}
