package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.client.s3.S3Proxy;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.RawVideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.utils.S3BucketRegionalizer;
import com.amazonaws.videoanalytics.videologistics.schema.SchemaConst;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import static com.amazonaws.services.lambda.runtime.LambdaRuntime.getLogger;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

public class PutVideoTimelineHandler {

    private final LambdaLogger logger = getLogger();
    private final TimestampListDeserializer timestampListDeserializer;
    private final S3Proxy s3Proxy;
    private final BatchTimelineMapper batchTimelineMapper;
    private final S3BucketRegionalizer s3BucketRegionalizer;
    private final VideoTimelineUtils videoTimelineUtils;
    private final RawVideoTimelineDAO rawVideoTimelineDAO;

    @Inject
    PutVideoTimelineHandler(final TimestampListDeserializer timestampListDeserializer,
                            final S3Proxy s3Proxy,
                            final BatchTimelineMapper batchTimelineMapper,
                            final S3BucketRegionalizer s3BucketRegionalizer,
                            final VideoTimelineUtils videoTimelineUtils,
                            final RawVideoTimelineDAO rawVideoTimelineDAO){
        this.timestampListDeserializer = timestampListDeserializer;
        this.s3Proxy = s3Proxy;
        this.batchTimelineMapper = batchTimelineMapper;
        this.s3BucketRegionalizer = s3BucketRegionalizer;
        this.videoTimelineUtils = videoTimelineUtils;
        this.rawVideoTimelineDAO = rawVideoTimelineDAO;
    }

    public void addVideoTimelines(String deviceId, String timestamps, String location) {
        logger.log(String.format("Starting to write timeline information for deviceId=%s", deviceId));
        logger.log(String.format("Received timestamps: %s", timestamps));
        logger.log(String.format("Received location: %s", location));

        try {
            List<TimestampInfo> timestampList = timestampListDeserializer.deserialize(timestamps);
            logger.log(String.format("Deserialized timestamp list size: %d", timestampList.size()));

            if(!timestampList.isEmpty()) {
                if (timestampList.size() == 1) {
                    logger.log(String.format("Processing single timestamp for deviceId=%s", deviceId));
                    rawVideoTimelineDAO.save(deviceId, timestampList.get(0), location);
                } else {
                    logger.log(String.format("Writing %d timestamps to S3...", timestampList.size()));
                    String bucketName = s3BucketRegionalizer.getRegionalizedBucketName("videoanalytics-timeline-bucket");

                    BatchTimeline batchTimeline = BatchTimeline.builder()
                            .deviceId(deviceId)
                            .timestamps(timestampList)
                            .location(location)
                            .build();

                    s3Proxy.putObjectBytes(
                            bucketName,
                            videoTimelineUtils.generateS3Key(deviceId, UUID.randomUUID().toString()),
                            batchTimelineMapper.serialize(batchTimeline)
                    );
                    logger.log("Finished writing timeline information to s3...");
                }
            } else {
                logger.log("No timelines found, ignoring this request...");
            }
        } catch (Exception e) {
            logger.log(String.format("Error processing timestamps: %s", timestamps));
            logger.log(String.format("Error: %s", e.getMessage()));
            throw e;
        }
    }

}
