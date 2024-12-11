package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.client.s3.S3Proxy;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.RawVideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.utils.S3BucketRegionalizer;
import com.amazonaws.videoanalytics.videologistics.schema.SchemaConst;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

public class PutVideoTimelineHandler {

    private static final Logger LOG = LogManager.getLogger(PutVideoTimelineHandler.class);
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
        LOG.info(String.format("Starting to write timeline information for and deviceId=%s",
                deviceId));

        List<TimestampInfo> timestampList = timestampListDeserializer.deserialize(timestamps);

        if(!timestampList.isEmpty()) {
            if (timestampList.size() == 1) {
                rawVideoTimelineDAO.save(deviceId, timestampList.get(0), location);
            } else {
                LOG.info("Writing timeline information to s3...");

                String bucketName = s3BucketRegionalizer.getRegionalizedBucketName(SchemaConst.VIDEO_TIMELINE_TABLE_NAME);

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
                LOG.info("Finished writing timeline information to s3...");
            }
        } else {
            LOG.info("No timelines found, ignoring this request...");
        }
    }

}
