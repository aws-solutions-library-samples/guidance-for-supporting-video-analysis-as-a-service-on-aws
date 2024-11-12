package com.amazonaws.videoanalytics.videologistics.helper.ddb;


import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Provides a DynamoDB enhanced client for the given DynamoDB client. Implementations of this
 * interface are expected to have a public, no args constructor.
 */
public interface DynamoDbEnhancedClientProvider {

    DynamoDbEnhancedClient getEnhancedClient(DynamoDbClient dynamoDbClient);

    DynamoDbEnhancedAsyncClient getEnhancedAsyncClient(DynamoDbAsyncClient dynamoDbAsyncClient);
}
