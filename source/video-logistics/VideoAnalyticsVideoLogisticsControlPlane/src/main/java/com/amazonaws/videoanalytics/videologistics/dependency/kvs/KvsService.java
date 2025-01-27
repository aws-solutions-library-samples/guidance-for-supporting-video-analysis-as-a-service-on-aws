package com.amazonaws.videoanalytics.videologistics.dependency.kvs;

import com.amazonaws.videoanalytics.videologistics.IceServer;
import com.amazonaws.videoanalytics.videologistics.SourceInfo;
import com.amazonaws.videoanalytics.videologistics.SourceType;
import com.amazonaws.videoanalytics.videologistics.StreamSource;
import com.amazonaws.videoanalytics.videologistics.client.kvsarchivedmedia.KvsArchivedMediaClientFactory;
import com.amazonaws.videoanalytics.videologistics.client.kvssignaling.KvsSignalingClientFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient;
import software.amazon.awssdk.services.kinesisvideo.model.APIName;
import software.amazon.awssdk.services.kinesisvideo.model.DescribeSignalingChannelRequest;
import software.amazon.awssdk.services.kinesisvideo.model.DescribeSignalingChannelResponse;
import software.amazon.awssdk.services.kinesisvideo.model.GetDataEndpointRequest;
import software.amazon.awssdk.services.kinesisvideo.model.GetSignalingChannelEndpointRequest;
import software.amazon.awssdk.services.kinesisvideo.model.GetSignalingChannelEndpointResponse;
import software.amazon.awssdk.services.kinesisvideo.model.SingleMasterChannelEndpointConfiguration;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.KinesisVideoArchivedMediaClient;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.GetHlsStreamingSessionUrlRequest;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.GetHlsStreamingSessionUrlResponse;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.HLSDiscontinuityMode;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.HLSDisplayFragmentTimestamp;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.HLSFragmentSelector;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.HLSFragmentSelectorType;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.HLSPlaybackMode;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.HLSTimestampRange;
import software.amazon.awssdk.services.kinesisvideosignaling.KinesisVideoSignalingClient;
import software.amazon.awssdk.services.kinesisvideosignaling.model.GetIceServerConfigRequest;
import software.amazon.awssdk.services.kinesisvideosignaling.model.GetIceServerConfigResponse;

import javax.inject.Inject;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.MAX_MEDIA_PLAYLIST_FRAGMENTS;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.TWELVE_HOURS;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class KvsService {
    private final KinesisVideoClient kvsClient;
    private final KvsSignalingClientFactory kvsSignalingClientFactory;
    private final KvsArchivedMediaClientFactory kvsArchivedMediaClientFactory;

    @Inject
    public KvsService(KinesisVideoClient kinesisVideoClient,
                      KvsSignalingClientFactory kvsSignalingClientFactory,
                      KvsArchivedMediaClientFactory kvsArchivedMediaClientFactory) {
        this.kvsClient = kinesisVideoClient;
        this.kvsSignalingClientFactory = kvsSignalingClientFactory;
        this.kvsArchivedMediaClientFactory = kvsArchivedMediaClientFactory;
    }

    public Map<String, String> getSignalingChannelEndpoint(final String channelArn,
                                                           final SingleMasterChannelEndpointConfiguration configuration) {
        final GetSignalingChannelEndpointResponse getSignalingChannelEndpointRequest =
                this.kvsClient.getSignalingChannelEndpoint(
                        GetSignalingChannelEndpointRequest
                                .builder()
                                .channelARN(channelArn)
                                .singleMasterChannelEndpointConfiguration(configuration)
                                .build()
                );

        Map<String, String> signalingChannelMap = new HashMap<>();
        getSignalingChannelEndpointRequest.resourceEndpointList().forEach(
                resourceEndpointListItem -> signalingChannelMap.put(
                        resourceEndpointListItem.protocolAsString(),
                        resourceEndpointListItem.resourceEndpoint()
                )
        );
        return signalingChannelMap;
    }

    public String getDataEndpoint(final String streamName) {
        GetDataEndpointRequest getDataEndpointRequest = GetDataEndpointRequest.builder()
                .streamName(streamName)
                .apiName(APIName.GET_HLS_STREAMING_SESSION_URL)
                .build();
        return kvsClient.getDataEndpoint(getDataEndpointRequest).dataEndpoint();
    }

    public String getSignalingChannelArnFromName(final String signalingChannelName) {
        final DescribeSignalingChannelRequest describeSignalingChannelRequest =  DescribeSignalingChannelRequest.builder()
                .channelName(signalingChannelName).build();
        final DescribeSignalingChannelResponse describeSignalingChannelResponse = kvsClient
                .describeSignalingChannel(describeSignalingChannelRequest);
        return describeSignalingChannelResponse.channelInfo().channelARN();
    }

    public List<IceServer> getSyncIceServerConfigs(final String endpoint,
                                                   final String channelArn) {
        KinesisVideoSignalingClient kvsSignalingClient = kvsSignalingClientFactory.create(endpoint).getKvsSignalingClient();

        final GetIceServerConfigResponse getIceServerConfigResponse = kvsSignalingClient.getIceServerConfig(
                GetIceServerConfigRequest.builder().channelARN(channelArn).build()
        );
        List<IceServer> iceServerList = new ArrayList<>();
        getIceServerConfigResponse.iceServerList().forEach(
                iceServer -> iceServerList.add(IceServer.builder()
                        .uris(new HashSet<>(iceServer.uris()))
                        .password(iceServer.password())
                        .username(iceServer.username())
                        .build())
        );
        return iceServerList;
        
    }

    public StreamSource getStreamingSessionURL(final String streamName,
                                               final Date startTime,
                                               final Date endTime) {
        HLSTimestampRange timestampRange = HLSTimestampRange.builder()
                .startTimestamp(startTime.toInstant())
                .endTimestamp(endTime.toInstant())
                .build();

        HLSFragmentSelector hlsFragmentSelector = HLSFragmentSelector.builder()
                .fragmentSelectorType(HLSFragmentSelectorType.PRODUCER_TIMESTAMP)
                .timestampRange(timestampRange)
                .build();

        GetHlsStreamingSessionUrlRequest getHLSStreamingSessionURLRequest = GetHlsStreamingSessionUrlRequest.builder()
                .hlsFragmentSelector(hlsFragmentSelector)
                .playbackMode(HLSPlaybackMode.ON_DEMAND)
                .discontinuityMode(HLSDiscontinuityMode.ON_DISCONTINUITY)
                .maxMediaPlaylistFragmentResults(MAX_MEDIA_PLAYLIST_FRAGMENTS)
                .displayFragmentTimestamp(HLSDisplayFragmentTimestamp.ALWAYS)
                .streamName(streamName)
                .expires(TWELVE_HOURS)
                .build();

        String dataEndpoint = getDataEndpoint(streamName);

        KinesisVideoArchivedMediaClient kvsArchivedMediaClientWrapper = kvsArchivedMediaClientFactory.create(dataEndpoint).getKvsArchivedMediaClient();

        GetHlsStreamingSessionUrlResponse getHLSStreamingSessionURLResponse = kvsArchivedMediaClientWrapper.getHLSStreamingSessionURL(getHLSStreamingSessionURLRequest);

        String hlsStreamingSessionURL = getHLSStreamingSessionURLResponse.hlsStreamingSessionURL();
        SourceInfo source = SourceInfo.builder()
                .hLSStreamingURL(hlsStreamingSessionURL)
                .build();
        return StreamSource.builder()
                .sourceType(SourceType.HLS)
                .source(source)
                .build();
    }
}
