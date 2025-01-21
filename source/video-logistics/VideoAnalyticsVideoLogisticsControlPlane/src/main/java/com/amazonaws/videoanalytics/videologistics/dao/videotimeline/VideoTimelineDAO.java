package com.amazonaws.videoanalytics.videologistics.dao.videotimeline;


import com.amazonaws.videoanalytics.videologistics.VideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.AggregateVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.PaginatedListResponse;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.TimeIncrementUnits;
import com.amazonaws.videoanalytics.videologistics.timeline.TimelineKDSMetadata;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineAggregator;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineUtils;
import com.amazonaws.videoanalytics.videologistics.utils.GsonDDBNextTokenMarshaller;
import com.amazonaws.videoanalytics.videologistics.schema.SchemaConst;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.common.base.Strings;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

import static com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineListGenerator.buildVideoTimelineList;
import static com.amazonaws.services.lambda.runtime.LambdaRuntime.getLogger;

public class VideoTimelineDAO {
    private final LambdaLogger logger = getLogger();
    private final DynamoDbEnhancedClient ddbClient;
    private final DynamoDbTable<AggregateVideoTimeline> videoTimelineTable;
    private final VideoTimelineUtils videoTimelineUtils;
    private final VideoTimelineAggregator videoTimelineAggregator;

    @Inject
    public VideoTimelineDAO(final DynamoDbEnhancedClient ddbClient, final DynamoDbTable<AggregateVideoTimeline> videoTimelineTable,
            final VideoTimelineUtils videoTimelineUtils, final VideoTimelineAggregator videoTimelineAggregator) {
        this.ddbClient = ddbClient;
        this.videoTimelineTable = videoTimelineTable;
        this.videoTimelineUtils = videoTimelineUtils;
        this.videoTimelineAggregator = videoTimelineAggregator;
    }

    /**
     * Method creates/updates density for given aggregated timeline data
     */
    public void save(TimelineKDSMetadata timelineKDSMetadata) {
        logger.log("Starting transaction write for video timeline");

        String partitionKey = videoTimelineUtils.generateTimelinePartitionKey(
                timelineKDSMetadata.getDeviceId(),
                timelineKDSMetadata.getTimeIncrementUnits()
        );

        // get old aggregate timeline to be updated, null if no timeline exists
        AggregateVideoTimeline oldAggregateVideoTimeline = load(partitionKey, timelineKDSMetadata.getBucketStartTime());

        AggregateVideoTimeline newAggregateVideoTimeline = videoTimelineAggregator.getUpdatedVideoTimeline(
                timelineKDSMetadata.getTimeIncrementUnits(),
                timelineKDSMetadata.getBucketStartTime(),
                timelineKDSMetadata.getDurationInMillis(),
                partitionKey,
                oldAggregateVideoTimeline,
                timelineKDSMetadata.getVideoDensityLocation(),
                timelineKDSMetadata.isCatchup()
        );

        PutItemEnhancedRequest<AggregateVideoTimeline> request = PutItemEnhancedRequest.builder(AggregateVideoTimeline.class)
                .item(newAggregateVideoTimeline)
                .build();

        videoTimelineTable.putItem(request);
    }

    public AggregateVideoTimeline load(final String partitionKey, final Long bucketStartTime) {
        logger.log(String.format("Loading forwarding rule for PK=%s, SK=%s", partitionKey, bucketStartTime));
        return this.videoTimelineTable.getItem(Key.builder()
                .partitionValue(partitionKey)
                .sortValue(bucketStartTime)
                .build());
    }

    public PaginatedListResponse<VideoTimeline> listVideoTimelines(final String deviceId,
                                                                   final Date startTime,
                                                                   final Date endTime,
                                                                   final Long timeIncrementInMillis,
                                                                   final TimeIncrementUnits timeIncrementUnits,
                                                                   final String nextToken) {
        logger.log("Starting to list video timeline items.");
        String partitionKey = videoTimelineUtils.generateTimelinePartitionKey(
                deviceId,
                timeIncrementUnits
                );
        List<VideoTimeline> finalVideoTimelineList = new ArrayList<>();
        Map<String, AttributeValue> exclusiveStartKey = null;

        Long startTimeInMillis = startTime.getTime();
        Long endTimeInMillis = endTime.getTime();
        Long startPeriod = startTimeInMillis;
        if (!Strings.isNullOrEmpty(nextToken)) {
            exclusiveStartKey = GsonDDBNextTokenMarshaller.unmarshall(nextToken);
            Long lastEvaluatedTimestamp = Long.parseLong(exclusiveStartKey.get(SchemaConst.UNIT_TIMESTAMP).n());
            // adjust start period to the next period after the last evaluated timestamp to return accurate results
            startPeriod = videoTimelineUtils.getNextPeriod(startTimeInMillis, timeIncrementInMillis, lastEvaluatedTimestamp);
            logger.log(String.format("Found next token, last evaluated timestamp = %d, " +
                    "new timeline information will be returned from %d", lastEvaluatedTimestamp, startPeriod));
        }

        // If end time provided by user is 1500 hours, for time increment hour,
        // the final time we should get must up t0 1400 hours as BETWEEN includes the end time
        Long endTimeForQuery = endTimeInMillis - videoTimelineUtils.getBucketDuration(timeIncrementUnits.getChronoUnit());

        logger.log(String.format("Start time of query = %d, " + "end time of query = %d", startPeriod, endTimeForQuery));

        // condition for first call to DDB
        QueryConditional condition = QueryConditional.sortBetween(
                t -> t.partitionValue(partitionKey).sortValue(startTimeInMillis),
                t -> t.partitionValue(partitionKey).sortValue(endTimeForQuery));

        // paginated query list
        PageIterable<AggregateVideoTimeline> results = this.videoTimelineTable.query(QueryEnhancedRequest.builder()
                .queryConditional(condition)
                .exclusiveStartKey(exclusiveStartKey)
                .consistentRead(true)
                .build());

        Page<AggregateVideoTimeline> firstPage = results.stream().findFirst().get();
        List<AggregateVideoTimeline> aggregateVideoTimelineList = firstPage.items();
        int firstPageCount = firstPage.items().size();
        Map<String, AttributeValue> lastEvaluatedKey = firstPage.lastEvaluatedKey();

        if (firstPageCount > 0) {
            // collect items of the query list in a map with key as the unit timestamp
            // this will help us update density for existing time units for a given increment
            Long lastTimestampInList = aggregateVideoTimelineList.get(firstPageCount - 1).getUnitTimestamp();
            // get the end timestamp of the last period for the last timestamp to avoid returning trailing 0s as there
            // might be more data for next periods
            Long endPeriod = videoTimelineUtils.getNextPeriod(startTimeInMillis, timeIncrementInMillis, lastTimestampInList);

            // check if last timestamp is the end of a period
            boolean isLastTimestampMultiple = (lastTimestampInList - startTimeInMillis
                    + videoTimelineUtils.getBucketDuration(timeIncrementUnits.getChronoUnit())) % timeIncrementInMillis == 0;

            // if we get to the last timestamp, and it is not a multiple of time increment, get the next
            // remainder values to make it a multiple of the time increment, if they exist
            if (lastEvaluatedKey != null && !isLastTimestampMultiple) {
                logger.log("Last timestamp was not a multiple of increment, and more timeline" +
                        " information for last period might exist, fetching more items...");

                // get next x results till the start of next period or final end time, whichever is smaller
                // we use endTimeInMillis as LT does not include the time provided
                long nextMultipleTimestamp = Math.min(
                        videoTimelineUtils.getNextPeriod(startTimeInMillis, timeIncrementInMillis, lastTimestampInList),
                        endTimeInMillis
                );

                logger.log(String.format("Fetching information for time units less than %d", nextMultipleTimestamp));

                condition = QueryConditional.sortLessThan(
                        t -> t.partitionValue(partitionKey).sortValue(nextMultipleTimestamp));

                // Potential Improvement: Add filter for KVS TTL to make sure we are not returning deleted information
                results = this.videoTimelineTable.query(QueryEnhancedRequest.builder()
                        .queryConditional(condition)
                        .exclusiveStartKey(lastEvaluatedKey)
                        .consistentRead(true)
                        .build());

                Page<AggregateVideoTimeline> secondPage = results.stream().findFirst().get();
                int secondPageCount = secondPage.items().size();

                if (secondPageCount > 0) {
                    logger.log("Found " + secondPageCount + " more timelines for the last period in this page.");
                    aggregateVideoTimelineList.addAll(secondPage.items());
                    AggregateVideoTimeline lastEvaluatedTimeline = secondPage.items().get(secondPageCount - 1);

                    // if there is spillover, and we find more items, we will always return a last evaluated key
                    // this means that the next page can be potentially empty
                    lastEvaluatedKey = new HashMap<>();
                    lastEvaluatedKey.put(SchemaConst.VIDEO_TIMELINE_PARTITION_KEY,
                            AttributeValue.fromS(lastEvaluatedTimeline.getDeviceIdTimeUnit()));
                    lastEvaluatedKey.put(SchemaConst.UNIT_TIMESTAMP,
                            AttributeValue.fromN(lastEvaluatedTimeline.getUnitTimestamp().toString()));

                    // adjust endPeriod to correspond to the period containing the last timestamp of this list
                    endPeriod = videoTimelineUtils.getNextPeriod(startTimeInMillis, timeIncrementInMillis,
                            aggregateVideoTimelineList.get(aggregateVideoTimelineList.size() - 1).getUnitTimestamp());
                }
            }

            finalVideoTimelineList = buildVideoTimelineList(aggregateVideoTimelineList, startPeriod,
                    endPeriod, timeIncrementInMillis);
        }

        logger.log("Returning " + finalVideoTimelineList.size() + " video timeline items.");
        return new PaginatedListResponse<>(finalVideoTimelineList, GsonDDBNextTokenMarshaller.marshall(lastEvaluatedKey)
        );
    }
}
