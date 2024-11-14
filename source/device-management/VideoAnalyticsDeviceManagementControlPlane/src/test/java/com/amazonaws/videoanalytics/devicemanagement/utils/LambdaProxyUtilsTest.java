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
    public void parsePathParameter_withValidKey_returnsExpectedValue() {
        String deviceId = LambdaProxyUtils.parsePathParameter(lambdaProxyRequest, PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY);
        assertEquals(deviceId, DEVICE_ID);
    }

    @Test
    public void parsePathParameter_withInvalidKey_returnsNull() {
        String random = LambdaProxyUtils.parsePathParameter(lambdaProxyRequest, "random");
        assertEquals(random, null);
    }

    @Test
    public void parseBody_withValidRequest_returnsExpectedBody() {
        String parsedBody = LambdaProxyUtils.parseBody(lambdaProxyRequest);
        assertEquals(parsedBody, body);
    }

    @Test
    public void serializeResponse_withValidInput_returnsExpectedResponse() {
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
    public void parseRequestBody_withValidJson_returnsPopulatedObject() {
        TestRequestBody result = LambdaProxyUtils.parseRequestBody(lambdaProxyRequest, TestRequestBody.class);
        assertEquals(SHADOW_NAME, result.getShadowName());
    }

    @Test
    public void parseRequestBody_withInvalidJson_throwsRuntimeException() {
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
    public void parseRequestBody_withNullBody_throwsRuntimeException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            LambdaProxyUtils.parseRequestBody(null, TestRequestBody.class);
        });
        assertEquals("Failed to parse request body: body is null", exception.getMessage());
    }

    @Test
    public void parseRequestBody_withMismatchedSchema_returnsObjectWithNullFields() {
        Map<String, Object> mismatchedRequest = Map.of(
            "body", "{\"wrongField\": \"value\"}"
        );

        TestRequestBody result = LambdaProxyUtils.parseRequestBody(mismatchedRequest, TestRequestBody.class);
        assertNull(result.getShadowName());
    }
}
