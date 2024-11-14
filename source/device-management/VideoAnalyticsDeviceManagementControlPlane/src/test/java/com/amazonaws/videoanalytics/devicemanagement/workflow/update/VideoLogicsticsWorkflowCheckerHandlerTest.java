package com.amazonaws.videoanalytics.devicemanagement.workflow.update;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.dependency.ddb.DDBService;
import com.amazonaws.videoanalytics.devicemanagement.exceptions.RetryableException;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;

import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VideoLogicsticsWorkflowCheckerHandlerTest {
    private VideoLogicsticsWorkflowCheckerHandler handler;
    
    @Mock
    private DDBService ddbService;
    
    @Mock
    private StartCreateDeviceDAO startCreateDeviceDAO;
    
    @Mock
    private Context context;
    
    @Mock
    private LambdaLogger logger;
    
    private static final String JOB_ID = "test-job-id";
    private static final String MOCK_AWS_REGION = "us-west-2";
    private static final String MOCK_AWS_STAGE = "Dev";
    
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
        MockitoAnnotations.initMocks(this);
        
        handler = new VideoLogicsticsWorkflowCheckerHandler(ddbService, startCreateDeviceDAO);
        when(context.getLogger()).thenReturn(logger);
        
        createDevice = CreateDevice.builder()
                .vlJobId(JOB_ID)
                .build();
    }

    @Test
    public void handleRequest_CompletedState_ReturnsNull() {
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(createDevice);

        Map<String, Object> result = handler.handleRequest(requestMap, context);

        assertNull(result);
        verify(startCreateDeviceDAO).load(JOB_ID);
    }

    @Test
    public void handleRequest_NullJobId_ThrowsInvalidRequestException() {
        Map<String, Object> invalidRequest = Map.of();

        assertThrows(InvalidRequestException.class, () -> 
            handler.handleRequest(invalidRequest, context)
        );
    }

    @Test
    public void handleRequest_NullVlJobId_ThrowsInvalidRequestException() {
        createDevice.setVlJobId(null);
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(createDevice);

        assertThrows(InvalidRequestException.class, () -> 
            handler.handleRequest(requestMap, context)
        );
    }

    @Test
    public void handleRequest_DaoThrowsException_ThrowsInternalFailureException() {
        when(startCreateDeviceDAO.load(JOB_ID)).thenThrow(new RuntimeException("DAO Error"));

        assertThrows(InternalFailureException.class, () -> 
            handler.handleRequest(requestMap, context)
        );
    }
}