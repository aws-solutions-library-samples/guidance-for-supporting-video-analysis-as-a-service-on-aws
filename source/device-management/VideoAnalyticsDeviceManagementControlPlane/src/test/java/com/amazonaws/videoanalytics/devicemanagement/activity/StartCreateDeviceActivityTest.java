package com.amazonaws.videoanalytics.devicemanagement.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.devicemanagement.StartCreateDeviceRequestContent;
import com.amazonaws.videoanalytics.devicemanagement.StartCreateDeviceResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.workflow.WorkflowManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;

import java.util.Map;
import static java.util.Map.entry;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parseBody;

public class StartCreateDeviceActivityTest {
    private static final String JOB_ID = "jobId";
    private static final String DEVICE_ID = "deviceId";
    private static final String CERTIFICATE_ID = "certId";

    @Mock
    private WorkflowManager workflowManager;
    @Mock
    private Context context;
    @Mock
    private LambdaLogger logger;

    private StartCreateDeviceActivity startCreateDeviceActivity;

    private final Map<String, Object> lambdaProxyRequest = Map.ofEntries(
        entry("pathParameters", Map.ofEntries(
            entry("deviceId", DEVICE_ID)
        )),
        entry("body", "{\"certificateId\": \"" + CERTIFICATE_ID + "\"}")
    );

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(context.getLogger()).thenReturn(logger);
        startCreateDeviceActivity = new StartCreateDeviceActivity(workflowManager);
    }

    @Test
    public void startCreateDevice_WhenValidRequest_ReturnsResponse() throws Exception {
        when(workflowManager.startCreateDevice(DEVICE_ID, CERTIFICATE_ID)).thenReturn(JOB_ID);

        Map<String, Object> response = startCreateDeviceActivity.handleRequest(lambdaProxyRequest, context);
        
        assertEquals(200, response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY));
        StartCreateDeviceResponseContent responseContent = StartCreateDeviceResponseContent.fromJson(parseBody(response));
        assertEquals(JOB_ID, responseContent.getJobId());
    }

    @Test
    public void startCreateDevice_WhenInternalError_ReturnsInternalServerError() throws Exception {
        when(workflowManager.startCreateDevice(DEVICE_ID, CERTIFICATE_ID))
                .thenThrow(new RuntimeException("Failed to create device"));

        Map<String, Object> response = startCreateDeviceActivity.handleRequest(lambdaProxyRequest, context);
        
        assertEquals(500, response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY));
        InternalServerExceptionResponseContent responseContent = InternalServerExceptionResponseContent.fromJson(parseBody(response));
        assertEquals(INTERNAL_SERVER_EXCEPTION, responseContent.getMessage());
    }

    @Test
    public void startCreateDevice_WhenEmptyDeviceId_ReturnsValidationError() throws Exception {
        Map<String, Object> requestWithEmptyDeviceId = Map.ofEntries(
            entry("pathParameters", Map.ofEntries(
                entry("deviceId", "")
            )),
            entry("body", "{\"certificateId\": \"" + CERTIFICATE_ID + "\"}")
        );

        Map<String, Object> response = startCreateDeviceActivity.handleRequest(requestWithEmptyDeviceId, context);
        
        assertEquals(400, response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY));
        ValidationExceptionResponseContent responseContent = ValidationExceptionResponseContent.fromJson(parseBody(response));
        assertEquals(INVALID_INPUT_EXCEPTION, responseContent.getMessage());
    }

    @Test
    public void startCreateDevice_WhenEmptyCertificateId_ReturnsValidationError() throws Exception {
        Map<String, Object> requestWithEmptyCertId = Map.ofEntries(
            entry("pathParameters", Map.ofEntries(
                entry("deviceId", DEVICE_ID)
            )),
            entry("body", "{\"certificateId\": \"\"}")
        );

        Map<String, Object> response = startCreateDeviceActivity.handleRequest(requestWithEmptyCertId, context);
        
        assertEquals(400, response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY));
        ValidationExceptionResponseContent responseContent = ValidationExceptionResponseContent.fromJson(parseBody(response));
        assertEquals(INVALID_INPUT_EXCEPTION, responseContent.getMessage());
    }

    @Test
    public void startCreateDevice_WhenResourceNotFound_ReturnsError() throws Exception {
        when(workflowManager.startCreateDevice(DEVICE_ID, CERTIFICATE_ID))
                .thenThrow(ResourceNotFoundException.builder().build());

        Map<String, Object> response = startCreateDeviceActivity.handleRequest(lambdaProxyRequest, context);
        
        assertEquals(404, response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY));
    }
}