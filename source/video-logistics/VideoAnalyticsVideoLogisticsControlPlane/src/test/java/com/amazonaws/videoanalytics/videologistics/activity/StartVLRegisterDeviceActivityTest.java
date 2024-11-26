package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.videologistics.dao.VLRegisterDeviceJobDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import com.amazonaws.videoanalytics.videologistics.validator.DeviceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        activity = new StartVLRegisterDeviceActivity(deviceValidator, vlRegisterDeviceJobDAO);
    }

    @Test
    void handleRequest_ValidInput_Returns200WithJobId() {
        String deviceId = "testDevice";
        Map<String, Object> input = createValidInput(deviceId);

        Map<String, Object> response = activity.handleRequest(input, context);

        assertEquals(200, response.get("statusCode"));
        ArgumentCaptor<VLRegisterDeviceJob> jobCaptor = ArgumentCaptor.forClass(VLRegisterDeviceJob.class);
        verify(vlRegisterDeviceJobDAO).save(jobCaptor.capture());
        VLRegisterDeviceJob savedJob = jobCaptor.getValue();
        assertEquals(deviceId, savedJob.getDeviceId());
        assertEquals("RUNNING", savedJob.getStatus());
        assertNotNull(savedJob.getJobId());
        assertNotNull(savedJob.getCreateTime());
        assertNotNull(savedJob.getLastUpdated());
    }

    @Test
    void handleRequest_NullInput_Returns400() {
        Map<String, Object> response = activity.handleRequest(null, context);
        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void handleRequest_InvalidJson_Returns400() {
        Map<String, Object> input = new HashMap<>();
        input.put("body", "invalid json");

        Map<String, Object> response = activity.handleRequest(input, context);
        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void handleRequest_DeviceValidationFails_Returns500() {
        Map<String, Object> input = createValidInput("testDevice");
        doThrow(new RuntimeException("Device validation failed"))
                .when(deviceValidator)
                .validateDeviceExists(any());

        Map<String, Object> response = activity.handleRequest(input, context);

        assertEquals(500, response.get("statusCode"));
    }

    @Test
    void handleRequest_DynamoDbSaveFails_Returns500() {
        Map<String, Object> input = createValidInput("testDevice");
        doThrow(AwsServiceException.builder().build()).when(vlRegisterDeviceJobDAO).save(any());

        Map<String, Object> response = activity.handleRequest(input, context);
        assertEquals(500, response.get("statusCode"));
    }

    private Map<String, Object> createValidInput(String deviceId) {
        Map<String, Object> input = new HashMap<>();
        Map<String, String> pathParameters = new HashMap<>();
        pathParameters.put("deviceId", deviceId);
        input.put("pathParameters", pathParameters);
        return input;
    }
}
