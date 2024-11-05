package com.amazonaws.videoanalytics.devicemanagement.exceptions;

import com.amazonaws.videoanalytics.devicemanagement.AccessDeniedExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ConflictExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ResourceNotFoundExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.ValidationExceptionResponseContent;

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
        if (e instanceof InvalidRequestException ||
            e instanceof software.amazon.awssdk.services.iotdataplane.model.InvalidRequestException) {
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message(INVALID_INPUT_EXCEPTION)
                    .build();
            return serializeResponse(400, exception.toJson());
        } else if (e instanceof ResourceNotFoundException ||
                   e instanceof software.amazon.awssdk.services.iotdataplane.model.ResourceNotFoundException) {
            ResourceNotFoundExceptionResponseContent exception = ResourceNotFoundExceptionResponseContent.builder()
                    .message(RESOURCE_NOT_FOUND_EXCEPTION)
                    .build();
            return serializeResponse(404, exception.toJson());
        } else if (e instanceof UnauthorizedException || 
                   e instanceof software.amazon.awssdk.services.iotdataplane.model.UnauthorizedException || 
                   e.statusCode() == 403) {
            AccessDeniedExceptionResponseContent exception = AccessDeniedExceptionResponseContent.builder()
                    .message(UNAUTHORIZED_EXCEPTION)
                    .build();
            return serializeResponse(403, exception.toJson());
        } else if (e instanceof ThrottlingException ||
                   e instanceof software.amazon.awssdk.services.iotdataplane.model.ThrottlingException) {
            InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.builder()
                    .message(THROTTLING_EXCEPTION)
                    .build();
            return serializeResponse(500, exception.toJson());
        } else if (e instanceof InternalFailureException ||
                   e instanceof ServiceUnavailableException ||
                   e instanceof CertificateStateException || 
                   e instanceof software.amazon.awssdk.services.iotdataplane.model.InternalFailureException ||
                   e instanceof software.amazon.awssdk.services.iotdataplane.model.ServiceUnavailableException) {
            InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.builder()
                    .message(INTERNAL_SERVER_EXCEPTION)
                    .build();
            return serializeResponse(500, exception.toJson());
        } else if (e instanceof LimitExceededException) {
            InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.builder()
                    .message(LIMIT_EXCEEDED_EXCEPTION)
                    .build();
            return serializeResponse(500, exception.toJson());
        } else if (e instanceof DeleteConflictException) {
            ConflictExceptionResponseContent exception = ConflictExceptionResponseContent.builder()
                    .message(DELETE_CONFLICT_EXCEPTION)
                    .build();
            return serializeResponse(409, exception.toJson());
        } else if (e instanceof ResourceAlreadyExistsException) {
            ConflictExceptionResponseContent exception = ConflictExceptionResponseContent.builder()
                    .message(RESOURCE_ALREADY_EXISTS_EXCEPTION)
                    .build();
            return serializeResponse(409, exception.toJson());
        } else {
            InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.builder()
                    .message(INTERNAL_SERVER_EXCEPTION)
                    .build();
            return serializeResponse(500, exception.toJson());
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
