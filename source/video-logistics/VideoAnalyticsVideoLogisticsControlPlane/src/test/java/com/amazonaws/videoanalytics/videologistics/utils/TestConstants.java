package com.amazonaws.videoanalytics.videologistics.utils;

import java.util.Date;

public final class TestConstants {
    public static final String MOCK_AWS_REGION = "mock-region-value";
    public static final String EXPECTED_UUID = "12345678-abcd-2468-dcba-87654321abcd";

    public static final String DEVICE_ID = "testDeviceId";
    public static final String CLIENT_ID = "testClientId";
    public static final String SHADOW_NAME = "testShadowName";
    public static final String SESSION_ID = "testSessionId";
    public static final String WORKFLOW_NAME = "87654321-dcba-1357-abcd-12345678dcba";
    public final static String START_TIMESTAMP = "2023-02-17T16:20:01Z";
    public final static Date START_TIMESTAMP_DATE = new Date("Fri Feb 17 16:20:01 UTC 2023");
    public final static String END_TIMESTAMP = "2023-02-17T16:26:01Z";
    public final static Date END_TIMESTAMP_DATE = new Date("Fri Feb 17 16:26:01 UTC 2023");
    public static final String PLAYBACK_STREAM_NAME = "testPlaybackStreamName";
    public static final String HLS_STREAMING_URL = "arn:aws:kinesisvideo:us-west-2:123456789012:hls/device-123/0123456789012";
    public static final String SIGNALING_CHANNEL_ARN = "arn:aws:kinesisvideo:us-west-2:123412345494:channel/testDeviceId-LiveStreamSignalingChannel/1731962582824";
    public static final String HTTPS_RESOURCE_ENDPOINT = "https://r-fd57ec7b.kinesisvideo.us-west-2.amazonaws.com";
    public static final String WSS_RESOURCE_ENDPOINT = "wss://v-5dc527ea.kinesisvideo.us-west-2.amazonaws.com";
    public static final String DATA_ENDPOINT = "https://b-35e552dc.kinesisvideo.us-west-2.amazonaws.com";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
}
