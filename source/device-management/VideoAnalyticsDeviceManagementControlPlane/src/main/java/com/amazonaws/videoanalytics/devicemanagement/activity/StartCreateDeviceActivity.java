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

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import com.amazonaws.videoanalytics.devicemanagement.workflow.WorkflowManager;

import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.*;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parseRequestBody;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.serializeResponse;
import static software.amazon.awssdk.utils.StringUtils.isBlank;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parsePathParameter;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parseBody;

public class StartCreateDeviceActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {
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
        logger.log("Entered startCreateDevice method");

        String deviceId;
        StartCreateDeviceRequestContent requestContent;
        try {
            deviceId = parsePathParameter(input, "deviceId");
            String requestBody = parseBody(input);
            requestContent = StartCreateDeviceRequestContent.fromJson(requestBody);
        } catch (Exception e) {
            logger.log(e.toString());
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message(INVALID_INPUT_EXCEPTION)
                    .build();
            return serializeResponse(400, exception.toJson());
        }

        if (isBlank(deviceId) || requestContent == null || isBlank(requestContent.getCertificateId())) {
            logger.log("deviceId or certificateId is null or empty");
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message(INVALID_INPUT_EXCEPTION)
                    .build();
            return serializeResponse(400, exception.toJson());
        }

        try {
            String jobId = workflowManager.startCreateDevice(deviceId, requestContent.getCertificateId());
            StartCreateDeviceResponseContent response = StartCreateDeviceResponseContent.builder()
                    .jobId(jobId)
                    .build();
            return serializeResponse(200, response.toJson());
        } catch (AwsServiceException e) {
            logger.log(e.toString());
            return ExceptionTranslator.translateIotExceptionToLambdaResponse(e);
        } catch (Exception e) {
            logger.log(e.toString());
            InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.builder()
                    .message(INTERNAL_SERVER_EXCEPTION)
                    .build();
            return serializeResponse(500, exception.toJson());
        }
    }
}
