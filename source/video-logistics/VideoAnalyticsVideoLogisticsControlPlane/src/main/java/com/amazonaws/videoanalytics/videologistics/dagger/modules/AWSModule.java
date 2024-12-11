package com.amazonaws.videoanalytics.videologistics.dagger.modules;

import dagger.Provides;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import org.apache.http.HttpRequestInterceptor;
import io.github.acm19.aws.interceptor.http.AwsRequestSigningApacheInterceptor;

import javax.inject.Named;
import javax.inject.Singleton;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.CREDENTIALS_PROVIDER;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.HTTP_CLIENT;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.REGION_NAME;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.OPENSEARCH_INTERCEPTOR_NAME;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.OPENSEARCH_SIGNER_NAME;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.OPENSEARCH_SERVICE_NAME;

import dagger.Module;

@Module
public class AWSModule {
    @Provides
    @Singleton
    @Named(CREDENTIALS_PROVIDER)
    public AwsCredentialsProvider providesAwsCredentialsProvider() {
        return DefaultCredentialsProvider.create();
    }

    @Provides
    @Singleton
    @Named(HTTP_CLIENT)
    public SdkHttpClient providesSdkHttpClient() {
        return ApacheHttpClient.builder().build();
    }

    @Provides
    @Singleton
    public S3Presigner providesS3Presigner(Region region) {
        return S3Presigner.builder()
                .region(region)
                .build();
    }

    @Provides
    @Singleton
    public Region providesRegion(@Named(REGION_NAME) String region) {
        return Region.of(region);
    }

    @Provides
    @Singleton
    public DynamoDbClient providesDynamoDbClient(@Named(HTTP_CLIENT) final SdkHttpClient sdkHttpClient,
                                                 final Region region) {
        return DynamoDbClient.builder()
                .region(region)
                .httpClient(sdkHttpClient)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .retryPolicy(RetryMode.ADAPTIVE)
                        .build())
                .build();
    }

    @Provides
    @Singleton
    public DynamoDbEnhancedClient providesDynamoDbEnhancedClient(final DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Provides
    @Singleton
    public KinesisVideoClient providesKinesisVideoClient(@Named(CREDENTIALS_PROVIDER) final AwsCredentialsProvider credentialsProvider,
                                                         @Named(HTTP_CLIENT) final SdkHttpClient sdkHttpClient,
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

    @Provides
    @Singleton
    public KinesisClient getKinesisClient(
        @Named(CREDENTIALS_PROVIDER) final AwsCredentialsProvider credentialsProvider,
        @Named(HTTP_CLIENT) final SdkHttpClient sdkHttpClient,
        final Region region) {

        return KinesisClient.builder()
            .httpClient(sdkHttpClient)
            .region(region)
            .credentialsProvider(credentialsProvider)
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                    .retryPolicy(RetryMode.ADAPTIVE)
                    .build())
            .build();
    }

    @Provides
    @Singleton
    @Named(OPENSEARCH_SIGNER_NAME)
    public AwsV4HttpSigner getOpenSearchAWS4Signer(@Named(REGION_NAME) final String regionName) {
        return AwsV4HttpSigner.create();
    }


    @Provides
    @Singleton
    @Named(OPENSEARCH_INTERCEPTOR_NAME)
    public HttpRequestInterceptor getOpenSearchSignerInterceptor(final @Named(CREDENTIALS_PROVIDER) AwsCredentialsProvider credentialsProvider,
                                                                 final @Named(OPENSEARCH_SIGNER_NAME) AwsV4HttpSigner signer,
                                                                 final @Named(REGION_NAME) String regionName) {
        return new AwsRequestSigningApacheInterceptor(OPENSEARCH_SERVICE_NAME, signer, credentialsProvider, regionName);
    }
}
