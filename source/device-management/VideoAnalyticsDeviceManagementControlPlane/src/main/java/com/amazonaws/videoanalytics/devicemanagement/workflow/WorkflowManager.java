package com.amazonaws.videoanalytics.devicemanagement.workflow;

import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import software.amazon.awssdk.services.iot.model.ConflictException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.devicemanagement.workflow.data.CreateDeviceData;
import static com.amazonaws.videoanalytics.devicemanagement.Status.RUNNING;
import static com.amazonaws.services.lambda.runtime.LambdaRuntime.getLogger;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.RESOURCE_NOT_FOUND_EXCEPTION;
import javax.inject.Inject;
import java.util.UUID;

/**
 * This is the domain logic layer for workflow related APIs
 * This class contains all business logic for workflow related APIs
 */
public class WorkflowManager {
    private final LambdaLogger logger = getLogger();
    private final StartCreateDeviceDAO startCreateDeviceDAO;
    private final IotService iotService;

    @Inject
    public WorkflowManager(StartCreateDeviceDAO startCreateDeviceDAO,
                           IotService iotService) {
        this.startCreateDeviceDAO = startCreateDeviceDAO;
        this.iotService = iotService;
    }

    /**
     * Start create device workflow
     *
     * @param deviceId      device identifier
     * @param certificateId device IoT certificate id
     * @return workflow job id
     * @throws ConflictException              if device id already exists
     * @throws ResourceNotFoundException      if certificate doesn't exist
     */
    public String startCreateDevice(String deviceId, String certificateId) throws ConflictException, ResourceNotFoundException {

        if (iotService.isAnExistingDevice(deviceId)) {  
            throw ConflictException.builder()
                .message("Device already exists: " + deviceId)
                .build();
        }
        
        // Verify certificate exists
        try {
            iotService.getCertificate(certificateId);
        } catch (ResourceNotFoundException e) {
            throw ResourceNotFoundException.builder()
                .message("Certificate not found: " + certificateId)
                .build();
        }

        String jobId = UUID.randomUUID().toString();
        String workflowName = UUID.randomUUID().toString();
        logger.log(String.format("Starting create device job with job id %s and workflow name %s", jobId, workflowName));
        startCreateDeviceDAO.save(CreateDevice.builder()
                .deviceId(deviceId)
                .certificateId(certificateId)
                .jobStatus(RUNNING.toString())
                .workflowName(workflowName)
                .jobId(jobId)
                .build());
        return jobId;
    }

    /**
     * Get create device workflow status
     *
     * @param jobId The workflow job id
     * @return create device job status data
     */
    public CreateDeviceData getCreateDeviceStatus(String jobId) throws ResourceNotFoundException {
        logger.log(String.format("get create device job with job id %s", jobId));
        CreateDevice createDeviceSchema = startCreateDeviceDAO.load(jobId);

        if (null == createDeviceSchema) {
            throw ResourceNotFoundException.builder()
                .message("Job doesn't exist")
                .build();
        }

        return CreateDeviceData.builder()
                .deviceId(createDeviceSchema.getDeviceId())
                .jobId(jobId)
                .status(createDeviceSchema.getJobStatus())
                .createTime(createDeviceSchema.getCreatedAt())
                .lastUpdatedTime(createDeviceSchema.getLastUpdated())
                .errorMessage(createDeviceSchema.getErrorMessage())
                .build();
    }
}
