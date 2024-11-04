package com.amazonaws.videoanalytics.devicemanagement.exceptions;

import software.amazon.awssdk.services.iot.model.CertificateStateException;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.DeleteConflictException;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;

import java.util.Map;

import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.DELETE_CONFLICT_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.RESOURCE_ALREADY_EXISTS_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.IOT_SERVICE_RETRYABLE;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.LIMIT_EXCEEDED_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.RESOURCE_NOT_FOUND_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.THROTTLING_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.UNAUTHORIZED_EXCEPTION;

import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.serializeResponse;

public class ExceptionTranslator {
    public static Map<String, Object> translateIotExceptionToLambdaResponse(AwsServiceException e) {
        if (e instanceof InvalidRequestException) {
            return serializeResponse(400, INVALID_INPUT_EXCEPTION);
        } else if (e instanceof ResourceNotFoundException |
                   e instanceof software.amazon.awssdk.services.iotdataplane.model.ResourceNotFoundException) {
            return serializeResponse(404, RESOURCE_NOT_FOUND_EXCEPTION);
        } else if (e instanceof UnauthorizedException || e.statusCode() == 403) {
            return serializeResponse(403, UNAUTHORIZED_EXCEPTION);
        } else if (e instanceof ThrottlingException) {
            return serializeResponse(500, THROTTLING_EXCEPTION);
        } else if (e instanceof InternalFailureException |
                   e instanceof ServiceUnavailableException |
                   e instanceof CertificateStateException) {
            return serializeResponse(500, INTERNAL_SERVER_EXCEPTION);
        } else if (e instanceof LimitExceededException) {
            return serializeResponse(500, LIMIT_EXCEEDED_EXCEPTION);
        } else if (e instanceof DeleteConflictException) {
            return serializeResponse(500, DELETE_CONFLICT_EXCEPTION);
        } else if (e instanceof ResourceAlreadyExistsException) {
            return serializeResponse(409, RESOURCE_ALREADY_EXISTS_EXCEPTION);
        } else {
            return serializeResponse(500, INTERNAL_SERVER_EXCEPTION);
        }
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
