package com.amazonaws.videoanalytics.videologistics.timeline;

import java.util.List;

import javax.inject.Inject;

import com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TimestampListDeserializer {
    private final ObjectMapper objectMapper;
    private final TypeReference<List<TimestampInfo>> typeReference = new TypeReference<>() {};
    @Inject
    public TimestampListDeserializer(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<TimestampInfo> deserialize(final String timestampListJson) {
        try {
            //parse as JsonNode to handle both direct JSON and string-encoded JSON
            JsonNode jsonNode = objectMapper.readTree(timestampListJson);
            if (jsonNode.isTextual()) {
                return objectMapper.readValue(jsonNode.asText(), typeReference);
            }
            return objectMapper.convertValue(jsonNode, typeReference);
        } catch (Exception e) {
            throw new RuntimeException(String.format(VideoAnalyticsExceptionMessage.TIMELINE_DESERIALIZATION_ERROR, timestampListJson), e);
        }
    }
}