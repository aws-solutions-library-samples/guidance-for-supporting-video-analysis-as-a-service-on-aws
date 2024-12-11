package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.RawVideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.VideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.RawVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.TimeIncrementUnits;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.VideoDensityLocation;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import javax.inject.Inject;

import java.util.List;
import java.util.Optional;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.NO_PARTITION_KEY_ERROR;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.NO_SORT_KEY_ERROR;
import static com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.TimeIncrementUnits.TIME_INCREMENT_UNITS_LIST;
import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.LOCATION;
import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.RAW_PARTITION_KEY;
import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.RAW_SORT_KEY;


public class TimelineForwarderLambda implements RequestHandler<DynamodbEvent, Void> {

    private static final Logger LOG = LogManager.getLogger(TimelineForwarderLambda.class);
    private static final String KINESIS_DATA_STREAM_NAME = "TimelineKDS";
    private final RawVideoTimelineDAO rawVideoTimelineDAO;
    private final VideoTimelineDAO videoTimelineDAO;
    private final VideoTimelineAggregator videoTimelineAggregator;
    private final KinesisClient kinesisClient;
    private final TimelineKDSMetadataSerDe timelineKDSMetadataSerDe;

    @Inject
    public TimelineForwarderLambda(
            final TimelineKDSMetadataSerDe timelineKDSMetadataSerDe,
            final KinesisClient kinesisClient,
            final RawVideoTimelineDAO rawVideoTimelineDAO,
            final VideoTimelineDAO videoTimelineDAO,
            final VideoTimelineAggregator videoTimelineAggregator) {
        this.timelineKDSMetadataSerDe = timelineKDSMetadataSerDe;
        this.kinesisClient = kinesisClient;
        this.rawVideoTimelineDAO = rawVideoTimelineDAO;
        this.videoTimelineDAO = videoTimelineDAO;
        this.videoTimelineAggregator = videoTimelineAggregator;
    }

    @Override
    public Void handleRequest(final DynamodbEvent event, final Context context) {
        final List<DynamodbEvent.DynamodbStreamRecord> ddbRecords = event.getRecords();
        for (DynamodbEvent.DynamodbStreamRecord record : ddbRecords) {
            final OperationType recordEventType = OperationType.fromValue(record.getEventName());
            if (OperationType.INSERT == recordEventType) {
                processTimeline(record, false);
            } else if (OperationType.MODIFY == recordEventType) {
                AttributeValue newLocation = record.getDynamodb().getNewImage().get(LOCATION);
                AttributeValue oldLocation = record.getDynamodb().getOldImage().get(LOCATION);
                if (newLocation != null && oldLocation != null) {
                    if (newLocation.getS().equals(VideoDensityLocation.CLOUD.name())
                            && oldLocation.getS().equals(VideoDensityLocation.DEVICE.name())) {
                        LOG.info(String.format("Location updated to %s from %s, starting catchup processing",
                                newLocation.getS(), oldLocation.getS()));
                        processTimeline(record, true);
                    } else {
                        LOG.info(String.format("Location updated to %s from %s, ignoring this request as no action " +
                                "needs to be taken in order to ensure idempotency", newLocation.getS(), oldLocation.getS()));
                    }
                }
            }
        }
        return null;
    }

    private Optional<String> getPKValueFromNewImage(final DynamodbEvent.DynamodbStreamRecord record,
                                                    final String attributeKey) {
        return Optional.ofNullable(record.getDynamodb())
                .map(StreamRecord::getNewImage)
                .map(attributes -> attributes.get(attributeKey))
                .map(AttributeValue::getS);
    }

    private Optional<Long> getSKValueFromNewImage(final DynamodbEvent.DynamodbStreamRecord record,
                                                  final String attributeKey) {
        return Optional.ofNullable(record.getDynamodb())
                .map(StreamRecord::getNewImage)
                .map(attributes -> attributes.get(attributeKey))
                .map(AttributeValue::getN)
                .map(Long::valueOf);
    }

    /**
     * Method processes incoming timeline information and forwards it to appropriate KDS streams to be aggregated
     * @param record DynamoDB Stream record
     * @param isCatchUp true/false
     */
    private void processTimeline(DynamodbEvent.DynamodbStreamRecord record, boolean isCatchUp) {
        LOG.info(String.format("Starting density update for all time increments for catch up = %s", isCatchUp));
        final Optional<String> rawPartitionKey = getPKValueFromNewImage(record, System.getenv(RAW_PARTITION_KEY));
        final Optional<Long> rawSortKey = getSKValueFromNewImage(record, System.getenv(RAW_SORT_KEY));
        if(rawPartitionKey.isEmpty()) {
            throw new RuntimeException(NO_PARTITION_KEY_ERROR);
        }
        if(rawSortKey.isEmpty()) {
            throw new RuntimeException(NO_SORT_KEY_ERROR);
        }
        LOG.info("Partition key from lambda: " + rawPartitionKey);
        RawVideoTimeline rawVideoTimeline = rawVideoTimelineDAO.load(rawPartitionKey.get(), rawSortKey.get());
        LOG.info("Forwarding to KDS for device ID " + rawVideoTimeline.getDeviceId());
        putTimelineToKDS(rawVideoTimeline, isCatchUp);
        LOG.info("Put information into KDS for processing...");
    }

    private void putTimelineToKDS(RawVideoTimeline rawVideoTimeline, boolean isCatchup) {

        for (TimeIncrementUnits timeIncrementUnits : TIME_INCREMENT_UNITS_LIST) {
            List<TimelineStorage> timelineStorageList = videoTimelineAggregator.getTimeBuckets(timeIncrementUnits,
                    rawVideoTimeline.getTimestamp(),
                    rawVideoTimeline.getDurationInMillis());
            for(TimelineStorage timelineStorage: timelineStorageList) {
                TimelineKDSMetadata timelineKDSMetadata = new TimelineKDSMetadata(
                        rawVideoTimeline.getDeviceId(), timeIncrementUnits, timelineStorage.getBucketStartDate(),
                        rawVideoTimeline.getTimestamp(), timelineStorage.getDurationToBeStored(),
                        rawVideoTimeline.getLocation(), isCatchup);
                PutRecordRequest request = PutRecordRequest.builder()
                        .partitionKey(timelineKDSMetadata.getKDSPartitionKey())
                        .streamName(KINESIS_DATA_STREAM_NAME)
                        .data(SdkBytes.fromUtf8String(timelineKDSMetadataSerDe.serialize(timelineKDSMetadata)))
                        .build();
                PutRecordResponse response = kinesisClient.putRecord(request);
                LOG.info("Completed to put timeline into shard {} with seqN {} for {} with KDS partition key {}",
                        response.shardId(), response.sequenceNumber(), timelineKDSMetadata, timelineKDSMetadata.getKDSPartitionKey());
            }
        }

    }
}
