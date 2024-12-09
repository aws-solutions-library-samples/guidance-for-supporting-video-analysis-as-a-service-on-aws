package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.VideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.AggregateVideoTimeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class VideoTimelineListGeneratorTest {

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void buildVideoTimelineList_withValidAggregateTimelines_returnsExpectedDensities() {
        List<VideoTimeline> response = VideoTimelineListGenerator.buildVideoTimelineList(
                buildAggregateVideoTimelineList(),
                1696444405000L,
                1696444470000L,
                10000L
        );

        List<VideoTimeline> expectedVideoTimelineList = List.of(
                VideoTimeline.builder()
                        .cloudDensity(0.4F)
                        .deviceDensity(0.0F)
                        .build(),
                VideoTimeline.builder()
                        .cloudDensity(0.0F)
                        .deviceDensity(0.0F)
                        .build(),
                VideoTimeline.builder()
                        .cloudDensity(0.2F)
                        .deviceDensity(0.0F)
                        .build(),
                VideoTimeline.builder()
                        .cloudDensity(0.0F)
                        .deviceDensity(0.0F)
                        .build(),
                VideoTimeline.builder()
                        .cloudDensity(0.4F)
                        .deviceDensity(0.4F)
                        .build(),
                VideoTimeline.builder()
                        .cloudDensity(0.2F)
                        .deviceDensity(0.0F)
                        .build(),
                VideoTimeline.builder()
                        .cloudDensity(0.0F)
                        .deviceDensity(0.0F)
                        .build()
        );

        assertEquals(7, response.size());
        assertIterableEquals(expectedVideoTimelineList, response);
    }

    private List<AggregateVideoTimeline> buildAggregateVideoTimelineList() {
        return List.of(
                AggregateVideoTimeline.builder()
                        .unitTimestamp(1696444405000L)
                        .cloudDensityInMillis(2000L)
                        .deviceDensityInMillis(0L)
                        .build(),
                AggregateVideoTimeline.builder()
                        .unitTimestamp(1696444410000L)
                        .cloudDensityInMillis(2000L)
                        .deviceDensityInMillis(-900L)
                        .build(),
                AggregateVideoTimeline.builder()
                        .unitTimestamp(1696444430000L)
                        .cloudDensityInMillis(2000L)
                        .deviceDensityInMillis(0L)
                        .build(),
                AggregateVideoTimeline.builder()
                        .unitTimestamp(1696444445000L)
                        .cloudDensityInMillis(4000L)
                        .deviceDensityInMillis(4000L)
                        .build(),
                AggregateVideoTimeline.builder()
                        .unitTimestamp(1696444455000L)
                        .cloudDensityInMillis(2000L)
                        .deviceDensityInMillis(-2350L)
                        .build()
        );
    }
}