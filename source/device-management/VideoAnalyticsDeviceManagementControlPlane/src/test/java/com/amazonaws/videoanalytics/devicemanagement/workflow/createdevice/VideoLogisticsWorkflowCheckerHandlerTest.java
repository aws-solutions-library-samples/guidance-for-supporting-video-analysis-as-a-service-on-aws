package com.amazonaws.videoanalytics.devicemanagement.workflow.createdevice;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.dependency.ddb.DDBService;
import com.amazonaws.videoanalytics.devicemanagement.dependency.apig.ApigService;
import com.amazonaws.videoanalytics.devicemanagement.exceptions.RetryableException;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.AbortableInputStream;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

public class VideoLogisticsWorkflowCheckerHandlerTest {
    private VideoLogisticsWorkflowCheckerHandler handler;
    
    @Mock
    private DDBService ddbService;
    
    @Mock
    private StartCreateDeviceDAO startCreateDeviceDAO;
    
    @Mock
    private ApigService apigService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private Context context;
    
    @Mock
    private LambdaLogger logger;
    
    private static final String JOB_ID = "test-job-id";
    private static final String MOCK_AWS_REGION = "us-west-2";
    private static final String MOCK_AWS_STAGE = "Prod";
    
    @Rule
    private final EnvironmentVariables environmentVariables = new EnvironmentVariables();
    
    private CreateDevice createDevice;
    
    private static final Map<String, Object> requestMap = Map.ofEntries(
            entry("jobId", JOB_ID)
    );

    @BeforeEach
    public void setup() {
        environmentVariables.set("AWS_REGION", MOCK_AWS_REGION);
        environmentVariables.set("Stage", MOCK_AWS_STAGE);
        environmentVariables.set("VlApiEndpoint", "https://mock-api-endpoint.com");
        MockitoAnnotations.initMocks(this);
        
        handler = new VideoLogisticsWorkflowCheckerHandler(
            ddbService, 
            startCreateDeviceDAO,
            apigService,
            objectMapper
        );
        when(context.getLogger()).thenReturn(logger);
        
        createDevice = CreateDevice.builder()
                .vlJobId(JOB_ID)
                .build();
    }

    @Test
    public void handleRequest_CompletedState_ReturnsNull() throws Exception {
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(createDevice);
        
        HttpExecuteResponse mockHttpResponse = mock(HttpExecuteResponse.class, RETURNS_DEEP_STUBS);
        String responseJson = "{\"status\":\"COMPLETED\"}";
        AbortableInputStream responseStream = AbortableInputStream.create(
            new ByteArrayInputStream(responseJson.getBytes(StandardCharsets.UTF_8))
        );
        when(mockHttpResponse.responseBody()).thenReturn(Optional.of(responseStream));
        
        when(apigService.invokeGetVlRegisterDeviceStatus(
            eq(JOB_ID),
            isNull(),
            isNull()
        )).thenReturn(mockHttpResponse);
        
        JsonNode mockJsonNode = mock(JsonNode.class);
        JsonNode statusNode = new TextNode("COMPLETED");
        when(mockJsonNode.get("status")).thenReturn(statusNode);
        when(objectMapper.readTree(anyString())).thenReturn(mockJsonNode);

        Map<String, Object> result = handler.handleRequest(requestMap, context);

        assertNull(result);
        verify(startCreateDeviceDAO).load(JOB_ID);
        verify(apigService).invokeGetVlRegisterDeviceStatus(
            eq(JOB_ID),
            isNull(),
            isNull()
        );
    }

    @Test
    public void handleRequest_NullJobId_ThrowsInvalidRequestException() {
        Map<String, Object> invalidRequest = Map.of();

        assertThrows(InvalidRequestException.class, () -> 
            handler.handleRequest(invalidRequest, context)
        );
    }


    @Test
    public void handleRequest_DaoThrowsException_ThrowsInternalFailureException() {
        when(startCreateDeviceDAO.load(JOB_ID)).thenThrow(new RuntimeException("DAO Error"));

        assertThrows(InternalFailureException.class, () -> 
            handler.handleRequest(requestMap, context)
        );
    }

    @Test
    public void handleRequest_InProgressState_ThrowsRetryableException() throws Exception {
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(createDevice);
        
        HttpExecuteResponse mockHttpResponse = mock(HttpExecuteResponse.class, RETURNS_DEEP_STUBS);
        String responseJson = "{\"status\":\"IN_PROGRESS\"}";
        AbortableInputStream responseStream = AbortableInputStream.create(
            new ByteArrayInputStream(responseJson.getBytes(StandardCharsets.UTF_8))
        );
        when(mockHttpResponse.responseBody()).thenReturn(Optional.of(responseStream));
        
        when(apigService.invokeGetVlRegisterDeviceStatus(
            eq(JOB_ID),
            isNull(),
            isNull()
        )).thenReturn(mockHttpResponse);
        
        JsonNode mockJsonNode = mock(JsonNode.class);
        when(mockJsonNode.get("status")).thenReturn(new TextNode("IN_PROGRESS"));
        when(objectMapper.readTree(anyString())).thenReturn(mockJsonNode);

        assertThrows(RetryableException.class, () -> 
            handler.handleRequest(requestMap, context)
        );
    }

    @Test
    public void handleRequest_PendingState_ThrowsRetryableException() throws Exception {
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(createDevice);
        
        HttpExecuteResponse mockHttpResponse = mock(HttpExecuteResponse.class, RETURNS_DEEP_STUBS);
        String responseJson = "{\"status\":\"PENDING\"}";
        AbortableInputStream responseStream = AbortableInputStream.create(
            new ByteArrayInputStream(responseJson.getBytes(StandardCharsets.UTF_8))
        );
        when(mockHttpResponse.responseBody()).thenReturn(Optional.of(responseStream));
        
        when(apigService.invokeGetVlRegisterDeviceStatus(
            eq(JOB_ID),
            isNull(),
            isNull()
        )).thenReturn(mockHttpResponse);
        
        JsonNode mockJsonNode = mock(JsonNode.class);
        when(mockJsonNode.get("status")).thenReturn(new TextNode("PENDING"));
        when(objectMapper.readTree(anyString())).thenReturn(mockJsonNode);

        assertThrows(RetryableException.class, () -> 
            handler.handleRequest(requestMap, context)
        );
    }

    @Test
    public void handleRequest_FailedState_ThrowsInternalFailureException() throws Exception {
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(createDevice);
        
        HttpExecuteResponse mockHttpResponse = mock(HttpExecuteResponse.class, RETURNS_DEEP_STUBS);
        String responseJson = "{\"status\":\"FAILED\"}";
        AbortableInputStream responseStream = AbortableInputStream.create(
            new ByteArrayInputStream(responseJson.getBytes(StandardCharsets.UTF_8))
        );
        when(mockHttpResponse.responseBody()).thenReturn(Optional.of(responseStream));
        
        when(apigService.invokeGetVlRegisterDeviceStatus(
            eq(JOB_ID),
            isNull(),
            isNull()
        )).thenReturn(mockHttpResponse);
        
        JsonNode mockJsonNode = mock(JsonNode.class);
        when(mockJsonNode.get("status")).thenReturn(new TextNode("FAILED"));
        when(objectMapper.readTree(anyString())).thenReturn(mockJsonNode);

        assertThrows(InternalFailureException.class, () -> 
            handler.handleRequest(requestMap, context)
        );
    }

    @Test
    public void handleRequest_UnknownState_ThrowsInvalidRequestException() throws Exception {
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(createDevice);
        
        HttpExecuteResponse mockHttpResponse = mock(HttpExecuteResponse.class, RETURNS_DEEP_STUBS);
        String responseJson = "{\"status\":\"UNKNOWN\"}";
        AbortableInputStream responseStream = AbortableInputStream.create(
            new ByteArrayInputStream(responseJson.getBytes(StandardCharsets.UTF_8))
        );
        when(mockHttpResponse.responseBody()).thenReturn(Optional.of(responseStream));
        
        when(apigService.invokeGetVlRegisterDeviceStatus(
            eq(JOB_ID),
            isNull(),
            isNull()
        )).thenReturn(mockHttpResponse);
        
        JsonNode mockJsonNode = mock(JsonNode.class);
        when(mockJsonNode.get("status")).thenReturn(new TextNode("UNKNOWN"));
        when(objectMapper.readTree(anyString())).thenReturn(mockJsonNode);

        assertThrows(InvalidRequestException.class, () -> 
            handler.handleRequest(requestMap, context)
        );
    }

    @Test
    public void handleRequest_NullVlJobId_ThrowsInvalidRequestException() {
        CreateDevice deviceWithNullVlJobId = CreateDevice.builder().build();
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(deviceWithNullVlJobId);

        assertThrows(InvalidRequestException.class, () -> 
            handler.handleRequest(requestMap, context)
        );
    }

    @Test
    public void handleRequest_ApiServiceThrowsException_ThrowsInternalFailureException() throws Exception {
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(createDevice);
        when(apigService.invokeGetVlRegisterDeviceStatus(
            eq(JOB_ID),
            isNull(),
            isNull()
        )).thenThrow(new RuntimeException("API Error"));

        assertThrows(InternalFailureException.class, () -> 
            handler.handleRequest(requestMap, context)
        );
    }
}