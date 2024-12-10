package com.amazonaws.videoanalytics.videologistics.dao.videotimeline;

import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.AggregateVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.TimeIncrementUnits;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.VideoDensityLocation;
import com.amazonaws.videoanalytics.videologistics.timeline.TimelineKDSMetadata;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineAggregator;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VideoTimelineDAOTest {
    @Mock
    private DynamoDbEnhancedClient ddbClient;
    
    @Mock
    private DynamoDbTable<AggregateVideoTimeline> videoTimelineTable;
    
    @Mock
    private VideoTimelineUtils videoTimelineUtils;
    
    @Mock
    private VideoTimelineAggregator videoTimelineAggregator;

    private VideoTimelineDAO videoTimelineDAO;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        videoTimelineDAO = new VideoTimelineDAO(ddbClient, videoTimelineTable, videoTimelineUtils, videoTimelineAggregator);
    }

    @Test
    public void save_newTimelineData_savesToDynamoDB() {
        String deviceId = "device123";
        TimeIncrementUnits timeIncrementUnits = TimeIncrementUnits.HOURS;
        String partitionKey = "device123#HOURS";
        Long bucketStartTime = 1696442400000L;
        Long durationInMillis = 3600000L;

        TimelineKDSMetadata metadata = new TimelineKDSMetadata(
                deviceId,
                timeIncrementUnits,
                bucketStartTime,
                bucketStartTime,
                durationInMillis,
                VideoDensityLocation.CLOUD,
                false
        );

        when(videoTimelineUtils.generateTimelinePartitionKey(deviceId, timeIncrementUnits))
                .thenReturn(partitionKey);

        AggregateVideoTimeline newTimeline = AggregateVideoTimeline.builder()
                .deviceIdTimeUnit(partitionKey)
                .timeIncrementUnits(timeIncrementUnits)
                .cloudDensityInMillis(durationInMillis)
                .deviceDensityInMillis(0L)
                .unitTimestamp(bucketStartTime)
                .createdAt(new Date())
                .lastUpdated(new Date())
                .build();

        when(videoTimelineAggregator.getUpdatedVideoTimeline(
                timeIncrementUnits,
                bucketStartTime,
                durationInMillis,
                partitionKey,
                null,
                VideoDensityLocation.CLOUD,
                false
        )).thenReturn(newTimeline);

        videoTimelineDAO.save(metadata);

        verify(videoTimelineTable).putItem(any(PutItemEnhancedRequest.class));
    }

    @Test
    public void load_existingTimeline_retrievesFromDynamoDB() {
        String partitionKey = "device123#HOURS";
        Long bucketStartTime = 1696442400000L;

        videoTimelineDAO.load(partitionKey, bucketStartTime);

        verify(videoTimelineTable).getItem(Key.builder()
                .partitionValue(partitionKey)
                .sortValue(bucketStartTime)
                .build());
    }

    @Test
    public void save_existingTimelineUpdate_updatesInDynamoDB() {
        String deviceId = "device123";
        TimeIncrementUnits timeIncrementUnits = TimeIncrementUnits.HOURS;
        String partitionKey = "device123#HOURS";
        Long bucketStartTime = 1696442400000L;
        Long durationInMillis = 3600000L;

        TimelineKDSMetadata metadata = new TimelineKDSMetadata(
                deviceId,
                timeIncrementUnits,
                bucketStartTime,
                bucketStartTime,
                durationInMillis,
                VideoDensityLocation.DEVICE,
                false
        );

        when(videoTimelineUtils.generateTimelinePartitionKey(deviceId, timeIncrementUnits))
                .thenReturn(partitionKey);

        AggregateVideoTimeline existingTimeline = AggregateVideoTimeline.builder()
                .deviceIdTimeUnit(partitionKey)
                .timeIncrementUnits(timeIncrementUnits)
                .cloudDensityInMillis(0L)
                .deviceDensityInMillis(1800000L)
                .unitTimestamp(bucketStartTime)
                .createdAt(new Date())
                .lastUpdated(new Date())
                .build();

        when(videoTimelineTable.getItem(any(Key.class))).thenReturn(existingTimeline);

        AggregateVideoTimeline updatedTimeline = AggregateVideoTimeline.builder()
                .deviceIdTimeUnit(partitionKey)
                .timeIncrementUnits(timeIncrementUnits)
                .cloudDensityInMillis(0L)
                .deviceDensityInMillis(5400000L)
                .unitTimestamp(bucketStartTime)
                .createdAt(existingTimeline.getCreatedAt())
                .lastUpdated(new Date())
                .build();

        when(videoTimelineAggregator.getUpdatedVideoTimeline(
                timeIncrementUnits,
                bucketStartTime,
                durationInMillis,
                partitionKey,
                existingTimeline,
                VideoDensityLocation.DEVICE,
                false
        )).thenReturn(updatedTimeline);

        videoTimelineDAO.save(metadata);

        verify(videoTimelineTable).putItem(any(PutItemEnhancedRequest.class));
    }
}