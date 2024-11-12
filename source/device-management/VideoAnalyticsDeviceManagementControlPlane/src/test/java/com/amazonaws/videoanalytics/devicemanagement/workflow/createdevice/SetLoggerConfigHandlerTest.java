package com.amazonaws.videoanalytics.devicemanagement.workflow.createdevice;

import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import com.amazonaws.videoanalytics.devicemanagement.Status;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.InternalFailureException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SetLoggerConfigHandlerTest {
    private static final String TEST_JOB_ID = "job id";
    private static final String DEVICE_ID = "deviceId";
    private static final String CERTIFICATE_ID = "certId";
    private static final CreateDevice CREATE_DEVICE = CreateDevice.builder()
            .deviceId(DEVICE_ID)
            .certificateId(CERTIFICATE_ID)
            .build();

    @Mock
    private IotService iotService;
    @Mock
    private StartCreateDeviceDAO startCreateDeviceDAO;
    @Mock
    private Context context;
    @Mock
    private LambdaLogger logger;

    private SetLoggerConfigHandler setLoggerConfigHandler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        setLoggerConfigHandler = new SetLoggerConfigHandler(iotService, startCreateDeviceDAO);
        when(context.getLogger()).thenReturn(logger);
        when(startCreateDeviceDAO.load(TEST_JOB_ID)).thenReturn(CREATE_DEVICE);
    }

    @Test
    public void testPublishDefaultLogConfigToProvisionShadowHandler_success() {
        setLoggerConfigHandler.handleRequest(ImmutableMap.of("jobId", TEST_JOB_ID), context);

        verify(iotService).publishLogConfigurationToProvisioningShadow(DEVICE_ID);
        
        CreateDevice expectedDevice = CreateDevice.builder()
                .deviceId(DEVICE_ID)
                .certificateId(CERTIFICATE_ID)
                .jobStatus(Status.COMPLETED.toString())
                .build();
        verify(startCreateDeviceDAO).save(expectedDevice);
    }

    @Test
    public void testPublishDefaultLogConfigToProvisionShadowHandler_throwRetryableException() {
        doThrow(InternalFailureException.builder().build())
            .when(iotService).publishLogConfigurationToProvisioningShadow(DEVICE_ID);

        assertThrows(RuntimeException.class, () -> setLoggerConfigHandler.handleRequest(
                ImmutableMap.of("jobId", TEST_JOB_ID), context));
    }
}
