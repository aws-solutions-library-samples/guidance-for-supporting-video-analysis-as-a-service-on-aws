package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.StartVLRegisterDeviceResponseContent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.exceptions.ExceptionTranslator;
import com.amazonaws.videoanalytics.videologistics.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.amazonaws.videoanalytics.videologistics.validator.DeviceValidator;
import com.amazonaws.videoanalytics.videologistics.dao.VLRegisterDeviceJobDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.amazonaws.videoanalytics.videologistics.Status;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;
import java.util.Arrays;

/**
 * Class for handling the request for StartVLRegisterDevice API.
 */
public class StartVLRegisterDeviceActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DeviceValidator deviceValidator;
    private final VLRegisterDeviceJobDAO vlRegisterDeviceJobDAO;

    @Inject
    public StartVLRegisterDeviceActivity(final DeviceValidator deviceValidator,
                                       final VLRegisterDeviceJobDAO vlRegisterDeviceJobDAO) {
        this.deviceValidator = deviceValidator;
        this.vlRegisterDeviceJobDAO = vlRegisterDeviceJobDAO;
    }

    @ExcludeFromJacocoGeneratedReport
    public StartVLRegisterDeviceActivity() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.deviceValidator = component.getDeviceValidator();
        this.vlRegisterDeviceJobDAO = component.getVLRegisterDeviceJobDAO();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Entered StartVLRegisterDevice method");

        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .build();

        if (Objects.isNull(input)) {
            return serializeResponse(400, exception.toJson());
        }

        try {
            Map<String, String> pathParameters = (Map<String, String>) input.get("pathParameters");
            if (pathParameters == null || !pathParameters.containsKey("deviceId")) {
                logger.log("Invalid or missing pathParameters: " + pathParameters);
                return serializeResponse(400, exception.toJson());
            }

            String deviceId = pathParameters.get("deviceId");
            logger.log("Processing request for deviceId: " + deviceId);
            
            deviceValidator.validateDeviceExists(deviceId);
            logger.log("Device validation successful for deviceId: " + deviceId);

            String jobId = UUID.randomUUID().toString();
            String currentTime = Instant.now().toString();
            logger.log("Generated jobId: " + jobId);

            VLRegisterDeviceJob job = VLRegisterDeviceJob.builder()
                    .jobId(jobId)
                    .deviceId(deviceId)
                    .status(Status.RUNNING.toString())
                    .createTime(currentTime)
                    .lastUpdated(currentTime)
                    .workflowName(UUID.randomUUID().toString())
                    .build();
            logger.log("Created VLRegisterDeviceJob: " + job.toString());

            logger.log("Attempting to save job to DynamoDB...");
            vlRegisterDeviceJobDAO.save(job);
            logger.log("Successfully saved job to DynamoDB");

            StartVLRegisterDeviceResponseContent response = StartVLRegisterDeviceResponseContent
                    .builder()
                    .jobId(jobId)
                    .build();

            return serializeResponse(200, response.toJson());
        } catch (AwsServiceException e) {
            logger.log("AWS Service Exception during device registration: " + e.toString());
            if (e.awsErrorDetails() != null) {
                logger.log("Error details - Status code: " + e.statusCode() + 
                          ", Error type: " + e.awsErrorDetails().errorCode() +
                          ", Error message: " + e.awsErrorDetails().errorMessage());
            } else {
                logger.log("No AWS error details available. Status code: " + e.statusCode());
            }
            return ExceptionTranslator.translateKvsExceptionToLambdaResponse(e);
        } catch (Exception e) {
            logger.log("Unexpected error during device registration: " + e.toString());
            logger.log("Stack trace: " + Arrays.toString(e.getStackTrace()));
            return ExceptionTranslator.translateToLambdaResponse(e);
        }
    }

    @ExcludeFromJacocoGeneratedReport
    public void assertPrivateFieldNotNull() {
        if (deviceValidator == null || vlRegisterDeviceJobDAO == null) {
            throw new AssertionError("private field is null");
        }
    }
}