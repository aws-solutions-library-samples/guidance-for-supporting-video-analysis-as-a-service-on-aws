package com.amazonaws.videoanalytics.devicemanagement.activity;

import javax.inject.Inject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.devicemanagement.GetDeviceResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.AWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.DaggerAWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.videoanalytics.devicemanagement.exceptions.ExceptionTranslator;
import com.amazonaws.videoanalytics.devicemanagement.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.JSON_PROCESSING_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY;
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

    // used for unit tests
    @ExcludeFromJacocoGeneratedReport
    public void assertPrivateFieldNotNull() {
        if (iotService == null) {
            throw new AssertionError("iotService is null");
        }
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Entered GetDevice method");

        String deviceId;
        try {
            deviceId = parsePathParameter(input, PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY);
        } catch (Exception e) {
            logger.log(e.toString());
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message(INVALID_INPUT_EXCEPTION)
                    .build();
            return serializeResponse(400, exception.toJson());
        }

        if (isBlank(deviceId)) {
            logger.log("deviceId is null or empty");
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message(INVALID_INPUT_EXCEPTION)
                    .build();
            return serializeResponse(400, exception.toJson());
        }
        return getResponseForValidRequest(deviceId, logger);
    }

    private Map<String, Object> getResponseForValidRequest(String deviceId, LambdaLogger logger) {
        GetDeviceResponseContent getDeviceResponse;
        try {
            getDeviceResponse = iotService.getDevice(deviceId);  
        // generalizing from IotException to AwsServiceException because getDeviceShadow is an IoTDataPlane action
        } catch (AwsServiceException e) {
            logger.log(e.toString());
            return ExceptionTranslator.translateIotExceptionToLambdaResponse(e);
        } catch (JsonProcessingException e) {
            logger.log(e.toString());
            InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.builder()
                    .message(JSON_PROCESSING_EXCEPTION)
                    .build();
            return serializeResponse(500, exception.toJson());
        } catch (Exception e){
            logger.log(e.toString());
            InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.builder()
                    .message(INTERNAL_SERVER_EXCEPTION)
                    .build();
            return serializeResponse(500, exception.toJson());
        }
        
        return serializeResponse(200, getDeviceResponse.toJson());
    }
}
