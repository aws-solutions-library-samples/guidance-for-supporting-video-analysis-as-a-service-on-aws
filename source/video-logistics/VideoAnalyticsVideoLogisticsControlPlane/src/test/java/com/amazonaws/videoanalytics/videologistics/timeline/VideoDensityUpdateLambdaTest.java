package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.RawVideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.VideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.RawVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.VideoDensityLocation;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.RAW_PARTITION_KEY;
import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.RAW_SORT_KEY;
import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.RAW_VIDEO_TIMELINE_PARTITION_KEY;
import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.TIMESTAMP;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VideoDensityUpdateLambdaTest {
    private static final String DUMMY_PARTITION_KEY = "Dummy Key";
    private static final Long DURATION_IN_MILLIS = 60000L;
    private static final Long TIMESTAMP_VALUE = 1696444408000L;
    private static final String DEVICE_ID = "d234";
    
    @Mock
    private VideoTimelineDAO videoTimelineDAO;
    @Mock
    private Context context;
    @Mock
    private TimelineKDSMetadataSerDe timelineKDSMetadataSerDe;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
    
    @InjectMocks
    private VideoDensityUpdateLambda videoDensityUpdateLambda;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TimelineKDSMetadataSerDe deserializer = new TimelineKDSMetadataSerDe(objectMapper);
    private static final String TIMELINE_KDS_DATA_1 = "{\"deviceId\":\"device_001\",\"timeIncrementUnits\":\"SECONDS\",\"bucketStartTime\":1622505600000,\"timestampToBeStored\":1622505660000,\"durationInMillis\":60000,\"videoDensityLocation\":\"CLOUD\",\"isCatchup\":true}";
    private static final String TIMELINE_KDS_DATA_2 = "{\"deviceId\":\"device_001\",\"timeIncrementUnits\":\"SECONDS\",\"bucketStartTime\":1622505605000,\"timestampToBeStored\":1622505660000,\"durationInMillis\":60000,\"videoDensityLocation\":\"CLOUD\",\"isCatchup\":true}";

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        environmentVariables.set(RAW_PARTITION_KEY, RAW_VIDEO_TIMELINE_PARTITION_KEY);
        environmentVariables.set(RAW_SORT_KEY, TIMESTAMP);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    @Test
    public void handleRequest_validKinesisEvents_saveTimelineTwice() {
        KinesisEvent event = createKinesisEvent(List.of(TIMELINE_KDS_DATA_1, TIMELINE_KDS_DATA_2));
        when(timelineKDSMetadataSerDe.deserialize(anyString()))
                .thenReturn(deserializer.deserialize(TIMELINE_KDS_DATA_1))
                .thenReturn(deserializer.deserialize(TIMELINE_KDS_DATA_2));

        videoDensityUpdateLambda.handleRequest(event, context);
        verify(videoTimelineDAO, times(2)).save(any());
    }

    @Test
    public void handleRequest_deserializationFails_throwsException() {
        KinesisEvent event = createKinesisEvent(List.of(TIMELINE_KDS_DATA_1, TIMELINE_KDS_DATA_2));
        when(timelineKDSMetadataSerDe.deserialize(TIMELINE_KDS_DATA_1))
                .thenThrow(new RuntimeException("Deserialization failed"));

        assertThrows(RuntimeException.class, () -> videoDensityUpdateLambda.handleRequest(event, context));
    }

    private KinesisEvent createKinesisEvent(List<String> timelineJsonList) {
        List<KinesisEvent.KinesisEventRecord> records = new ArrayList<>();
        int sequenceNumber = 1;
        
        for (String timelineJson : timelineJsonList) {
            KinesisEvent.KinesisEventRecord record = new KinesisEvent.KinesisEventRecord();
            ByteBuffer data = ByteBuffer.wrap(timelineJson.getBytes(StandardCharsets.UTF_8));
            
            KinesisEvent.Record kinesisRecord = new KinesisEvent.Record();
            kinesisRecord.setPartitionKey(DUMMY_PARTITION_KEY);
            kinesisRecord.setSequenceNumber(String.valueOf(sequenceNumber++));
            kinesisRecord.setData(data);
            kinesisRecord.setApproximateArrivalTimestamp(new Date());

            record.setKinesis(kinesisRecord);
            records.add(record);
        }

        KinesisEvent kinesisEvent = new KinesisEvent();
        kinesisEvent.setRecords(records);
        return kinesisEvent;
    }
}
