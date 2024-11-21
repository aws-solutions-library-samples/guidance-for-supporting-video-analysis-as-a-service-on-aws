package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.StartVLRegisterDeviceRequestContent;
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
            String body = parseBody(input);
            if (body == null) {
                return serializeResponse(400, exception.toJson());
            }

            StartVLRegisterDeviceRequestContent request;
            try {
                request = StartVLRegisterDeviceRequestContent.fromJson(body);
            } catch (Exception e) {
                logger.log("Invalid JSON format: " + e.toString());
                return serializeResponse(400, exception.toJson());
            }

            String deviceId = request.getDeviceId();
            deviceValidator.validateDeviceExists(deviceId);

            String jobId = UUID.randomUUID().toString();
            String currentTime = Instant.now().toString();

            VLRegisterDeviceJob job = VLRegisterDeviceJob.builder()
                    .jobId(jobId)
                    .deviceId(deviceId)
                    .status(Status.RUNNING.toString())
                    .createTime(currentTime)
                    .lastUpdated(currentTime)
                    .build();

            vlRegisterDeviceJobDAO.save(job);

            StartVLRegisterDeviceResponseContent response = StartVLRegisterDeviceResponseContent
                    .builder()
                    .jobId(jobId)
                    .build();

            return serializeResponse(200, response.toJson());
        } catch (AwsServiceException e) {
            logger.log("Error during device registration: " + e.toString());
            return ExceptionTranslator.translateKvsExceptionToLambdaResponse(e);
        } catch (Exception e) {
            logger.log("Error during device registration: " + e.toString());
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