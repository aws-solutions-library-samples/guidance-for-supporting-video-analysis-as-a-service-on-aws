package com.amazonaws.videoanalytics.devicemanagement.workflow.createdevice;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.dependency.ddb.DDBService;
import com.amazonaws.videoanalytics.devicemanagement.dependency.apig.ApigService;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.AbortableInputStream;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CreateKVSStreamHandlerTest {
    private CreateKVSStreamHandler handler;
    
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
    private static final String DEVICE_ID = "test-device-id";
    private static final String VL_JOB_ID = "vl-job-id";
    
    private CreateDevice createDevice;
    private Map<String, Object> requestMap;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        
        handler = new CreateKVSStreamHandler(
            ddbService, 
            startCreateDeviceDAO,
            apigService,
            objectMapper
        );
        
        when(context.getLogger()).thenReturn(logger);
        
        createDevice = CreateDevice.builder()
                .deviceId(DEVICE_ID)
                .build();
                
        requestMap = Map.of("jobId", JOB_ID);
    }

    @Test
    public void handleRequest_ValidRequest_ReturnsNull() throws Exception {
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(createDevice);
        
        HttpExecuteResponse mockResponse = mock(HttpExecuteResponse.class, RETURNS_DEEP_STUBS);
        String responseJson = String.format("{\"jobId\":\"%s\"}", VL_JOB_ID);
        AbortableInputStream responseStream = AbortableInputStream.create(
            new ByteArrayInputStream(responseJson.getBytes(StandardCharsets.UTF_8))
        );
        when(mockResponse.responseBody()).thenReturn(Optional.of(responseStream));
        when(mockResponse.httpResponse().statusCode()).thenReturn(200);
        
        when(apigService.invokeStartVlRegisterDevice(
            eq(DEVICE_ID),
            isNull(),
            isNull()
        )).thenReturn(mockResponse);
        
        JsonNode mockJsonNode = mock(JsonNode.class);
        when(mockJsonNode.get("jobId")).thenReturn(new TextNode(VL_JOB_ID));
        when(objectMapper.readTree(anyString())).thenReturn(mockJsonNode);

        Void result = handler.handleRequest(requestMap, context);

        assertNull(result);
        verify(startCreateDeviceDAO).load(JOB_ID);
        verify(apigService).invokeStartVlRegisterDevice(eq(DEVICE_ID), isNull(), isNull());
        verify(startCreateDeviceDAO).save(argThat(device -> 
            VL_JOB_ID.equals(device.getVlJobId()) && DEVICE_ID.equals(device.getDeviceId())
        ));
    }

    @Test
    public void handleRequest_MissingJobId_ThrowsRuntimeExceptionWithIllegalArgumentCause() {
        Map<String, Object> emptyRequest = Map.of();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            handler.handleRequest(emptyRequest, context)
        );
        
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertEquals("Job ID is required", exception.getCause().getMessage());
    }

    @Test
    public void handleRequest_EmptyJobId_ThrowsRuntimeExceptionWithIllegalArgumentCause() {
        Map<String, Object> requestWithEmptyJobId = Map.of("jobId", "");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            handler.handleRequest(requestWithEmptyJobId, context)
        );
        
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertEquals("Job ID is required", exception.getCause().getMessage());
    }

    @Test
    public void handleRequest_DaoLoadThrowsException_ThrowsRuntimeException() {
        when(startCreateDeviceDAO.load(JOB_ID)).thenThrow(new RuntimeException("DAO Error"));

        assertThrows(RuntimeException.class, () -> 
            handler.handleRequest(requestMap, context)
        );
    }

    @Test
    public void handleRequest_ApiServiceThrowsException_ThrowsRuntimeException() throws Exception {
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(createDevice);
        when(apigService.invokeStartVlRegisterDevice(
            eq(DEVICE_ID),
            isNull(),
            isNull()
        )).thenThrow(new RuntimeException("API Error"));

        assertThrows(RuntimeException.class, () -> 
            handler.handleRequest(requestMap, context)
        );
    }

    @Test
    public void handleRequest_JsonParsingFails_ThrowsRuntimeException() throws Exception {
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(createDevice);
        
        HttpExecuteResponse mockResponse = mock(HttpExecuteResponse.class, RETURNS_DEEP_STUBS);
        String responseJson = "invalid json";
        AbortableInputStream responseStream = AbortableInputStream.create(
            new ByteArrayInputStream(responseJson.getBytes(StandardCharsets.UTF_8))
        );
        when(mockResponse.responseBody()).thenReturn(Optional.of(responseStream));
        
        when(apigService.invokeStartVlRegisterDevice(
            eq(DEVICE_ID),
            isNull(),
            isNull()
        )).thenReturn(mockResponse);
        
        when(objectMapper.readTree(anyString())).thenThrow(new RuntimeException("JSON Parse Error"));

        assertThrows(RuntimeException.class, () -> 
            handler.handleRequest(requestMap, context)
        );
    }

    @Test
    public void handleRequest_DaoSaveThrowsException_ThrowsRuntimeException() throws Exception {
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(createDevice);
        
        HttpExecuteResponse mockResponse = mock(HttpExecuteResponse.class, RETURNS_DEEP_STUBS);
        String responseJson = String.format("{\"jobId\":\"%s\"}", VL_JOB_ID);
        AbortableInputStream responseStream = AbortableInputStream.create(
            new ByteArrayInputStream(responseJson.getBytes(StandardCharsets.UTF_8))
        );
        when(mockResponse.responseBody()).thenReturn(Optional.of(responseStream));
        
        when(apigService.invokeStartVlRegisterDevice(
            eq(DEVICE_ID),
            isNull(),
            isNull()
        )).thenReturn(mockResponse);
        
        JsonNode mockJsonNode = mock(JsonNode.class);
        when(mockJsonNode.get("jobId")).thenReturn(new TextNode(VL_JOB_ID));
        when(objectMapper.readTree(anyString())).thenReturn(mockJsonNode);
        
        doThrow(new RuntimeException("Save Error")).when(startCreateDeviceDAO).save(any(CreateDevice.class));

        assertThrows(RuntimeException.class, () -> 
            handler.handleRequest(requestMap, context)
        );
    }
}



