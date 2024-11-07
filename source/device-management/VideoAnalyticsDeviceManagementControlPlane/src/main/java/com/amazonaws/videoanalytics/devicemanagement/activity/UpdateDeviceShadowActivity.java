package com.amazonaws.videoanalytics.devicemanagement.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.devicemanagement.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ShadowMap;
import com.amazonaws.videoanalytics.devicemanagement.UpdateDeviceShadowRequestContent;
import com.amazonaws.videoanalytics.devicemanagement.UpdateDeviceShadowResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.AWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.DaggerAWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.exceptions.ExceptionTranslator;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.videoanalytics.devicemanagement.utils.annotations.ExcludeFromJacocoGeneratedReport;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.util.Map;
import javax.inject.Inject;

import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parsePathParameter;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.serializeResponse;

public class UpdateDeviceShadowActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final IotService iotService;

    @Inject
    public UpdateDeviceShadowActivity(IotService iotService) {
        this.iotService = iotService;
    }

    public UpdateDeviceShadowActivity() {
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
        logger.log("Entered UpdateDevice method");

        String deviceId;
        ShadowMap shadowPayload;
        try {
            deviceId = parsePathParameter(input, PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY);
            // parseBody will throw an exception if deviceId is empty
            String requestBody = parseBody(input);
            shadowPayload = UpdateDeviceShadowRequestContent.fromJson(requestBody).getShadowPayload();
        } catch (Exception e) {
            logger.log(e.toString());
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message(INVALID_INPUT_EXCEPTION)
                    .build();
            return serializeResponse(400, exception.toJson());
        }

        UpdateDeviceShadowResponseContent updateDeviceShadowResponse;
        try {
            updateDeviceShadowResponse = iotService.updateDeviceShadow(deviceId, shadowPayload);
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

        return serializeResponse(200, updateDeviceShadowResponse.toJson());
    }
}