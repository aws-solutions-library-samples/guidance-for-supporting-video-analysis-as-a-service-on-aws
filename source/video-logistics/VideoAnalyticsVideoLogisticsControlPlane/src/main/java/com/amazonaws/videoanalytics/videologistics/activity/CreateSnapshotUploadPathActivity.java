package com.amazonaws.videoanalytics.videologistics.activity;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.ACCOUNT_ID;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.UPLOAD_BUCKET_FORMAT;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBodyMap;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;

import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.CreateSnapshotUploadPathRequestContent;
import com.amazonaws.videoanalytics.videologistics.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionReason;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.client.s3.SnapshotS3Presigner;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dependency.apig.ApigService;

import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;


/**
 * Class for handling the request for CreateSnapshotUploadPathActivity API.
 */
public class CreateSnapshotUploadPathActivity implements RequestHandler<Map<String, Object>, Map<String, Object>>  {
    
    private final S3Presigner s3Presigner;
    private SnapshotS3Presigner snapshotS3Presigner;
    private final String SNAPSHOT = "snapshot";
    private final String region;
    private final String accountId;
    private final ApigService apigService;

    @Inject
    public CreateSnapshotUploadPathActivity(final S3Presigner s3Presigner,
                                            final Region region,
                                            @Named(ACCOUNT_ID) final String accountId,
                                            ApigService apigService) {
        this.s3Presigner = s3Presigner;
        this.region = region.toString();
        this.accountId = accountId;
        this.apigService = apigService;
    }

    public CreateSnapshotUploadPathActivity() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.s3Presigner = component.getS3Presigner();
        this.region = component.getRegion().toString();
        this.accountId = component.getAccountId();
        this.apigService = component.apigService();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Entered CreateSnapshotUploadPath method.");

        final ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .reason(ValidationExceptionReason.FIELD_VALIDATION_FAILED)
                .build();

        if (Objects.isNull(input)) {
            logger.log("Invalid input, " + input);
            return serializeResponse(400, exception.toJson());
        }

        logger.log("Input: " + input.toString());

        String deviceId;
        BigDecimal contentLength;
        String checkSum;
        try {
            CreateSnapshotUploadPathRequestContent request = CreateSnapshotUploadPathRequestContent.fromJson(parseBodyMap(input));
            deviceId = request.getDeviceId();
            contentLength = request.getContentLength();
            checkSum = request.getChecksum();
        } catch (Exception e) {
            logger.log(e.toString());
            return serializeResponse(400, exception.toJson());
        }

        if (deviceId.isEmpty() || checkSum.isEmpty() || contentLength.longValue() < 1L) {
            logger.log("Invalid input, " + input);
            return serializeResponse(400, exception.toJson());
        }

        // used for bucketName
        String snapshotUploadBucketName = String.format(UPLOAD_BUCKET_FORMAT, accountId, region);

        // Generate presigned url for snapshot
        snapshotS3Presigner = new SnapshotS3Presigner(
                s3Presigner,
                snapshotUploadBucketName,
                deviceId,
                contentLength.longValue());

        URL rawPresignedUrl = snapshotS3Presigner.generateImageUploadURL(checkSum);
        String presignedUrl = rawPresignedUrl.toString();

        // send url back to camera via a topic
        // connect and publish
        HashMap<String, Document> message = new HashMap<>();
        message.put("presignedUrl", Document.fromString(presignedUrl));

        String updateDeviceInternalRequest = String.format(
            "{\"shadowPayload\":{\"shadowName\":\"%s\",\"stateDocument\":%s}}",
            SNAPSHOT,
            Document.fromMap(message).toString()
        );

        try {
            // Start the device shadow update 
            HttpExecuteResponse response = apigService.invokeUpdateDeviceShadow(
                deviceId,
                null,  // headers 
                updateDeviceInternalRequest   // body 
            );

            // Return appropriate response
            if (response.httpResponse().isSuccessful()) {
                return serializeResponse(200, updateDeviceInternalRequest);
            } else {
                AbortableInputStream errorStream = response.responseBody().get();
                logger.log("DM API Error: " + new String(errorStream.delegate().readAllBytes(), StandardCharsets.UTF_8));
                errorStream.abort();
                InternalServerExceptionResponseContent internalServerException = InternalServerExceptionResponseContent.builder()
                        .message(INTERNAL_SERVER_EXCEPTION)
                        .build();
                return serializeResponse(500, internalServerException.toJson());
            }
        }
        catch (Exception e) {
            logger.log(e.getMessage());
            InternalServerExceptionResponseContent internalServerException = InternalServerExceptionResponseContent.builder()
                    .message(INTERNAL_SERVER_EXCEPTION)
                    .build();
            return serializeResponse(500, internalServerException.toJson());
        }
    }
}
