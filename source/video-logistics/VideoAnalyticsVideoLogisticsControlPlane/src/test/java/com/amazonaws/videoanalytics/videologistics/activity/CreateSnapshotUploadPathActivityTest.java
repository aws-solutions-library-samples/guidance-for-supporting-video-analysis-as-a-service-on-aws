package com.amazonaws.videoanalytics.videologistics.activity;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_BODY_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;

import static java.util.Map.entry;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.videologistics.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionReason;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.client.s3.SnapshotS3Presigner;
import com.amazonaws.videoanalytics.videologistics.dependency.apig.ApigService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

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
    private Context context;
    @Mock
    private LambdaLogger logger;
    @Mock
    private ApigService apigService;

    @InjectMocks
    private CreateSnapshotUploadPathActivity createSnapshotUploadPathActivity;

    Map<String, Object> proxyLambdaBody = Map.ofEntries(
        entry("deviceId", DEVICE_ID),
        entry("checksum", CHECKSUM),
        entry("contentLength", CONTENT_LENGTH)
    );
    Map<String, Object> lambdaProxyRequest = Map.ofEntries(
        entry(PROXY_LAMBDA_BODY_KEY, proxyLambdaBody)
    );

    SdkHttpResponse code200 = SdkHttpResponse.builder().statusCode(200).build();
    AbortableInputStream emptyStream = AbortableInputStream.createEmpty();
    HttpExecuteResponse successResp = HttpExecuteResponse.builder().response(code200).responseBody(emptyStream).build();

    SdkHttpResponse code500 = SdkHttpResponse.builder().statusCode(500).build();
    HttpExecuteResponse errResp = HttpExecuteResponse.builder().response(code500).responseBody(emptyStream).build();

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(context.getLogger()).thenReturn(logger);
        when(apigService.invokeUpdateDeviceShadow(eq(DEVICE_ID), eq(null), any())).thenReturn(successResp);
    }

    @Test
    public void handleRequest_WhenValidRequest_ReturnsSuccess() throws MalformedURLException {
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
        URL expectedUrl = new URL(S3_PRESIGNED_URL); 
        assertEquals(expectedUrl, presignedUrl);
        
        Map<String, Object> actualMessage = new CreateSnapshotUploadPathActivity(s3Presigner, region, SNAPSHOT_ACCOUNT_ID, apigService)
            .handleRequest(lambdaProxyRequest, context);
        Map<String, Object> expectedMessage = serializeResponse(200, 
            "{\"shadowPayload\":{\"shadowName\":\"snapshot\",\"stateDocument\":{\"presignedUrl\": \"http://s3.amazonaws.zom/testBucket/key.jpeg?pregin-bits\"}}}");
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void handleRequest_WhenNullInput_ThrowsValidationException() {
        Map<String, Object> actualMessage = new CreateSnapshotUploadPathActivity(s3Presigner, region, SNAPSHOT_ACCOUNT_ID, apigService)
            .handleRequest(null, context);

        final ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .reason(ValidationExceptionReason.FIELD_VALIDATION_FAILED)
                .build();

        Map<String, Object> expectedMessage = serializeResponse(400, exception.toJson());
        assertEquals(actualMessage, expectedMessage);
    }

    
    @Test
    public void handleRequest_WhenBlankDeviceId_ThrowsValidationException() {
        Map<String, Object> blankDeviceInputProxyLambdaBody = Map.ofEntries(
            entry("deviceId", ""),
            entry("checksum", CHECKSUM),
            entry("contentLength", CONTENT_LENGTH)
        );
        Map<String, Object> blankDeviceInputLambdaProxyRequest = Map.ofEntries(
            entry(PROXY_LAMBDA_BODY_KEY, blankDeviceInputProxyLambdaBody)
        );

        Map<String, Object> actualMessage = new CreateSnapshotUploadPathActivity(s3Presigner, region, SNAPSHOT_ACCOUNT_ID, apigService)
            .handleRequest(blankDeviceInputLambdaProxyRequest, context);

        final ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .reason(ValidationExceptionReason.FIELD_VALIDATION_FAILED)
                .build();

        Map<String, Object> expectedMessage = serializeResponse(400, exception.toJson());
        assertEquals(actualMessage, expectedMessage);
    }

    @Test
    public void handleRequest_WhenBlankChecksum_ThrowsValidationException() {
        Map<String, Object> blankChecksumProxyLambdaBody = Map.ofEntries(
            entry("deviceId", DEVICE_ID),
            entry("checksum", ""),
            entry("contentLength", CONTENT_LENGTH)
        );
        Map<String, Object> blankChecksumLambdaProxyRequest = Map.ofEntries(
            entry(PROXY_LAMBDA_BODY_KEY, blankChecksumProxyLambdaBody)
        );

        Map<String, Object> actualMessage = new CreateSnapshotUploadPathActivity(s3Presigner, region, SNAPSHOT_ACCOUNT_ID, apigService)
            .handleRequest(blankChecksumLambdaProxyRequest, context);

        final ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .reason(ValidationExceptionReason.FIELD_VALIDATION_FAILED)
                .build();

        Map<String, Object> expectedMessage = serializeResponse(400, exception.toJson());
        assertEquals(actualMessage, expectedMessage);
    }

    @Test
    public void handleRequest_WhenBlankContentLength_ThrowsValidationException() {
        Map<String, Object> blankContentLengthProxyLambdaBody = Map.ofEntries(
            entry("deviceId", DEVICE_ID),
            entry("checksum", CHECKSUM),
            entry("contentLength", "")
        );
        Map<String, Object> blankChecksumLambdaProxyRequest = Map.ofEntries(
            entry(PROXY_LAMBDA_BODY_KEY, blankContentLengthProxyLambdaBody)
        );

        Map<String, Object> actualMessage = new CreateSnapshotUploadPathActivity(s3Presigner, region, SNAPSHOT_ACCOUNT_ID, apigService)
            .handleRequest(blankChecksumLambdaProxyRequest, context);

        final ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .reason(ValidationExceptionReason.FIELD_VALIDATION_FAILED)
                .build();

        Map<String, Object> expectedMessage = serializeResponse(400, exception.toJson());
        assertEquals(actualMessage, expectedMessage);
    }

    @Test
    public void handleRequest_WhenContentLengthLessThan1_ThrowsValidationException() {
        Map<String, Object> contentLengthLessThan1ProxyLambdaBody = Map.ofEntries(
            entry("deviceId", DEVICE_ID),
            entry("checksum", CHECKSUM),
            entry("contentLength", 0)
        );
        Map<String, Object> blankChecksumLambdaProxyRequest = Map.ofEntries(
            entry(PROXY_LAMBDA_BODY_KEY, contentLengthLessThan1ProxyLambdaBody)
        );

        Map<String, Object> actualMessage = new CreateSnapshotUploadPathActivity(s3Presigner, region, SNAPSHOT_ACCOUNT_ID, apigService)
            .handleRequest(blankChecksumLambdaProxyRequest, context);

        final ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .reason(ValidationExceptionReason.FIELD_VALIDATION_FAILED)
                .build();

        Map<String, Object> expectedMessage = serializeResponse(400, exception.toJson());
        assertEquals(actualMessage, expectedMessage);
    }

    @Test
    public void handleRequest_WhenS3Exception_ThrowsInternalServerException() throws IOException {
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
            .thenThrow(S3Exception.builder().message("Internal server error").build());
        
        Exception exception = assertThrows(S3Exception.class, () -> {
            createSnapshotUploadPathActivity.handleRequest(lambdaProxyRequest, context);    
        });

        assertEquals(exception.getMessage(), INTERNAL_SERVER_EXCEPTION);
    }

    @Test
    public void handleRequest_WhenApiGServiceException_ThrowsInternalServerException() throws Exception {
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
        when(apigService.invokeUpdateDeviceShadow(eq(DEVICE_ID), eq(null), any())).thenReturn(errResp);
        
        Map<String, Object> actualMessage = createSnapshotUploadPathActivity.handleRequest(lambdaProxyRequest, context);

        final InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.builder()
                .message(INTERNAL_SERVER_EXCEPTION)
                .build();

        Map<String, Object> expectedMessage = serializeResponse(500, exception.toJson());
        assertEquals(actualMessage, expectedMessage);
    }
}
