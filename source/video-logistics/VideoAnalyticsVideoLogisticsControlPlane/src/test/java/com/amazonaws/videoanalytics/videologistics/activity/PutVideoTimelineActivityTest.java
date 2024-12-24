package com.amazonaws.videoanalytics.videologistics.activity;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_BODY_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.DEVICE_ID;
import static java.util.Map.entry;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.videoanalytics.videologistics.timeline.PutVideoTimelineHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PutVideoTimelineActivityTest {

    @Mock
    private PutVideoTimelineHandler putVideoTimelineHandler;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Context context;

    private PutVideoTimelineActivity activity;

    String timestamps = "[{\"duration\":1938,\"timestamp\":1734981826886}]";
    Map<String, Object> proxyLambdaBody = Map.ofEntries(
        entry("deviceId", DEVICE_ID),
        entry("location", "CLOUD"),
        entry("timestamps", timestamps)
    );
    Map<String, Object> proxyLambdaRequest = Map.ofEntries(
        entry(PROXY_LAMBDA_BODY_KEY, proxyLambdaBody)
    );

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        activity = new PutVideoTimelineActivity(putVideoTimelineHandler, objectMapper);
    }

    @Test
    public void handleRequest_validInput_returnsSuccess() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn(timestamps);

        Map<String, Object> result = activity.handleRequest(proxyLambdaRequest, context);

        assertEquals(200, result.get("statusCode"));
        verify(putVideoTimelineHandler).addVideoTimelines(eq(DEVICE_ID), eq(timestamps), eq("CLOUD"));
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


