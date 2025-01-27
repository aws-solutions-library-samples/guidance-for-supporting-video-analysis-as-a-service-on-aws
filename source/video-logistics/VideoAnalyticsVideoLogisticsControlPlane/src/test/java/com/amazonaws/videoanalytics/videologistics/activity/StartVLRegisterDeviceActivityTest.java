package com.amazonaws.videoanalytics.videologistics.activity;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.RESOURCE_NOT_FOUND;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.DEVICE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.videologistics.ResourceNotFoundExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.dao.VLRegisterDeviceJobDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import com.amazonaws.videoanalytics.videologistics.validator.DeviceValidator;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

class StartVLRegisterDeviceActivityTest {

    @Mock
    private DeviceValidator deviceValidator;

    @Mock
    private VLRegisterDeviceJobDAO vlRegisterDeviceJobDAO;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger lambdaLogger;

    private StartVLRegisterDeviceActivity activity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(context.getLogger()).thenReturn(lambdaLogger);
        when(deviceValidator.validateDeviceExists(eq(DEVICE_ID), any())).thenReturn(true);
        activity = new StartVLRegisterDeviceActivity(deviceValidator, vlRegisterDeviceJobDAO);
    }

    @Test
    void handleRequest_ValidInput_Returns200WithJobId() {
        Map<String, Object> input = createValidInput(DEVICE_ID);

        Map<String, Object> response = activity.handleRequest(input, context);

        assertEquals(200, response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY));
        ArgumentCaptor<VLRegisterDeviceJob> jobCaptor = ArgumentCaptor.forClass(VLRegisterDeviceJob.class);
        verify(vlRegisterDeviceJobDAO).save(jobCaptor.capture());
        VLRegisterDeviceJob savedJob = jobCaptor.getValue();
        assertEquals(DEVICE_ID, savedJob.getDeviceId());
        assertEquals("RUNNING", savedJob.getStatus());
        assertNotNull(savedJob.getJobId());
        assertNotNull(savedJob.getCreateTime());
        assertNotNull(savedJob.getLastUpdated());
    }

    @Test
    void handleRequest_NullInput_Returns400() {
        Map<String, Object> response = activity.handleRequest(null, context);
        assertEquals(400, response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY));
    }

    @Test
    void handleRequest_InvalidJson_Returns400() {
        Map<String, Object> input = new HashMap<>();
        input.put("body", "invalid json");

        Map<String, Object> response = activity.handleRequest(input, context);
        assertEquals(400, response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY));
    }

    @Test
    void handleRequest_DynamoDbSaveFails_Returns500() {
        Map<String, Object> input = createValidInput(DEVICE_ID);
        doThrow(AwsServiceException.builder().build()).when(vlRegisterDeviceJobDAO).save(any());

        Map<String, Object> response = activity.handleRequest(input, context);
        assertEquals(500, response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY));
    }

    @Test
    public void handleRequest_WhenDeviceDoesNotExist_ThrowsResourceNotFoundException() throws IOException {
        Map<String, Object> input = createValidInput(DEVICE_ID);
        when(deviceValidator.validateDeviceExists(eq(DEVICE_ID), any())).thenReturn(false);
        Map<String, Object> responseMap = activity.handleRequest(input, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 404);
        ResourceNotFoundExceptionResponseContent exception = ResourceNotFoundExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), RESOURCE_NOT_FOUND);
    }

    private Map<String, Object> createValidInput(String deviceId) {
        Map<String, Object> input = new HashMap<>();
        Map<String, String> pathParameters = new HashMap<>();
        pathParameters.put("deviceId", deviceId);
        input.put(PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY, pathParameters);
        return input;
    }
}
