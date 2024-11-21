package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.GetVLRegisterDeviceStatusResponseContent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ResourceNotFoundExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.Status;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dao.VLRegisterDeviceJobDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import com.amazonaws.videoanalytics.videologistics.exceptions.ExceptionTranslator;
import com.amazonaws.videoanalytics.videologistics.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.videoanalytics.videologistics.KVSStreamARNs;
import java.util.Date;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class GetVLRegisterDeviceStatusActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final VLRegisterDeviceJobDAO vlRegisterDeviceJobDAO;
    private final ObjectMapper objectMapper;

    @Inject
    public GetVLRegisterDeviceStatusActivity(final VLRegisterDeviceJobDAO vlRegisterDeviceJobDAO,
                                           final ObjectMapper objectMapper) {
        this.vlRegisterDeviceJobDAO = vlRegisterDeviceJobDAO;
        this.objectMapper = objectMapper;
    }

    @ExcludeFromJacocoGeneratedReport
    public GetVLRegisterDeviceStatusActivity() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.vlRegisterDeviceJobDAO = component.getVLRegisterDeviceJobDAO();
        this.objectMapper = component.getObjectMapper();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Entered GetVLRegisterDeviceStatus method");

        // Validate input
        if (Objects.isNull(input)) {
            return serializeResponse(400, ValidationExceptionResponseContent.builder()
                    .message(INVALID_INPUT_EXCEPTION)
                    .build()
                    .toJson());
        }

        // Get jobId from path parameters
        Map<String, String> pathParameters = (Map<String, String>) input.get("pathParameters");
        if (pathParameters == null || !pathParameters.containsKey("jobId")) {
            return serializeResponse(400, ValidationExceptionResponseContent.builder()
                    .message(INVALID_INPUT_EXCEPTION)
                    .build()
                    .toJson());
        }

        String jobId = pathParameters.get("jobId");

        try {
            // Get job from DynamoDB using the load method instead of getJob
            VLRegisterDeviceJob job = vlRegisterDeviceJobDAO.load(jobId);
            if (job == null) {
                return serializeResponse(404, ResourceNotFoundExceptionResponseContent.builder()
                        .message("Job not found")
                        .build()
                        .toJson());
            }

            GetVLRegisterDeviceStatusResponseContent response = GetVLRegisterDeviceStatusResponseContent.builder()
                    .jobId(job.getJobId())
                    .deviceId(job.getDeviceId())
                    .status(Status.fromValue(job.getStatus()))
                    .createTime(Date.from(Instant.parse(job.getCreateTime())))
                    .modifiedTime(Date.from(Instant.parse(job.getLastUpdated())))
                    .kvsStreamArns(KVSStreamARNs.builder()
                            .kvsStreamARNForPlayback(job.getKvsStreamArn())
                            .build())
                    .build();

            return serializeResponse(200, response.toJson());

        } catch (Exception e) {
            logger.log("Error retrieving job status: " + e.toString());
            return ExceptionTranslator.translateToLambdaResponse(e);
        }
    }

    @ExcludeFromJacocoGeneratedReport
    public void assertPrivateFieldNotNull() {
        if (vlRegisterDeviceJobDAO == null || objectMapper == null) {
            throw new AssertionError("private field is null");
        }
    }
}