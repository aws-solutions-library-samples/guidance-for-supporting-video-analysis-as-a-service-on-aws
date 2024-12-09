package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimestampListDeserializerTest {
    private TimestampListDeserializer deserializer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        deserializer = new TimestampListDeserializer(objectMapper);
    }

    @Test
    void deserialize_validJson_returnsTimestampList() {
        String validJson = "[{\"timestamp\":1234567890,\"duration\":5000}]";
        List<TimestampInfo> result = deserializer.deserialize(validJson);
        
        assertEquals(1, result.size());
        assertEquals(1234567890L, result.get(0).getTimestamp());
        assertEquals(5000L, result.get(0).getDuration());
    }

    @Test
    void deserialize_invalidJson_throwsRuntimeException() {
        String invalidJson = "invalid json";
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> deserializer.deserialize(invalidJson));
        
        assertEquals(String.format(VideoAnalyticsExceptionMessage.TIMELINE_DESERIALIZATION_ERROR, invalidJson), 
            exception.getMessage());
    }

    @Test
    void deserialize_emptyJson_returnsEmptyList() {
        String emptyJson = "[]";
        List<TimestampInfo> result = deserializer.deserialize(emptyJson);
        
        assertEquals(0, result.size());
    }
}