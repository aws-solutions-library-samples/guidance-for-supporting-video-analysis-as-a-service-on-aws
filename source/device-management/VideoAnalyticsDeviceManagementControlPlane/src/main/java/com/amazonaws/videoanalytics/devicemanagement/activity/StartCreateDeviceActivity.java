package com.amazonaws.videoanalytics.devicemanagement.activity;

import javax.inject.Inject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.devicemanagement.StartCreateDeviceRequestContent;
import com.amazonaws.videoanalytics.devicemanagement.StartCreateDeviceResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.AWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.DaggerAWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.videoanalytics.devicemanagement.exceptions.ExceptionTranslator;
import com.amazonaws.videoanalytics.devicemanagement.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;
import java.util.Arrays;
import java.io.IOException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import com.amazonaws.videoanalytics.devicemanagement.workflow.WorkflowManager;

import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.*;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parseRequestBody;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.serializeResponse;
import static software.amazon.awssdk.utils.StringUtils.isBlank;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parsePathParameter;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parseBody;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartCreateDeviceActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final Logger logger = LoggerFactory.getLogger(StartCreateDeviceActivity.class);
    private final WorkflowManager workflowManager;

    @Inject
    public StartCreateDeviceActivity(WorkflowManager workflowManager) {
        this.workflowManager = workflowManager;
    }

    @ExcludeFromJacocoGeneratedReport
    public StartCreateDeviceActivity() {
        AWSVideoAnalyticsDMControlPlaneComponent component = DaggerAWSVideoAnalyticsDMControlPlaneComponent.create();
        component.inject(this);
        this.workflowManager = component.workflowManager();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Entered StartCreateDevice method with input: " + input);

        String deviceId;
        String certificateId;
        try {
            deviceId = parsePathParameter(input, "deviceId");
            
            Map<String, String> queryParams = (Map<String, String>) input.get("queryStringParameters");
            if (queryParams != null && queryParams.containsKey("certificateId")) {
                certificateId = queryParams.get("certificateId");
            } else {
                String requestBody = parseBody(input);
                StartCreateDeviceRequestContent requestContent = StartCreateDeviceRequestContent.fromJson(requestBody);
                certificateId = requestContent.getCertificateId();
            }
        } catch (IOException e) {
            logger.log("Error parsing request: " + e.getMessage());
            logger.log("Stack trace: " + Arrays.toString(e.getStackTrace()));
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message(INVALID_INPUT_EXCEPTION)
                    .build();
            return serializeResponse(400, exception.toJson());
        } catch (Exception e) {
            logger.log("Unexpected error: " + e.getMessage());
            logger.log("Stack trace: " + Arrays.toString(e.getStackTrace()));
            InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.builder()
                    .message(INTERNAL_SERVER_EXCEPTION)
                    .build();
            return serializeResponse(500, exception.toJson());
        }

        if (isBlank(deviceId) || isBlank(certificateId)) {
            logger.log(String.format("deviceId or certificateId is null or empty. deviceId: %s, certificateId: %s", 
                deviceId, certificateId));
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message(INVALID_INPUT_EXCEPTION)
                    .build();
            return serializeResponse(400, exception.toJson());
        }

        try {
            String jobId = workflowManager.startCreateDevice(deviceId, certificateId);
            StartCreateDeviceResponseContent response = StartCreateDeviceResponseContent.builder()
                    .jobId(jobId)
                    .build();
            return serializeResponse(200, response.toJson());
        } catch (AwsServiceException e) {
            logger.log("AWS service error: " + e.getMessage());
            return ExceptionTranslator.translateIotExceptionToLambdaResponse(e);
        } catch (Exception e) {
            logger.log("Internal server error: " + e.getMessage());
            InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.builder()
                    .message(INTERNAL_SERVER_EXCEPTION)
                    .build();
            return serializeResponse(500, exception.toJson());
        }
    }
}
