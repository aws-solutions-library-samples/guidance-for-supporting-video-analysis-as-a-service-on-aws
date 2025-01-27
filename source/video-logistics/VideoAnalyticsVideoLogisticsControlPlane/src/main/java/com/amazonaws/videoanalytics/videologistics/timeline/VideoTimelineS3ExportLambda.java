package com.amazonaws.videoanalytics.videologistics.timeline;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.videoanalytics.videologistics.client.s3.S3Proxy;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.RawVideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage;
import com.amazonaws.videoanalytics.videologistics.utils.S3BucketRegionalizer;
import com.amazonaws.videoanalytics.videologistics.utils.annotations.ExcludeFromJacocoGeneratedReport;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

public class VideoTimelineS3ExportLambda implements RequestHandler<ScheduledEvent, Void> {

    private static final Logger LOG = LogManager.getLogger(VideoTimelineS3ExportLambda.class);

    private final RawVideoTimelineDAO rawVideoTimelineDAO;
    private final BatchTimelineMapper batchTimelineMapper;
    private final S3Proxy s3Proxy;
    private final S3BucketRegionalizer s3BucketRegionalizer;

    @ExcludeFromJacocoGeneratedReport
    public VideoTimelineS3ExportLambda() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.rawVideoTimelineDAO = component.getRawVideoTimelineDAO();
        this.batchTimelineMapper = component.getBatchTimelineMapper();
        this.s3Proxy = component.getS3Proxy();
        this.s3BucketRegionalizer = component.getS3BucketRegionalizer();
    }

    @Inject
    public VideoTimelineS3ExportLambda(RawVideoTimelineDAO rawVideoTimelineDAO, 
            BatchTimelineMapper batchTimelineMapper, 
            S3Proxy s3Proxy, 
            S3BucketRegionalizer s3BucketRegionalizer) {
        this.rawVideoTimelineDAO = rawVideoTimelineDAO;
        this.batchTimelineMapper = batchTimelineMapper;
        this.s3Proxy = s3Proxy;
        this.s3BucketRegionalizer = s3BucketRegionalizer;
    }

    public Void handleRequest(ScheduledEvent scheduledEvent, Context context) {
        if (scheduledEvent == null) {
            throw new RuntimeException(VideoAnalyticsExceptionMessage.INVALID_INPUT);
        }
        LOG.info("Start migration to S3 for event: {}", scheduledEvent);
        String bucketName = s3BucketRegionalizer.getRegionalizedBucketName("videoanalytics-timeline-bucket");
        String continuationToken = null;

        ListObjectsV2Response result;
        do {
            result = s3Proxy.listS3Objects(bucketName, continuationToken);
            // Potential Improvement: Add threading to parallelize these calls
            for (S3Object s3Object : result.contents()) {
                BatchTimeline batchTimeline;
                try {
                    ResponseBytes<GetObjectResponse> objectBytes = s3Proxy.getS3ObjectBytes(bucketName, s3Object.key());
                    // size of this array is limited by MQTT payload size limit = 128KB
                    batchTimeline = batchTimelineMapper.deserialize(objectBytes.asByteArray());
                    LOG.info(String.format("Attempting to save %d timelines.", batchTimeline.getTimestamps().size()));
                    for(TimestampInfo timestampInfo : batchTimeline.getTimestamps()) {
                        rawVideoTimelineDAO.save(
                            batchTimeline.getDeviceId(),
                            timestampInfo,
                            batchTimeline.getLocation()
                        );
                    }
                } catch (IOException e) {
                    LOG.info("Could not deserialize timestamp information, ignoring json...");
                } catch (Exception e) {
                    throw new RuntimeException("Error while exporting timestamp information", e);
                }
                // Delete the object once all the timestamps have been read into DynamoDB/timestamp serialization fails
                s3Proxy.deleteObject(bucketName, s3Object.key());
            }
            continuationToken = result.nextContinuationToken();
        } while(result.isTruncated());

        LOG.info("Completed migration from S3 for event: {}", scheduledEvent);

        return null;
    }
}
