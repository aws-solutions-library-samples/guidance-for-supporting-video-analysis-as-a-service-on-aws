package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.Timeline;
import com.amazonaws.videoanalytics.videologistics.DetailedVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.RawVideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.PaginatedListResponse;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.RawVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.DetailedTimelinePaginatedResponse;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.VideoDensityLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.when;

public class DetailedVideoTimelineGeneratorTest {
    private static final String DEVICE_ID = "dev123";
    private static final Long START_TIME = 1696444405000L;
    private static final Long END_TIME = 1696444420000L;
    private final List<RawVideoTimeline> rawVideoTimelineList = getRawVideoTimelineList();
    private final PaginatedListResponse<RawVideoTimeline> paginatedListTimelineResponse = 
            new PaginatedListResponse<>(rawVideoTimelineList, "next_token");

    @Mock
    private RawVideoTimelineDAO rawVideoTimelineDAO;

    @InjectMocks
    private DetailedVideoTimelineGenerator detailedVideoTimelineGenerator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getDetailedVideoTimeLine_WithinTimeRange_ReturnsFullTimeline() {
        Long startTime = START_TIME - DetailedVideoTimelineGenerator.MAX_KVS_FRAGMENT_DURATION_BUFFER;
        when(rawVideoTimelineDAO.listRawVideoTimelines(DEVICE_ID, startTime, END_TIME, null))
                .thenReturn(paginatedListTimelineResponse);

        DetailedTimelinePaginatedResponse response = 
                detailedVideoTimelineGenerator.getDetailedVideoTimeLine(DEVICE_ID, START_TIME, END_TIME, null);

        assertEquals("next_token", response.getNextToken());
        assertIterableEquals(getDetailedVideoTimeline().getCloud(), response.getCloudTimeline());
        assertIterableEquals(getDetailedVideoTimeline().getDevice(), response.getDeviceTimeline());
    }

    @Test
    void getDetailedVideoTimeLine_TruncatedEndTime_ReturnsTruncatedTimeline() {
        Long startTime = START_TIME - DetailedVideoTimelineGenerator.MAX_KVS_FRAGMENT_DURATION_BUFFER;
        Long truncatedEndTime = 1696444417123L;
        when(rawVideoTimelineDAO.listRawVideoTimelines(DEVICE_ID, startTime, truncatedEndTime, null))
                .thenReturn(paginatedListTimelineResponse);

        DetailedTimelinePaginatedResponse response = 
                detailedVideoTimelineGenerator.getDetailedVideoTimeLine(DEVICE_ID, START_TIME, truncatedEndTime, null);

        assertIterableEquals(getDetailedVideoTimelineTruncated().getCloud(), response.getCloudTimeline());
        assertIterableEquals(getDetailedVideoTimelineTruncated().getDevice(), response.getDeviceTimeline());
    }

    private DetailedVideoTimeline getDetailedVideoTimeline() {
        return DetailedVideoTimeline.builder()
                .cloud(List.of(Timeline.builder()
                        .startTime(Double.valueOf(START_TIME))
                        .endTime(Double.valueOf(1696444413123L))
                        .build()))
                .device(List.of(Timeline.builder()
                        .startTime(Double.valueOf(1696444412000L))
                        .endTime(Double.valueOf(1696444419123L))
                        .build()))
                .build();
    }

    private DetailedVideoTimeline getDetailedVideoTimelineTruncated() {
        return DetailedVideoTimeline.builder()
                .cloud(List.of(Timeline.builder()
                        .startTime(Double.valueOf(START_TIME))
                        .endTime(Double.valueOf(1696444413123L))
                        .build()))
                .device(List.of(Timeline.builder()
                        .startTime(Double.valueOf(1696444412000L))
                        .endTime(Double.valueOf(1696444417123L))
                        .build()))
                .build();
    }

    private List<RawVideoTimeline> getRawVideoTimelineList() {
        List<RawVideoTimeline> list = new ArrayList<>();
        list.add(getRawVideoTimeline(1696444397234L, 4000L, VideoDensityLocation.CLOUD));
        list.add(getRawVideoTimeline(1696444401234L, 4000L, VideoDensityLocation.CLOUD));
        list.add(getRawVideoTimeline(1696444405234L, 4000L, VideoDensityLocation.CLOUD));
        list.add(getRawVideoTimeline(1696444409123L, 4000L, VideoDensityLocation.CLOUD));
        list.add(getRawVideoTimeline(1696444412000L, 3000L, VideoDensityLocation.DEVICE));
        list.add(getRawVideoTimeline(1696444416123L, 3000L, VideoDensityLocation.DEVICE));
        return list;
    }

    private RawVideoTimeline getRawVideoTimeline(Long timestamp, Long duration, VideoDensityLocation location) {
        return RawVideoTimeline.builder()
                .deviceId(DEVICE_ID)
                .timestamp(timestamp)
                .durationInMillis(duration)
                .location(location)
                .build();
    }
}