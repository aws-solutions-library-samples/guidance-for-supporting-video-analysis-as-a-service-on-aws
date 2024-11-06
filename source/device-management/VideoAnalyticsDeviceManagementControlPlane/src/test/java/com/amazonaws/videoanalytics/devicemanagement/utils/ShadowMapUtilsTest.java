package com.amazonaws.videoanalytics.devicemanagement.utils;

import java.io.UncheckedIOException;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_DESIRED_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_STATE_KEY;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.amazonaws.videoanalytics.devicemanagement.ShadowMap;
import com.fasterxml.jackson.core.JsonProcessingException;

import software.amazon.awssdk.core.SdkBytes;

public class ShadowMapUtilsTest {
    @Test
    public void serialize_WhenValidRequest_ReturnsResponse() throws JsonProcessingException, UncheckedIOException {
        ShadowMap shadowMap = ShadowMap.builder()
            .stateDocument(Map.of("test", true))
            .build();
        SdkBytes responseBytes = ShadowMapUtils.serialize(shadowMap);
        SdkBytes expectedBytes = SdkBytes.fromUtf8String("{\"" + SHADOW_STATE_KEY + "\":{\"" + SHADOW_DESIRED_KEY + "\":{\"test\":true}}}");
        assertEquals(responseBytes, expectedBytes);
    }
}
