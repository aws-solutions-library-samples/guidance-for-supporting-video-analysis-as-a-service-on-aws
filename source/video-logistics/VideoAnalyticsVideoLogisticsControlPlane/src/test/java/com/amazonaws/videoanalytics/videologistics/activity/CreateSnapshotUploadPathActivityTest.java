package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import com.amazonaws.videoanalytics.videologistics.CreateSnapshotUploadPathRequestContent;
import com.amazonaws.videoanalytics.videologistics.client.s3.SnapshotS3Presigner;

import com.amazonaws.videoanalytics.videologistics.ValidationExceptionReason;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.InternalServerExceptionResponseContent;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.UPLOAD_BUCKET_FORMAT;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.ACCOUNT_ID;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_BODY_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBody;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Map.entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CreateSnapshotUploadPathActivityTest {
    private static final String S3_PRESIGNED_URL = "http://s3.amazonaws.zom/testBucket/key.jpeg?pregin-bits";
    private static final String SNAPSHOT_SHADOW_NAME = "snapshot";
    private static final String CHECKSUM = "someStringUsedForChecksum";
    private static final String DEVICE_ID = "someDeviceId";
    private static final String SNAPSHOT_ACCOUNT_ID = "someAccountId";
    private static final Long CONTENT_LENGTH = 12345L;

    @Mock
    S3Presigner s3Presigner;
    @Mock
    SnapshotS3Presigner snapshotS3Presigner;
    @Mock
    Region region;
    @Mock
    URL url;
    @Mock
    private Context context;
    @Mock
    private LambdaLogger logger;

    @InjectMocks
    private CreateSnapshotUploadPathActivity createSnapshotUploadPathActivity;

    String proxyLambdaBody = String.format(
        "{\"deviceId\": \"%s\", \"checksum\": \"%s\", \"contentLength\": \"%s\"}", 
        DEVICE_ID, 
        CHECKSUM, 
        CONTENT_LENGTH
    );
    Map<String, Object> lambdaProxyRequest = Map.ofEntries(
        entry(PROXY_LAMBDA_BODY_KEY, proxyLambdaBody)
    );

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    public void createSnapshotUploadPathActivityTest() throws MalformedURLException {
        snapshotS3Presigner = new SnapshotS3Presigner(
                s3Presigner,
                SNAPSHOT_SHADOW_NAME,
                DEVICE_ID,
                1234L);
        PresignedPutObjectRequest presignedRequest = PresignedPutObjectRequest
                .builder()
                .expiration(Instant.EPOCH)
                .isBrowserExecutable(false)
                .signedHeaders(ImmutableMap.of("header", ImmutableList.of("Content-Type")))
                .httpRequest(SdkHttpRequest.builder()
                        .uri(URI.create(S3_PRESIGNED_URL))
                        .method(SdkHttpMethod.PUT)
                        .build())
                .build();
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);
        URL presignedUrl = snapshotS3Presigner.generateImageUploadURL(CHECKSUM);
        HashMap<String, Document> message = new HashMap<>();
        message.put("presignedUrl", Document.fromString(presignedUrl.toString()));

        URL expectedUrl = new URL(S3_PRESIGNED_URL); 
        assertEquals(expectedUrl, presignedUrl);
        
        Map<String, Object> actualMessage = new CreateSnapshotUploadPathActivity(s3Presigner, region, SNAPSHOT_ACCOUNT_ID)
            .handleRequest(lambdaProxyRequest, context);
        Map<String, Object> expectedMessage = serializeResponse(200, "");
        assertEquals(expectedMessage, actualMessage);
        /*
        // TODO: after update device internal logic added in activity code, modify to match expected behavior

        ShadowMap shadowMap = ShadowMap
                .builder()
                .stateDocument(Document.fromMap(message))
                .shadowName(SNAPSHOT_SHADOW_NAME)
                .build();

        UpdateDeviceInternalRequest updateDeviceInternalRequest = UpdateDeviceInternalRequest
                .builder()
                .deviceId(DEVICE_ID)
                .shadowPayload(shadowMap)
                .build();
        
        assertNull(internalClient.updateDeviceInternal(updateDeviceInternalRequest));
        */
    }

    @Test
    public void createSnapshotUploadPathNullInputActivityTest() {
        Map<String, Object> actualMessage = new CreateSnapshotUploadPathActivity(s3Presigner, region, SNAPSHOT_ACCOUNT_ID)
            .handleRequest(null, context);

        final ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .reason(ValidationExceptionReason.FIELD_VALIDATION_FAILED)
                .build();

        Map<String, Object> expectedMessage = serializeResponse(400, exception.toJson());
        assertEquals(actualMessage, expectedMessage);
    }

    
    @Test
    public void createSnapshotUploadPathBlankDeviceInputActivityTest() {
        String blankDeviceInputProxyLambdaBody = String.format(
            "{\"deviceId\": \"\", \"checksum\": \"%s\", \"contentLength\": \"%s\"}",  
            CHECKSUM, 
            CONTENT_LENGTH
        );
        Map<String, Object> blankDeviceInputLambdaProxyRequest = Map.ofEntries(
            entry(PROXY_LAMBDA_BODY_KEY, blankDeviceInputProxyLambdaBody)
        );

        Map<String, Object> actualMessage = new CreateSnapshotUploadPathActivity(s3Presigner, region, SNAPSHOT_ACCOUNT_ID)
            .handleRequest(blankDeviceInputLambdaProxyRequest, context);

        final ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .reason(ValidationExceptionReason.FIELD_VALIDATION_FAILED)
                .build();

        Map<String, Object> expectedMessage = serializeResponse(400, exception.toJson());
        assertEquals(actualMessage, expectedMessage);
    }

    @Test
    public void createSnapshotUploadPathBlankChecksumInputActivityTest() {
        String blankChecksumProxyLambdaBody = String.format(
            "{\"deviceId\": \"%s\", \"checksum\": \"\", \"contentLength\": \"%s\"}", 
            DEVICE_ID, 
            CONTENT_LENGTH
        );
        Map<String, Object> blankChecksumLambdaProxyRequest = Map.ofEntries(
            entry(PROXY_LAMBDA_BODY_KEY, blankChecksumProxyLambdaBody)
        );

        Map<String, Object> actualMessage = new CreateSnapshotUploadPathActivity(s3Presigner, region, SNAPSHOT_ACCOUNT_ID)
            .handleRequest(blankChecksumLambdaProxyRequest, context);

        final ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .reason(ValidationExceptionReason.FIELD_VALIDATION_FAILED)
                .build();

        Map<String, Object> expectedMessage = serializeResponse(400, exception.toJson());
        assertEquals(actualMessage, expectedMessage);
    }

    @Test
    public void createSnapshotUploadPathBlankContentLengthInputActivityTest() {
        String blankChecksumProxyLambdaBody = String.format(
            "{\"deviceId\": \"%s\", \"checksum\": \"%s\", \"contentLength\": \"\"}", 
            DEVICE_ID, 
            CHECKSUM
        );
        Map<String, Object> blankChecksumLambdaProxyRequest = Map.ofEntries(
            entry(PROXY_LAMBDA_BODY_KEY, blankChecksumProxyLambdaBody)
        );

        Map<String, Object> actualMessage = new CreateSnapshotUploadPathActivity(s3Presigner, region, SNAPSHOT_ACCOUNT_ID)
            .handleRequest(blankChecksumLambdaProxyRequest, context);

        final ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .reason(ValidationExceptionReason.FIELD_VALIDATION_FAILED)
                .build();

        Map<String, Object> expectedMessage = serializeResponse(400, exception.toJson());
        assertEquals(actualMessage, expectedMessage);
    }

    @Test
    public void createSnapshotUploadPathInternalExceptionThrownActivityTest() throws IOException {
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
            .thenThrow(S3Exception.builder().message("Internal server error").build());
        
        Exception exception = assertThrows(S3Exception.class, () -> {
            createSnapshotUploadPathActivity.handleRequest(lambdaProxyRequest, context);    
        });

        assertEquals(exception.getMessage(), INTERNAL_SERVER_EXCEPTION);
    }
}