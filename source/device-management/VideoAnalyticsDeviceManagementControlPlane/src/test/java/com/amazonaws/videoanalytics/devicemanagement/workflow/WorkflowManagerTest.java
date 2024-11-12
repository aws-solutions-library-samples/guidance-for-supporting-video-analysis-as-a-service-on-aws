package com.amazonaws.videoanalytics.devicemanagement.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import com.amazonaws.videoanalytics.devicemanagement.workflow.data.CreateDeviceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.ConflictException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class WorkflowManagerTest {
    private static final String JOB_ID = "jobId";
    private static final String DEVICE_ID = "deviceId";
    private static final String CERTIFICATE_ID = "certId";
    private static final Instant TIME_NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private static final CreateDevice CREATE_DEVICE = CreateDevice.builder()
            .deviceId(DEVICE_ID)
            .jobId(JOB_ID)
            .createdAt(TIME_NOW)
            .lastUpdated(TIME_NOW)
            .certificateId(CERTIFICATE_ID)
            .jobStatus("RUNNING")
            .workflowName("workflow name")
            .build();

    @Mock
    private StartCreateDeviceDAO startCreateDeviceDAO;

    @Mock
    private IotService iotService;

    private WorkflowManager workflowManager;

    private final ArgumentCaptor<CreateDevice> createDeviceArgumentCaptor = ArgumentCaptor.forClass(CreateDevice.class);

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        workflowManager = new WorkflowManager(startCreateDeviceDAO, iotService);
    }

    @Test
    public void startCreateDevice_returnResult() throws ConflictException, ResourceNotFoundException {
        when(iotService.isAnExistingDevice(DEVICE_ID)).thenReturn(false);

        String jobId = workflowManager.startCreateDevice(DEVICE_ID, CERTIFICATE_ID);

        verify(startCreateDeviceDAO).save(createDeviceArgumentCaptor.capture());
        CreateDevice createDevice = createDeviceArgumentCaptor.getValue();
        assertEquals(DEVICE_ID, createDevice.getDeviceId());
        assertEquals(CERTIFICATE_ID, createDevice.getCertificateId());
        assertEquals("RUNNING", createDevice.getJobStatus());
        assertEquals(jobId, createDevice.getJobId());
        assertTrue(isValidUUID(createDevice.getWorkflowName()));
        assertTrue(isValidUUID(jobId));
    }

    @Test
    public void startCreateDevice_deviceAlreadyExists() {
        when(iotService.isAnExistingDevice(DEVICE_ID)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> workflowManager.startCreateDevice(DEVICE_ID, CERTIFICATE_ID));

        assertEquals("Device already exists: " + DEVICE_ID, exception.getMessage());
    }

    @Test
    public void startCreateDevice_certificateNotFound() {
        when(iotService.isAnExistingDevice(DEVICE_ID)).thenReturn(false);
        when(iotService.getCertificate(CERTIFICATE_ID))
                .thenThrow(ResourceNotFoundException.builder().build());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> workflowManager.startCreateDevice(DEVICE_ID, CERTIFICATE_ID));

        assertEquals("Certificate not found: " + CERTIFICATE_ID, exception.getMessage());
    }

    @Test
    public void getCreateDeviceStatus_returnResult() throws ResourceNotFoundException {
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(CREATE_DEVICE);

        CreateDeviceData createDeviceData = workflowManager.getCreateDeviceStatus(JOB_ID);

        assertEquals(DEVICE_ID, createDeviceData.getDeviceId());
        assertEquals("RUNNING", createDeviceData.getStatus());
        assertEquals(JOB_ID, createDeviceData.getJobId());
        assertEquals(TIME_NOW, createDeviceData.getLastUpdatedTime());
        assertEquals(TIME_NOW, createDeviceData.getCreateTime());
        assertEquals(null, createDeviceData.getErrorMessage());
    }

    @Test
    public void getCreateDeviceStatus_jobNotFound_throwResourceNotFoundException() {
        when(startCreateDeviceDAO.load(JOB_ID)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> workflowManager.getCreateDeviceStatus(JOB_ID));

        assertEquals("Job doesn't exist", exception.getMessage());
    }

    private static boolean isValidUUID(String str) {
        try {
            java.util.UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

