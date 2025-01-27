package com.amazonaws.videoanalytics.videologistics.dao.videotimeline;

import com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.PaginatedListResponse;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.RawVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.VideoDensityLocation;
import com.amazonaws.videoanalytics.videologistics.timeline.TimestampInfo;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineUtils;
import com.amazonaws.videoanalytics.videologistics.utils.GsonDDBNextTokenMarshaller;
import com.amazonaws.videoanalytics.videologistics.schema.SchemaConst;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.common.base.Strings;

import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import javax.inject.Inject;

import java.util.Date;
import java.util.Map;

import static com.amazonaws.services.lambda.runtime.LambdaRuntime.getLogger;

public class RawVideoTimelineDAO {
    // Hardcoded placeholder for TTL duration for video on KVS in seconds = 3 months in seconds
    public static final Long KVS_TTL_DURATION = 3L * 30 * 24 * 60 * 60;
    private static final String RAW_VIDEO_TYPE = "RAW";
    private final LambdaLogger logger = getLogger();
    private final DynamoDbTable<RawVideoTimeline> videoTimelineTable;
    private final VideoTimelineUtils videoTimelineUtils;

    @Inject
    public RawVideoTimelineDAO(final DynamoDbTable<RawVideoTimeline> videoTimelineTable, final VideoTimelineUtils videoTimelineUtils) {
        this.videoTimelineTable = videoTimelineTable;
        this.videoTimelineUtils = videoTimelineUtils;
    }

    public void save(final String deviceId, final TimestampInfo timestampInfo, final String location) {
        logger.log(String.format("Starting save for raw timeline for deviceId=%s, timestamp=%d, location=%s",
                deviceId, timestampInfo.getTimestamp(), location));
        logger.log(String.format("Timestamp duration: %d ms", timestampInfo.getDuration()));

        Long timestampToBeStored = timestampInfo.getTimestamp();
        Date currentTime = new Date();
        
        String partitionKey = videoTimelineUtils.generateRawPartitionKey(deviceId);
        
        logger.log(String.format("Creating RawVideoTimeline with timestamp=%d, currentTime=%s", 
                timestampToBeStored, currentTime));

        RawVideoTimeline rawTimeline = RawVideoTimeline.builder()
                .timestamp(timestampToBeStored)
                .deviceId(partitionKey)
                .createdAt(currentTime)
                .lastUpdated(currentTime)
                .expirationTimestamp(timestampToBeStored / 1000L + KVS_TTL_DURATION)
                .durationInMillis(timestampInfo.getDuration())
                .location(VideoDensityLocation.valueOf(location))
                .build();
                
        logger.log(String.format("Built RawVideoTimeline object: %s", rawTimeline.toString()));

        if (VideoDensityLocation.DEVICE.equals(rawTimeline.getLocation())) {
            try {
                logger.log("Location is DEVICE, checking for existing raw video timeline on CLOUD...");
                this.videoTimelineTable.putItem(PutItemEnhancedRequest.builder(RawVideoTimeline.class)
                        .item(rawTimeline)
                        .conditionExpression(createSaveExpressionForDeviceStorage())
                        .build());
                logger.log("Successfully saved RawVideoTimeline for DEVICE location");
            } catch (ConditionalCheckFailedException e){
                logger.log(String.format("Conditional check failed: There already exists video for timestamp=%d on CLOUD. " +
                        "Ignoring this save request.", timestampToBeStored));
            }
        } else {
            logger.log(String.format("Saving RawVideoTimeline with location=%s", rawTimeline.getLocation()));
            this.videoTimelineTable.putItem(rawTimeline);
            logger.log("Successfully saved RawVideoTimeline");
        }
    }

    /**
     * Method to return list of RawVideoTimelines
     * @param deviceId device id
     * @param startTimeInMillis start time of list
     * @param endTimeInMillis end time of list
     * @param nextToken next token
     * @return list of rawVideoTimeline
     */
    public PaginatedListResponse<RawVideoTimeline> listRawVideoTimelines(final String deviceId,
                                                                         final Long startTimeInMillis,
                                                                         final Long endTimeInMillis,
                                                                         final String nextToken) {
        logger.log(String.format("Starting list for raw video timelines for deviceId=%s, " +
                "startTime=%s, endTime=%s", deviceId, startTimeInMillis, endTimeInMillis));

       String partitionKey = videoTimelineUtils.generateRawPartitionKey(deviceId);

        QueryConditional condition = QueryConditional.sortBetween(
                item -> item.partitionValue(partitionKey).sortValue(startTimeInMillis),
                item -> item.partitionValue(partitionKey).sortValue(endTimeInMillis));

        Map<String, AttributeValue> exclusiveStartKey = null;
        if(!Strings.isNullOrEmpty(nextToken)) {
            exclusiveStartKey = GsonDDBNextTokenMarshaller.unmarshall(nextToken);
        }

        SdkIterable<Page<RawVideoTimeline>> results = videoTimelineTable.query(QueryEnhancedRequest.builder()
                .queryConditional(condition)
                .exclusiveStartKey(exclusiveStartKey)
                .consistentRead(false)
                .build());

        Page<RawVideoTimeline> firstPage = results.stream().findFirst().get();

        return new PaginatedListResponse<>(firstPage.items(),
                GsonDDBNextTokenMarshaller.marshall(firstPage.lastEvaluatedKey()));
    }

    /**
     * Method generates conditional expression that checks to make sure that:
     * 1. Raw Video Timeline item does not exist, OR
     * 2. Location of existing item is not CLOUD (ie, if video is already on cloud, density cannot be updated for device)
     * @return Conditional Expression
     */
    private static Expression createSaveExpressionForDeviceStorage() {
        final Expression videoTimelineNotExists = Expression.builder()
                .expression("attribute_not_exists(#pk)")
                .putExpressionName("#pk", SchemaConst.DEVICE_ID)
                .build();
        final Expression locationNotCloud = Expression.builder()
                .expression("#location = :location")
                .putExpressionName("#location", SchemaConst.LOCATION)
                .putExpressionValue(":location", AttributeValues.stringValue(VideoDensityLocation.DEVICE.name()))
                .build();
        return Expression.join(videoTimelineNotExists, locationNotCloud, "OR");
    }

    public RawVideoTimeline load(String rawPartitionKey, final Long timestamp) {
        logger.log("Loading the DDB with partitionKey " +  rawPartitionKey + " and sort key value = " + timestamp);
        return this.videoTimelineTable.getItem(Key.builder()
                .partitionValue(rawPartitionKey)
                .sortValue(timestamp)
                .build());
    }
}