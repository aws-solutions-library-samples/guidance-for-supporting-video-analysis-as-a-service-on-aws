package com.amazonaws.videoanalytics.videologistics.workflow;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.amazonaws.videoanalytics.videologistics.dao.VLRegisterDeviceJobDAO;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient;
import software.amazon.awssdk.services.kinesisvideo.model.DeleteSignalingChannelRequest;
import software.amazon.awssdk.services.kinesisvideo.model.DeleteStreamRequest;
import software.amazon.awssdk.services.kinesisvideo.model.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import com.amazonaws.videoanalytics.videologistics.Status;

public class FailAndCleanupVLDeviceRegistrationHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final Logger LOG = LogManager.getLogger(FailAndCleanupVLDeviceRegistrationHandler.class);
    private static final String FAILURE_REASON = "FailureReason";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String JOB_ID = "jobId";
    private final VLRegisterDeviceJobDAO vlRegisterDeviceJobDAO;
    private final ObjectMapper objectMapper;
    private final KinesisVideoClient kinesisVideoClient;

    @Inject
    public FailAndCleanupVLDeviceRegistrationHandler(
            VLRegisterDeviceJobDAO vlRegisterDeviceJobDAO,
            ObjectMapper objectMapper,
            KinesisVideoClient kinesisVideoClient) {
        this.vlRegisterDeviceJobDAO = vlRegisterDeviceJobDAO;
        this.objectMapper = objectMapper;
        this.kinesisVideoClient = kinesisVideoClient;
    }

    @ExcludeFromJacocoGeneratedReport
    public FailAndCleanupVLDeviceRegistrationHandler() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.vlRegisterDeviceJobDAO = component.getVLRegisterDeviceJobDAO();
        this.objectMapper = component.getObjectMapper();
        this.kinesisVideoClient = component.getKinesisVideoClient();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LOG.info("Entering cleanup handler with input: {}", input);
        
        String jobId = (String) input.get(JOB_ID);
        if (StringUtils.isBlank(jobId)) {
            throw new IllegalArgumentException("JobId cannot be null or empty");
        }

        VLRegisterDeviceJob job = vlRegisterDeviceJobDAO.load(jobId);
        if (job == null) {
            throw new IllegalStateException("Could not find job with id: " + jobId);
        }

        if (StringUtils.isNotBlank(job.getPlaybackSignalingChannelArn())) {
            LOG.info("Deleting playback signaling channel {}", job.getPlaybackSignalingChannelArn());
            deleteKvsSignalingChannel(job.getPlaybackSignalingChannelArn());
            job.setPlaybackSignalingChannelArn(null);
        }

        if (StringUtils.isNotBlank(job.getLiveStreamSignalingChannelArn())) {
            LOG.info("Deleting livestream signaling channel {}", job.getLiveStreamSignalingChannelArn());
            deleteKvsSignalingChannel(job.getLiveStreamSignalingChannelArn());
            job.setLiveStreamSignalingChannelArn(null);
        }

        if (StringUtils.isNotBlank(job.getKvsStreamArn())) {
            LOG.info("Deleting KVS stream {}", job.getKvsStreamArn());
            deleteKvsStream(job.getKvsStreamArn());
            job.setKvsStreamArn(null);
        }

        if (input.containsKey(FAILURE_REASON)) {
            try {
                String failureReason = input.get(FAILURE_REASON).toString();
                // Parse the JSON string to get the error message
                Map<String, Object> errorMap = objectMapper.readValue(failureReason, Map.class);
                String errorMessage = (String) errorMap.get(ERROR_MESSAGE);
                if (errorMessage != null) {
                    job.setErrorMessage(errorMessage);
                } else {
                    job.setErrorMessage("Unknown error occurred");
                }
            } catch (JsonProcessingException | ClassCastException e) {
                LOG.error("Failed to parse error message from input: {}", input.get(FAILURE_REASON), e);
                job.setErrorMessage("Error parsing failure reason: " + input.get(FAILURE_REASON));
            }
        }

        job.setStatus(Status.FAILED.toString());
        vlRegisterDeviceJobDAO.save(job);
        
        return new HashMap<>();
    }

    private void deleteKvsSignalingChannel(String signalingChannelArn) {
        try {
            DeleteSignalingChannelRequest request = DeleteSignalingChannelRequest.builder()
                    .channelARN(signalingChannelArn)
                    .build();
            kinesisVideoClient.deleteSignalingChannel(request);
        } catch (ResourceNotFoundException e) {
            LOG.info("Failed to delete signaling channel {} as resource not found. Likely already deleted.", 
                    signalingChannelArn, e);
        }
    }

    private void deleteKvsStream(String streamArn) {
        try {
            DeleteStreamRequest request = DeleteStreamRequest.builder()
                    .streamARN(streamArn)
                    .build();
            kinesisVideoClient.deleteStream(request);
        } catch (ResourceNotFoundException e) {
            LOG.info("Failed to delete KVS stream {} as resource not found. Likely already deleted.", 
                    streamArn, e);
        }
    }

    @ExcludeFromJacocoGeneratedReport
    public void assertPrivateFieldNotNull() {
        if (vlRegisterDeviceJobDAO == null || objectMapper == null || kinesisVideoClient == null) {
            throw new AssertionError("private field is null");
        }
    }
}   