package com.amazonaws.videoanalytics.devicemanagement.activity;

import javax.inject.Inject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.devicemanagement.GetCreateDeviceStatusResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ResourceNotFoundExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.AWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.DaggerAWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.workflow.WorkflowManager;
import com.amazonaws.videoanalytics.devicemanagement.workflow.data.CreateDeviceData;
import com.amazonaws.videoanalytics.devicemanagement.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.amazonaws.videoanalytics.devicemanagement.Status;

import java.util.Map;
import java.time.Instant;

import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.*;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parsePathParameter;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.serializeResponse;
import static software.amazon.awssdk.utils.StringUtils.isBlank;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;

public class GetCreateDeviceStatusActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final WorkflowManager workflowManager;

    @Inject
    public GetCreateDeviceStatusActivity(WorkflowManager workflowManager) {
        this.workflowManager = workflowManager;
    }

    @ExcludeFromJacocoGeneratedReport
    public GetCreateDeviceStatusActivity() {
        AWSVideoAnalyticsDMControlPlaneComponent component = DaggerAWSVideoAnalyticsDMControlPlaneComponent.create();
        component.inject(this);
        this.workflowManager = component.workflowManager();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Entered getCreateDeviceStatus method");

        String jobId;
        try {
            jobId = parsePathParameter(input, "jobId");
        } catch (Exception e) {
            logger.log(e.toString());
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message(INVALID_INPUT_EXCEPTION)
                    .build();
            return serializeResponse(400, exception.toJson());
        }

        if (isBlank(jobId)) {
            logger.log("jobId is null or empty");
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message(INVALID_INPUT_EXCEPTION)
                    .build();
            return serializeResponse(400, exception.toJson());
        }

        CreateDeviceData createDeviceData;
        try {
            createDeviceData = workflowManager.getCreateDeviceStatus(jobId);
            
            double createTimeSeconds = createDeviceData.getCreateTime().getEpochSecond();
            double modifiedTimeSeconds = createDeviceData.getLastUpdatedTime().getEpochSecond();

            GetCreateDeviceStatusResponseContent response = GetCreateDeviceStatusResponseContent.builder()
                    .jobId(jobId)
                    .createTime(createTimeSeconds)
                    .modifiedTime(modifiedTimeSeconds)
                    .deviceId(createDeviceData.getDeviceId())
                    .status(Status.valueOf(createDeviceData.getStatus()))
                    .errorMessage(createDeviceData.getErrorMessage())
                    .build();
            return serializeResponse(200, response.toJson());
        } catch (ResourceNotFoundException e) {
            logger.log("Job not found for jobId: " + jobId + ", " + e.toString());
            ResourceNotFoundExceptionResponseContent exception = ResourceNotFoundExceptionResponseContent.builder()
                    .message(RESOURCE_NOT_FOUND_EXCEPTION)
                    .build();
            return serializeResponse(404, exception.toJson());
        } catch (IllegalArgumentException e) {
            logger.log("Invalid job id received: " + jobId + ", " + e.toString());
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message(INVALID_INPUT_EXCEPTION)
                    .build();
            return serializeResponse(400, exception.toJson());
        } catch (Exception e) {
            logger.log("Exception when calling GetCreateDeviceStatus: " + e.toString());
            InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.builder()
                    .message(INTERNAL_SERVER_EXCEPTION)
                    .build();
            return serializeResponse(500, exception.toJson());
        }
    }
}

