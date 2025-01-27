package com.amazonaws.videoanalytics.videologistics.utils;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.LIVE_STREAM_SIGNALING_CHANNEL;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PLAYBACK_SIGNALING_CHANNEL;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.DEVICE_ID;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.PLAYBACK_STREAM_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ResourceNameConversionUtilsTest {
    @Test
    public void getPlaybackStreamNameFromDeviceId_WhenDeviceId_ReturnsPlaybackStream() {
        String playbackStream = ResourceNameConversionUtils.getPlaybackStreamNameFromDeviceId(DEVICE_ID);
        assertEquals(playbackStream, DEVICE_ID);
    }

    @Test
    public void getDeviceIdFromPlaybackStreamName_WhenPlaybackStream_ReturnsDeviceId() {
        String deviceId = ResourceNameConversionUtils.getDeviceIdFromPlaybackStreamName(PLAYBACK_STREAM_NAME);
        assertEquals(deviceId, PLAYBACK_STREAM_NAME);
    }

    @Test
    public void getLivestreamSignalingChannelNameFromDeviceId_WhenDeviceId_ReturnsLivestreamSignalingChannel() {
        String livestreamSignalingChannel = ResourceNameConversionUtils.getLivestreamSignalingChannelNameFromDeviceId(DEVICE_ID);
        assertEquals(livestreamSignalingChannel, String.format(LIVE_STREAM_SIGNALING_CHANNEL, DEVICE_ID));
    }

    @Test
    public void getPlaybackSignalingChannelNameFromDeviceId_WhenDeviceId_ReturnsPlaybackSignalingChannel() {
        String playbackSignalingChannel = ResourceNameConversionUtils.getPlaybackSignalingChannelNameFromDeviceId(DEVICE_ID);
        assertEquals(playbackSignalingChannel, String.format(PLAYBACK_SIGNALING_CHANNEL, DEVICE_ID));
    }
}
