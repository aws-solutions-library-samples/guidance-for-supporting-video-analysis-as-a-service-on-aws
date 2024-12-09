package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import java.io.IOException;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.BATCH_TIMELINE_DESERIALIZATION_ERROR;

public class BatchTimelineMapper {
    private final ObjectMapper objectMapper;
    @Inject
    public BatchTimelineMapper(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public BatchTimeline deserialize(final byte[] batchTimeline) throws IOException {
        return objectMapper.readValue(batchTimeline, BatchTimeline.class);
    }

    /**
     * Maps batch timeline object to a byte array
     * @param batchTimeline batch timeline object to be serialized
     * @return byte array representation of batch timeline
     * @throws RuntimeException if serialization fails
     */
    public byte[] serialize(final BatchTimeline batchTimeline) {
        try {
            return objectMapper.writeValueAsBytes(batchTimeline);
        } catch (JsonProcessingException e) {
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(BATCH_TIMELINE_DESERIALIZATION_ERROR)
                .build();
            throw new RuntimeException(exception.getMessage(), e);
        }
    }
}