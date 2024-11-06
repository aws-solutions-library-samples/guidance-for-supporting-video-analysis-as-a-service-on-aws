package com.amazonaws.videoanalytics.devicemanagement.activity;

import java.io.IOException;

import com.amazonaws.videoanalytics.devicemanagement.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ShadowMap;
import com.amazonaws.videoanalytics.devicemanagement.UpdateDeviceShadowResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.iotdataplane.model.ThrottlingException;

import java.text.ParseException;
import java.util.Map;
import static java.util.Map.entry;

import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.THROTTLING_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_BODY_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DEVICE_ID;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.SHADOW_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class UpdateDeviceShadowActivityTest {
    @InjectMocks
    UpdateDeviceShadowActivity updateDeviceShadowActivity;
    @Mock
    private IotService iotService;
    @Mock
    private Context context;
    @Mock
    private LambdaLogger logger;

    private final Map<String, Object> lambdaProxyRequest = Map.ofEntries(
        entry(PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY, Map.ofEntries(
            entry(PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY, DEVICE_ID)
        )),
        entry(PROXY_LAMBDA_BODY_KEY, "{\"shadowPayload\": { \"shadowName\": \"" + SHADOW_NAME + "\", \"stateDocument\": { \"test\": true } } }")
    );

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    public void updateDeviceShadowActivity_WhenValidRequest_ReturnsResponse() throws IOException {
        UpdateDeviceShadowResponseContent responseFromIotService = UpdateDeviceShadowResponseContent
                .builder()
                .deviceId(DEVICE_ID)
                .build();

        when(iotService.updateDeviceShadow(eq(DEVICE_ID), any(ShadowMap.class))).thenReturn(responseFromIotService);
        Map<String, Object> responseMap = updateDeviceShadowActivity.handleRequest(lambdaProxyRequest, context);
        UpdateDeviceShadowResponseContent updateDeviceShadowResponse = UpdateDeviceShadowResponseContent.fromJson(parseBody(responseMap));
        assertEquals(DEVICE_ID, updateDeviceShadowResponse.getDeviceId());
    }

    @Test
    public void updateDeviceShadowActivity_WhenValidRequestNoShadowName_ReturnsResponse() throws IOException {
        UpdateDeviceShadowResponseContent responseFromIotService = UpdateDeviceShadowResponseContent
                .builder()
                .deviceId(DEVICE_ID)
                .build();

        when(iotService.updateDeviceShadow(eq(DEVICE_ID), any(ShadowMap.class))).thenReturn(responseFromIotService);
        Map<String, Object> responseMap = updateDeviceShadowActivity.handleRequest(lambdaProxyRequest, context);
        UpdateDeviceShadowResponseContent updateDeviceShadowResponse = UpdateDeviceShadowResponseContent.fromJson(parseBody(responseMap));
        assertEquals(DEVICE_ID, updateDeviceShadowResponse.getDeviceId());
    }

    @Test
    public void updateDeviceShadowActivity_WhenEmptyDeviceId_ThrowsInvalidInputException() throws IOException {
        Map<String, Object> lambdaProxyRequestEmptyDeviceId = Map.ofEntries(
            entry(PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY, Map.ofEntries(
                entry(PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY, " "),
                entry(PROXY_LAMBDA_BODY_KEY, "{\"shadowPayload\": { \"shadowName\": \"" + SHADOW_NAME + "\", \"stateDocument\": { \"test\": true } } }")
            ))
        );
        Map<String, Object> responseMap = updateDeviceShadowActivity.handleRequest(lambdaProxyRequestEmptyDeviceId, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), INVALID_INPUT_EXCEPTION);
    }

    @Test
    public void updateDeviceShadowActivity_WhenThrottlingException_ThrowsThrottlingException() throws IOException, ParseException, JsonProcessingException {
        when(iotService.updateDeviceShadow(eq(DEVICE_ID), any(ShadowMap.class))).thenThrow(ThrottlingException.builder().build());
        Map<String, Object> responseMap = updateDeviceShadowActivity.handleRequest(lambdaProxyRequest, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), THROTTLING_EXCEPTION);
    }

    @Test
    public void updateDeviceShadowActivity_WhenRuntimeException_ThrowsInternalException() throws IOException, ParseException, JsonProcessingException {
        when(iotService.updateDeviceShadow(eq(DEVICE_ID), any(ShadowMap.class))).thenThrow(RuntimeException.class);
        Map<String, Object> responseMap = updateDeviceShadowActivity.handleRequest(lambdaProxyRequest, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), INTERNAL_SERVER_EXCEPTION);
    }
}
