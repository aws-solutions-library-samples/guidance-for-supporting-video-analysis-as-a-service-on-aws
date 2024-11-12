package com.amazonaws.videoanalytics.devicemanagement.dagger.modules;

import javax.inject.Named;
import javax.inject.Singleton;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.CREDENTIALS_PROVIDER;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.HTTP_CLIENT;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.REGION_NAME;

import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/*
    Module to maintain singletons for AWS service dependencies
 */
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
    @Named(HTTP_CLIENT)
    public SdkHttpClient provideSdkHttpClient() {
        return ApacheHttpClient.builder().build();
    }

    @Provides
    @Singleton
    public IotClient providesIotClient(@Named(CREDENTIALS_PROVIDER) final AwsCredentialsProvider credentialsProvider,
                                       @Named(HTTP_CLIENT) final SdkHttpClient sdkHttpClient,
                                       @Named(REGION_NAME) final String region) {
        return IotClient.builder()
                .credentialsProvider(credentialsProvider)
                .httpClient(sdkHttpClient)
                .region(Region.of(region))
                .build();
    }

    @Provides
    @Singleton
    public IotDataPlaneClient providesIotDataPlaneClient(@Named(CREDENTIALS_PROVIDER) final AwsCredentialsProvider credentialsProvider,
                                                         @Named(HTTP_CLIENT) final SdkHttpClient sdkHttpClient,
                                                         @Named(REGION_NAME) final String region) {
        return IotDataPlaneClient.builder()
                .credentialsProvider(credentialsProvider)
                .httpClient(sdkHttpClient)
                .region(Region.of(region))
                .build();
    }

    @Provides
    @Singleton
    public DynamoDbClient provideDynamoDbClient(@Named(HTTP_CLIENT) final SdkHttpClient sdkHttpClient,
                                                @Named(REGION_NAME) final String region) {
        return DynamoDbClient.builder()
                .httpClient(sdkHttpClient)
                .region(Region.of(region))
                .build();
    }

    @Provides
    @Singleton
    public DynamoDbEnhancedClient provideDynamoDbEnhancedClient(final DynamoDbClient ddbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddbClient)
                .build();
    }
}
