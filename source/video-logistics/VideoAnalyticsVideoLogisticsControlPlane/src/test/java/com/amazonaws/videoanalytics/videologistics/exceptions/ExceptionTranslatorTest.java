package com.amazonaws.videoanalytics.videologistics.exceptions;

import org.junit.jupiter.api.Test;

import com.amazonaws.videoanalytics.videologistics.AccessDeniedExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ConflictExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ResourceNotFoundExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.kinesisvideo.model.AccessDeniedException;
import software.amazon.awssdk.services.kinesisvideo.model.ResourceInUseException;
import software.amazon.awssdk.services.kinesisvideo.model.ResourceNotFoundException;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.NoDataRetentionException;

import java.io.IOException;
import java.util.Map;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBody;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExceptionTranslatorTest {
    @Test
    public void translateKvsExceptionToLambdaResponse_WhenResourceNotFoundException_ThrowsResourceNotFoundException() throws IOException {
        ResourceNotFoundException kvsException = ResourceNotFoundException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateKvsExceptionToLambdaResponse(kvsException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 404);
        ResourceNotFoundExceptionResponseContent exception = ResourceNotFoundExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.DEVICE_NOT_REGISTERED);
    }

    @Test
    public void translateKvsExceptionToLambdaResponse_WhenArchivedMediaResourceNotFoundException_ThrowsResourceNotFoundException() throws IOException {
        software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.ResourceNotFoundException kvsException = 
            software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.ResourceNotFoundException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateKvsExceptionToLambdaResponse(kvsException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 404);
        ResourceNotFoundExceptionResponseContent exception = ResourceNotFoundExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.NO_VIDEO_FRAGMENTS);
    }

    @Test
    public void translateKvsExceptionToLambdaResponse_WhenAccessDenied_ThrowsAccessDeniedException() throws IOException {
        AccessDeniedException kvsException = AccessDeniedException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateKvsExceptionToLambdaResponse(kvsException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 403);
        AccessDeniedExceptionResponseContent exception = AccessDeniedExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.NOT_AUTHORIZED);
    }

    @Test
    public void translateKvsExceptionToLambdaResponse_WhenResourceInUse_ThrowsConflictException() throws IOException {
        ResourceInUseException kvsException = ResourceInUseException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateKvsExceptionToLambdaResponse(kvsException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 409);
        ConflictExceptionResponseContent exception = ConflictExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.DEVICE_NOT_REGISTERED);
    }

    @Test
    public void translateKvsExceptionToLambdaResponse_WhenNoDataRetention_ThrowsValidationException() throws IOException {
        NoDataRetentionException kvsException = NoDataRetentionException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateKvsExceptionToLambdaResponse(kvsException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.NO_DATA_RETENTION);
    }

    @Test
    public void translateKvsExceptionToLambdaResponse_WhenAwsServiceException_ThrowsInternalServiceException() throws IOException {
        AwsServiceException awsServiceException = AwsServiceException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateKvsExceptionToLambdaResponse(awsServiceException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION);
    }
}
