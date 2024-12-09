package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.TimeIncrementUnits;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VideoTimelineUtilsTest {

    private VideoTimelineUtils videoTimelineUtils;

    @BeforeEach
    public void setup() {
        videoTimelineUtils = new VideoTimelineUtils();
    }

    @Test
    public void generateTimelinePartitionKey_ValidInput_ReturnsFormattedKey() {
        String deviceId = "d123";
        TimeIncrementUnits timeUnit = TimeIncrementUnits.SECONDS;
        
        String result = videoTimelineUtils.generateTimelinePartitionKey(deviceId, timeUnit);
        assertEquals(String.format("%s#%s", deviceId, timeUnit.name()), result);
    }


    @Test
    public void encodedTimestampsToList_ValidInput_ReturnsLongList() {
        String encodedTimestamps = "MTIxLDIzMiwzNDU=";
        List<Long> expected = Arrays.asList(121L, 232L, 345L);
        List<Long> result = videoTimelineUtils.encodedTimestampsToList(encodedTimestamps);
        assertEquals(expected, result);
    }
}