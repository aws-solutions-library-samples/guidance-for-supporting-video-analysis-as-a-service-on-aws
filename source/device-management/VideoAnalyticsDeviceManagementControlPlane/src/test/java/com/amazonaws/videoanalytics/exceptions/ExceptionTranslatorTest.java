package com.amazonaws.videoanalytics.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.amazonaws.videoanalytics.utils.TestConstants.REQUEST_ID;

public class ExceptionTranslatorTest {
    @Test
    public void translateIotExceptionToRuntimeException_WhenInvalidRequestException_ThrowsInvalidInputException() {
        InvalidRequestException iotException = InvalidRequestException.builder().build();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ExceptionTranslator.translateIotExceptionToRuntimeException(iotException, REQUEST_ID);
        });
        assertTrue(exception.getMessage().contains("400"));
        assertTrue(exception.getMessage().contains(VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION));
    }

    @Test
    public void translateIotExceptionToRuntimeException_WhenResourceNotFoundException_ThrowsResourceNotFoundException() {
        ResourceNotFoundException iotException = ResourceNotFoundException.builder().build();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ExceptionTranslator.translateIotExceptionToRuntimeException(iotException, REQUEST_ID);
        });
        assertTrue(exception.getMessage().contains("404"));
        assertTrue(exception.getMessage().contains(VideoAnalyticsExceptionMessage.RESOURCE_NOT_FOUND));
    }

    @Test
    public void translateIotExceptionToRuntimeException_WhenUnauthorizedException_ThrowsUnauthorizedException() {
        UnauthorizedException iotException = UnauthorizedException.builder().build();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ExceptionTranslator.translateIotExceptionToRuntimeException(iotException, REQUEST_ID);
        });
        assertTrue(exception.getMessage().contains("403"));
        assertTrue(exception.getMessage().contains(VideoAnalyticsExceptionMessage.UNAUTHORIZED_EXCEPTION));
    }

    @Test
    public void translateIotExceptionToRuntimeException_WhenThrottlingException_ThrowsThrottlingException() {
        ThrottlingException iotException = ThrottlingException.builder().build();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ExceptionTranslator.translateIotExceptionToRuntimeException(iotException, REQUEST_ID);
        });
        assertTrue(exception.getMessage().contains("500"));
        assertTrue(exception.getMessage().contains(VideoAnalyticsExceptionMessage.THROTTLING_EXCEPTION));
    }

    @Test
    public void translateIotExceptionToRuntimeException_WhenInternalFailureException_ThrowsInternalServerException() {
        InternalFailureException iotException = InternalFailureException.builder().build();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ExceptionTranslator.translateIotExceptionToRuntimeException(iotException, REQUEST_ID);
        });
        assertTrue(exception.getMessage().contains("500"));
        assertTrue(exception.getMessage().contains(VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION));
    }

    @Test
    public void translateIotExceptionToRuntimeException_WhenLimitExceededException_ThrowsLimitExceededException() {
        LimitExceededException iotException = LimitExceededException.builder().build();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ExceptionTranslator.translateIotExceptionToRuntimeException(iotException, REQUEST_ID);
        });
        assertTrue(exception.getMessage().contains("500"));
        assertTrue(exception.getMessage().contains(VideoAnalyticsExceptionMessage.LIMIT_EXCEEDED_EXCEPTION));
    }

    @Test
    public void translateIotExceptionToRuntimeException_WhenDeleteConflictException_ThrowsDeleteConflictException() {
        DeleteConflictException iotException = DeleteConflictException.builder().build();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ExceptionTranslator.translateIotExceptionToRuntimeException(iotException, REQUEST_ID);
        });
        assertTrue(exception.getMessage().contains("500"));
        assertTrue(exception.getMessage().contains(VideoAnalyticsExceptionMessage.DELETE_CONFLICT_EXCEPTION));
    }

    @Test
    public void translateIotExceptionToRuntimeException_WhenResourceAlreadyExistsException_ThrowsResourceAlreadyExistsException() {
        ResourceAlreadyExistsException iotException = ResourceAlreadyExistsException.builder().build();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ExceptionTranslator.translateIotExceptionToRuntimeException(iotException, REQUEST_ID);
        });
        assertTrue(exception.getMessage().contains("409"));
        assertTrue(exception.getMessage().contains(VideoAnalyticsExceptionMessage.RESOURCE_ALREADY_EXISTS));
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
