package com.amazonaws.videoanalytics.devicemanagement.dependency.apig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApigServiceTest {
    private static final String TEST_URL = "https://api.example.com/test";
    private static final String TEST_BODY = "{\"key\":\"value\"}";
    
    @Mock
    private SdkHttpClient httpClient;
    
    private ApigService apigService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        apigService = new ApigService(httpClient);
    }

    @Test
    void testInvokeGet() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        HttpExecuteResponse mockResponse = createMockResponse(200);
        mockHttpClientCall(mockResponse);

        HttpExecuteResponse response = apigService.invokeGet(TEST_URL, headers);

        assertNotNull(response);
        assertEquals(200, response.httpResponse().statusCode());
        verify(httpClient).prepareRequest(any(HttpExecuteRequest.class));
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