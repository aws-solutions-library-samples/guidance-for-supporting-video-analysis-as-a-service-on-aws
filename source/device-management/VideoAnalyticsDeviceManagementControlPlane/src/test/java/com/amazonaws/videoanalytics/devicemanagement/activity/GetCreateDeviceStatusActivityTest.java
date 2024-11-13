package com.amazonaws.videoanalytics.devicemanagement.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.devicemanagement.GetCreateDeviceStatusResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.Status;
import com.amazonaws.videoanalytics.devicemanagement.workflow.WorkflowManager;
import com.amazonaws.videoanalytics.devicemanagement.workflow.data.CreateDeviceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.RESOURCE_NOT_FOUND_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

public class GetCreateDeviceStatusActivityTest {
    private static final String JOB_ID = "jobId";
    private static final String DEVICE_ID = "deviceId";
    private static final Instant TIME_NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final CreateDeviceData CREATE_DEVICE_DATA = CreateDeviceData.builder()
            .deviceId(DEVICE_ID)
            .jobId(JOB_ID)
            .status(Status.RUNNING.toString())
            .lastUpdatedTime(TIME_NOW)
            .createTime(TIME_NOW)
            .build();

    @Mock
    private WorkflowManager workflowManager;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    private GetCreateDeviceStatusActivity getCreateDeviceStatusActivity;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(context.getLogger()).thenReturn(logger);
        getCreateDeviceStatusActivity = new GetCreateDeviceStatusActivity(workflowManager);
    }

    @Test
    public void handleRequest_Success() {
        when(workflowManager.getCreateDeviceStatus(JOB_ID)).thenReturn(CREATE_DEVICE_DATA);
        Map<String, Object> input = Map.of("pathParameters", Map.of("jobId", JOB_ID));

        Map<String, Object> response = getCreateDeviceStatusActivity.handleRequest(input, context);

        assertNotNull(response);
        assertEquals(200, response.get("statusCode"));
        String body = (String) response.get("body");
        assertNotNull(body);
    }

    @Test
    public void handleRequest_EmptyJobId() {
        Map<String, Object> input = Map.of("pathParameters", Map.of("jobId", ""));

        Map<String, Object> response = getCreateDeviceStatusActivity.handleRequest(input, context);

        assertEquals(400, response.get("statusCode"));
        String body = (String) response.get("body");
        assertNotNull(body);
        assert(body.contains(INVALID_INPUT_EXCEPTION));
    }

    @Test
    public void handleRequest_ResourceNotFound() {
        when(workflowManager.getCreateDeviceStatus(anyString()))
                .thenThrow(ResourceNotFoundException.builder()
                        .message(RESOURCE_NOT_FOUND_EXCEPTION)
                        .build());
        Map<String, Object> input = Map.of("pathParameters", Map.of("jobId", JOB_ID));

        Map<String, Object> response = getCreateDeviceStatusActivity.handleRequest(input, context);

        assertEquals(404, response.get("statusCode"));
        String body = (String) response.get("body");
        assertNotNull(body);
        assert(body.contains(RESOURCE_NOT_FOUND_EXCEPTION));
    }

    @Test
    public void handleRequest_InternalServerError() {
        when(workflowManager.getCreateDeviceStatus(anyString()))
                .thenThrow(new RuntimeException("Internal error"));
        Map<String, Object> input = Map.of("pathParameters", Map.of("jobId", JOB_ID));

        Map<String, Object> response = getCreateDeviceStatusActivity.handleRequest(input, context);

        assertEquals(500, response.get("statusCode"));
        String body = (String) response.get("body");
        assertNotNull(body);
        assert(body.contains(INTERNAL_SERVER_EXCEPTION));
    }

    @Test
    public void handleRequest_InvalidPathParameters() {
        Map<String, Object> input = Map.of();

        Map<String, Object> response = getCreateDeviceStatusActivity.handleRequest(input, context);

        assertEquals(400, response.get("statusCode"));
        String body = (String) response.get("body");
        assertNotNull(body);
        assert(body.contains(INVALID_INPUT_EXCEPTION));
    }
}

