package com.amazonaws.videoanalytics.devicemanagement.utils;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_BODY_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DEVICE_ID;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.SHADOW_NAME;
import static java.util.Map.entry;
import static org.junit.Assert.assertEquals;

public class LambdaProxyUtilsTest {
    private final String body = "{\"shadowName\": \"" + SHADOW_NAME + "\"}";
    private final Map<String, Object> lambdaProxyRequest = Map.ofEntries(
        entry("pathParameters", Map.ofEntries(
            entry("deviceId", DEVICE_ID)
        )),
        entry("body", body)
    );
    private final int statusCode = 200;

    @Test
    public void parsePathParameter_WhenValidKey_ReturnsValue() {
        String deviceId = LambdaProxyUtils.parsePathParameter(lambdaProxyRequest, "deviceId");
        assertEquals(deviceId, DEVICE_ID);
    }

    @Test
    public void parsePathParameter_WhenInvalidKey_ReturnsNull() {
        String random = LambdaProxyUtils.parsePathParameter(lambdaProxyRequest, "random");
        assertEquals(random, null);
    }

    @Test
    public void parseBody_WhenValidRequest_ReturnsBody() {
        String parsedBody = LambdaProxyUtils.parseBody(lambdaProxyRequest);
        assertEquals(parsedBody, body);
    }

    @Test
    public void serializeResponse_WhenValidRequest_ReturnsResponse() {
        Map<String, Object> serializedResponse = LambdaProxyUtils.serializeResponse(statusCode, body);
        assertEquals(serializedResponse.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), statusCode);
        String parsedBody = LambdaProxyUtils.parseBody(lambdaProxyRequest);
        assertEquals(parsedBody, body);
    }
}
