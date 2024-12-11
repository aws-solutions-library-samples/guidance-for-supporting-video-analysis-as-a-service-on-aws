package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.videoanalytics.videologistics.PutVideoTimelineRequestContent;
import com.amazonaws.videoanalytics.videologistics.VideoDensityLocation;
import com.amazonaws.videoanalytics.videologistics.timeline.PutVideoTimelineHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;

public class PutVideoTimelineActivityTest {

    @Mock
    private PutVideoTimelineHandler putVideoTimelineHandler;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Context context;

    private PutVideoTimelineActivity activity;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        activity = new PutVideoTimelineActivity(putVideoTimelineHandler, objectMapper);
    }

    @Test
    public void handleRequest_validInput_returnsSuccess() throws Exception {
        String deviceId = "testDevice";
        ObjectNode timestamps = new ObjectMapper().createObjectNode();
        timestamps.put("test", "value");
        
        Map<String, Object> input = new HashMap<>();
        input.put("body", "{\"deviceId\":\"" + deviceId + "\",\"location\":\"CLOUD\",\"timestamps\":" + timestamps.toString() + "}");

        when(objectMapper.writeValueAsString(any())).thenReturn(timestamps.toString());

        Map<String, Object> result = activity.handleRequest(input, context);

        assertEquals(200, result.get("statusCode"));
        verify(putVideoTimelineHandler).addVideoTimelines(eq(deviceId), eq(timestamps.toString()), eq("CLOUD"));
    }

    @Test
    public void handleRequest_nullInput_returnsBadRequest() {
        Map<String, Object> result = activity.handleRequest(null, context);
        assertEquals(400, result.get("statusCode"));
    }

    @Test
    public void handleRequest_invalidJson_returnsBadRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("body", "invalid json");

        Map<String, Object> result = activity.handleRequest(input, context);
        assertEquals(400, result.get("statusCode"));
    }

    @Test
    public void handleRequest_missingRequiredFields_returnsBadRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("body", "{}");

        Map<String, Object> result = activity.handleRequest(input, context);
        assertEquals(400, result.get("statusCode"));
    }
}


