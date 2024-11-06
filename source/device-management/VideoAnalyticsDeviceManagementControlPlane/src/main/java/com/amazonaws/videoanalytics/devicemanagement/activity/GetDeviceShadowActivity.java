package com.amazonaws.videoanalytics.devicemanagement.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.devicemanagement.GetDeviceShadowRequestContent;
import com.amazonaws.videoanalytics.devicemanagement.GetDeviceShadowResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.AWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.DaggerAWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.amazonaws.videoanalytics.devicemanagement.exceptions.ExceptionTranslator;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.util.Map;

import javax.inject.Inject;

import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parsePathParameter;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.serializeResponse;
import static software.amazon.awssdk.utils.StringUtils.isBlank;

public class GetDeviceShadowActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final IotService iotService;

    @Inject
    public GetDeviceShadowActivity(IotService iotService) {
        this.iotService = iotService;
    }

    @ExcludeFromJacocoGeneratedReport
    public GetDeviceShadowActivity() {
        AWSVideoAnalyticsDMControlPlaneComponent component = DaggerAWSVideoAnalyticsDMControlPlaneComponent.create();
        component.inject(this);
        this.iotService = component.iotService();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Entered GetDeviceShadow method");

        String deviceId;
        String shadowName;
        try {
            deviceId = parsePathParameter(input, PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY);
            String requestBody = parseBody(input);
            shadowName = GetDeviceShadowRequestContent.fromJson(requestBody).getShadowName();
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

        GetDeviceShadowResponseContent getDeviceShadowResponse;
        try {
            getDeviceShadowResponse = iotService.getDeviceShadow(deviceId, shadowName);
        // generalizing from IotException to AwsServiceException because getDeviceShadow is an IoTDataPlane action
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
    
        return serializeResponse(200, getDeviceShadowResponse.toJson());
    }
}
