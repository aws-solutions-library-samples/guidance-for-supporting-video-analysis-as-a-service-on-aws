package com.amazonaws.videoanalytics.devicemanagement.utils;

import java.io.UncheckedIOException;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_DESIRED_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_STATE_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_REPORTED_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_RULE_IDS_KEY;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.amazonaws.videoanalytics.devicemanagement.ShadowMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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

    @Test
    public void createAnUpdateDesiredStateMessage_WhenValidJsonPayload_ReturnsFormattedShadowDoc() {
        JsonObject payload = new JsonObject();
        payload.addProperty("enabled", true);
        
        SdkBytes response = ShadowMapUtils.createAnUpdateDesiredStateMessage(payload);
        SdkBytes expected = SdkBytes.fromUtf8String(
            "{\"" + SHADOW_STATE_KEY + "\":{\"" + SHADOW_DESIRED_KEY + "\":{\"enabled\":true}}}"
        );
        
        assertEquals(expected, response);
    }

    @Test
    public void createAnUpdateReportedStateMessage_WhenValidJsonPayload_ReturnsFormattedShadowDoc() {
        JsonObject payload = new JsonObject();
        payload.addProperty("status", "running");
        
        SdkBytes response = ShadowMapUtils.createAnUpdateReportedStateMessage(payload);
        SdkBytes expected = SdkBytes.fromUtf8String(
            "{\"" + SHADOW_STATE_KEY + "\":{\"" + SHADOW_REPORTED_KEY + "\":{\"status\":\"running\"}}}"
        );
        
        assertEquals(expected, response);
    }

    @Test
    public void createAnUpdateRuleIdsDesiredStateMessage_WhenValidJsonPayload_ReturnsFormattedShadowDoc() {
        JsonObject payload = new JsonObject();
        payload.add("rule1", new JsonPrimitive("enabled"));
        
        SdkBytes response = ShadowMapUtils.createAnUpdateRuleIdsDesiredStateMessage(payload);
        SdkBytes expected = SdkBytes.fromUtf8String(
            "{\"" + SHADOW_STATE_KEY + "\":{\"" + SHADOW_DESIRED_KEY + "\":{\"" + 
            SHADOW_RULE_IDS_KEY + "\":{\"rule1\":\"enabled\"}}}}"
        );
        
        assertEquals(expected, response);
    }
}
