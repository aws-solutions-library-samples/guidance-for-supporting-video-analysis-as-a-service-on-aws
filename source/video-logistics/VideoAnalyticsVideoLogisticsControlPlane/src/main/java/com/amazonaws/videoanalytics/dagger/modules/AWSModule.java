package com.amazonaws.videoanalytics.dagger.modules;
import dagger.Provides;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

import javax.inject.Named;
import javax.inject.Singleton;

import static com.amazonaws.videoanalytics.schema.util.GuidanceVLConstants.REGION_NAME;
import static com.amazonaws.videoanalytics.utils.AWSVideoAnalyticsServiceLambdaConstants.CREDENTIALS_PROVIDER;

import dagger.Module;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Module
public class AWSModule {
    @Provides
    @Singleton
    @Named(CREDENTIALS_PROVIDER)
    public AwsCredentialsProvider provideAwsCredentialsProvider() {
        return DefaultCredentialsProvider.create();
    }

    @Provides
    @Singleton
    public SdkHttpClient provideSdkHttpClient() {
        return ApacheHttpClient.builder().build();
    }

    @Provides
    @Singleton
    @Named(REGION_NAME)
    public String getRegionName() {
        return System.getenv("AWS_REGION");
    }

    @Provides
    @Singleton
    public Region providesRegion(@Named(REGION_NAME) final String regionName) {
        return Region.of(regionName);
    }

    @Provides
    @Singleton
    public DynamoDbClient getDynamoDbClient(@Named(REGION_NAME) final String regionName,
                                            final SdkHttpClient sdkHttpClient) {
        return DynamoDbClient.builder()
                .region(Region.of(regionName))
                .httpClient(sdkHttpClient)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .retryPolicy(RetryMode.ADAPTIVE)
                        .build())
                .build();
    }

    @Provides
    @Singleton
    public DynamoDbEnhancedClient getDynamoDbEnhancedClient(final DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
