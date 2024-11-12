package com.amazonaws.videoanalytics.devicemanagement.workflow.createdevice;

import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.IotException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateDeviceHandlerTest {
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

    private CreateDeviceHandler createDeviceHandler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        createDeviceHandler = new CreateDeviceHandler(iotService, startCreateDeviceDAO);
        when(context.getLogger()).thenReturn(logger);
        when(startCreateDeviceDAO.load(TEST_JOB_ID)).thenReturn(CREATE_DEVICE);
    }

    @Test
    public void testRegisterDeviceHandler_success() {
        createDeviceHandler.handleRequest(ImmutableMap.of("jobId", TEST_JOB_ID), context);

        verify(iotService).workflowRegisterDevice(CERTIFICATE_ID, DEVICE_ID);
        verify(startCreateDeviceDAO).save(CREATE_DEVICE);
    }

    @Test
    public void testRegisterDeviceHandler_throwRetryableException() {
        doThrow(InternalFailureException.builder().build())
            .when(iotService).workflowRegisterDevice(CERTIFICATE_ID, DEVICE_ID);

        assertThrows(RuntimeException.class, () -> createDeviceHandler.handleRequest(
                ImmutableMap.of("jobId", TEST_JOB_ID), context));
    }
}
