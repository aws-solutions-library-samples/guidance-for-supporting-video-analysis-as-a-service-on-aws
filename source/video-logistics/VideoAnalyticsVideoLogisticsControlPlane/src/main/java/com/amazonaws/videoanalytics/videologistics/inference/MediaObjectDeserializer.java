package com.amazonaws.videoanalytics.videologistics.inference;

import com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;

public class MediaObjectDeserializer {
    private final ObjectMapper objectMapper;

    @Inject
    public MediaObjectDeserializer(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    public MediaObject deserialize(final String mediaObject) {
        try {
            return objectMapper.readValue(mediaObject, MediaObject.class);
        } catch (Exception e) {
            throw new RuntimeException(String.format(VideoAnalyticsExceptionMessage.DESERIALIZATION_ERROR, "media", e));
        }
    }
}