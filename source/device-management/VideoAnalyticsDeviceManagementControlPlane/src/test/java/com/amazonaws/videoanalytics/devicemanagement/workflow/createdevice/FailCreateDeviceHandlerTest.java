package com.amazonaws.videoanalytics.devicemanagement.workflow.createdevice;

import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import com.amazonaws.videoanalytics.devicemanagement.Status;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.CertificateStatus;
import software.amazon.awssdk.services.iot.model.InternalFailureException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FailCreateDeviceHandlerTest {
    private static final String TEST_JOB_ID = "job id";
    private static final String DEVICE_ID = "deviceId";
    private static final String CERTIFICATE_ID = "certId";
    private static final String FAILURE_REASON = "failureReason";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String ERROR = "error";
    private static final Map<String, String> ERROR_MESSAGE_MAP = ImmutableMap.of(ERROR_MESSAGE, ERROR);

    @Mock
    private IotService iotService;
    @Mock
    private StartCreateDeviceDAO startCreateDeviceDAO;
    @Mock
    private Context context;
    @Mock
    private LambdaLogger logger;
    @Mock
    private ObjectMapper objectMapper;

    private FailCreateDeviceHandler failCreateDeviceHandler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        failCreateDeviceHandler = new FailCreateDeviceHandler(iotService, startCreateDeviceDAO, objectMapper);
        when(context.getLogger()).thenReturn(logger);
        when(startCreateDeviceDAO.load(TEST_JOB_ID)).thenReturn(CreateDevice.builder()
                .deviceId(DEVICE_ID)
                .certificateId(CERTIFICATE_ID)
                .jobStatus(Status.RUNNING.toString())
                .errorMessage(StringUtils.EMPTY)
                .build());
    }

    @Test
    public void handleRequest_withValidErrorMessage_updatesDeviceAndCertificate() throws JsonProcessingException {
        when(objectMapper.readValue(ERROR_MESSAGE_MAP.toString(), Map.class)).thenReturn(ERROR_MESSAGE_MAP);

        failCreateDeviceHandler.handleRequest(ImmutableMap.of("jobId", TEST_JOB_ID,
                FAILURE_REASON, ERROR_MESSAGE_MAP.toString()), context);

        verify(iotService).updateCertificate(CERTIFICATE_ID, CertificateStatus.INACTIVE);
        verify(startCreateDeviceDAO).save(CreateDevice.builder()
                .deviceId(DEVICE_ID)
                .certificateId(CERTIFICATE_ID)
                .jobStatus(Status.FAILED.toString())
                .errorMessage(ERROR)
                .build());
    }

    @Test
    public void handleRequest_withInvalidJson_savesEmptyErrorMessage() throws JsonProcessingException {
        when(objectMapper.readValue(any(String.class), eq(Map.class))).thenThrow(JsonProcessingException.class);

        failCreateDeviceHandler.handleRequest(ImmutableMap.of("jobId", TEST_JOB_ID,
                FAILURE_REASON, ERROR_MESSAGE_MAP.toString()), context);

        CreateDevice expectCreateDevice = CreateDevice.builder()
                .deviceId(DEVICE_ID)
                .certificateId(CERTIFICATE_ID)
                .jobStatus(Status.FAILED.toString())
                .errorMessage(StringUtils.EMPTY)
                .build();

        verify(iotService).updateCertificate(CERTIFICATE_ID, CertificateStatus.INACTIVE);
        verify(startCreateDeviceDAO).save(expectCreateDevice);
    }

    @Test
    public void handleRequest_withoutFailureMessage_savesEmptyErrorMessage() {
        failCreateDeviceHandler.handleRequest(ImmutableMap.of("jobId", TEST_JOB_ID), context);

        CreateDevice expectCreateDevice = CreateDevice.builder()
                .deviceId(DEVICE_ID)
                .certificateId(CERTIFICATE_ID)
                .jobStatus(Status.FAILED.toString())
                .errorMessage(StringUtils.EMPTY)
                .build();

        verify(iotService).updateCertificate(CERTIFICATE_ID, CertificateStatus.INACTIVE);
        verify(startCreateDeviceDAO).save(expectCreateDevice);
    }

    @Test
    public void handleRequest_whenIotServiceFails_throwsRuntimeException() {
        doThrow(InternalFailureException.builder().build())
            .when(iotService).updateCertificate(CERTIFICATE_ID, CertificateStatus.INACTIVE);

        assertThrows(RuntimeException.class, () -> failCreateDeviceHandler.handleRequest(
                ImmutableMap.of("jobId", TEST_JOB_ID), context)
        );
    }
}
