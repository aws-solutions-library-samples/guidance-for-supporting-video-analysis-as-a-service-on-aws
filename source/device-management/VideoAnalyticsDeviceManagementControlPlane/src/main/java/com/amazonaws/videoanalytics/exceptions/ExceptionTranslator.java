package com.amazonaws.videoanalytics.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.iot.model.CertificateStateException;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.DeleteConflictException;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;

import static com.amazonaws.videoanalytics.exceptions.VideoAnalyticsExceptionMessage.DELETE_CONFLICT_EXCEPTION;
import static com.amazonaws.videoanalytics.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.exceptions.VideoAnalyticsExceptionMessage.RESOURCE_ALREADY_EXISTS;
import static com.amazonaws.videoanalytics.exceptions.VideoAnalyticsExceptionMessage.IOT_SERVICE_RETRYABLE;
import static com.amazonaws.videoanalytics.exceptions.VideoAnalyticsExceptionMessage.LIMIT_EXCEEDED_EXCEPTION;
import static com.amazonaws.videoanalytics.exceptions.VideoAnalyticsExceptionMessage.RESOURCE_NOT_FOUND;
import static com.amazonaws.videoanalytics.exceptions.VideoAnalyticsExceptionMessage.THROTTLING_EXCEPTION;
import static com.amazonaws.videoanalytics.exceptions.VideoAnalyticsExceptionMessage.UNAUTHORIZED_EXCEPTION;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

public class ExceptionTranslator {
    // Followed for below link for error handling
    // https://aws.amazon.com/blogs/compute/error-handling-patterns-in-amazon-api-gateway-and-aws-lambda/
    public static void translateIotExceptionToRuntimeException(final IotException e, String requestId) throws RuntimeException{
        if (e instanceof InvalidRequestException) {
            throw new RuntimeException(buildErrorPayload(400, INVALID_INPUT_EXCEPTION, requestId));
        } else if (e instanceof ResourceNotFoundException) {
            throw new RuntimeException(buildErrorPayload(404, RESOURCE_NOT_FOUND, requestId));
        } else if (e instanceof UnauthorizedException || e.statusCode() == 403) {
            throw new RuntimeException(buildErrorPayload(403, UNAUTHORIZED_EXCEPTION, requestId));
        } else if (e instanceof ThrottlingException) {
            throw new RuntimeException(buildErrorPayload(500, THROTTLING_EXCEPTION, requestId));
        } else if (e instanceof InternalFailureException |
                e instanceof ServiceUnavailableException |
                e instanceof CertificateStateException) {
            throw new RuntimeException(buildErrorPayload(500, INTERNAL_SERVER_EXCEPTION, requestId));
        } else if (e instanceof LimitExceededException) {
            throw new RuntimeException(buildErrorPayload(500, LIMIT_EXCEEDED_EXCEPTION, requestId));
        } else if (e instanceof DeleteConflictException) {
            throw new RuntimeException(buildErrorPayload(500, DELETE_CONFLICT_EXCEPTION, requestId));
        } else if (e instanceof ResourceAlreadyExistsException) {
            throw new RuntimeException(buildErrorPayload(409, RESOURCE_ALREADY_EXISTS, requestId));
        } else {
            throw new RuntimeException(buildErrorPayload(500, INTERNAL_SERVER_EXCEPTION, requestId));
        }
    }

    public static String buildErrorPayload(int errorCode, String errorMsg, String requestId) {
        Map<String, Object> errorPayload = new HashMap();
        errorPayload.put("httpStatus", errorCode);
        errorPayload.put("requestId", requestId);
        errorPayload.put("message", errorMsg);
        String message;
        try {
            message = new ObjectMapper().writeValueAsString(errorPayload);
        } catch (JsonProcessingException e) {
            message = "";
        }

        return message;
    }

    // Used for checking if a lambda handler should retry after an IotException is thrown
    public static Exception translateIotExceptionToRetryable(final IotException e) {
        if (e instanceof InternalException |
            e instanceof InternalFailureException | 
            e instanceof software.amazon.awssdk.services.iot.model.InternalServerException |
            e instanceof ServiceUnavailableException |
            e instanceof ThrottlingException |
            e instanceof ConflictingResourceUpdateException) {
            throw new RetryableException(IOT_SERVICE_RETRYABLE);
        } else {
            throw e;
        }
    }

    private ExceptionTranslator() {
        // Private default constructor so that JaCoCo marks utility class as covered
    }
}
