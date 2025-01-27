package com.amazonaws.videoanalytics.videologistics.dependency.kvs;

import com.amazonaws.videoanalytics.videologistics.IceServer;
import com.amazonaws.videoanalytics.videologistics.SourceInfo;
import com.amazonaws.videoanalytics.videologistics.SourceType;
import com.amazonaws.videoanalytics.videologistics.StreamSource;
import com.amazonaws.videoanalytics.videologistics.client.kvsarchivedmedia.KvsArchivedMediaClientFactory;
import com.amazonaws.videoanalytics.videologistics.client.kvsarchivedmedia.KvsArchivedMediaClientWrapper;
import com.amazonaws.videoanalytics.videologistics.client.kvssignaling.KvsSignalingClientFactory;
import com.amazonaws.videoanalytics.videologistics.client.kvssignaling.KvsSignalingClientWrapper;

import software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient;
import software.amazon.awssdk.services.kinesisvideo.model.APIName;
import software.amazon.awssdk.services.kinesisvideo.model.ChannelInfo;
import software.amazon.awssdk.services.kinesisvideo.model.ChannelRole;
import software.amazon.awssdk.services.kinesisvideo.model.DescribeSignalingChannelRequest;
import software.amazon.awssdk.services.kinesisvideo.model.DescribeSignalingChannelResponse;
import software.amazon.awssdk.services.kinesisvideo.model.GetDataEndpointRequest;
import software.amazon.awssdk.services.kinesisvideo.model.GetDataEndpointResponse;
import software.amazon.awssdk.services.kinesisvideo.model.GetSignalingChannelEndpointRequest;
import software.amazon.awssdk.services.kinesisvideo.model.GetSignalingChannelEndpointResponse;
import software.amazon.awssdk.services.kinesisvideo.model.ResourceEndpointListItem;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static software.amazon.awssdk.services.kinesisvideo.model.ChannelProtocol.HTTPS;
import static software.amazon.awssdk.services.kinesisvideo.model.ChannelProtocol.WSS;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.MAX_MEDIA_PLAYLIST_FRAGMENTS;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.TWELVE_HOURS;
import static com.amazonaws.videoanalytics.videologistics.utils.ResourceNameConversionUtils.getLivestreamSignalingChannelNameFromDeviceId;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.DATA_ENDPOINT;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.DEVICE_ID;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.END_TIMESTAMP_DATE;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.HLS_STREAMING_URL;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.HTTPS_RESOURCE_ENDPOINT;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.PASSWORD;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.SIGNALING_CHANNEL_ARN;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.START_TIMESTAMP_DATE;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.USERNAME;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.WSS_RESOURCE_ENDPOINT;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class KvsServiceTest {
    @Mock
    private KinesisVideoClient kinesisVideoClient;
    @Mock
    private KvsSignalingClientFactory kvsSignalingClientFactory;
    @Mock
    private KvsSignalingClientWrapper kvsSignalingClientWrapper;
    @Mock
    private KinesisVideoSignalingClient kinesisVideoSignalingClient;
    @Mock
    private KvsArchivedMediaClientFactory kvsArchivedMediaClientFactory;
    @Mock
    private KvsArchivedMediaClientWrapper kvsArchivedMediaClientWrapper;
    @Mock
    private KinesisVideoArchivedMediaClient kinesisVideoArchivedMediaClient;
    
    private KvsService kvsService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        GetDataEndpointRequest getDataEndpointRequest = GetDataEndpointRequest.builder()
                .streamName(DEVICE_ID)
                .apiName(APIName.GET_HLS_STREAMING_SESSION_URL)
                .build();
        GetDataEndpointResponse getDataEndpointResponse = GetDataEndpointResponse.builder()
                .dataEndpoint(DATA_ENDPOINT)
                .build();
        when(kinesisVideoClient.getDataEndpoint(getDataEndpointRequest)).thenReturn(getDataEndpointResponse);

        when(kvsSignalingClientFactory.create(HTTPS_RESOURCE_ENDPOINT)).thenReturn(kvsSignalingClientWrapper);
        when(kvsSignalingClientWrapper.getKvsSignalingClient()).thenReturn(kinesisVideoSignalingClient);
        when(kvsArchivedMediaClientFactory.create(DATA_ENDPOINT)).thenReturn(kvsArchivedMediaClientWrapper);
        when(kvsArchivedMediaClientWrapper.getKvsArchivedMediaClient()).thenReturn(kinesisVideoArchivedMediaClient);
        kvsService = new KvsService(
            kinesisVideoClient,
            kvsSignalingClientFactory,
            kvsArchivedMediaClientFactory
        );
    }

    @Test
    public void getSignalingChannelArnFromName_WhenValidRequest_ReturnsSignalingChannelArn() {
        String signalingChannelName = getLivestreamSignalingChannelNameFromDeviceId(DEVICE_ID);
        DescribeSignalingChannelRequest request = DescribeSignalingChannelRequest.builder()
                .channelName(signalingChannelName)
                .build();
        DescribeSignalingChannelResponse response = DescribeSignalingChannelResponse.builder()
                .channelInfo(ChannelInfo.builder()
                        .channelName(signalingChannelName)
                        .channelARN(SIGNALING_CHANNEL_ARN)
                        .build()
                )
                .build();
        when(kinesisVideoClient.describeSignalingChannel(request)).thenReturn(response);

        String channelArn = kvsService.getSignalingChannelArnFromName(signalingChannelName);
        assertEquals(channelArn, SIGNALING_CHANNEL_ARN);
    }

    @Test
    public void getSignalingChannelEndpoint_WhenValidRequest_ReturnsSignalingChannelMap() {
        SingleMasterChannelEndpointConfiguration singleMasterChannelEndpointConfigurationAsViewer =
                SingleMasterChannelEndpointConfiguration
                        .builder()
                        .protocols(Arrays.asList(WSS, HTTPS))
                        .role(ChannelRole.VIEWER)
                        .build();
        
        GetSignalingChannelEndpointRequest request = GetSignalingChannelEndpointRequest
                .builder()
                .channelARN(SIGNALING_CHANNEL_ARN)
                .singleMasterChannelEndpointConfiguration(singleMasterChannelEndpointConfigurationAsViewer)
                .build();
        GetSignalingChannelEndpointResponse response = GetSignalingChannelEndpointResponse.builder()
                .resourceEndpointList(Arrays.asList(
                        ResourceEndpointListItem.builder()
                                .protocol(HTTPS)
                                .resourceEndpoint(HTTPS_RESOURCE_ENDPOINT)
                                .build(),
                        ResourceEndpointListItem.builder()
                                .protocol(WSS)
                                .resourceEndpoint(WSS_RESOURCE_ENDPOINT)
                                .build()
                ))
                .build();
        when(kinesisVideoClient.getSignalingChannelEndpoint(request)).thenReturn(response);

        Map<String, String> signalingChannelMap = kvsService.getSignalingChannelEndpoint(SIGNALING_CHANNEL_ARN, singleMasterChannelEndpointConfigurationAsViewer);
        
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put(HTTPS.toString(), HTTPS_RESOURCE_ENDPOINT);
        expectedMap.put(WSS.toString(), WSS_RESOURCE_ENDPOINT);
        assertEquals(signalingChannelMap, expectedMap);
    }

    @Test
    public void getDataEndpoint_WhenValidRequest_ReturnsDataEndpoint() {
        String dataEndpoint = kvsService.getDataEndpoint(DEVICE_ID);
        assertEquals(dataEndpoint, DATA_ENDPOINT);
    }

    @Test
    public void getSyncIceServerConfigs_WhenValidRequest_ReturnsIceServerList() {
        HashSet<String> uris = new HashSet<>();
        uris.add("turns:54-200-133-255.t-67e8ec01.kinesisvideo.us-west-2.amazonaws.com:443?transport=udp");
        uris.add("turns:54-200-133-255.t-67e8ec01.kinesisvideo.us-west-2.amazonaws.com:443?transport=tcp");
        uris.add("turn:54-200-133-255.t-67e8ec01.kinesisvideo.us-west-2.amazonaws.com:443?transport=udp");

        GetIceServerConfigRequest request = GetIceServerConfigRequest.builder()
                .channelARN(SIGNALING_CHANNEL_ARN)
                .build();
        GetIceServerConfigResponse response = GetIceServerConfigResponse.builder()
                .iceServerList(List.of(software.amazon.awssdk.services.kinesisvideosignaling.model.IceServer.builder()
                    .uris(uris)
                    .password(PASSWORD)
                    .username(USERNAME)
                    .build()
                ))
                .build();
        when(kinesisVideoSignalingClient.getIceServerConfig(request)).thenReturn(response);

        List<IceServer> expectedList = List.of(
                IceServer.builder()
                        .uris(uris)
                        .password(PASSWORD)
                        .username(USERNAME)
                        .build()
        );
        List<IceServer> iceServerList = kvsService.getSyncIceServerConfigs(HTTPS_RESOURCE_ENDPOINT, SIGNALING_CHANNEL_ARN);
        assertEquals(iceServerList, expectedList);
    }

    @Test
    public void getStreamingSessionURL_WhenValidRequest_ReturnsStreamSource() {
        HLSTimestampRange timestampRange = HLSTimestampRange.builder()
                .startTimestamp(START_TIMESTAMP_DATE.toInstant())
                .endTimestamp(END_TIMESTAMP_DATE.toInstant())
                .build();
        HLSFragmentSelector hlsFragmentSelector = HLSFragmentSelector.builder()
                .fragmentSelectorType(HLSFragmentSelectorType.PRODUCER_TIMESTAMP)
                .timestampRange(timestampRange)
                .build();
        GetHlsStreamingSessionUrlRequest getHlsStreamingSessionUrlRequest = GetHlsStreamingSessionUrlRequest.builder()
                .hlsFragmentSelector(hlsFragmentSelector)
                .playbackMode(HLSPlaybackMode.ON_DEMAND)
                .discontinuityMode(HLSDiscontinuityMode.ON_DISCONTINUITY)
                .maxMediaPlaylistFragmentResults(MAX_MEDIA_PLAYLIST_FRAGMENTS)
                .displayFragmentTimestamp(HLSDisplayFragmentTimestamp.ALWAYS)
                .streamName(DEVICE_ID)
                .expires(TWELVE_HOURS)
                .build();
        GetHlsStreamingSessionUrlResponse getHlsStreamingSessionUrlResponse = GetHlsStreamingSessionUrlResponse.builder()
                .hlsStreamingSessionURL(HLS_STREAMING_URL)
                .build();
        when(kinesisVideoArchivedMediaClient.getHLSStreamingSessionURL(getHlsStreamingSessionUrlRequest)).thenReturn(getHlsStreamingSessionUrlResponse);

        StreamSource expectedStreamSource = StreamSource.builder()
                .sourceType(SourceType.HLS)
                .source(SourceInfo.builder()
                    .hLSStreamingURL(HLS_STREAMING_URL)
                    .build())
                .build();

        StreamSource streamSource = kvsService.getStreamingSessionURL(DEVICE_ID, START_TIMESTAMP_DATE, END_TIMESTAMP_DATE);
        assertEquals(streamSource, expectedStreamSource);
    }
}
