package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.RawVideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.VideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.util.List;
import com.amazonaws.videoanalytics.videologistics.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;

public class VideoDensityUpdateLambda implements RequestHandler<KinesisEvent, Void> {

    private static final Logger LOG = LogManager.getLogger(VideoDensityUpdateLambda.class);
    private final RawVideoTimelineDAO rawVideoTimelineDAO;
    private final VideoTimelineDAO videoTimelineDAO;
    private final TimelineKDSMetadataSerDe timelineKDSMetadataSerDe;

    @Inject
    public VideoDensityUpdateLambda(RawVideoTimelineDAO rawVideoTimelineDAO, VideoTimelineDAO videoTimelineDAO, TimelineKDSMetadataSerDe timelineKDSMetadataSerDe) {
        this.rawVideoTimelineDAO = rawVideoTimelineDAO;
        this.videoTimelineDAO = videoTimelineDAO;
        this.timelineKDSMetadataSerDe = timelineKDSMetadataSerDe;
    }

    @ExcludeFromJacocoGeneratedReport
    public VideoDensityUpdateLambda() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.rawVideoTimelineDAO = component.getRawVideoTimelineDAO();
        this.videoTimelineDAO = component.getVideoTimelineDAO();
        this.timelineKDSMetadataSerDe = component.getTimelineKDSMetadataSerDe();
    }

    @Override
    public Void handleRequest(final KinesisEvent event, final Context context) {
        if (event == null) {
            throw new IllegalArgumentException(VideoAnalyticsExceptionMessage.INVALID_INPUT);
        }

        List<KinesisEvent.KinesisEventRecord> kinesisEventRecords = event.getRecords();
        LOG.info("Received {} timelines from KDS.", kinesisEventRecords.size());

        for (KinesisEvent.KinesisEventRecord kinesisEventRecord : kinesisEventRecords) {
            KinesisEvent.Record record = kinesisEventRecord.getKinesis();
            String kdsTimelineJson = new String(record.getData().array(), StandardCharsets.UTF_8);
            try {
                TimelineKDSMetadata timelineKDSMetadata = timelineKDSMetadataSerDe.deserialize(kdsTimelineJson);
                updateAggregatedTimeline(timelineKDSMetadata);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Failed to parse timeline for partition %s with SeqN %s",
                        record.getPartitionKey(), record.getSequenceNumber()), e);
            }
        }
        return null;
    }

    /**
     * Method updates all time increments for incoming video timestamp from KDS for a given time increment
     */
    private void updateAggregatedTimeline (TimelineKDSMetadata timelineKDSMetadata) {
        LOG.info(String.format("Starting density update for catch up = %s", timelineKDSMetadata.isCatchup()));

        LOG.info("Updating video density for {}", timelineKDSMetadata);

        videoTimelineDAO.save(timelineKDSMetadata);
    }



}
