package com.amazonaws.videoanalytics.schema.util;

import static com.amazonaws.videoanalytics.schema.util.GuidanceVLConstants.LIVE_STREAM_SIGNALING_CHANNEL;

public class ResourceNameConversionUtils {
    private ResourceNameConversionUtils() {}

    public static String getPlaybackStreamNameFromDeviceId(final String deviceId) {
        return deviceId;
    }
    public static String getDeviceIdFromPlaybackStreamName(final String playbackStreamName) {
        return playbackStreamName;
    }

    public static String getPlaybackSignalingChannelNameFromDeviceId(final String deviceId) {
        return deviceId;
    }

    public static String getLivestreamSignalingChannelNameFromDeviceId(final String deviceId) {
        return String.format(LIVE_STREAM_SIGNALING_CHANNEL, deviceId);
    }

    public static String getDeviceIdFromPlaybackSignalingChannelName(final String signalingChannelName) {
        return signalingChannelName;
    }
}
