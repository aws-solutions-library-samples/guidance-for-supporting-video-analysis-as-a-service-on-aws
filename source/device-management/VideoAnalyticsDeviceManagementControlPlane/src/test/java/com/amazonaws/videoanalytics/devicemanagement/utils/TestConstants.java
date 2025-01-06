package com.amazonaws.videoanalytics.devicemanagement.utils;

public final class TestConstants {
    public static final String MOCK_AWS_REGION = "mock-region-value";

    public static final String AI_CHIP_SET_VALUE = "novatek";
    public static final String AI_MODEL_VERSION_VALUE = "0.0.1";
    public static final String AI_SDK_VERSION_VALUE = "0.1.0";
    public static final String DEVICE_ID = "11111111111";
    public static final String DEVICE_TYPE_NAME = "TestThingTypeName";
    public static final String FIRMWARE_VERSION_VALUE = "20230331 1.0.1";
    public static final String MAC_VALUE = "11::22::33::44::55::66";
    public static final String MANUFACTURER_VALUE = "TestManufacturer";
    public static final String MODEL_VALUE = "IPC-DM2A1-W";
    public static final String PRIVATE_IP_VALUE = "192.168.6.10";
    public static final String PUBLIC_IP_VALUE  = "10.15.1.100";
    public static final String SD_CARD_ID = "sc1";
    public static final String SHADOW_NAME = "testShadowName";
    public static final String TEST_ATTRIBUTE_KEY = "testKey";
    public static final String TEST_ATTRIBUTE_VALUE = "testValue";
    public static final String TEST_DEVICE_CAP_VAL = "{\"videoCapabilities\":{\"bitRate\":{\"CBR\":\"10,20,30\",\"VBR\":\"1,2,3\"}}}";
    public static final String THING_ARN = "arn:aws:iot:us-west-2:123456781234:thing/TestThing";
    public static final String THING_ID = "12345678-abcd-8765-dcba-1234abcd5678";
    public static final long SD_CARD_TOTAL_CAPACITY = 14887;
    public static final long SD_CARD_USED_CAPACITY = 40;
    public static final long TIMESTAMP = 1675268761;
    public static final String CERTIFICATE_ID = "fakeCertificateID";
    public static final String DATE = "Wed Feb 01 16:26:01 UTC 2023";
    public static final Long DATE_LONG = 1675268761000L;
    public static final Long DATE_LONG_IN_MILLIS = 1675268761000L / 1000;

    public final static String FLEET_INDEXING_QUERY_STRING = "connectivity.connected:true";

    public final static String EXPECTED_DEVICE_SETTINGS_STRING = "{videoSettings=[{\"codec\":\"H264\",\"bitRateType\":\"VBR\",\"frameRateLimit\":15,\"gopLength\":30,\"profileId\":\"vec2\",\"resolution\":{\"width\":320,\"height\":240},\"bitRateLimit\":128}, {\"codec\":\"H264\",\"bitRateType\":\"CBR\",\"frameRateLimit\":30,\"gopLength\":10,\"profileId\":\"vec1\",\"resolution\":{\"width\":1280,\"height\":720},\"bitRateLimit\":800}]}";
}

