package com.amazonaws.videoanalytics.devicemanagement.activity;

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

import java.util.Map;

import software.amazon.awssdk.services.iot.model.IotException;

import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.JSON_PROCESSING_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parsePathParameter;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.serializeResponse;
import static software.amazon.awssdk.utils.StringUtils.isBlank;

public class GetDeviceActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {
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
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) throws RuntimeException {
        LambdaLogger logger = context.getLogger();

        logger.log("Entered getDeviceRequest method");

        String deviceId;
        try {
            deviceId = parsePathParameter(input, "deviceId");
        } catch (Exception e) {
            return serializeResponse(400, INVALID_INPUT_EXCEPTION);
        }

        if (isBlank(deviceId)){
            return serializeResponse(400, INVALID_INPUT_EXCEPTION);
        }
        return getResponseForValidRequest(deviceId, logger);
    }

    private Map<String, Object> getResponseForValidRequest(String deviceId, LambdaLogger logger) {
        GetDeviceResponseContent getDeviceResponse;
        try {
            getDeviceResponse = iotService.getDevice(deviceId);  
        } catch (IotException e) {
            logger.log(e.toString());
            return ExceptionTranslator.translateIotExceptionToLambdaResponse(e);
        } catch (JsonProcessingException e) {
            logger.log(e.toString());
            return serializeResponse(500, JSON_PROCESSING_EXCEPTION);
        } catch (Exception e){
            logger.log(e.toString());
            return serializeResponse(500, INTERNAL_SERVER_EXCEPTION);
        }
        
        return serializeResponse(200, getDeviceResponse.toJson());
    }
}
