package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.videoanalytics.videologistics.DetailedVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.Timeline;
import com.amazonaws.videoanalytics.videologistics.ListDetailedVideoTimelineRequestContent;
import com.amazonaws.videoanalytics.videologistics.ListDetailedVideoTimelineResponseContent;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.DetailedTimelinePaginatedResponse;
import com.amazonaws.videoanalytics.videologistics.timeline.DetailedVideoTimelineGenerator;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.TIME_CHRONOLOGY_MISMATCH;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ListDetailedVideoTimelineActivityTest {
    private static final String DEVICE_ID = "dev123";
    
    @Mock
    private DetailedVideoTimelineGenerator detailedVideoTimelineGenerator;
    
    @Mock
    private Context context;
    
    private ListDetailedVideoTimelineActivity activity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        activity = new ListDetailedVideoTimelineActivity(detailedVideoTimelineGenerator);
    }

    @Test
    void handleRequest_validInput_returnsSuccessResponse() {
        Map<String, Object> input = createValidInput();
        when(detailedVideoTimelineGenerator.getDetailedVideoTimeLine(anyString(), anyLong(), anyLong(), any()))
                .thenReturn(getDetailedVideoTimeline());

        Map<String, Object> response = activity.handleRequest(input, context);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void handleRequest_nullInput_returnsErrorResponse() {
        Map<String, Object> response = activity.handleRequest(null, context);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void handleRequest_endTimeBeforeStartTime_returnsErrorResponse() {
        Map<String, Object> input = createInvalidTimeInput();

        Map<String, Object> response = activity.handleRequest(input, context);

        assertEquals(400, response.get("statusCode"));
    }

    private Map<String, Object> createValidInput() {
        Map<String, Object> input = new HashMap<>();
        String body = String.format(
            "{\"deviceId\":\"%s\",\"startTime\":%d,\"endTime\":%d,\"nextToken\":\"\"}",
            DEVICE_ID, 1696444405000L, 1696444421000L
        );
        input.put("body", body);
        return input;
    }

    private Map<String, Object> createInvalidTimeInput() {
        Map<String, Object> input = new HashMap<>();
        String body = String.format(
            "{\"deviceId\":\"%s\",\"startTime\":%d,\"endTime\":%d,\"nextToken\":\"\"}",
            DEVICE_ID, 1696444415000L, 1696444410000L
        );
        input.put("body", body);
        return input;
    }

    private DetailedTimelinePaginatedResponse getDetailedVideoTimeline() {
        ImmutableList<Timeline> cloudTimeline = ImmutableList.of(
            Timeline.builder()
                .startTime(Double.valueOf(1696444405234L))
                .endTime(Double.valueOf(1696444413123L))
                .build()
        );

        ImmutableList<Timeline> deviceTimeline = ImmutableList.of(
            Timeline.builder()
                .startTime(Double.valueOf(1696444412000L))
                .endTime(Double.valueOf(1696444418123L))
                .build()
        );

        return new DetailedTimelinePaginatedResponse(cloudTimeline, deviceTimeline, "next-token");
    }
}