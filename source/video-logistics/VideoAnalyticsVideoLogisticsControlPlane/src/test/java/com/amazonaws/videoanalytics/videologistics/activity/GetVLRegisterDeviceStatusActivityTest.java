package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.videologistics.GetVLRegisterDeviceStatusResponseContent;
import com.amazonaws.videoanalytics.videologistics.KVSStreamARNs;
import com.amazonaws.videoanalytics.videologistics.Status;
import com.amazonaws.videoanalytics.videologistics.dao.VLRegisterDeviceJobDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

public class GetVLRegisterDeviceStatusActivityTest {

    private GetVLRegisterDeviceStatusActivity activity;

    @Mock
    private VLRegisterDeviceJobDAO vlRegisterDeviceJobDAO;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(context.getLogger()).thenReturn(logger);
        activity = new GetVLRegisterDeviceStatusActivity(vlRegisterDeviceJobDAO, objectMapper);
    }

    @Test
    void handleRequest_NullInput_Returns400() {
        Map<String, Object> response = activity.handleRequest(null, context);
        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void handleRequest_NoPathParameters_Returns400() {
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> response = activity.handleRequest(input, context);
        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void handleRequest_NoJobIdInPathParameters_Returns400() {
        Map<String, Object> input = new HashMap<>();
        input.put("pathParameters", new HashMap<String, String>());
        Map<String, Object> response = activity.handleRequest(input, context);
        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void handleRequest_JobNotFound_Returns404() {
        String jobId = "test-job-id";
        Map<String, Object> input = new HashMap<>();
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("jobId", jobId);
        input.put("pathParameters", pathParams);

        when(vlRegisterDeviceJobDAO.load(jobId)).thenReturn(null);

        Map<String, Object> response = activity.handleRequest(input, context);
        assertEquals(404, response.get("statusCode"));
    }

    @Test
    void handleRequest_ValidJob_Returns200WithCorrectResponse() throws JsonProcessingException {
        String jobId = "test-job-id";
        String deviceId = "test-device-id";
        String status = Status.RUNNING.toString();
        String createTime = "2024-03-20T10:00:00Z";
        String lastUpdated = "2024-03-20T11:00:00Z";
        String kvsStreamArn = "arn:aws:kinesisvideo:us-west-2:123456789012:stream/test-stream";

        VLRegisterDeviceJob job = VLRegisterDeviceJob.builder()
                .jobId(jobId)
                .deviceId(deviceId)
                .status(status)
                .createTime(createTime)
                .lastUpdated(lastUpdated)
                .kvsStreamArn(kvsStreamArn)
                .build();

        Map<String, Object> input = new HashMap<>();
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("jobId", jobId);
        input.put("pathParameters", pathParams);

        String expectedJson = String.format(
                "{\"jobId\":\"%s\",\"deviceId\":\"%s\",\"status\":\"%s\"," +
                "\"createTime\":\"%s\",\"modifiedTime\":\"%s\"," +
                "\"kvsStreamArns\":{\"kvsStreamARNForPlayback\":\"%s\"}}",
                jobId, deviceId, status,
                "2024-03-20T10:00:00.000Z",
                "2024-03-20T11:00:00.000Z",
                kvsStreamArn);

        when(vlRegisterDeviceJobDAO.load(jobId)).thenReturn(job);
        when(objectMapper.writeValueAsString(any(GetVLRegisterDeviceStatusResponseContent.class)))
                .thenReturn(expectedJson);

        Map<String, Object> response = activity.handleRequest(input, context);
        assertEquals(200, response.get("statusCode"));
        assertEquals(expectedJson, response.get("body"));
    }

    @Test
    void handleRequest_DaoThrowsException_Returns500() {
        String jobId = "test-job-id";
        Map<String, Object> input = new HashMap<>();
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("jobId", jobId);
        input.put("pathParameters", pathParams);

        when(vlRegisterDeviceJobDAO.load(jobId)).thenThrow(new RuntimeException("Test exception"));

        Map<String, Object> response = activity.handleRequest(input, context);
        assertEquals(500, response.get("statusCode"));
    }

    @Test
    void handleRequest_InvalidDateFormat_Returns500() {
        String jobId = "test-job-id";
        VLRegisterDeviceJob job = VLRegisterDeviceJob.builder()
                .jobId(jobId)
                .createTime("invalid-date")
                .lastUpdated("invalid-date")
                .build();

        Map<String, Object> input = new HashMap<>();
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("jobId", jobId);
        input.put("pathParameters", pathParams);

        when(vlRegisterDeviceJobDAO.load(jobId)).thenReturn(job);

        Map<String, Object> response = activity.handleRequest(input, context);
        assertEquals(500, response.get("statusCode"));
    }
}

