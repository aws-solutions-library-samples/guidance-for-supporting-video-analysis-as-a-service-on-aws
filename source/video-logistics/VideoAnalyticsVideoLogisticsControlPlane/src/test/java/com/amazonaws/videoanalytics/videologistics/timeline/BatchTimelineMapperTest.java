package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.BATCH_TIMELINE_DESERIALIZATION_ERROR;

public class BatchTimelineMapperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BatchTimelineMapper batchTimelineMapper = new BatchTimelineMapper(objectMapper);

    @Mock
    ObjectMapper mockObjectMapper;

    @InjectMocks
    BatchTimelineMapper mockBatchTimelineMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void serialize_ValidBatchTimeline_ReturnsSerializedBytes() throws IOException {
        BatchTimeline batchTimeline = BatchTimeline.builder()
                .customerId("c123")
                .deviceId("d123")
                .timestamps(List.of(new TimestampInfo(1696444408123L, 4000L)))
                .build();

        byte[] serializedBatchTimeline = batchTimelineMapper.serialize(batchTimeline);
        assertNotNull(serializedBatchTimeline);
    }

    @Test
    public void serialize_ProcessingError_ThrowsRuntimeException() throws JsonProcessingException {
        doThrow(JsonProcessingException.class).when(mockObjectMapper).writeValueAsBytes(any());
        Exception exception = assertThrows(RuntimeException.class, () -> mockBatchTimelineMapper.serialize(any()));
        assertEquals(BATCH_TIMELINE_DESERIALIZATION_ERROR, exception.getMessage());
    }

    @Test
    public void deserialize_SingleTimestamp_ReturnsValidBatchTimeline() throws IOException {
        String json = "{\"customerId\": \"c123\", \"deviceId\": \"d123\"," +
                "\"timestamps\": [{\"timestamp\":1696444408123,\"duration\":4000}]}";

        BatchTimeline deserializedList = batchTimelineMapper.deserialize(json.getBytes(StandardCharsets.UTF_8));
        assertEquals(1696444408123L, deserializedList.getTimestamps().get(0).getTimestamp());
        assertEquals(4000L, deserializedList.getTimestamps().get(0).getDuration());
    }

    @Test
    public void deserialize_MultipleTimestamps_ReturnsValidBatchTimeline() throws IOException {
        String json = "{\"customerId\": \"c123\", \"deviceId\": \"d123\"," +
                "\"timestamps\": [{\"timestamp\":1696444408123,\"duration\":4000}, " +
                "{\"timestamp\":1696444408123,\"duration\":4000}," +
                "{\"timestamp\":1696444408123,\"duration\":4000}," +
                "{\"timestamp\":1696444408123,\"duration\":4000}]}";

        BatchTimeline deserializedList = batchTimelineMapper.deserialize(json.getBytes(StandardCharsets.UTF_8));
        assertNotNull(deserializedList);
        assertEquals(4, deserializedList.getTimestamps().size());
        assertEquals("c123", deserializedList.getCustomerId());
        assertEquals("d123", deserializedList.getDeviceId());
    }

    @Test
    public void deserialize_InvalidJson_ThrowsIOException() {
        String timelineJson = "[{\"test\":1696444408123,\"duration\":4000}]";
        assertThrows(IOException.class, () ->
                batchTimelineMapper.deserialize(timelineJson.getBytes(StandardCharsets.UTF_8)));
    }
}