package com.amazonaws.videoanalytics.devicemanagement.dao;

import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import com.amazonaws.videoanalytics.devicemanagement.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StartCreateDeviceDAOTest {
    private static final String JOB_ID = "jobId";
    private static final String CERTIFICATE_ID = "certificateId";

    @Mock
    private DynamoDbTable<CreateDevice> ddbTable;
    
    private StartCreateDeviceDAO startCreateDeviceDAO;

    @BeforeEach
    void setup() {
        startCreateDeviceDAO = new StartCreateDeviceDAO(ddbTable);
    }

    @Test
    public void startCreateDeviceDAOSaveNonTerminalStatusTest() {
        CreateDevice createDevice = CreateDevice.builder()
                .jobId(JOB_ID)
                .jobStatus(Status.RUNNING.toString())
                .certificateId(CERTIFICATE_ID)
                .build();
                
        when(ddbTable.getItem(any(Key.class))).thenReturn(createDevice);

        startCreateDeviceDAO.save(createDevice);
        CreateDevice persistedObject = startCreateDeviceDAO.load(JOB_ID);

        verify(ddbTable).putItem(createDevice);
        assertEquals(JOB_ID, persistedObject.getJobId());
        assertEquals(Status.RUNNING.toString(), persistedObject.getJobStatus());
        assertEquals(CERTIFICATE_ID, persistedObject.getCertificateId());
    }

    @Test
    public void startCreateDeviceDAOSaveFailedStatusTest() {
        CreateDevice createDeviceFailed = CreateDevice.builder()
                .jobId(JOB_ID)
                .jobStatus(Status.FAILED.toString())
                .certificateId(CERTIFICATE_ID)
                .build();
                
        when(ddbTable.getItem(any(Key.class))).thenReturn(createDeviceFailed);

        startCreateDeviceDAO.save(createDeviceFailed);
        CreateDevice persistedObject = startCreateDeviceDAO.load(JOB_ID);

        verify(ddbTable).putItem(createDeviceFailed);
        assertEquals(JOB_ID, persistedObject.getJobId());
        assertEquals(Status.FAILED.toString(), persistedObject.getJobStatus());
        assertEquals(CERTIFICATE_ID, persistedObject.getCertificateId());
    }

    @Test
    public void startCreateDeviceDAOSaveCompletedStatusTest() {
        CreateDevice createDeviceCompleted = CreateDevice.builder()
                .jobId(JOB_ID)
                .jobStatus(Status.COMPLETED.toString())
                .certificateId(CERTIFICATE_ID)
                .build();
                
        when(ddbTable.getItem(any(Key.class))).thenReturn(createDeviceCompleted);

        startCreateDeviceDAO.save(createDeviceCompleted);
        CreateDevice persistedObject = startCreateDeviceDAO.load(JOB_ID);

        verify(ddbTable).putItem(createDeviceCompleted);
        assertEquals(JOB_ID, persistedObject.getJobId());
        assertEquals(Status.COMPLETED.toString(), persistedObject.getJobStatus());
        assertEquals(CERTIFICATE_ID, persistedObject.getCertificateId());
    }
}