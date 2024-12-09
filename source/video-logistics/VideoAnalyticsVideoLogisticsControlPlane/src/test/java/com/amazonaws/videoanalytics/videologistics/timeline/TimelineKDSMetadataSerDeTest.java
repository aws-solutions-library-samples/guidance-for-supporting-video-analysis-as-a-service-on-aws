package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.TimeIncrementUnits;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.VideoDensityLocation;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.DESERIALIZATION_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TimelineKDSMetadataSerDeTest {
    private static final String DEVICE_ID = "device_001";
    private static final TimelineKDSMetadata TIMELINE_KDS_METADATA = new TimelineKDSMetadata(
            DEVICE_ID,
            TimeIncrementUnits.SECONDS,
            1696444405000L,
            1696444408000L,
            2000L,
            VideoDensityLocation.CLOUD,
            false
    );
    private static final String TIMELINE_KDS_METADATA_STR = "{\"deviceId\":\"device_001\"," +
            "\"timeIncrementUnits\":\"SECONDS\",\"bucketStartTime\":1696444405000,\"timestampToBeStored\":1696444408000," +
            "\"durationInMillis\":2000,\"videoDensityLocation\":\"CLOUD\",\"isCatchup\":false,\"catchup\":false}";

    @Mock
    private ObjectMapper mockObjectMapper;

    @InjectMocks
    private TimelineKDSMetadataSerDe mockSerDe;

    private TimelineKDSMetadataSerDe serDe;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper.setVisibility(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.NONE)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        serDe = new TimelineKDSMetadataSerDe(objectMapper);
    }

    @Test
    public void serialize_ValidMetadata_ReturnsSerializedString() {
        String timeline = serDe.serialize(TIMELINE_KDS_METADATA);
        assertEquals(TIMELINE_KDS_METADATA_STR, timeline);
    }

    @Test
    public void serialize_ProcessingError_ThrowsRuntimeException() throws Exception {
        when(mockObjectMapper.writeValueAsString(any())).thenThrow(new RuntimeException());
        Exception exception = assertThrows(RuntimeException.class, () -> 
            mockSerDe.serialize(TIMELINE_KDS_METADATA));
        assertEquals(InternalServerExceptionResponseContent.builder()
            .message(INTERNAL_SERVER_EXCEPTION)
            .build()
            .toJson(), exception.getMessage());
    }

    @Test
    public void deserialize_ValidString_ReturnsTimelineKDSMetadata() {
        TimelineKDSMetadata metadata = serDe.deserialize(TIMELINE_KDS_METADATA_STR);
        assertEquals(1696444405000L, metadata.getBucketStartTime());
    }

    @Test
    public void deserialize_InvalidInput_ThrowsRuntimeException() throws Exception {
        RuntimeException mockException = new RuntimeException("Mock error");
        when(mockObjectMapper.readValue(TIMELINE_KDS_METADATA_STR, TimelineKDSMetadata.class))
                .thenThrow(mockException);

        Exception exception = assertThrows(RuntimeException.class, () -> 
            mockSerDe.deserialize(TIMELINE_KDS_METADATA_STR));
        assertEquals(ValidationExceptionResponseContent.builder()
            .message(String.format(DESERIALIZATION_ERROR, "TimelineKDSMetadata", mockException))
            .build()
            .toJson(), exception.getMessage());
    }
    
}