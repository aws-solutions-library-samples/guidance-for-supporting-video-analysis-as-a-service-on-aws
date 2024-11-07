package com.amazonaws.videoanalytics.client.KvsClient;

import com.amazonaws.videoanalytics.exceptions.ValidationException;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;

import software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient;
import software.amazon.awssdk.services.kinesisvideo.model.APIName;
import software.amazon.awssdk.services.kinesisvideo.model.AccessDeniedException;
import software.amazon.awssdk.services.kinesisvideo.model.DescribeSignalingChannelRequest;
import software.amazon.awssdk.services.kinesisvideo.model.DescribeSignalingChannelResponse;
import software.amazon.awssdk.services.kinesisvideo.model.GetDataEndpointRequest;
import software.amazon.awssdk.services.kinesisvideo.model.GetSignalingChannelEndpointRequest;
import software.amazon.awssdk.services.kinesisvideo.model.GetSignalingChannelEndpointResponse;
import software.amazon.awssdk.services.kinesisvideo.model.NotAuthorizedException;
import software.amazon.awssdk.services.kinesisvideo.model.ResourceInUseException;
import software.amazon.awssdk.services.kinesisvideo.model.ResourceNotFoundException;
import software.amazon.awssdk.services.kinesisvideo.model.SingleMasterChannelEndpointConfiguration;


import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.videoanalytics.exceptions.GuidanceExceptionMessage.DEVICE_NOT_REGISTERED;
import static com.amazonaws.videoanalytics.exceptions.GuidanceExceptionMessage.NOT_AUTHORIZED;
import static com.amazonaws.videoanalytics.schema.util.ResourceNameConversionUtils.getDeviceIdFromPlaybackSignalingChannelName;
import static com.amazonaws.videoanalytics.schema.util.ResourceNameConversionUtils.getPlaybackSignalingChannelNameFromDeviceId;
import static com.amazonaws.videoanalytics.utils.AWSVideoAnalyticsServiceLambdaConstants.REGION_NAME;

public class KvsClientWrapper {
    private final KinesisVideoClient kvsClient;
    private final AwsCredentialsProvider credentialsProvider;
    private static final Logger LOG = LogManager.getLogger(KvsClientWrapper.class);
    @AssistedInject
    public KvsClientWrapper(@Assisted("credentialsProvider") final AwsCredentialsProvider credentialsProvider,
                            final Region region,
                            final SdkHttpClient sdkHttpClient) {
        this.credentialsProvider = credentialsProvider;
        this.kvsClient = createKvsClient(credentialsProvider, sdkHttpClient, region);
    }

    private KinesisVideoClient createKvsClient(final AwsCredentialsProvider credentialsProvider,
                                               final SdkHttpClient sdkHttpClient,
                                               final Region region) {
        return KinesisVideoClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(region)
                .httpClient(sdkHttpClient)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .retryPolicy(RetryMode.ADAPTIVE)
                        .build())
                .build();
    }

    public KinesisVideoClient getKvsClient() {
        return this.kvsClient;
    }

    public AwsCredentialsProvider getCredentialsProvider() {
        return this.credentialsProvider;
    }

    public Map<String, String> getSignalingChannelEndpoint(final String channelArn,
                                                           final SingleMasterChannelEndpointConfiguration configuration,
                                                           final String deviceId) {
        String signalingChannelName = getPlaybackSignalingChannelNameFromDeviceId(deviceId);
        try {
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
        catch (AccessDeniedException exception) {
            throw new com.amazonaws.videoanalytics.exceptions.AccessDeniedException(exception.getMessage() + NOT_AUTHORIZED);
        }
        catch (ResourceInUseException exception) {
            throw new ValidationException(String.format(DEVICE_NOT_REGISTERED, deviceId, signalingChannelName));
        }
        catch (ResourceNotFoundException exception) {
            throw new com.amazonaws.videoanalytics.exceptions.ResourceNotFoundException(String.format(DEVICE_NOT_REGISTERED, deviceId, signalingChannelName));
        }
    }

    public String getDataEndpoint(final String streamName) {
        String deviceId = getDeviceIdFromPlaybackSignalingChannelName(streamName);
        try {
            GetDataEndpointRequest getDataEndpointRequest = GetDataEndpointRequest.builder()
                    .streamName(streamName)
                    .apiName(APIName.GET_HLS_STREAMING_SESSION_URL)
                    .build();
            return kvsClient.getDataEndpoint(getDataEndpointRequest).dataEndpoint();
        }
        catch (NotAuthorizedException exception) {
            throw new com.amazonaws.videoanalytics.exceptions.AccessDeniedException(exception.getMessage() + NOT_AUTHORIZED);
        }
        catch (software.amazon.awssdk.services.kinesisvideo.model.ResourceNotFoundException exception) {
            throw new com.amazonaws.videoanalytics.exceptions.ResourceNotFoundException(String.format(DEVICE_NOT_REGISTERED, deviceId, streamName));
        }
    }

    public String getDataEndpointWithApiName(final String streamName, final APIName apiName) {
        String deviceId = getDeviceIdFromPlaybackSignalingChannelName(streamName);
        try {
            LOG.info(String.format("Fetching data endpoint for stream: %s with API name: %s", streamName, apiName));
            GetDataEndpointRequest getDataEndpointRequest = GetDataEndpointRequest.builder()
                    .streamName(streamName)
                    .apiName(apiName)
                    .build();
            String dataEndpoint = kvsClient.getDataEndpoint(getDataEndpointRequest).dataEndpoint();
            LOG.info(String.format("returning data endpoint: %s", dataEndpoint));
            return dataEndpoint;
        }
        catch (NotAuthorizedException exception) {
            throw new com.amazonaws.videoanalytics.exceptions.AccessDeniedException(exception.getMessage() + NOT_AUTHORIZED, exception);
        }
        catch (software.amazon.awssdk.services.kinesisvideo.model.ResourceNotFoundException exception) {
            throw new com.amazonaws.videoanalytics.exceptions.ResourceNotFoundException(String.format(DEVICE_NOT_REGISTERED, deviceId, streamName));
        }
    }

    public String getSignalingChannelArnFromName(final String signalingChannelName) {
        String deviceId = getDeviceIdFromPlaybackSignalingChannelName(signalingChannelName);
        final DescribeSignalingChannelRequest describeSignalingChannelRequest =  DescribeSignalingChannelRequest.builder()
                .channelName(signalingChannelName).build();
        try {
            final DescribeSignalingChannelResponse describeSignalingChannelResponse = kvsClient
                    .describeSignalingChannel(describeSignalingChannelRequest);
            return describeSignalingChannelResponse.channelInfo().channelARN();
        }
        catch (ResourceNotFoundException exception) {
            throw new com.amazonaws.videoanalytics.exceptions.ResourceNotFoundException(String.format(DEVICE_NOT_REGISTERED, deviceId, signalingChannelName));
        }
    }
}
