package com.amazonaws.videoanalytics.devicemanagement.exceptions;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.iot.model.DeleteConflictException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;

import java.util.Map;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_BODY_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExceptionTranslatorTest {
    @Test
    public void translateIotExceptionToRuntimeException_WhenInvalidRequestException_ThrowsInvalidInputException() {
        InvalidRequestException iotException = InvalidRequestException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_BODY_KEY), VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToRuntimeException_WhenResourceNotFoundException_ThrowsResourceNotFoundException() {
        ResourceNotFoundException iotException = ResourceNotFoundException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 404);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_BODY_KEY), VideoAnalyticsExceptionMessage.RESOURCE_NOT_FOUND_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToRuntimeException_WhenUnauthorizedException_ThrowsUnauthorizedException() {
        UnauthorizedException iotException = UnauthorizedException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 403);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_BODY_KEY), VideoAnalyticsExceptionMessage.UNAUTHORIZED_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToRuntimeException_WhenThrottlingException_ThrowsThrottlingException() {
        ThrottlingException iotException = ThrottlingException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_BODY_KEY), VideoAnalyticsExceptionMessage.THROTTLING_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToRuntimeException_WhenInternalFailureException_ThrowsInternalServerException() {
        InternalFailureException iotException = InternalFailureException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_BODY_KEY), VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToRuntimeException_WhenLimitExceededException_ThrowsLimitExceededException() {
        LimitExceededException iotException = LimitExceededException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_BODY_KEY), VideoAnalyticsExceptionMessage.LIMIT_EXCEEDED_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToRuntimeException_WhenDeleteConflictException_ThrowsDeleteConflictException() {
        DeleteConflictException iotException = DeleteConflictException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_BODY_KEY), VideoAnalyticsExceptionMessage.DELETE_CONFLICT_EXCEPTION);
    }

    @Test
    public void translateIotExceptionToRuntimeException_WhenResourceAlreadyExistsException_ThrowsResourceAlreadyExistsException() {
        ResourceAlreadyExistsException iotException = ResourceAlreadyExistsException.builder().build();
        Map<String, Object> responseMap = ExceptionTranslator.translateIotExceptionToLambdaResponse(iotException);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 409);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_BODY_KEY), VideoAnalyticsExceptionMessage.RESOURCE_ALREADY_EXISTS_EXCEPTION);
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
