package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.videoanalytics.videologistics.ListVideoTimelinesRequestContent;
import com.amazonaws.videoanalytics.videologistics.TimeIncrementUnits;
import com.amazonaws.videoanalytics.videologistics.VideoTimeline;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.VideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineUtils;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.PaginatedListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ListVideoTimelinesActivityTest {

    @Mock
    private VideoTimelineDAO videoTimelineDAO;

    @Mock
    private VideoTimelineUtils videoTimelineUtils;

    private ListVideoTimelinesActivity activity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        activity = new ListVideoTimelinesActivity(videoTimelineDAO, videoTimelineUtils);
    }

    @Test
    void handleRequest_ValidInput_ReturnsSuccess() {
        ListVideoTimelinesRequestContent request = ListVideoTimelinesRequestContent.builder()
            .deviceId("testDevice")
            .startTime(new Date(1000L))
            .endTime(new Date(2000L))
            .timeIncrement(BigDecimal.valueOf(5))
            .timeIncrementUnits(TimeIncrementUnits.SECONDS)
            .build();

        when(videoTimelineUtils.getUnitTime(any(), eq(1000L))).thenReturn(1000L);
        when(videoTimelineUtils.getUnitTime(any(), eq(2000L))).thenReturn(2000L);
        
        List<VideoTimeline> emptyList = new ArrayList<>();
        PaginatedListResponse<VideoTimeline> mockResponse = new PaginatedListResponse<>(emptyList, null);
        when(videoTimelineDAO.listVideoTimelines(any(), any(), any(), any(), any(), any()))
            .thenReturn(mockResponse);

        Map<String, Object> input = new HashMap<>();
        input.put("body", request.toJson());
        Map<String, Object> result = activity.handleRequest(input, null);

        assertNotNull(result);
        assertEquals(200, result.get("statusCode"));
    }

    @Test
    void handleRequest_NullInput_ReturnsBadRequest() {
        Map<String, Object> result = activity.handleRequest(null, null);

        assertNotNull(result);
        assertEquals(400, result.get("statusCode"));
    }

    @Test
    void handleRequest_InvalidTimeChronology_ReturnsBadRequest() {
        ListVideoTimelinesRequestContent request = ListVideoTimelinesRequestContent.builder()
            .deviceId("testDevice")
            .startTime(new Date(2000L))
            .endTime(new Date(1000L))
            .timeIncrement(BigDecimal.valueOf(5))
            .timeIncrementUnits(TimeIncrementUnits.SECONDS)
            .build();

        Map<String, Object> input = new HashMap<>();
        input.put("body", request.toJson());
        Map<String, Object> result = activity.handleRequest(input, null);

        assertNotNull(result);
        assertEquals(400, result.get("statusCode"));
    }

    @Test
    void handleRequest_InvalidSecondsIncrement_ReturnsBadRequest() {
        ListVideoTimelinesRequestContent request = ListVideoTimelinesRequestContent.builder()
            .deviceId("testDevice")
            .startTime(new Date(1000L))
            .endTime(new Date(2000L))
            .timeIncrement(BigDecimal.valueOf(3))
            .timeIncrementUnits(TimeIncrementUnits.SECONDS)
            .build();

        Map<String, Object> input = new HashMap<>();
        input.put("body", request.toJson());
        Map<String, Object> result = activity.handleRequest(input, null);

        assertNotNull(result);
        assertEquals(400, result.get("statusCode"));
    }
}