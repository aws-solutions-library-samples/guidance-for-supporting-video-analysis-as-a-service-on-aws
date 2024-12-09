package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineAggregator.AggregateVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineAggregator.TimelineStorage;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineAggregator.VideoDensityLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.Iterator;
import java.util.List;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class VideoTimelineAggregatorTest {
    private static final VideoTimelineUtils VIDEO_TIMELINE_UTILS = new VideoTimelineUtils();
    private VideoTimelineAggregator videoTimelineAggregator;

    private AggregateVideoTimeline videoTimelineBuilder(Long cloudDensity, Long deviceDensity) {
        return AggregateVideoTimeline.builder()
                .deviceIdTimeUnit("1234#d123#HOURS")
                .timeIncrementUnits("HOURS")
                .cloudDensityInMillis(cloudDensity)
                .deviceDensityInMillis(deviceDensity)
                .unitTimestamp(1696442400000L)
                .expirationTimestamp(1696442400000L + 7776000L)
                .createdAt(new Date())
                .lastUpdated(new Date())
                .build();
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        videoTimelineAggregator = new VideoTimelineAggregator(VIDEO_TIMELINE_UTILS);
    }

    @Test
    public void getTimeBuckets_twoSecondInterval_returnsCorrectBuckets() {
        String timeIncrementUnits = "SECONDS";
        Long timestampToBeStored = 1696444404000L;
        Long durationInMillis = 4000L;

        List<TimelineStorage> timelineStorageList = videoTimelineAggregator.getTimeBuckets(
                timeIncrementUnits,
                timestampToBeStored,
                durationInMillis);

        assertNotNull(timelineStorageList);
        assertEquals(2, timelineStorageList.size());

        Iterator<TimelineStorage> it = timelineStorageList.iterator();
        TimelineStorage first = it.next();
        TimelineStorage second = it.next();
        if(first.getBucketStartDate() == 1696444400000L) {
            assertEquals(1000L, first.getDurationToBeStored());
            assertEquals(3000L, second.getDurationToBeStored());
        } else {
            assertEquals(3000L, first.getDurationToBeStored());
            assertEquals(1000L, second.getDurationToBeStored());
        }
    }

    @Test
    public void getUpdatedVideoTimeline_emptyMapSingleSecondsBucketCloud_returnsCorrectDensity() {
        String timeIncrementUnits = "SECONDS";
        String partialPK = "1234#d123#SECONDS";
        Long timestampToBeStored = 1696444400000L;
        VideoDensityLocation location = VideoDensityLocation.CLOUD;

        AggregateVideoTimeline result = videoTimelineAggregator.getUpdatedVideoTimeline(
                timeIncrementUnits,
                timestampToBeStored, 4000L,
                partialPK, null, location, false);

        assertEquals(4000L, result.getCloudDensityInMillis());
        assertEquals("1234#d123#SECONDS", result.getDeviceIdTimeUnit());
    }

    @Test
    public void getUpdatedVideoTimeline_nullSingleSecondsBucketDevice_returnsCorrectDensity() {
        String timeIncrementUnits = "SECONDS";
        String PK = "1234#d123#SECONDS";
        Long timestampToBeStored = 1696444400000L;
        VideoDensityLocation location = VideoDensityLocation.DEVICE;

        AggregateVideoTimeline result = videoTimelineAggregator.getUpdatedVideoTimeline(
                timeIncrementUnits,
                timestampToBeStored, 4000L,
                PK, null, location, false);

        assertEquals(4000L, result.getDeviceDensityInMillis());
        assertEquals("1234#d123#SECONDS", result.getDeviceIdTimeUnit());
    }

    @Test
    public void getUpdatedVideoTimeline_existingObjectHoursBucketCloud_updatesCloudDensity() {
        String timeIncrementUnits = "HOURS";
        String PK = "1234#d123#HOURS";
        Long timestampToBeStored = 1696444404000L;
        VideoDensityLocation location = VideoDensityLocation.CLOUD;

        AggregateVideoTimeline existingTimeline = videoTimelineBuilder(6000L, 0L);

        AggregateVideoTimeline result = videoTimelineAggregator.getUpdatedVideoTimeline(
                timeIncrementUnits,
                timestampToBeStored, 4000L,
                PK, existingTimeline, location, false);

        assertEquals(10000L, result.getCloudDensityInMillis());
        assertEquals("1234#d123#HOURS", result.getDeviceIdTimeUnit());
    }

    @Test
    public void getUpdatedVideoTimeline_existingObjectHoursBucketCatchupTrue_updatesBothDensities() {
        String timeIncrementUnits = "HOURS";
        String PK = "1234#d123#HOURS";
        Long timestampToBeStored = 1696444404000L;
        VideoDensityLocation location = VideoDensityLocation.CLOUD;

        AggregateVideoTimeline existingTimeline = videoTimelineBuilder(1000L, 6000L);

        AggregateVideoTimeline result = videoTimelineAggregator.getUpdatedVideoTimeline(
                timeIncrementUnits,
                timestampToBeStored, 4000L,
                PK, existingTimeline, location, true);

        assertNotNull(result);
        assertEquals(5000L, result.getCloudDensityInMillis());
        assertEquals(2000L, result.getDeviceDensityInMillis());
        assertEquals("1234#d123#HOURS", result.getDeviceIdTimeUnit());
    }

    @Test
    public void getUpdatedVideoTimeline_existingObjectHoursBucketDevice_updatesDeviceDensity() {
        String timeIncrementUnits = "HOURS";
        String PK = "1234#d123#HOURS";
        Long timestampToBeStored = 1696444404000L;
        VideoDensityLocation location = VideoDensityLocation.DEVICE;

        AggregateVideoTimeline existingTimeline = videoTimelineBuilder(1000L, 6000L);

        AggregateVideoTimeline result = videoTimelineAggregator.getUpdatedVideoTimeline(
                timeIncrementUnits,
                timestampToBeStored, 4000L,
                PK, existingTimeline, location, false);

        assertNotNull(result);
        assertEquals(1000L, result.getCloudDensityInMillis());
        assertEquals(10000L, result.getDeviceDensityInMillis());
        assertEquals("1234#d123#HOURS", result.getDeviceIdTimeUnit());
    }
}
