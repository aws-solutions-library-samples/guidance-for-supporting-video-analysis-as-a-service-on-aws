package com.amazonaws.videoanalytics.devicemanagement.dependency.apig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.GetRestApisRequest;
import software.amazon.awssdk.services.apigateway.model.GetRestApisResponse;
import software.amazon.awssdk.services.apigateway.model.RestApi;

public class ApigServiceTest {
    private static final String TEST_URL = "https://api.example.com/test";
    private static final String TEST_BODY = "{\"key\":\"value\"}";
    private static final String TEST_API_NAME = "test-api";
    private static final String TEST_API_ID = "abc123";
    private static final String TEST_DEVICE_ID = "device-123";
    private static final String TEST_JOB_ID = "job-123";
    private static final String TEST_REGION = "us-west-2";
    
    @Mock
    private SdkHttpClient httpClient;
    
    @Mock
    private ApiGatewayClient apiGatewayClient;

    @Mock
    private AwsCredentialsProvider credentialsProvider;
    
    private ApigService apigService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        System.clearProperty("VIDEO_LOGISTICS_API_NAME");
        System.setProperty("VIDEO_LOGISTICS_API_NAME", TEST_API_NAME);
        
        apigService = new ApigService(httpClient, credentialsProvider, TEST_REGION, apiGatewayClient);
        
        // Mock API Gateway response
        RestApi mockApi = RestApi.builder()
            .name(TEST_API_NAME)
            .id(TEST_API_ID)
            .build();

        GetRestApisResponse mockResponse = GetRestApisResponse.builder()
            .items(List.of(mockApi))
            .build();

        when(apiGatewayClient.getRestApis(any(GetRestApisRequest.class)))
            .thenReturn(mockResponse);

        when(credentialsProvider.resolveCredentials()).thenReturn(AwsSessionCredentials.create("accessKeyId", "secretAccessKey", "sessionToken"));

        // Mock HTTP client response for all tests
        HttpExecuteResponse mockHttpResponse = createMockResponse(200);
        try {
            mockHttpClientCall(mockHttpResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("VIDEO_LOGISTICS_API_NAME");
    }

    @Test
    void testInvokePost() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        HttpExecuteResponse mockResponse = createMockResponse(201);
        mockHttpClientCall(mockResponse);

        HttpExecuteResponse response = apigService.invokePost(TEST_URL, headers, TEST_BODY);

        assertNotNull(response);
        assertEquals(201, response.httpResponse().statusCode());
        verify(httpClient).prepareRequest(any(HttpExecuteRequest.class));
    }

    @Test
    void testInvokeApiEndpoint() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        
        HttpExecuteResponse mockResponse = createMockResponse(200);
        mockHttpClientCall(mockResponse);

        HttpExecuteResponse response = apigService.invokeApiEndpoint(
            TEST_URL,
            ApigService.HttpMethod.PUT,
            headers,
            TEST_BODY
        );

        assertNotNull(response);
        assertEquals(200, response.httpResponse().statusCode());
        verify(httpClient).prepareRequest(any(HttpExecuteRequest.class));
    }

    @Test
    void testInvokeStartVlRegisterDevice() throws Exception {
        // Prepare test data
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");

        // Execute test
        HttpExecuteResponse response = apigService.invokeStartVlRegisterDevice(TEST_DEVICE_ID, headers, TEST_BODY);

        // Verify
        assertNotNull(response);
        assertEquals(200, response.httpResponse().statusCode());
        verify(httpClient).prepareRequest(any(HttpExecuteRequest.class));
    }

    @Test
    void testInvokeGetVlRegisterDeviceStatus() throws Exception {
        // Prepare test data
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");

        // Execute test
        HttpExecuteResponse response = apigService.invokeGetVlRegisterDeviceStatus(TEST_JOB_ID, headers, TEST_BODY);

        // Verify
        assertNotNull(response);
        assertEquals(200, response.httpResponse().statusCode());
        verify(httpClient).prepareRequest(any(HttpExecuteRequest.class));
    }

    @Test
    void testApiGatewayClientFailure() {
        when(apiGatewayClient.getRestApis(any(GetRestApisRequest.class)))
            .thenThrow(new RuntimeException("API Gateway error"));

        Map<String, String> headers = new HashMap<>();
        assertThrows(IllegalStateException.class, () -> 
            apigService.invokeStartVlRegisterDevice(TEST_DEVICE_ID, headers, TEST_BODY));
    }

    private HttpExecuteResponse createMockResponse(int statusCode) {
        SdkHttpFullResponse httpResponse = SdkHttpFullResponse.builder()
            .statusCode(statusCode)
            .build();
        
        return HttpExecuteResponse.builder()
            .response(httpResponse)
            .responseBody(AbortableInputStream.create(new ByteArrayInputStream(new byte[0])))
            .build();
    }

    private void mockHttpClientCall(HttpExecuteResponse mockResponse) throws IOException {
        ExecutableHttpRequest executableRequest = mock(ExecutableHttpRequest.class);
        when(executableRequest.call()).thenReturn(mockResponse);
        when(httpClient.prepareRequest(any(HttpExecuteRequest.class))).thenReturn(executableRequest);
    }
}
