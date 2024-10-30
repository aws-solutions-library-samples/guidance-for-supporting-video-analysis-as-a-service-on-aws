package com.amazonaws.videoanalytics.activity;

import javax.inject.Inject;

import com.amazonaws.videoanalytics.devicemanagement.GetDeviceResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.AWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.DaggerAWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.videoanalytics.devicemanagement.exceptions.ExceptionTranslator;
import com.amazonaws.videoanalytics.devicemanagement.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;

import software.amazon.awssdk.services.iot.model.IotException;

import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.JSON_PROCESSING_EXCEPTION;

import static software.amazon.awssdk.utils.StringUtils.isBlank;

public class GetDeviceActivity implements RequestHandler<String, GetDeviceResponseContent> {
    private final IotService iotService;

    @Inject
    public GetDeviceActivity(IotService iotService) {
        this.iotService = iotService;
    }

    @ExcludeFromJacocoGeneratedReport
    public GetDeviceActivity() {
        AWSVideoAnalyticsDMControlPlaneComponent component = DaggerAWSVideoAnalyticsDMControlPlaneComponent.create();
        component.inject(this);
        this.iotService = component.iotService();
    }

    @Override
    public GetDeviceResponseContent handleRequest(String deviceId, Context context) throws RuntimeException {
        LambdaLogger logger = context.getLogger();
        String requestId = context.getAwsRequestId();

        logger.log("Entered getDeviceRequest method");
        if (isBlank(deviceId)){
            throw new RuntimeException(ExceptionTranslator.buildErrorPayload(400, INVALID_INPUT_EXCEPTION, requestId));
        }
        return getResponseForValidRequest(deviceId, logger, requestId);
    }

    private GetDeviceResponseContent getResponseForValidRequest(String deviceId, LambdaLogger logger, String requestId) {
        GetDeviceResponseContent getDeviceResponse = null;
        try {
            getDeviceResponse = iotService.getDevice(deviceId);  
        } catch (IotException e) {
            logger.log(e.toString());
            ExceptionTranslator.translateIotExceptionToRuntimeException(e, requestId);
        } catch (JsonProcessingException e) {
            logger.log(e.toString());
            throw new RuntimeException(ExceptionTranslator.buildErrorPayload(500, JSON_PROCESSING_EXCEPTION, requestId));
        } catch (Exception e){
            logger.log(e.toString());
            throw new RuntimeException(ExceptionTranslator.buildErrorPayload(500, INTERNAL_SERVER_EXCEPTION, requestId));
        }
        return getDeviceResponse;
    }
}