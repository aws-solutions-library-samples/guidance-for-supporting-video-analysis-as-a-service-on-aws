package com.amazonaws.videoanalytics.devicemanagement.dependency.apig;

import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

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

    public enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS;
    }

    private final SdkHttpClient httpClient;

    @Inject
    public ApigService(@Named(HTTP_CLIENT) final SdkHttpClient httpClient) {
        this.httpClient = httpClient;
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
        
        String baseUrl = System.getenv("VlApiEndpoint");
        if (baseUrl == null || baseUrl.isEmpty()) {
            log.error("VlApiEndpoint environment variable is not set");
            throw new IllegalStateException("VlApiEndpoint environment variable is not set");
        }

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
        
        String baseUrl = System.getenv("VlApiEndpoint");
        if (baseUrl == null || baseUrl.isEmpty()) {
            log.error("VlApiEndpoint environment variable is not set");
            throw new IllegalStateException("VlApiEndpoint environment variable is not set");
        }

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