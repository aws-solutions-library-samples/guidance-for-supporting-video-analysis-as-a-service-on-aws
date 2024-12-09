package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage;
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
        String customerId = "837493034878";
        String deviceId = "d123";
        String timeUnit = "SECONDS";
        String result = videoTimelineUtils.generateTimelinePartitionKey(customerId, deviceId, timeUnit);
        assertEquals(customerId + "#" + deviceId + "#" + timeUnit, result);
    }

    @Test
    public void generateTimelinePartitionKey_NullInput_ThrowsException() {
        String customerId = "837493034878";
        String deviceId = null;
        String timeUnit = "SECONDS";
        assertThrows(RuntimeException.class, () -> 
            videoTimelineUtils.generateTimelinePartitionKey(customerId, deviceId, timeUnit));
    }

    @Test
    public void encodedTimestampsToList_ValidInput_ReturnsLongList() {
        String encodedTimestamps = "MTIxLDIzMiwzNDU=";
        List<Long> expected = Arrays.asList(121L, 232L, 345L);
        List<Long> result = videoTimelineUtils.encodedTimestampsToList(encodedTimestamps);
        assertEquals(expected, result);
    }
}