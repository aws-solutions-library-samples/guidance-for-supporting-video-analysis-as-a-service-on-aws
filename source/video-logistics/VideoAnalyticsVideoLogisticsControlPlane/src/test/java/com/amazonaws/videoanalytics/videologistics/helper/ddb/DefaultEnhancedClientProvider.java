package com.amazonaws.videoanalytics.videologistics.helper.ddb;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Default implementation of a {@link DynamoDbEnhancedClientProvider} which provides instances of
 * the enhanced client with no customizations (such as extensions).
 */
public class DefaultEnhancedClientProvider implements DynamoDbEnhancedClientProvider {

    @Override
    public DynamoDbEnhancedClient getEnhancedClient(final DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Override
    public DynamoDbEnhancedAsyncClient getEnhancedAsyncClient(final DynamoDbAsyncClient dynamoDbAsyncClient) {
        return DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(dynamoDbAsyncClient)
                .build();
    }
}
