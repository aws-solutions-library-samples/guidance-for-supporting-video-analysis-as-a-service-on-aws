package com.amazonaws.videoanalytics.videologistics.dependency.kvs;

import com.amazonaws.videoanalytics.videologistics.IceServer;
import com.amazonaws.videoanalytics.videologistics.schema.Source;

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
import software.amazon.awssdk.services.kinesisvideosignaling.KinesisVideoSignalingClient;
import software.amazon.awssdk.services.kinesisvideosignaling.KinesisVideoSignalingClientBuilder;
import software.amazon.awssdk.services.kinesisvideosignaling.model.GetIceServerConfigRequest;
import software.amazon.awssdk.services.kinesisvideosignaling.model.GetIceServerConfigResponse;

import javax.inject.Inject;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class KvsService {
    private final KinesisVideoClient kvsClient;
    private final KinesisVideoSignalingClientBuilder kvsSignalingClientBuilder;
    private static final Logger LOG = LogManager.getLogger(KvsService.class);

    @Inject
    public KvsService(KinesisVideoClient kinesisVideoClient,
                      KinesisVideoSignalingClientBuilder kinesisVideoSignalingClientBuilder) {
        this.kvsClient = kinesisVideoClient;
        this.kvsSignalingClientBuilder = kinesisVideoSignalingClientBuilder;
    }

    public Map<String, String> getSignalingChannelEndpoint(final String channelArn,
                                                           final SingleMasterChannelEndpointConfiguration configuration,
                                                           final String deviceId) {
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

    public String getDataEndpointWithApiName(final String streamName, final APIName apiName) {
        LOG.info(String.format("Fetching data endpoint for stream: %s with API name: %s", streamName, apiName));
        GetDataEndpointRequest getDataEndpointRequest = GetDataEndpointRequest.builder()
                .streamName(streamName)
                .apiName(apiName)
                .build();
        String dataEndpoint = kvsClient.getDataEndpoint(getDataEndpointRequest).dataEndpoint();
        LOG.info(String.format("Returning data endpoint: %s", dataEndpoint));
        return dataEndpoint;
    }

    public String getSignalingChannelArnFromName(final String signalingChannelName) {
        final DescribeSignalingChannelRequest describeSignalingChannelRequest =  DescribeSignalingChannelRequest.builder()
                .channelName(signalingChannelName).build();
        final DescribeSignalingChannelResponse describeSignalingChannelResponse = kvsClient
                .describeSignalingChannel(describeSignalingChannelRequest);
        return describeSignalingChannelResponse.channelInfo().channelARN();
    }

    public List<IceServer> getSyncIceServerConfigs(final String endpoint,
                                                   final String channelArn,
                                                   final String deviceId) {
        KinesisVideoSignalingClient kvsSignalingClient = kvsSignalingClientBuilder.endpointOverride(URI.create(endpoint)).build();

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

    public List<Source.GuidanceIceServer> getIceServerConfigs(final String endpoint,
                                                              final String channelArn,
                                                              final String deviceId) {
        KinesisVideoSignalingClient kvsSignalingClient = kvsSignalingClientBuilder.endpointOverride(URI.create(endpoint)).build();

        final GetIceServerConfigResponse getIceServerConfigResponse = kvsSignalingClient.getIceServerConfig(
                GetIceServerConfigRequest.builder().channelARN(channelArn).build()
        );
        List<Source.GuidanceIceServer> iceServerList = new ArrayList<>();
        getIceServerConfigResponse.iceServerList().forEach(
                iceServer -> iceServerList.add(Source.GuidanceIceServer.builder()
                        .uris(iceServer.uris())
                        .password(iceServer.password())
                        .username(iceServer.username())
                        .build())
        );
        return iceServerList;
    }
}
