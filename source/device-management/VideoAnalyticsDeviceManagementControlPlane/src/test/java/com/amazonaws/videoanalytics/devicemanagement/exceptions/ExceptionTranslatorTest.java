package com.amazonaws.videoanalytics.devicemanagement.exceptions;

import org.junit.jupiter.api.Test;

import com.amazonaws.videoanalytics.devicemanagement.AccessDeniedExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ConflictExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ResourceNotFoundExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ValidationExceptionResponseContent;

import software.amazon.awssdk.services.iot.model.DeleteConflictException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;

import java.io.IOException;
import java.util.Map;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parseBody;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExceptionTranslatorTest {
    @Test
    public void translateIotExceptionToLambdaResponse_WhenInvalidRequestException_ThrowsValidationException() throws IOException {
        InvalidRequestException iotException = InvalidRequestException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToLambdaResponse_WhenResourceNotFoundException_ThrowsResourceNotFoundException() throws IOException {
        ResourceNotFoundException iotException = ResourceNotFoundException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 404);
        ResourceNotFoundExceptionResponseContent exception = ResourceNotFoundExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.RESOURCE_NOT_FOUND_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToLambdaResponse_WhenUnauthorizedException_ThrowsAccessDeniedException() throws IOException {
        UnauthorizedException iotException = UnauthorizedException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 403);
        AccessDeniedExceptionResponseContent exception = AccessDeniedExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.UNAUTHORIZED_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToLambdaResponse_WhenThrottlingException_ThrowsThrottlingException() throws IOException {
        ThrottlingException iotException = ThrottlingException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.THROTTLING_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToLambdaResponse_WhenInternalFailureException_ThrowsInternalServerException() throws IOException {
        InternalFailureException iotException = InternalFailureException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToLambdaResponse_WhenLimitExceededException_ThrowsLimitExceededException() throws IOException {
        LimitExceededException iotException = LimitExceededException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.LIMIT_EXCEEDED_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToLambdaResponse_WhenDeleteConflictException_ThrowsDeleteConflictException() throws IOException {
        DeleteConflictException iotException = DeleteConflictException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 409);
        ConflictExceptionResponseContent exception = ConflictExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.DELETE_CONFLICT_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToLambdaResponse_WhenResourceAlreadyExistsException_ThrowsResourceAlreadyExistsException() throws IOException {
        ResourceAlreadyExistsException iotException = ResourceAlreadyExistsException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 409);
        ConflictExceptionResponseContent exception = ConflictExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.RESOURCE_ALREADY_EXISTS_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToRetryable_WhenRetryable_ThrowsRetryable() {
        IotException iotException = (IotException) ThrottlingException.builder().build();
        Exception exception = assertThrows(RetryableException.class, () -> {
            ExceptionTranslator.translateIotExceptionToRetryable(iotException);
        });
        assertEquals(exception.getMessage(), VideoAnalyticsExceptionMessage.IOT_SERVICE_RETRYABLE);
    }

    @Test
    public void translateIotExceptionToRetryable_WhenNotRetryable_DoesNotThrowRetryable() {
        IotException iotException = (IotException) IotException.builder().build();
        assertThrows(IotException.class, () -> {
            ExceptionTranslator.translateIotExceptionToRetryable(iotException);
        });
    }
}
