package com.amazonaws.videoanalytics.videologistics.workflow;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.amazonaws.videoanalytics.videologistics.dao.VLRegisterDeviceJobDAO;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient;
import software.amazon.awssdk.services.kinesisvideo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.DATA_RETENTION_TIME_PERIOD_IN_HOURS;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.LIVE_STREAM_SIGNALING_CHANNEL;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PLAYBACK_SIGNALING_CHANNEL;
import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.JOB_ID;

public class KVSResourceCreateLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final Logger LOG = LoggerFactory.getLogger(KVSResourceCreateLambda.class);
    private final VLRegisterDeviceJobDAO vlRegisterDeviceJobDAO;
    private final KinesisVideoClient kinesisVideoClient;

    @Inject
    public KVSResourceCreateLambda(VLRegisterDeviceJobDAO vlRegisterDeviceJobDAO, KinesisVideoClient kinesisVideoClient) {
        this.vlRegisterDeviceJobDAO = vlRegisterDeviceJobDAO;
        this.kinesisVideoClient = kinesisVideoClient;
    }

    @ExcludeFromJacocoGeneratedReport
    public KVSResourceCreateLambda() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.vlRegisterDeviceJobDAO = component.getVLRegisterDeviceJobDAO();
        this.kinesisVideoClient = component.getKinesisVideoClient();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        String jobId = (String) input.get(JOB_ID);
        VLRegisterDeviceJob vlRegisterDeviceJob = vlRegisterDeviceJobDAO.load(jobId);
        String deviceId = vlRegisterDeviceJob.getDeviceId();
        LOG.info("JobId received from DDB as {} deviceId is {}", jobId, deviceId);

        try {
            // Create playback signaling channel
            String playbackChannelArn = createSignalingChannel(
                String.format(PLAYBACK_SIGNALING_CHANNEL, deviceId));
            vlRegisterDeviceJob.setPlaybackSignalingChannelArn(playbackChannelArn);

            // Create livestream signaling channel
            String livestreamChannelArn = createSignalingChannel(
                String.format(LIVE_STREAM_SIGNALING_CHANNEL, deviceId));
            vlRegisterDeviceJob.setLiveStreamSignalingChannelArn(livestreamChannelArn);

            // Create video stream
            String streamArn = createVideoStream(deviceId);
            vlRegisterDeviceJob.setKvsStreamArn(streamArn);
            vlRegisterDeviceJob.setStatus("COMPLETED");

            vlRegisterDeviceJobDAO.save(vlRegisterDeviceJob);
            LOG.info("Successfully created KVS resources for device {}", deviceId);
            
            return new HashMap<>();
        } catch (Exception e) {
            LOG.error("Failed to create KVS resources for device {}", deviceId, e);
            vlRegisterDeviceJob.setStatus("FAILED");
            vlRegisterDeviceJob.setErrorMessage(e.getMessage());
            vlRegisterDeviceJobDAO.save(vlRegisterDeviceJob);
            throw e;
        }
    }

    private String createVideoStream(String streamName) {
        try {
            CreateStreamRequest streamRequest = CreateStreamRequest.builder()
                    .streamName(streamName)
                    .dataRetentionInHours(DATA_RETENTION_TIME_PERIOD_IN_HOURS)
                    .build();
            CreateStreamResponse stream = kinesisVideoClient.createStream(streamRequest);
            return stream.streamARN();
        } catch (ResourceInUseException e) {
            LOG.info("Stream {} already exists, retrieving existing ARN", streamName);
            DescribeStreamRequest describeStreamRequest = DescribeStreamRequest.builder()
                    .streamName(streamName)
                    .build();
            DescribeStreamResponse describeStreamResponse = kinesisVideoClient.describeStream(describeStreamRequest);
            return describeStreamResponse.streamInfo().streamARN();
        }
    }

    private String createSignalingChannel(String channelName) {
        try {
            CreateSignalingChannelRequest request = CreateSignalingChannelRequest.builder()
                    .channelName(channelName)
                    .build();
            CreateSignalingChannelResponse response = kinesisVideoClient.createSignalingChannel(request);
            return response.channelARN();
        } catch (ResourceInUseException e) {
            LOG.info("Signaling channel {} already exists, retrieving existing ARN", channelName);
            DescribeSignalingChannelRequest signalingRequest = DescribeSignalingChannelRequest.builder()
                    .channelName(channelName)
                    .build();
            DescribeSignalingChannelResponse signalingResult = kinesisVideoClient.describeSignalingChannel(signalingRequest);
            return signalingResult.channelInfo().channelARN();
        }
    }

    @ExcludeFromJacocoGeneratedReport
    public void assertPrivateFieldNotNull() {
        if (vlRegisterDeviceJobDAO == null || kinesisVideoClient == null) {
            throw new AssertionError("private field is null");
        }
    }
}
