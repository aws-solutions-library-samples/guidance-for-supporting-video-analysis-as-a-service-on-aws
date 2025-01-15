package com.amazonaws.videoanalytics.videologistics.dependency.apig;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.CREDENTIALS_PROVIDER;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.HTTP_CLIENT;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.REGION_NAME;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.GetRestApisRequest;
import software.amazon.awssdk.services.apigateway.model.RestApi;

@Singleton
public class ApigService {
    private static final Logger log = LoggerFactory.getLogger(ApigService.class);
    private static final String DM_API_NAME = System.getProperty("DEVICE_MANAGEMENT_API_NAME", System.getenv("DEVICE_MANAGEMENT_API_NAME"));
    private final SdkHttpClient httpClient;
    private final AwsCredentialsProvider credentialsProvider;
    private final String region;
    private final ApiGatewayClient apiGatewayClient;
    private String dmApiEndpoint;

    @Inject
    public ApigService(
            @Named(HTTP_CLIENT) final SdkHttpClient httpClient,
            @Named(CREDENTIALS_PROVIDER) final AwsCredentialsProvider credentialsProvider,
            @Named(REGION_NAME) final String region,
            ApiGatewayClient apiGatewayClient) {
        this.httpClient = httpClient;
        this.credentialsProvider = credentialsProvider;
        this.region = region;
        this.apiGatewayClient = apiGatewayClient;
    }

    private String getDmApiEndpoint() {
        if (dmApiEndpoint == null) {
            log.info("Fetching DM API endpoint URL for API: {}", DM_API_NAME);
            try {
                var restApis = apiGatewayClient.getRestApis(GetRestApisRequest.builder().build());
                log.info("GetRestApis Response - Total APIs: {}, Position: {}", 
                    restApis.items().size(),
                    restApis.position());
                String apiId = restApis.items().stream()
                    .peek(api -> log.info("Found API: name={}, id={}", api.name(), api.id()))
                    .filter(api -> DM_API_NAME.equals(api.name()))
                    .map(RestApi::id)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Could not find API Gateway with name: " + DM_API_NAME));
                
                String stage = "prod"; 
                dmApiEndpoint = String.format("https://%s.execute-api.%s.amazonaws.com/%s",
                    apiId, region, stage);
                
                log.info("Found DM API endpoint: {}", dmApiEndpoint);
            } catch (Exception e) {
                log.error("Failed to fetch DM API endpoint", e);
                throw new IllegalStateException("Failed to fetch DM API endpoint", e);
            }
        }
        return dmApiEndpoint;
    }

    public enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS;
    }

    /**
     * Invokes an API Gateway endpoint.
     */
    public HttpExecuteResponse invokeApiEndpoint(
            String url,
            HttpMethod httpMethod,
            Map<String, String> headers,
            String body) throws Exception {
        log.info("Invoking API endpoint - method: {}, url: {}", httpMethod, url);
        try {
            SdkHttpRequest.Builder requestBuilder = SdkHttpRequest.builder()
                    .method(SdkHttpMethod.fromValue(httpMethod.name()))
                    .uri(URI.create(url));

            if (headers != null && !headers.isEmpty()) {
                headers.forEach(requestBuilder::putHeader);
            }

            AwsV4HttpSigner signer = AwsV4HttpSigner.create();
            AwsSessionCredentials credentials = (AwsSessionCredentials) credentialsProvider.resolveCredentials();
            AwsCredentialsIdentity identity = AwsSessionCredentialsIdentity.create(credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken());

            // Sign the request.
            SignedRequest signedRequest;
            if (body != null && !body.isEmpty()) {
                signedRequest = signer.sign(SignRequest.builder(identity)
                        .request(requestBuilder.build())
                        .payload(() -> new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)))
                        .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "execute-api")
                        .putProperty(AwsV4HttpSigner.REGION_NAME, region)
                        .build());
            } else {
                signedRequest = signer.sign(SignRequest.builder(identity)
                        .request(requestBuilder.build())
                        .payload(null)
                        .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "execute-api")
                        .putProperty(AwsV4HttpSigner.REGION_NAME, region)
                        .build());
            }

            HttpExecuteRequest httpExecuteRequest =
            HttpExecuteRequest.builder()
                    .request(signedRequest.request())
                    .contentStreamProvider(signedRequest.payload().orElse(null))
                    .build();
        
            HttpExecuteResponse response = httpClient.prepareRequest(httpExecuteRequest).call();
            log.info("API call completed - status: {}", response.httpResponse().statusCode());
            return response;
        } catch (Exception e) {
            log.error("Failed to invoke API endpoint - method: {}, url: {}", httpMethod, url, e);
            throw e;
        }
    }

    /**
     * Convenience method to invoke a GET endpoint.
     */
    public HttpExecuteResponse invokeGet(
            String url,
            Map<String, String> headers) throws Exception {
        return invokeApiEndpoint(url, HttpMethod.GET, headers, null);
    }

    /**
     * Convenience method to invoke a POST endpoint.
     */
    public HttpExecuteResponse invokePost(
            String url,
            Map<String, String> headers,
            String body) throws Exception {
        return invokeApiEndpoint(url, HttpMethod.POST, headers, body);
    }

    /**
     * Invokes the update-device-shadow API endpoint for the given device ID.
     *
     * @param deviceId The ID of the device to update the shadow of
     * @param headers The HTTP headers to include in the request
     * @param body The request body
     * @return The HTTP response from the API
     * @throws Exception if the API call fails
     */
    public HttpExecuteResponse invokeUpdateDeviceShadow(
            String deviceId,
            Map<String, String> headers,
            String body) throws Exception {
        log.info("Updating shadow for deviceId: {}", deviceId);
        
        String baseUrl = getDmApiEndpoint();
        String url = String.format("%s/update-device-shadow/%s", baseUrl, deviceId);
        try {
            HttpExecuteResponse response = invokePost(url, headers, body);
            log.info("Shadow update initiated - deviceId: {}, status: {}", 
                    deviceId, response.httpResponse().statusCode());
            return response;
        } catch (Exception e) {
            log.error("Failed to update shadow - deviceId: {}", deviceId, e);
            throw e;
        }
    }

    /**
     * Invokes the get-device API endpoint for the given device ID.
     *
     * @param deviceId The ID of the device to get
     * @param headers The HTTP headers to include in the request
     * @param body The request body
     * @return The HTTP response from the API
     * @throws Exception if the API call fails
     */
    public HttpExecuteResponse invokeGetDevice(
            String deviceId,
            Map<String, String> headers,
            String body) throws Exception {
        log.info("Getting device for deviceId: {}", deviceId);
        
        String baseUrl = getDmApiEndpoint();
        String url = String.format("%s/get-device/%s", baseUrl, deviceId);
        try {
            HttpExecuteResponse response = invokePost(url, headers, body);
            log.info("Get device initiated - deviceId: {}, status: {}", 
                    deviceId, response.httpResponse().statusCode());
            return response;
        } catch (Exception e) {
            log.error("Failed to get device - deviceId: {}", deviceId, e);
            throw e;
        }
    }
}
