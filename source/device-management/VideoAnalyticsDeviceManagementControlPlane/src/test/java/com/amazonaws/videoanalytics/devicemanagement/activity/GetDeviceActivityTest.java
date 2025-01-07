package com.amazonaws.videoanalytics.devicemanagement.activity;

import java.io.IOException;

import com.amazonaws.videoanalytics.devicemanagement.CloudVideoStreamingElement;
import com.amazonaws.videoanalytics.devicemanagement.DeviceConnection;
import com.amazonaws.videoanalytics.devicemanagement.DeviceMetaData;
import com.amazonaws.videoanalytics.devicemanagement.DeviceState;
import com.amazonaws.videoanalytics.devicemanagement.DeviceStatus;
import com.amazonaws.videoanalytics.devicemanagement.GetDeviceResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.IpAddress;
import com.amazonaws.videoanalytics.devicemanagement.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.apache.logging.log4j.util.Strings;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.iotdataplane.model.ThrottlingException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;

import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.JSON_PROCESSING_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.THROTTLING_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DATE;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DEVICE_ID;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DEVICE_TYPE_NAME;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.MOCK_AWS_REGION;
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
        entry(PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY, Map.ofEntries(
            entry(PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY, DEVICE_ID)
        ))
    );

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    public void handleRequest_WhenValidRequest_ReturnsResponse() throws IOException, JsonProcessingException, ParseException {
        Map<String, String> deviceSettings = new HashMap<>();
        deviceSettings.put("videoSettings", "{\"profile1\":{\"codec\":\"H264\"}}");

        Map<String, String> deviceCapabilities = new HashMap<>();
        deviceCapabilities.put("videoCapabilities","{\"codec\":\"H264\"}");

        DeviceMetaData deviceMetaData = buildExpectedDeviceMetaData();
        GetDeviceResponseContent responseFromIotService = GetDeviceResponseContent
                .builder()
                .deviceId(DEVICE_ID)
                .deviceType(DEVICE_TYPE_NAME)
                .deviceSettings(deviceSettings)
                .deviceCapabilities(deviceCapabilities)
                .deviceMetaData(deviceMetaData)
                .build();

        when(iotService.getDevice(DEVICE_ID)).thenReturn(responseFromIotService);
        Map<String, Object> responseMap = getDeviceActivity.handleRequest(lambdaProxyRequest, context);
        GetDeviceResponseContent getDeviceResponse = GetDeviceResponseContent.fromJson(parseBody(responseMap));
        assertEquals(DEVICE_ID, getDeviceResponse.getDeviceId());
        assertEquals(DEVICE_TYPE_NAME, getDeviceResponse.getDeviceType());
        assertEquals(deviceMetaData, getDeviceResponse.getDeviceMetaData());
        assertEquals(deviceCapabilities, getDeviceResponse.getDeviceCapabilities());
        assertEquals(deviceSettings, getDeviceResponse.getDeviceSettings());
    }

    @Test
    public void handleRequest_WhenEmptyRequest_ThrowsValidationException() throws IOException {
        Map<String, Object> lambdaProxyRequestEmpty = Map.ofEntries();
        Map<String, Object> responseMap = getDeviceActivity.handleRequest(lambdaProxyRequestEmpty, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), INVALID_INPUT_EXCEPTION);
    }

    @Test
    public void handleRequest_WhenEmptyDeviceId_ThrowsValidationException() throws IOException {
        Map<String, Object> lambdaProxyRequestEmptyDeviceId = Map.ofEntries(
            entry(PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY, Map.ofEntries(
                entry(PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY, "")
            ))
        );
        Map<String, Object> responseMap = getDeviceActivity.handleRequest(lambdaProxyRequestEmptyDeviceId, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), INVALID_INPUT_EXCEPTION);
    }

    @Test
    public void handleRequest_WhenThrottlingException_ThrowsInternalServerException() throws IOException, ParseException, JsonProcessingException {
        when(iotService.getDevice(DEVICE_ID)).thenThrow(ThrottlingException.builder().build());
        Map<String, Object> responseMap = getDeviceActivity.handleRequest(lambdaProxyRequest, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), THROTTLING_EXCEPTION);
    }

    @Test
    public void handleRequest_WhenJsonProcessingException_ThrowsInternalServerException() throws IOException, ParseException, JsonProcessingException {
        when(iotService.getDevice(DEVICE_ID)).thenThrow(JsonProcessingException.class);
        Map<String, Object> responseMap = getDeviceActivity.handleRequest(lambdaProxyRequest, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), JSON_PROCESSING_EXCEPTION);
    }

    @Test
    public void handleRequest_WhenRuntimeException_ThrowsInternalServerException() throws IOException, ParseException, JsonProcessingException {
        when(iotService.getDevice(DEVICE_ID)).thenThrow(RuntimeException.class);
        Map<String, Object> responseMap = getDeviceActivity.handleRequest(lambdaProxyRequest, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), INTERNAL_SERVER_EXCEPTION);
    }

    @Test
    public void getDeviceActivity_InjectsDependencies() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        environmentVariables.set("AWS_REGION", MOCK_AWS_REGION);
        GetDeviceActivity getDeviceActivityDagger = new GetDeviceActivity();
        getDeviceActivityDagger.assertPrivateFieldNotNull();
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

        List<CloudVideoStreamingElement> videoStreamingStateList = new ArrayList<>();

        DeviceStatus deviceStatus = DeviceStatus
                .builder()
                .deviceState(DeviceState.ENABLED)
                .deviceConnection(deviceConnection)
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