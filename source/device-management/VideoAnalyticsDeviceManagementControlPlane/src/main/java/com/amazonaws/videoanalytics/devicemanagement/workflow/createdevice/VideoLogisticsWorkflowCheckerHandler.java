package com.amazonaws.videoanalytics.devicemanagement.workflow.createdevice;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.dependency.ddb.DDBService;
import com.amazonaws.videoanalytics.devicemanagement.dependency.apig.ApigService;
import com.amazonaws.videoanalytics.devicemanagement.dagger.AWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.DaggerAWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import com.amazonaws.videoanalytics.devicemanagement.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.amazonaws.videoanalytics.devicemanagement.exceptions.RetryableException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.HttpExecuteResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;

import javax.inject.Inject;
import java.util.Map;

public class VideoLogisticsWorkflowCheckerHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final String JOB_ID = "jobId";
    private final String ENTERING_MESSAGE = "Entering " + this.getClass().getName() + " method.";
    private static final String WORKFLOW_IN_PROGRESS = "Video Analytics workflow is still in progress. Will retry.";
    private static final String WORKFLOW_FAILED = "Video Analytics workflow failed.";
    private static final String WORKFLOW_INVALID_STATUS = "Video Analytics workflow returned an unexpected status.";

    private final DDBService ddbService;
    private final StartCreateDeviceDAO startCreateDeviceDAO;
    private final ApigService apigService;
    private final ObjectMapper objectMapper;

    @Inject
    public VideoLogisticsWorkflowCheckerHandler(
            DDBService ddbService, 
            StartCreateDeviceDAO startCreateDeviceDAO,
            ApigService apigService,
            ObjectMapper objectMapper) {
        this.ddbService = ddbService;
        this.startCreateDeviceDAO = startCreateDeviceDAO;
        this.apigService = apigService;
        this.objectMapper = objectMapper;
    }

    @ExcludeFromJacocoGeneratedReport
    public VideoLogisticsWorkflowCheckerHandler() {
        AWSVideoAnalyticsDMControlPlaneComponent component = DaggerAWSVideoAnalyticsDMControlPlaneComponent.create();
        component.inject(this);
        this.ddbService = component.ddbService();
        this.startCreateDeviceDAO = component.startCreateDeviceDAO();
        this.apigService = component.apigService();
        this.objectMapper = component.objectMapper();
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
            
            String jobId = (String) requestParams.get(JOB_ID);
            CreateDevice createDevice = startCreateDeviceDAO.load(jobId);

            String vlJobId = createDevice.getVlJobId();
            if (vlJobId == null) {
                throw InvalidRequestException.builder()
                    .message("VL Job ID not found in device record")
                    .build();
            }

            // Get VL registration status
            HttpExecuteResponse response = apigService.invokeGetVlRegisterDeviceStatus(
                vlJobId,
                null,  // headers
                null   // body
            );
            
            String responseBody = new BufferedReader(
                new InputStreamReader(response.responseBody().get(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
            logger.log("VL device registration status response: " + responseBody);
                
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String status = jsonNode.get("status").asText();
            
            switch (status) {
                case "COMPLETED":
                    logger.log("VL device registration completed successfully");
                    return null;
                case "IN_PROGRESS":
                case "PENDING":
                    logger.log("VL device registration still in progress");
                    throw new RetryableException(WORKFLOW_IN_PROGRESS);
                case "FAILED":
                    logger.log("VL device registration failed");
                    throw InternalFailureException.builder()
                        .message(WORKFLOW_FAILED)
                        .build();
                default:
                    logger.log("VL device registration returned unexpected status: " + status);
                    throw InvalidRequestException.builder()
                        .message(WORKFLOW_INVALID_STATUS)
                        .build();
            }
        } catch (InvalidRequestException | RetryableException e) {
            logger.log(e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.log("Unexpected error occurred: " + e.getMessage());
            throw InternalFailureException.builder()
                .message("Internal server error: " + e.getMessage())
                .build();
        }
    }
}