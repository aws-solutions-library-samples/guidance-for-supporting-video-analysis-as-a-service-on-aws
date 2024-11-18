package com.amazonaws.videoanalytics.videologistics.client.kvssignaling;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesisvideosignaling.KinesisVideoSignalingClient;

import javax.inject.Named;
import java.net.URI;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.CREDENTIALS_PROVIDER;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.HTTP_CLIENT;

public class KvsSignalingClientWrapper {
    private final KinesisVideoSignalingClient kvsSignalingClient;

    @AssistedInject
    public KvsSignalingClientWrapper(@Assisted("endpoint") String endpoint,
                                     @Named(CREDENTIALS_PROVIDER) AwsCredentialsProvider credentialsProvider,
                                     Region region,
                                     @Named(HTTP_CLIENT) SdkHttpClient sdkHttpClient) {
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
                .build();
    }

    public KinesisVideoSignalingClient getKvsSignalingClient() {
        return this.kvsSignalingClient;
    }
}
