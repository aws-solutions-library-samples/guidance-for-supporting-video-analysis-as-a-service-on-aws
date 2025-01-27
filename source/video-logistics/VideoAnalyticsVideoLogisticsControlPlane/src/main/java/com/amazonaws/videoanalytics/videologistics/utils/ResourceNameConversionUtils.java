package com.amazonaws.videoanalytics.videologistics.utils;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.LIVE_STREAM_SIGNALING_CHANNEL;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PLAYBACK_SIGNALING_CHANNEL;

public class ResourceNameConversionUtils {
    private ResourceNameConversionUtils() {}

    public static String getPlaybackStreamNameFromDeviceId(final String deviceId) {
        return deviceId;
    }
    public static String getDeviceIdFromPlaybackStreamName(final String playbackStreamName) {
        return playbackStreamName;
    }

    public static String getPlaybackSignalingChannelNameFromDeviceId(final String deviceId) {
        return String.format(PLAYBACK_SIGNALING_CHANNEL, deviceId);
    }

    public static String getLivestreamSignalingChannelNameFromDeviceId(final String deviceId) {
        return String.format(LIVE_STREAM_SIGNALING_CHANNEL, deviceId);
    }
}

