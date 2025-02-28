package com.amazonaws.videoanalytics.videologistics.activity;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.DEVICE_NOT_REGISTERED;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.RESOURCE_NOT_FOUND;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_BODY_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.CLIENT_ID;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.DEVICE_ID;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.videologistics.CreateLivestreamSessionResponseContent;
import com.amazonaws.videoanalytics.videologistics.IceServer;
import com.amazonaws.videoanalytics.videologistics.ResourceNotFoundExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.dependency.kvs.KvsService;
import com.amazonaws.videoanalytics.videologistics.utils.KVSWebRTCUtils;
import com.amazonaws.videoanalytics.videologistics.validator.DeviceValidator;

import software.amazon.awssdk.services.kinesisvideo.model.ResourceNotFoundException;

public class CreateLivestreamSessionActivityTest {
    @Mock
    private DeviceValidator deviceValidator;
    @Mock
    private KvsService kvsService;
    @Mock
    private KVSWebRTCUtils kvsWebRTCUtils;
    @Mock
    private LambdaLogger logger;
    @Mock
    private Context context;

    @InjectMocks
    private CreateLivestreamSessionActivity createLivestreamSessionActivity;

    private final List<IceServer> iceServerList = Arrays.asList();

    private final Map<String, Object> lambdaProxyRequest = Map.ofEntries(
        entry(PROXY_LAMBDA_BODY_KEY, "{\"deviceId\": \"" + DEVICE_ID + "\", \"clientId\": \""+ CLIENT_ID +"\"}")
    );

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(context.getLogger()).thenReturn(logger);
        when(deviceValidator.validateDeviceExists(eq(DEVICE_ID), any())).thenReturn(true);
    }

    @Test
    public void handleRequest_WhenValidRequest_ReturnsResponse() throws IOException {
        when(kvsWebRTCUtils.sign(any(), any(), any())).thenReturn("presignedUrl");
        when(kvsService.getSyncIceServerConfigs(any(), any())).thenReturn(iceServerList);
        Map<String, Object> response = createLivestreamSessionActivity.handleRequest(lambdaProxyRequest, context);
        CreateLivestreamSessionResponseContent createLivestreamSessionResponse = 
            CreateLivestreamSessionResponseContent.fromJson(parseBody(response));
        assertEquals(CLIENT_ID, createLivestreamSessionResponse.getClientId());
        assertEquals("presignedUrl", createLivestreamSessionResponse.getSignalingChannelURL());
        assertEquals(iceServerList, createLivestreamSessionResponse.getIceServers());
    }

    @Test
    public void handleRequest_WhenNullRequest_ThrowsValidationException() throws IOException {
        Map<String, Object> response = createLivestreamSessionActivity.handleRequest(null, context);
        assertEquals(response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(response));
        assertEquals(exception.getMessage(), INVALID_INPUT_EXCEPTION);
    }

    @Test
    public void handleRequest_WhenEmptyRequest_ThrowsValidationException() throws IOException {
        Map<String, Object> lambdaProxyRequestEmpty = Map.ofEntries();
        Map<String, Object> responseMap = createLivestreamSessionActivity.handleRequest(lambdaProxyRequestEmpty, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), INVALID_INPUT_EXCEPTION);
    }

    @Test
    public void handleRequest_WhenResourceNotFoundException_ThrowsInternalServerException() throws IOException {
        when(kvsService.getSignalingChannelArnFromName(any())).thenThrow(ResourceNotFoundException.builder().build());
        Map<String, Object> responseMap = createLivestreamSessionActivity.handleRequest(lambdaProxyRequest, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 404);
        ResourceNotFoundExceptionResponseContent exception = ResourceNotFoundExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), DEVICE_NOT_REGISTERED);
    }

    @Test
    public void handleRequest_WhenDeviceDoesNotExist_ThrowsResourceNotFoundException() throws IOException {
        when(deviceValidator.validateDeviceExists(eq(DEVICE_ID), any())).thenReturn(false);
        Map<String, Object> responseMap = createLivestreamSessionActivity.handleRequest(lambdaProxyRequest, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 404);
        ResourceNotFoundExceptionResponseContent exception = ResourceNotFoundExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), RESOURCE_NOT_FOUND);
    }

    // @Test
    // public void createLivestreamSessionActivity_InjectsDependencies() {
    //     EnvironmentVariables environmentVariables = new EnvironmentVariables();
    //     environmentVariables.set("AWS_REGION", "us-east-1");
    //     CreateLivestreamSessionActivity activity = new CreateLivestreamSessionActivity();
    //     assertNotNull(activity);
    // }
}
