package com.amazonaws.videoanalytics.videologistics.activity;

import java.util.Arrays;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.ImportMediaObjectRequestContent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionReason;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;

import com.amazonaws.videoanalytics.videologistics.inference.ImportMediaObjectHandler;
import com.amazonaws.videoanalytics.videologistics.inference.KdsMetadata;


import javax.inject.Inject;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.nio.ByteBuffer;
import java.math.BigDecimal;

import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;

/**
 * Class for handling the request for ImportMediaObject API.
 */
public class ImportMediaObjectActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final ImportMediaObjectHandler importMediaObjectHandler;

    @Inject
    ImportMediaObjectActivity(final ImportMediaObjectHandler importMediaObjectHandler){
        this.importMediaObjectHandler = importMediaObjectHandler;
    }

    @ExcludeFromJacocoGeneratedReport
    public ImportMediaObjectActivity() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.importMediaObjectHandler = component.getImportMediaObjectHandler();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Entered ImportMediaObjectActivity method");

        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
        .message(INVALID_INPUT_EXCEPTION)
        .build();

        if (Objects.isNull(input)) {
            return serializeResponse(400, ValidationExceptionResponseContent.builder()
                    .message(INVALID_INPUT_EXCEPTION)
                    .build()
                    .toJson());
        }

        String body = parseBody(input);

        ImportMediaObjectRequestContent request;
        String deviceId;
        ByteBuffer mediaObject;
        try {
            request = ImportMediaObjectRequestContent.fromJson(body);
            deviceId = request.getDeviceId();
            mediaObject = ByteBuffer.wrap(request.getMediaObject());
        } catch (Exception e) {
            logger.log("Invalid JSON format: " + e.toString());
            return serializeResponse(400, exception.toJson());
        }

        importMediaObjectHandler.importMediaObject(deviceId, mediaObject);
        return serializeResponse(200, "");
    }
}
