package com.amazonaws.videoanalytics.devicemanagement.activity;

import java.io.IOException;

import com.amazonaws.videoanalytics.devicemanagement.CloudVideoStreamingElement;
import com.amazonaws.videoanalytics.devicemanagement.DeviceConnection;
import com.amazonaws.videoanalytics.devicemanagement.DeviceMetaData;
import com.amazonaws.videoanalytics.devicemanagement.DeviceState;
import com.amazonaws.videoanalytics.devicemanagement.DeviceStatus;
import com.amazonaws.videoanalytics.devicemanagement.GetDeviceResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.IpAddress;
import com.amazonaws.videoanalytics.devicemanagement.StorageElement;
import com.amazonaws.videoanalytics.devicemanagement.StorageState;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.iot.model.ThrottlingException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;

import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.THROTTLING_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_BODY_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DATE;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DEVICE_ID;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DEVICE_GROUP_ID;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DEVICE_TYPE_NAME;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class GetDeviceActivityTest {
    @InjectMocks
    GetDeviceActivity getDeviceActivity;
    @Mock
    private IotService iotService;
    @Mock
    private Context context;
    @Mock
    private LambdaLogger logger;

    private final Map<String, Object> lambdaProxyRequest = Map.ofEntries(
        entry("pathParameters", Map.ofEntries(
            entry("deviceId", DEVICE_ID)
        ))
    );

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    public void getDeviceActivity_WhenValidRequest_ReturnsResponse() throws IOException, JsonProcessingException, ParseException {
        Map<String, String> deviceSettings = new HashMap<>();
        deviceSettings.put("videoSettings", "{\"profile1\":{\"codec\":\"H264\"}}");

        Map<String, String> deviceCapabilities = new HashMap<>();
        deviceCapabilities.put("videoCapabilities","{\"codec\":\"H264\"}");

        DeviceMetaData deviceMetaData = buildExpectedDeviceMetaData();
        List<String> deviceGroupIdsList = new ArrayList<>();
        deviceGroupIdsList.add(DEVICE_GROUP_ID);
        GetDeviceResponseContent responseFromIotService = GetDeviceResponseContent
                .builder()
                .deviceId(DEVICE_ID)
                .deviceGroupIds(deviceGroupIdsList)
                .deviceType(DEVICE_TYPE_NAME)
                .deviceSettings(deviceSettings)
                .deviceCapabilities(deviceCapabilities)
                .deviceMetaData(deviceMetaData)
                .build();

        when(iotService.getDevice(DEVICE_ID)).thenReturn(responseFromIotService);
        Map<String, Object> response = getDeviceActivity.handleRequest(lambdaProxyRequest, context);
        ObjectMapper mapper = new ObjectMapper();
        GetDeviceResponseContent getDeviceResponse = GetDeviceResponseContent.fromJson(mapper.convertValue(response.get("body"), String.class));
        assertEquals(DEVICE_ID, getDeviceResponse.getDeviceId());
        assertEquals(DEVICE_GROUP_ID, getDeviceResponse.getDeviceGroupIds().get(0));
        assertEquals(DEVICE_TYPE_NAME, getDeviceResponse.getDeviceType());
        assertEquals(deviceMetaData, getDeviceResponse.getDeviceMetaData());
        assertEquals(deviceCapabilities, getDeviceResponse.getDeviceCapabilities());
        assertEquals(deviceSettings, getDeviceResponse.getDeviceSettings());
    }

    @Test
    public void getDeviceActivity_WhenEmptyDeviceId_ThrowsInvalidInputException() {
        Map<String, Object> lambdaProxyRequestEmptyDeviceId = Map.ofEntries(
            entry("pathParameters", Map.ofEntries(
                entry("deviceId", "")
            ))
        );
        Map<String, Object> responseMap = getDeviceActivity.handleRequest(lambdaProxyRequestEmptyDeviceId, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_BODY_KEY), INVALID_INPUT_EXCEPTION);
    }

    @Test
    public void getDeviceActivity_WhenThrottlingException_ThrowsThrottlingException() throws ParseException, JsonProcessingException {
        when(iotService.getDevice(DEVICE_ID)).thenThrow(ThrottlingException.builder().build());
        Map<String, Object> responseMap = getDeviceActivity.handleRequest(lambdaProxyRequest, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_BODY_KEY), THROTTLING_EXCEPTION);
    }

    @Test
    public void getDeviceActivity_WhenRuntimeException_ThrowsInternalException() throws ParseException, JsonProcessingException {
        when(iotService.getDevice(DEVICE_ID)).thenThrow(RuntimeException.class);
        Map<String, Object> responseMap = getDeviceActivity.handleRequest(lambdaProxyRequest, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_BODY_KEY), INTERNAL_SERVER_EXCEPTION);
    }

    private DeviceMetaData buildExpectedDeviceMetaData() {
        IpAddress ipAddress = IpAddress
                .builder()
                .privateIpAddress(Strings.EMPTY)
                .publicIpAddress(Strings.EMPTY)
                .build();

        DeviceConnection deviceConnection = DeviceConnection
                .builder()
                .status(false)
                .updatedAt(new Date(DATE))
                .build();

        StorageElement storageState = StorageElement
                .builder()
                .status(StorageState.FULL)
                .totalCapacity(Strings.EMPTY)
                .usedCapacity(Strings.EMPTY)
                .updatedAt(new Date(DATE))
                .build();

        List<StorageElement> storageList = new ArrayList<>();
        storageList.add(storageState);

        List<CloudVideoStreamingElement> videoStreamingStateList = new ArrayList<>();

        DeviceStatus deviceStatus = DeviceStatus
                .builder()
                .deviceState(DeviceState.ENABLED)
                .deviceConnection(deviceConnection)
                .storage(storageList)
                .cloudVideoStreaming(videoStreamingStateList)
                .build();

        return DeviceMetaData
                .builder()
                .aiChipset(Strings.EMPTY)
                .aiModelVersion(Strings.EMPTY)
                .aiSdkVersion(Strings.EMPTY)
                .mac(Strings.EMPTY)
                .firmwareVersion(Strings.EMPTY)
                .sdkVersion(Strings.EMPTY)
                .ipAddress(ipAddress)
                .deviceStatus(deviceStatus)
                .build();
    }
}