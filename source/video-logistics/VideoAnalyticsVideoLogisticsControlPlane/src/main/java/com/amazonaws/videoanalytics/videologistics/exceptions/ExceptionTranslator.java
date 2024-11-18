package com.amazonaws.videoanalytics.videologistics.exceptions;

import com.amazonaws.videoanalytics.videologistics.AccessDeniedExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ConflictExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ResourceNotFoundExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.kinesisvideo.model.AccessDeniedException;
import software.amazon.awssdk.services.kinesisvideo.model.ResourceInUseException;
import software.amazon.awssdk.services.kinesisvideo.model.ResourceNotFoundException;
import software.amazon.awssdk.services.kinesisvideosignaling.model.NotAuthorizedException;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.NoDataRetentionException;

import java.util.Map;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.DEVICE_NOT_REGISTERED;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.NOT_AUTHORIZED;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.NO_DATA_RETENTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.NO_VIDEO_FRAGMENTS;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;

import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;


public class ExceptionTranslator {
    public static Map<String, Object> translateKvsExceptionToLambdaResponse(AwsServiceException e) {
        if (e instanceof ResourceNotFoundException ||
            e instanceof software.amazon.awssdk.services.kinesisvideosignaling.model.ResourceNotFoundException) {
            ResourceNotFoundExceptionResponseContent exception = ResourceNotFoundExceptionResponseContent.builder()
                    .message(DEVICE_NOT_REGISTERED)
                    .build();
            return serializeResponse(404, exception.toJson());
        } else if (e instanceof software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.ResourceNotFoundException) {
            ResourceNotFoundExceptionResponseContent exception = ResourceNotFoundExceptionResponseContent.builder()
                    .message(NO_VIDEO_FRAGMENTS)
                    .build();
            return serializeResponse(404, exception.toJson());
        } else if (e instanceof AccessDeniedException ||
                   e instanceof NotAuthorizedException ||
                   e instanceof software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.NotAuthorizedException ||
                   e.statusCode() == 403) {
            AccessDeniedExceptionResponseContent exception = AccessDeniedExceptionResponseContent.builder()
                    .message(NOT_AUTHORIZED)
                    .build();
            return serializeResponse(403, exception.toJson());
        } else if (e instanceof ResourceInUseException) {
            ConflictExceptionResponseContent exception = ConflictExceptionResponseContent.builder()
                    .message(DEVICE_NOT_REGISTERED)
                    .build();
            return serializeResponse(409, exception.toJson());
        } else if (e instanceof NoDataRetentionException) {
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message(NO_DATA_RETENTION)
                    .build();
            return serializeResponse(400, exception.toJson());
        } else {
            InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.builder()
                    .message(INTERNAL_SERVER_EXCEPTION)
                    .build();
            return serializeResponse(500, exception.toJson());
        }
    }

    private ExceptionTranslator() {
        // Private default constructor so that JaCoCo marks utility class as covered
    }
}
