package com.amazonaws.videoanalytics.devicemanagement.utils;

import com.amazonaws.videoanalytics.devicemanagement.ShadowMap;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.SdkBytes;

import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_DESIRED_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_STATE_KEY;
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
}
