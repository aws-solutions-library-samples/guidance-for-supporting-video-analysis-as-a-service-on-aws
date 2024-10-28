package com.amazonaws.videoanalytics.exceptions;

import com.amazonaws.ApiException;
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

public class ExceptionTranslator {

    public static Exception translateIotExceptionToApiException(final IotException e) throws ApiException {
        if (e instanceof InvalidRequestException) {
            throw new ApiException(400, INVALID_INPUT_EXCEPTION);
        } else if (e instanceof ResourceNotFoundException) {
            throw new ApiException(404, RESOURCE_NOT_FOUND);
        } else if (e instanceof UnauthorizedException || e.statusCode() == 403) {
            throw new ApiException(403, UNAUTHORIZED_EXCEPTION);
        } else if (e instanceof ThrottlingException) {
            throw new ApiException(500, THROTTLING_EXCEPTION);
        } else if (e instanceof InternalFailureException |
                e instanceof ServiceUnavailableException |
                e instanceof CertificateStateException) {
            throw new ApiException(500, INTERNAL_SERVER_EXCEPTION);
        } else if (e instanceof LimitExceededException) {
            throw new ApiException(500, LIMIT_EXCEEDED_EXCEPTION);
        } else if (e instanceof DeleteConflictException) {
            throw new ApiException(500, DELETE_CONFLICT_EXCEPTION);
        } else if (e instanceof ResourceAlreadyExistsException) {
            throw new ApiException(409, RESOURCE_ALREADY_EXISTS);
        } else {
            throw new ApiException(500, INTERNAL_SERVER_EXCEPTION);
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
