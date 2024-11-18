package com.amazonaws.videoanalytics.videologistics.client.kvsarchivedmedia;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.KinesisVideoArchivedMediaClient;

import javax.inject.Named;
import java.net.URI;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.CREDENTIALS_PROVIDER;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.HTTP_CLIENT;

public class KvsArchivedMediaClientWrapper {
    private final KinesisVideoArchivedMediaClient kvsArchivedMediaClient;

    @AssistedInject
    public KvsArchivedMediaClientWrapper(@Assisted("endpoint") String endpoint,
                                         @Named(CREDENTIALS_PROVIDER) AwsCredentialsProvider credentialsProvider,
                                         Region region,
                                         @Named(HTTP_CLIENT) SdkHttpClient sdkHttpClient) {
        this.kvsArchivedMediaClient = createKvsArchivedMediaClient(credentialsProvider, region, endpoint, sdkHttpClient);
    }

    private KinesisVideoArchivedMediaClient createKvsArchivedMediaClient(final AwsCredentialsProvider credentialsProvider,
                                                                         final Region region,
                                                                         final String endpoint,
                                                                         final SdkHttpClient sdkHttpClient) {
        return KinesisVideoArchivedMediaClient.builder()
                .region(region)
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(credentialsProvider)
                .httpClient(sdkHttpClient)
                .build();
    }

    public KinesisVideoArchivedMediaClient getKvsArchivedMediaClient() {
        return this.kvsArchivedMediaClient;
    }
}
