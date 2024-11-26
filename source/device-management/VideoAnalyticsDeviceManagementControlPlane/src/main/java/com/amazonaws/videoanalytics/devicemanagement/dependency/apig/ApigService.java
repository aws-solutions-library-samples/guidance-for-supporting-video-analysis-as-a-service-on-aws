package com.amazonaws.videoanalytics.devicemanagement.dependency.apig;

import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.GetRestApisRequest;
import software.amazon.awssdk.services.apigateway.model.RestApi;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.HTTP_CLIENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ApigService {
    private static final Logger log = LoggerFactory.getLogger(ApigService.class);
    private static final String VL_API_NAME = System.getProperty("VIDEO_LOGISTICS_API_NAME", System.getenv("VIDEO_LOGISTICS_API_NAME"));
    private final SdkHttpClient httpClient;
    private final ApiGatewayClient apiGatewayClient;
    private String vlApiEndpoint;

    @Inject
    public ApigService(
            @Named(HTTP_CLIENT) final SdkHttpClient httpClient,
            ApiGatewayClient apiGatewayClient) {
        this.httpClient = httpClient;
        this.apiGatewayClient = apiGatewayClient;
    }

    private String getVlApiEndpoint() {
        if (vlApiEndpoint == null) {
            log.info("Fetching VL API endpoint URL for API: {}", VL_API_NAME);
            try {
                var restApis = apiGatewayClient.getRestApis(GetRestApisRequest.builder().build());
                log.info("GetRestApis Response - Total APIs: {}, Position: {}", 
                    restApis.items().size(),
                    restApis.position());
                String apiId = restApis.items().stream()
                    .peek(api -> log.info("Found API: name={}, id={}", api.name(), api.id()))
                    .filter(api -> VL_API_NAME.equals(api.name()))
                    .map(RestApi::id)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Could not find API Gateway with name: " + VL_API_NAME));

                String region = System.getenv("AWS_REGION");
                String stage = "prod"; 
                vlApiEndpoint = String.format("https://%s.execute-api.%s.amazonaws.com/%s",
                    apiId, region, stage);
                
                log.info("Found VL API endpoint: {}", vlApiEndpoint);
            } catch (Exception e) {
                log.error("Failed to fetch VL API endpoint", e);
                throw new IllegalStateException("Failed to fetch VL API endpoint", e);
            }
        }
        return vlApiEndpoint;
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

            HttpExecuteRequest.Builder executeRequestBuilder = HttpExecuteRequest.builder()
                    .request(requestBuilder.build());

            if (body != null && !body.isEmpty()) {
                executeRequestBuilder.contentStreamProvider(() -> 
                    new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
            }

            HttpExecuteResponse response = httpClient.prepareRequest(executeRequestBuilder.build()).call();
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
     * Invokes the start-vl-register-device API endpoint for the given device ID.
     *
     * @param deviceId The ID of the device to register
     * @param headers The HTTP headers to include in the request
     * @param body The request body
     * @return The HTTP response from the API
     * @throws Exception if the API call fails
     */
    public HttpExecuteResponse invokeStartVlRegisterDevice(
            String deviceId,
            Map<String, String> headers,
            String body) throws Exception {
        log.info("Starting VL device registration for deviceId: {}", deviceId);
        
        String baseUrl = getVlApiEndpoint();
        String url = String.format("%s/start-vl-register-device/%s", baseUrl, deviceId);
        try {
            HttpExecuteResponse response = invokePost(url, headers, body);
            log.info("VL device registration initiated - deviceId: {}, status: {}", 
                    deviceId, response.httpResponse().statusCode());
            return response;
        } catch (Exception e) {
            log.error("Failed to start VL device registration - deviceId: {}", deviceId, e);
            throw e;
        }
    }

    /**
     * Invokes the get-vl-register-device-status API endpoint for the given job ID.
     *
     * @param jobId The ID of the registration job to check
     * @param headers The HTTP headers to include in the request
     * @param body The request body
     * @return The HTTP response from the API
     * @throws Exception if the API call fails
     */
    public HttpExecuteResponse invokeGetVlRegisterDeviceStatus(
            String jobId,
            Map<String, String> headers,
            String body) throws Exception {
        log.info("Checking VL device registration status for jobId: {}", jobId);
        
        String baseUrl = getVlApiEndpoint();
        String url = String.format("%s/get-vl-register-device-status/%s", baseUrl, jobId);
        try {
            HttpExecuteResponse response = invokePost(url, headers, body);
            log.info("Retrieved VL device registration status - jobId: {}, status: {}", 
                    jobId, response.httpResponse().statusCode());
            return response;
        } catch (Exception e) {
            log.error("Failed to get VL device registration status - jobId: {}", jobId, e);
            throw e;
        }
    }
}