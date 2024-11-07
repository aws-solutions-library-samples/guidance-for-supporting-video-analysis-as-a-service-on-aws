package com.amazonaws.videoanalytics.helper.ddb;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import org.apache.commons.cli.ParseException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;

/**
 * LocalDynamoDb implementation used in: https://github.com/aws/aws-sdk-java-v2/
 */
public class LocalDynamoDb {
    private DynamoDBProxyServer server;
    private final int port;

    public LocalDynamoDb(int port) {
        if (port >= 1 && port <= 65535) { // valid port number else random
            this.port = port;
        } else {
            try {
                this.port = getFreePort();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Start the local DynamoDb service and run in background
     */
    public void start() {
        try {
            String portString = Integer.toString(port);
            server = createServer(portString);
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a standard AWS v2 SDK client pointing to the local DynamoDb instance
     *
     * @return A DynamoDbClient pointing to the local DynamoDb instance
     */
    public DynamoDbClient createClient() {
        String endpoint = String.format("http://localhost:%d", port);
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                // The region is meaningless for local DynamoDb but required for client builder validation
                .region(Region.US_EAST_1)
                // Starting 1.23.0 and 2.0.0 above DynamoDB local AWS_ACCESS_KEY_ID can contain only letters (A–Z, a–z) and numbers (0–9)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummykey", "dummysecret")))
                .build();
    }

    /**
     * Create a standard AWS v2 SDK client pointing to the local DynamoDb instance
     *
     * @return A DynamoDbAsyncClient pointing to the local DynamoDb instance
     */
    public DynamoDbAsyncClient createAsyncClient() {
        String endpoint = String.format("http://localhost:%d", port);
        return DynamoDbAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                // The region is meaningless for local DynamoDb but required for client builder validation
                .region(Region.US_EAST_1)
                // Starting 1.23.0 and 2.0.0 above DynamoDB local AWS_ACCESS_KEY_ID can contain only letters (A–Z, a–z) and numbers (0–9)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummykey", "dummysecret")))
                .build();
    }

    /**
     * Stops the local DynamoDb service and frees up resources it is using.
     */
    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DynamoDBProxyServer createServer(String portString) throws ParseException {
        return ServerRunner.createServerFromCommandLineArgs(
                new String[]{
                        "-inMemory",
                        "-port", portString
                });
    }

    private int getFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
