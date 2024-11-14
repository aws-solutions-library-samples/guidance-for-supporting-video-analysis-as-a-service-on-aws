package com.amazonaws.videoanalytics.devicemanagement.workflow.update;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.dependency.ddb.DDBService;
import com.amazonaws.videoanalytics.devicemanagement.dagger.AWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.DaggerAWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import com.amazonaws.videoanalytics.devicemanagement.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.amazonaws.videoanalytics.devicemanagement.exceptions.RetryableException;
import com.amazonaws.videoanalytics.devicemanagement.exceptions.ExceptionTranslator;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;

import javax.inject.Inject;
import java.util.Map;

public class VideoLogicsticsWorkflowCheckerHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final String JOB_ID = "jobId";
    private final String ENTERING_MESSAGE = "Entering " + this.getClass().getName() + " method.";
    private static final String WORKFLOW_IN_PROGRESS = "Video Analytics workflow is still in progress. Will retry.";
    private static final String WORKFLOW_FAILED = "Video Analytics workflow failed.";
    private static final String WORKFLOW_INVALID_STATUS = "Video Analytics workflow returned an unexpected status.";

    private final DDBService ddbService;
    private final StartCreateDeviceDAO startCreateDeviceDAO;

    @Inject
    public VideoLogicsticsWorkflowCheckerHandler(DDBService ddbService, StartCreateDeviceDAO startCreateDeviceDAO) {
        this.ddbService = ddbService;
        this.startCreateDeviceDAO = startCreateDeviceDAO;
    }

    @ExcludeFromJacocoGeneratedReport
    public VideoLogicsticsWorkflowCheckerHandler() {
        AWSVideoAnalyticsDMControlPlaneComponent component = DaggerAWSVideoAnalyticsDMControlPlaneComponent.create();
        component.inject(this);
        this.ddbService = component.ddbService();
        this.startCreateDeviceDAO = component.startCreateDeviceDAO();
    }

    @Override
    public Map<String, Object> handleRequest(final Map<String, Object> requestParams, final Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log(ENTERING_MESSAGE);

        try {
            // Validate job ID
            if (!requestParams.containsKey(JOB_ID)) {
                throw InvalidRequestException.builder()
                    .message("Missing required parameter: jobId")
                    .build();
            }

            // Get the job ID and load the device record
            String jobId = (String) requestParams.get(JOB_ID);
            CreateDevice createDevice = startCreateDeviceDAO.load(jobId);

            // Get VL Job ID from the device record
            String vlJobId = createDevice.getVlJobId();
            if (vlJobId == null) {
                throw InvalidRequestException.builder()
                    .message("VL Job ID not found in device record")
                    .build();
            }

            // TODO: Call VL client to get actual job status
            // For now, hardcoding to COMPLETED for pass-through logic
            String currentState = "COMPLETED";
            
            // Check the workflow status based on device state
            switch (currentState) {
                case "COMPLETED":
                    // Workflow completed successfully, proceed to next handler
                    return null;
                case "RUNNING":
                    logger.log("Video Analytics workflow still in progress.");
                    throw new RetryableException(WORKFLOW_IN_PROGRESS);
                case "FAILED":
                    logger.log("Video Analytics workflow failed.");
                    throw InternalFailureException.builder()
                        .message(WORKFLOW_FAILED)
                        .build();
                default:
                    logger.log("Video Analytics workflow status was of unexpected format.");
                    throw InvalidRequestException.builder()
                        .message(WORKFLOW_INVALID_STATUS)
                        .build();
            }
        } catch (InvalidRequestException e) {
            logger.log("Invalid request: " + e.getMessage());
            throw e;
        } catch (RetryableException e) {
            logger.log("Retryable exception occurred: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.log("Unexpected error occurred: " + e.getMessage());
            throw InternalFailureException.builder()
                .message("Internal server error: " + e.getMessage())
                .build();
        }
    }
}