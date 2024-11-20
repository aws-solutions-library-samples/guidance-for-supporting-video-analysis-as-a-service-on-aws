package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.videoanalytics.videologistics.CreateSnapshotUploadPathRequestContent;
import com.amazonaws.videoanalytics.videologistics.client.s3.SnapshotS3Presigner;
import com.amazonaws.videoanalytics.videologistics.client.s3.ImageS3Presigner;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.iot.model.InternalServerException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import static software.amazon.awssdk.utils.StringUtils.isBlank;

import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;

import java.net.URL;
import javax.inject.Inject;
import javax.inject.Named;
import software.amazon.awssdk.regions.Region;


import com.amazonaws.videoanalytics.videologistics.ValidationExceptionReason;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.UPLOAD_BUCKET_FORMAT;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.ACCOUNT_ID;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * Class for handling the request for CreateSnapshotUploadPathActivity API.
 */
public class CreateSnapshotUploadPathActivity implements RequestHandler<Map<String, Object>, Map<String, Object>>  {
    
    private S3Presigner s3Presigner;
    private SnapshotS3Presigner snapshotS3Presigner;
    private final String SNAPSHOT = "snapshot";
    private String region;
    private String accountId;

    @Inject
    public CreateSnapshotUploadPathActivity(final S3Presigner s3Presigner,
                                            final Region region,
                                            @Named(ACCOUNT_ID) final String accountId) {
        this.s3Presigner = s3Presigner;
        this.region = region.toString();
        this.accountId = accountId;
    }

    public CreateSnapshotUploadPathActivity() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.s3Presigner = component.getS3Presigner();
        this.region = component.getRegion().toString();
        this.accountId = component.getAccountId();
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

        String deviceId;
        Long contentLength;
        byte[] checkSum;
        try {
            CreateSnapshotUploadPathRequestContent request = CreateSnapshotUploadPathRequestContent.fromJson(parseBody(input));
            deviceId = request.getDeviceId();
            contentLength = request.getContentLength().longValue();
            checkSum = request.getChecksum().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.log(e.toString());
            return serializeResponse(400, exception.toJson());
        }

        // used for bucketName
        String snapshotUploadBucketName = String.format(UPLOAD_BUCKET_FORMAT, accountId, region);
        AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.builder().build();

        // Generate presigned url for snapshot
        snapshotS3Presigner = new SnapshotS3Presigner(
                S3Presigner.builder().credentialsProvider(credentialsProvider).build(),
                snapshotUploadBucketName,
                deviceId,
                contentLength);

        URL rawPresignedUrl = snapshotS3Presigner.generateImageUploadURL(checkSum);
        String presignedUrl = rawPresignedUrl.toString();

        return serializeResponse(200, "");

        /* *
        // TODO: add update device internal logic here

        // send url back to camera via a topic
        // connect and publish
        HashMap<String, Document> message = new HashMap<>();
        message.put("presignedUrl", Document.fromString(presignedUrl));
        // add topic name to shadow map
        ShadowMap shadowMap = ShadowMap.builder()
                .shadowName(SNAPSHOT)
                .stateDocument(Document.fromMap(message))
                .build();

        UpdateDeviceInternalRequest updateDeviceInternalRequest = UpdateDeviceInternalRequest.builder()
                .deviceId(input.getDeviceId())
                .shadowPayload(shadowMap)
                .build();
        try {
            internalClient.updateDeviceInternal(updateDeviceInternalRequest);
        }
        catch (Exception e) {
            logger.log(e.getMessage());
            InternalServerExceptionResponseContent internalServerException = InternalServerExceptionResponseContent.builder()
                    .message(INTERNAL_SERVER_EXCEPTION)
                    .build();
            return serializeResponse(500, internalServerException.toJson());
        }
        */
    }
}