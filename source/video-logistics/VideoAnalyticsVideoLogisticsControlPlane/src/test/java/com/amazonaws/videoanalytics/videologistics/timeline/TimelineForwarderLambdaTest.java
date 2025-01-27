package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.RawVideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.VideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.RawVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.VideoDensityLocation;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.LOCATION;
import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.RAW_PARTITION_KEY;
import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.RAW_SORT_KEY;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TimelineForwarderLambdaTest {
    private DynamodbEvent ddbEvent;
    private DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord;
    private static final String DEVICE_ID = "device123";
    private static final String RAW_PARTITION_KEY_VALUE = "account123#device123";
    private static final Long RAW_SORT_KEY_VALUE = 1696444408000L;
    private static final Long DURATION_MS = 60000L;
    private static final String TIMELINE_KDS_METADATA = "{\"metadata\":\"value\"}";

    @Mock
    private RawVideoTimelineDAO rawVideoTimelineDAO;
    @Mock
    private VideoTimelineDAO videoTimelineDAO;
    @Mock
    private Context context;
    @Mock
    private VideoTimelineAggregator videoTimelineAggregator;
    @Mock
    private KinesisClient kinesisClient;
    @Mock
    private TimelineKDSMetadataSerDe timelineKDSMetadataSerDe;

    @Captor
    private ArgumentCaptor<PutRecordRequest> putRecordRequestCaptor;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @InjectMocks
    private TimelineForwarderLambda timelineForwarderLambda;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        environmentVariables.set(RAW_PARTITION_KEY, "partitionKey");
        environmentVariables.set(RAW_SORT_KEY, "sortKey");
        ddbEvent = new DynamodbEvent();

        RawVideoTimeline rawVideoTimeline = RawVideoTimeline.builder()
                .deviceId(DEVICE_ID)
                .timestamp(RAW_SORT_KEY_VALUE)
                .location(VideoDensityLocation.CLOUD)
                .durationInMillis(DURATION_MS)
                .build();

        when(rawVideoTimelineDAO.load(RAW_PARTITION_KEY_VALUE, RAW_SORT_KEY_VALUE))
                .thenReturn(rawVideoTimeline);
    }

    @Test
    public void handleRequest_insertEvent_processesTimeline() {
        TimelineStorage timelineStorage = TimelineStorage.builder()
                .bucketStartDate(1234567890L)
                .durationToBeStored(4000L)
                .build();

        setupStreamRecord(OperationType.INSERT);
        setupKinesisResponse();
        when(videoTimelineAggregator.getTimeBuckets(any(), anyLong(), anyLong()))
                .thenReturn(List.of(timelineStorage));
        when(timelineKDSMetadataSerDe.serialize(any())).thenReturn(TIMELINE_KDS_METADATA);

        timelineForwarderLambda.handleRequest(ddbEvent, context);

        verify(rawVideoTimelineDAO, times(1)).load(RAW_PARTITION_KEY_VALUE, RAW_SORT_KEY_VALUE);
        verify(kinesisClient, times(4)).putRecord(any(PutRecordRequest.class));
    }

    @Test
    public void handleRequest_modifyEventCloudToDevice_processesTimeline() {
        TimelineStorage timelineStorage = TimelineStorage.builder()
                .bucketStartDate(RAW_SORT_KEY_VALUE)
                .durationToBeStored(4000L)
                .build();

        setupModifyStreamRecord(VideoDensityLocation.CLOUD.name(), VideoDensityLocation.DEVICE.name());
        setupKinesisResponse();
        when(videoTimelineAggregator.getTimeBuckets(any(), anyLong(), anyLong()))
                .thenReturn(List.of(timelineStorage));
        when(timelineKDSMetadataSerDe.serialize(any())).thenReturn(TIMELINE_KDS_METADATA);

        timelineForwarderLambda.handleRequest(ddbEvent, context);

        verify(rawVideoTimelineDAO, times(1)).load(RAW_PARTITION_KEY_VALUE, RAW_SORT_KEY_VALUE);
        verify(kinesisClient, times(4)).putRecord(any(PutRecordRequest.class));
    }

    @Test
    public void handleRequest_modifyEventSameLocation_skipsProcessing() {
        setupModifyStreamRecord(VideoDensityLocation.CLOUD.name(), VideoDensityLocation.CLOUD.name());
        ddbEvent.setRecords(Collections.singletonList(dynamodbStreamRecord));

        timelineForwarderLambda.handleRequest(ddbEvent, context);

        verify(rawVideoTimelineDAO, times(0)).load(any(), any());
        verify(kinesisClient, times(0)).putRecord(any(PutRecordRequest.class));
    }

    @Test
    public void handleRequest_noPartitionKey_throwsException() {
        environmentVariables.set(RAW_PARTITION_KEY, "");
        setupStreamRecord(OperationType.INSERT);

        assertThrows(RuntimeException.class, () -> 
            timelineForwarderLambda.handleRequest(ddbEvent, context));
    }

    private void setupStreamRecord(OperationType operationType) {
        Map<String, AttributeValue> newImage = Map.of(
            "partitionKey", new AttributeValue().withS(RAW_PARTITION_KEY_VALUE),
            "sortKey", new AttributeValue().withN(RAW_SORT_KEY_VALUE.toString())
        );

        StreamRecord streamRecord = new StreamRecord();
        streamRecord.setNewImage(newImage);

        dynamodbStreamRecord = new DynamodbEvent.DynamodbStreamRecord();
        dynamodbStreamRecord.setEventName(operationType.toString());
        dynamodbStreamRecord.setDynamodb(streamRecord);

        ddbEvent.setRecords(Collections.singletonList(dynamodbStreamRecord));
    }

    private void setupModifyStreamRecord(String newLocation, String oldLocation) {
        Map<String, AttributeValue> newImage = Map.of(
            "partitionKey", new AttributeValue().withS(RAW_PARTITION_KEY_VALUE),
            "sortKey", new AttributeValue().withN(RAW_SORT_KEY_VALUE.toString()),
            LOCATION, new AttributeValue().withS(newLocation)
        );

        Map<String, AttributeValue> oldImage = Map.of(
            "partitionKey", new AttributeValue().withS(RAW_PARTITION_KEY_VALUE),
            "sortKey", new AttributeValue().withN(RAW_SORT_KEY_VALUE.toString()),
            LOCATION, new AttributeValue().withS(oldLocation)
        );

        StreamRecord streamRecord = new StreamRecord();
        streamRecord.setNewImage(newImage);
        streamRecord.setOldImage(oldImage);

        dynamodbStreamRecord = new DynamodbEvent.DynamodbStreamRecord();
        dynamodbStreamRecord.setEventName(OperationType.MODIFY.toString());
        dynamodbStreamRecord.setDynamodb(streamRecord);

        ddbEvent.setRecords(Collections.singletonList(dynamodbStreamRecord));
    }

    private void setupKinesisResponse() {
        PutRecordResponse response = PutRecordResponse.builder()
                .shardId("shard123")
                .sequenceNumber("seq1")
                .build();
        when(kinesisClient.putRecord(any(PutRecordRequest.class))).thenReturn(response);
    }
}
