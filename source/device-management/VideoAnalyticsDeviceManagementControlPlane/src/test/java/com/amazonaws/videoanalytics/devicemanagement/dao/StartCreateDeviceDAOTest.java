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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import java.util.Iterator;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StartCreateDeviceDAOTest {
    private static final String JOB_ID = "jobId";
    private static final String CERTIFICATE_ID = "certificateId";
    private static final String DEVICE_ID = "deviceId";
    private static final String DEVICE_STATE = "ACTIVE";

    @Mock
    private DynamoDbTable<CreateDevice> ddbTable;
    
    @Mock
    private DynamoDbIndex<CreateDevice> deviceIdIndex;
    
    @Mock
    private Page<CreateDevice> queryPage;
    
    @Mock
    private Iterator<Page<CreateDevice>> pagesIterator;
    
    private StartCreateDeviceDAO startCreateDeviceDAO;

    @BeforeEach
    void setup() {
        startCreateDeviceDAO = new StartCreateDeviceDAO(ddbTable);
    }

    @Test
    public void save_RunningStatus_SavesSuccessfully() {
        CreateDevice createDevice = CreateDevice.builder()
                .jobId(JOB_ID)
                .jobStatus(Status.RUNNING.toString())
                .certificateId(CERTIFICATE_ID)
                .build();

        startCreateDeviceDAO.save(createDevice);
        
        verify(ddbTable).putItem(createDevice);
    }

    @Test
    public void save_FailedStatus_SavesSuccessfully() {
        CreateDevice createDeviceFailed = CreateDevice.builder()
                .jobId(JOB_ID)
                .jobStatus(Status.FAILED.toString())
                .certificateId(CERTIFICATE_ID)
                .build();

        startCreateDeviceDAO.save(createDeviceFailed);
        
        verify(ddbTable).putItem(createDeviceFailed);
    }

    @Test
    public void save_CompletedStatus_SavesSuccessfully() {
        CreateDevice createDeviceCompleted = CreateDevice.builder()
                .jobId(JOB_ID)
                .jobStatus(Status.COMPLETED.toString())
                .certificateId(CERTIFICATE_ID)
                .build();

        startCreateDeviceDAO.save(createDeviceCompleted);
        
        verify(ddbTable).putItem(createDeviceCompleted);
    }

    @Test
    public void getVideoLogisticsDeviceStatus_DeviceExists_ReturnsDeviceState() {
        CreateDevice device = CreateDevice.builder()
                .deviceId(DEVICE_ID)
                .currentDeviceState(DEVICE_STATE)
                .build();

        when(ddbTable.index("deviceId-index")).thenReturn(deviceIdIndex);
        when(deviceIdIndex.query((QueryConditional) any())).thenReturn(() -> pagesIterator);
        when(pagesIterator.hasNext()).thenReturn(true, false);
        when(pagesIterator.next()).thenReturn(queryPage);
        when(queryPage.items()).thenReturn(Collections.singletonList(device));

        String result = startCreateDeviceDAO.getVideoLogisticsDeviceStatus(DEVICE_ID);

        assertEquals(DEVICE_STATE, result);
        verify(deviceIdIndex).query((QueryConditional) any());
    }

    @Test
    public void getVideoLogisticsDeviceStatus_DeviceDoesNotExist_ReturnsNull() {
        when(ddbTable.index("deviceId-index")).thenReturn(deviceIdIndex);
        when(deviceIdIndex.query((QueryConditional) any())).thenReturn(() -> pagesIterator);
        when(pagesIterator.hasNext()).thenReturn(false);

        String result = startCreateDeviceDAO.getVideoLogisticsDeviceStatus(DEVICE_ID);

        assertNull(result);
        verify(deviceIdIndex).query((QueryConditional) any());
    }
}