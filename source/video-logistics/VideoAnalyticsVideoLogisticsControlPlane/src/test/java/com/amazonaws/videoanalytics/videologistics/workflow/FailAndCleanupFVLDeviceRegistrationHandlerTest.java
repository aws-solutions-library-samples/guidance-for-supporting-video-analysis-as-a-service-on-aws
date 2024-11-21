package com.amazonaws.videoanalytics.videologistics.workflow;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.videoanalytics.videologistics.dao.VLRegisterDeviceJobDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient;
import software.amazon.awssdk.services.kinesisvideo.model.DeleteSignalingChannelRequest;
import software.amazon.awssdk.services.kinesisvideo.model.DeleteStreamRequest;
import software.amazon.awssdk.services.kinesisvideo.model.ResourceNotFoundException;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FailAndCleanupFVLDeviceRegistrationHandlerTest {

    @Mock
    private VLRegisterDeviceJobDAO vlRegisterDeviceJobDAO;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KinesisVideoClient kinesisVideoClient;

    @Mock
    private Context context;

    private FailAndCleanupFVLDeviceRegistrationHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new FailAndCleanupFVLDeviceRegistrationHandler(vlRegisterDeviceJobDAO, objectMapper, kinesisVideoClient);
    }

    @Test
    void handleRequest_WithAllResourcesPresent_DeletesAllResources() {
        String jobId = "testJobId";
        VLRegisterDeviceJob job = VLRegisterDeviceJob.builder()
                .jobId(jobId)
                .playbackSignalingChannelArn("playbackArn")
                .liveStreamSignalingChannelArn("livestreamArn")
                .kvsStreamArn("streamArn")
                .build();

        Map<String, Object> input = new HashMap<>();
        input.put("jobId", jobId);

        when(vlRegisterDeviceJobDAO.load(jobId)).thenReturn(job);

        handler.handleRequest(input, context);

        verify(kinesisVideoClient, times(2)).deleteSignalingChannel(any(DeleteSignalingChannelRequest.class));
        verify(kinesisVideoClient).deleteStream(any(DeleteStreamRequest.class));
        verify(vlRegisterDeviceJobDAO).save(job);
    }

    @Test
    void handleRequest_WithNoResources_SkipsDeletes() {
        String jobId = "testJobId";
        VLRegisterDeviceJob job = VLRegisterDeviceJob.builder()
                .jobId(jobId)
                .build();

        Map<String, Object> input = new HashMap<>();
        input.put("jobId", jobId);

        when(vlRegisterDeviceJobDAO.load(jobId)).thenReturn(job);

        handler.handleRequest(input, context);

        verify(kinesisVideoClient, never()).deleteSignalingChannel(any(DeleteSignalingChannelRequest.class));
        verify(kinesisVideoClient, never()).deleteStream(any(DeleteStreamRequest.class));
        verify(vlRegisterDeviceJobDAO).save(job);
    }

    @Test
    void handleRequest_WithFailureReason_SetsErrorMessage() throws JsonProcessingException {
        String jobId = "testJobId";
        String errorMessage = "Test error";
        VLRegisterDeviceJob job = VLRegisterDeviceJob.builder()
                .jobId(jobId)
                .build();

        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("errorMessage", errorMessage);

        Map<String, Object> input = new HashMap<>();
        input.put("jobId", jobId);
        input.put("FailureReason", "someFailureReason");

        when(vlRegisterDeviceJobDAO.load(jobId)).thenReturn(job);
        when(objectMapper.readValue("someFailureReason", Map.class)).thenReturn(errorMap);

        handler.handleRequest(input, context);

        verify(vlRegisterDeviceJobDAO).save(job);
    }

    @Test
    void handleRequest_WithResourceNotFoundException_ContinuesExecution() {
        String jobId = "testJobId";
        VLRegisterDeviceJob job = VLRegisterDeviceJob.builder()
                .jobId(jobId)
                .playbackSignalingChannelArn("playbackArn")
                .liveStreamSignalingChannelArn("livestreamArn")
                .kvsStreamArn("streamArn")
                .build();

        Map<String, Object> input = new HashMap<>();
        input.put("jobId", jobId);

        when(vlRegisterDeviceJobDAO.load(jobId)).thenReturn(job);
        doThrow(ResourceNotFoundException.class)
                .when(kinesisVideoClient)
                .deleteSignalingChannel(any(DeleteSignalingChannelRequest.class));
        doThrow(ResourceNotFoundException.class)
                .when(kinesisVideoClient)
                .deleteStream(any(DeleteStreamRequest.class));

        handler.handleRequest(input, context);

        verify(kinesisVideoClient, times(2)).deleteSignalingChannel(any(DeleteSignalingChannelRequest.class));
        verify(kinesisVideoClient).deleteStream(any(DeleteStreamRequest.class));
        verify(vlRegisterDeviceJobDAO).save(job);
    }

    @Test
    void handleRequest_WithJsonProcessingException_ContinuesExecution() throws JsonProcessingException {
        String jobId = "testJobId";
        VLRegisterDeviceJob job = VLRegisterDeviceJob.builder()
                .jobId(jobId)
                .build();

        Map<String, Object> input = new HashMap<>();
        input.put("jobId", jobId);
        input.put("FailureReason", "invalidJson");

        when(vlRegisterDeviceJobDAO.load(jobId)).thenReturn(job);
        when(objectMapper.readValue(any(String.class), any(Class.class)))
                .thenThrow(JsonProcessingException.class);

        handler.handleRequest(input, context);

        verify(vlRegisterDeviceJobDAO).save(job);
    }
}

