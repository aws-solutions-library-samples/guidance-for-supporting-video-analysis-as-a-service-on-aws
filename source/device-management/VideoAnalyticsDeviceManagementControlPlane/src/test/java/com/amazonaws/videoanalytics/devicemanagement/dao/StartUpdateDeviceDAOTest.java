package com.amazonaws.videoanalytics.devicemanagement.dao;

import com.amazonaws.videoanalytics.devicemanagement.schema.UpdateDevice;
import com.amazonaws.videoanalytics.devicemanagement.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StartUpdateDeviceDAOTest {
    private static final String JOB_ID = "12345";
    private static final String THING_GROUP_NAME = "testThingGroupName";

    @Mock
    private DynamoDbTable<UpdateDevice> ddbTable;
    
    private StartUpdateDeviceDAO startUpdateDeviceDAO;

    @BeforeEach
    void setup() {
        startUpdateDeviceDAO = new StartUpdateDeviceDAO(ddbTable);
    }

    @Test
    public void startUpdateDeviceDAOSaveNonTerminalStatusTest() {
        UpdateDevice updateDevice = UpdateDevice.builder()
                .jobId(JOB_ID)
                .jobStatus(Status.RUNNING.toString())
                .removeThingGroup(false)
                .thingGroupIdList(List.of(THING_GROUP_NAME))
                .build();
                
        when(ddbTable.getItem(any(Key.class))).thenReturn(updateDevice);

        startUpdateDeviceDAO.save(updateDevice);
        UpdateDevice persistedObject = startUpdateDeviceDAO.load(JOB_ID);

        verify(ddbTable).putItem(updateDevice);
        assertEquals(JOB_ID, persistedObject.getJobId());
        assertEquals(Status.RUNNING.toString(), persistedObject.getJobStatus());
        assertEquals(false, persistedObject.getRemoveThingGroup());
        assertEquals(List.of(THING_GROUP_NAME), persistedObject.getThingGroupIdList());
    }

    @Test
    public void startUpdateDeviceDAOSaveFailedStatusTest() {
        UpdateDevice updateDeviceFailedStatus = UpdateDevice.builder()
                .jobId(JOB_ID)
                .jobStatus(Status.FAILED.toString())
                .removeThingGroup(true)
                .thingGroupIdList(List.of(THING_GROUP_NAME))
                .build();
                
        when(ddbTable.getItem(any(Key.class))).thenReturn(updateDeviceFailedStatus);

        startUpdateDeviceDAO.save(updateDeviceFailedStatus);
        UpdateDevice persistedObject = startUpdateDeviceDAO.load(JOB_ID);

        verify(ddbTable).putItem(updateDeviceFailedStatus);
        assertEquals(JOB_ID, persistedObject.getJobId());
        assertEquals(Status.FAILED.toString(), persistedObject.getJobStatus());
        assertEquals(true, persistedObject.getRemoveThingGroup());
        assertEquals(List.of(THING_GROUP_NAME), persistedObject.getThingGroupIdList());
    }

    @Test
    public void startUpdateDeviceDAOSaveCompletedStatusTest() {
        UpdateDevice updateDeviceCompletedStatus = UpdateDevice.builder()
                .jobId(JOB_ID)
                .jobStatus(Status.COMPLETED.toString())
                .removeThingGroup(false)
                .thingGroupIdList(List.of(THING_GROUP_NAME))
                .build();
                
        when(ddbTable.getItem(any(Key.class))).thenReturn(updateDeviceCompletedStatus);

        startUpdateDeviceDAO.save(updateDeviceCompletedStatus);
        UpdateDevice persistedObject = startUpdateDeviceDAO.load(JOB_ID);

        verify(ddbTable).putItem(updateDeviceCompletedStatus);
        assertEquals(JOB_ID, persistedObject.getJobId());
        assertEquals(Status.COMPLETED.toString(), persistedObject.getJobStatus());
        assertEquals(false, persistedObject.getRemoveThingGroup());
        assertEquals(List.of(THING_GROUP_NAME), persistedObject.getThingGroupIdList());
    }
}
