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

import javax.inject.Inject;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class CreateKVSStreamHandler implements RequestHandler<Map<String, Object>, Void> {
    private static final String JOB_ID = "jobId";
    
    private final DDBService ddbService;
    private final StartCreateDeviceDAO startCreateDeviceDAO;
    private final ApigService apigService;
    private final ObjectMapper objectMapper;

    @Inject
    public CreateKVSStreamHandler(
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
    public CreateKVSStreamHandler() {
        AWSVideoAnalyticsDMControlPlaneComponent component = DaggerAWSVideoAnalyticsDMControlPlaneComponent.create();
        component.inject(this);
        this.ddbService = component.ddbService();
        this.startCreateDeviceDAO = component.startCreateDeviceDAO();
        this.apigService = component.apigService();
        this.objectMapper = component.objectMapper();
    }

    @Override
    public Void handleRequest(final Map<String, Object> requestParams, final Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Processing create KVS stream request for parameters: " + requestParams);

        try {
            // Get the job ID from request parameters
            String jobId = (String) requestParams.get(JOB_ID);
            if (jobId == null || jobId.isEmpty()) {
                logger.log("Job ID is missing from request parameters");
                throw new IllegalArgumentException("Job ID is required");
            }
            
            // Load the create device record
            CreateDevice createdDevice = startCreateDeviceDAO.load(jobId);
            logger.log("Loaded device record for jobId: " + jobId);
            
            // Start the VL device registration
            HttpExecuteResponse response = apigService.invokeStartVlRegisterDevice(
                createdDevice.getDeviceId(),
                null,  // headers 
                null   // body 
            );
            
            // Extract and log the response body
            String responseBody = new BufferedReader(
                new InputStreamReader(response.responseBody().get(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
            logger.log("VL device registration response body: " + responseBody);
                
            // Get the jobId from the response
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String vlJobId = jsonNode.get("jobId").asText();
            
            logger.log(String.format("Started VL device registration - deviceId: %s, status: %d, vlJobId: %s", 
                    createdDevice.getDeviceId(), 
                    response.httpResponse().statusCode(),
                    vlJobId));
            
            // Update the device record with VL jobId
            createdDevice.setVlJobId(vlJobId);
            startCreateDeviceDAO.save(createdDevice);
            logger.log("Successfully saved device record with VL jobId: " + vlJobId);

            return null;
        } catch (Exception e) {
            logger.log("Failed to process create KVS stream request: " + e.getMessage());
            throw new RuntimeException("Failed to process create KVS stream request", e);
        }
    }
}
