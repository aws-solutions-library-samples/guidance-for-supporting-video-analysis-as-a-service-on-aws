package com.amazonaws.videoanalytics.devicemanagement.utils;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_BODY_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DEVICE_ID;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.SHADOW_NAME;
import static java.util.Map.entry;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LambdaProxyUtilsTest {
    private final String body = "{\"shadowName\": \"" + SHADOW_NAME + "\"}";
    private final Map<String, Object> lambdaProxyRequest = Map.ofEntries(
        entry(PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY, Map.ofEntries(
            entry(PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY, DEVICE_ID)
        )),
        entry(PROXY_LAMBDA_BODY_KEY, body)
    );
    private final int statusCode = 200;

    @Test
    public void parsePathParameter_WhenValidKey_ReturnsValue() {
        String deviceId = LambdaProxyUtils.parsePathParameter(lambdaProxyRequest, PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY);
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TestRequestBody {
        @JsonProperty("shadowName")
        private String shadowName;
    }

    @Test
    public void parseRequestBody_WhenValidJson_ReturnsObject() {
        TestRequestBody result = LambdaProxyUtils.parseRequestBody(lambdaProxyRequest, TestRequestBody.class);
        assertEquals(SHADOW_NAME, result.getShadowName());
    }

    @Test
    public void parseRequestBody_WhenInvalidJson_ThrowsException() {
        Map<String, Object> invalidRequest = Map.ofEntries(
            entry("pathParameters", Map.ofEntries(
                entry("deviceId", DEVICE_ID)
            )),
            entry("body", "{invalid json}")
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            LambdaProxyUtils.parseRequestBody(invalidRequest, TestRequestBody.class);
        });
        assertEquals("Failed to parse request body", exception.getMessage());
    }

    @Test
    public void parseRequestBody_WhenNullBody_ThrowsException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            LambdaProxyUtils.parseRequestBody(null, TestRequestBody.class);
        });
        assertEquals("Failed to parse request body: body is null", exception.getMessage());
    }

    @Test
    public void parseRequestBody_WhenMismatchedSchema_ReturnsObjectWithNullFields() {
        Map<String, Object> mismatchedRequest = Map.of(
            "body", "{\"wrongField\": \"value\"}"
        );

        TestRequestBody result = LambdaProxyUtils.parseRequestBody(mismatchedRequest, TestRequestBody.class);
        assertNull(result.getShadowName());
    }
}
