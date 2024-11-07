package com.amazonaws.videoanalytics.client.KvsSignalingClient;

import com.amazonaws.videoanalytics.videologistics.IceServer;
import software.amazon.awssdk.services.kinesisvideosignaling.KinesisVideoSignalingClient;
import com.amazonaws.videoanalytics.schema.Source;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesisvideosignaling.model.GetIceServerConfigRequest;
import software.amazon.awssdk.services.kinesisvideosignaling.model.GetIceServerConfigResponse;
import software.amazon.awssdk.services.kinesisvideosignaling.model.InvalidClientException;
import software.amazon.awssdk.services.kinesisvideosignaling.model.NotAuthorizedException;
import software.amazon.awssdk.services.kinesisvideosignaling.model.ResourceNotFoundException;
import software.amazon.awssdk.services.kinesisvideosignaling.model.SessionExpiredException;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.amazonaws.videoanalytics.exceptions.GuidanceExceptionMessage.DEVICE_NOT_REGISTERED;
import static com.amazonaws.videoanalytics.exceptions.GuidanceExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.exceptions.GuidanceExceptionMessage.NOT_AUTHORIZED;
import static com.amazonaws.videoanalytics.schema.util.ResourceNameConversionUtils.getLivestreamSignalingChannelNameFromDeviceId;
import static com.amazonaws.videoanalytics.schema.util.ResourceNameConversionUtils.getPlaybackSignalingChannelNameFromDeviceId;

public class KvsSignalingClientWrapper {
    private final KinesisVideoSignalingClient kvsSignalingClient;
    private static final Logger LOG = LogManager.getLogger(KvsSignalingClientWrapper.class);

    @AssistedInject
    public KvsSignalingClientWrapper(@Assisted("endpoint") final String endpoint,
                                     @Assisted("credentialsProvider") final AwsCredentialsProvider credentialsProvider,
                                     final Region region,
                                     final SdkHttpClient sdkHttpClient) {
        this.kvsSignalingClient = createKvsSignalingClient(credentialsProvider, region, endpoint, sdkHttpClient);
    }

    private KinesisVideoSignalingClient createKvsSignalingClient(final AwsCredentialsProvider credentialsProvider,
                                                                 final Region region,
                                                                 final String endpoint,
                                                                 final SdkHttpClient sdkHttpClient) {
        return KinesisVideoSignalingClient.builder()
                .region(region)
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(credentialsProvider)
                .httpClient(sdkHttpClient)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .retryPolicy(RetryMode.ADAPTIVE)
                        .build())
                .build();
    }

    public List<IceServer> getSyncIceServerConfigs(final String channelArn,
                                                   final String deviceId) {

        String signalingChannelName = getLivestreamSignalingChannelNameFromDeviceId(deviceId);

        try {
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
        catch (SessionExpiredException | InvalidClientException exception) {
            LOG.error(exception.getMessage());
            throw new com.amazonaws.videoanalytics.exceptions.InternalServerException(INTERNAL_SERVER_EXCEPTION);
        }
        catch (ResourceNotFoundException exception) {
            throw new com.amazonaws.videoanalytics.exceptions.ResourceNotFoundException(String.format(DEVICE_NOT_REGISTERED, deviceId, signalingChannelName));
        }
        catch (NotAuthorizedException exception) {
            throw new com.amazonaws.videoanalytics.exceptions.AccessDeniedException(exception.getMessage() + NOT_AUTHORIZED);
        }
    }

    public List<Source.GuidanceIceServer> getIceServerConfigs(final String channelArn,
                                                              final String deviceId) {

        String signalingChannelName = getPlaybackSignalingChannelNameFromDeviceId(deviceId);

        try {
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
        catch (SessionExpiredException exception) {
            LOG.error(exception.getMessage());
            throw new com.amazonaws.videoanalytics.exceptions.InternalServerException(INTERNAL_SERVER_EXCEPTION, exception);
        }
        catch (ResourceNotFoundException exception) {
            throw new com.amazonaws.videoanalytics.exceptions.ResourceNotFoundException(String.format(DEVICE_NOT_REGISTERED, deviceId, signalingChannelName));
        }
        catch (NotAuthorizedException exception) {
            throw new com.amazonaws.videoanalytics.exceptions.AccessDeniedException(exception.getMessage() + NOT_AUTHORIZED);
        }
        catch (InvalidClientException exception) {
            LOG.error(exception.getMessage());
            throw new com.amazonaws.videoanalytics.exceptions.InternalServerException(INTERNAL_SERVER_EXCEPTION);
        }
    }
}
