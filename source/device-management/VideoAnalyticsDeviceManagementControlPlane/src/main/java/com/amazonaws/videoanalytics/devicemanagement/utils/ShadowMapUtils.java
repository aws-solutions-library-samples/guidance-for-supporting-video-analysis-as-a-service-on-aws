package com.amazonaws.videoanalytics.devicemanagement.utils;

import com.amazonaws.videoanalytics.devicemanagement.ShadowMap;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import software.amazon.awssdk.core.SdkBytes;

import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_DESIRED_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_STATE_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_REPORTED_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_RULE_IDS_KEY;
import com.fasterxml.jackson.core.JsonProcessingException;

public class ShadowMapUtils {

    private ShadowMapUtils() {
        // Private default constructor so that JaCoCo marks utility class as covered
    }

    public static SdkBytes serialize(ShadowMap requestShadowMap) throws JsonProcessingException {
        Map<String, Object> shadowMap = new HashMap<>();
        Map<String, Object> stateMap = new HashMap<>();
        Object desiredMapObject = requestShadowMap.getStateDocument();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> desiredMap = mapper.convertValue(desiredMapObject, new TypeReference<Map<String, Object>>() {});
        stateMap.put(SHADOW_DESIRED_KEY, desiredMap);
        shadowMap.put(SHADOW_STATE_KEY, stateMap);
        return SdkBytes.fromUtf8String(mapper.writeValueAsString(shadowMap));
    }

    /**
     * Wrap json object in a desired state.
     * @param jsonPayload json payload for desiredState of shadow doc, in the format <code>{"fieldName":true}</code>
     * @return SdkBytes - An in-memory representation of data used for shadow doc payload
     */
    public static SdkBytes createAnUpdateDesiredStateMessage(JsonElement jsonPayload) {
        JsonObject desiredState = new JsonObject();
        JsonObject topLevelState = new JsonObject();
        desiredState.add(SHADOW_DESIRED_KEY, jsonPayload);
        topLevelState.add(SHADOW_STATE_KEY, desiredState);
        return SdkBytes.fromUtf8String(topLevelState.toString());
    }

    /**
     * Creates a shadow document with the reported state.
     * @param jsonPayload The payload to be set in the reported state
     * @return SdkBytes representation of the shadow document
     */
    public static SdkBytes createAnUpdateReportedStateMessage(JsonElement jsonPayload) {
        JsonObject reportedState = new JsonObject();
        JsonObject topLevelState = new JsonObject();
        reportedState.add(SHADOW_REPORTED_KEY, jsonPayload);
        topLevelState.add(SHADOW_STATE_KEY, reportedState);
        return SdkBytes.fromUtf8String(topLevelState.toString());
    }

    /**
     * Creates a shadow document with rule IDs in the desired state.
     * @param jsonPayload The rule IDs payload
     * @return SdkBytes representation of the shadow document
     */
    public static SdkBytes createAnUpdateRuleIdsDesiredStateMessage(JsonElement jsonPayload) {
        JsonObject ruleIds = new JsonObject();
        ruleIds.add(SHADOW_RULE_IDS_KEY, jsonPayload);
        return createAnUpdateDesiredStateMessage(ruleIds);
    }
}
