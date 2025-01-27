package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.DESERIALIZATION_ERROR;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;

import javax.inject.Inject;

public class TimelineKDSMetadataSerDe {
    private final ObjectMapper objectMapper;

    @Inject
    public TimelineKDSMetadataSerDe(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(final TimelineKDSMetadata timelineKDSMetadata) {
        try {
            return objectMapper.writeValueAsString(timelineKDSMetadata);
        } catch (Exception e) {
            InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.builder()
                    .message(INTERNAL_SERVER_EXCEPTION)
                    .build();
            throw new RuntimeException(exception.toJson(), e);
        }
    }

    public TimelineKDSMetadata deserialize(final String timelineKDSMetadataStr) {
        try {
            return objectMapper.readValue(timelineKDSMetadataStr, TimelineKDSMetadata.class);
        } catch (Exception e) {
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message(String.format(DESERIALIZATION_ERROR, "TimelineKDSMetadata", e))
                    .build();
            throw new RuntimeException(exception.toJson(), e);
        }
    }
}