package com.amazonaws.videoanalytics.videologistics.dao;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VLRegisterDeviceJobDAOTest {
    @Mock
    private DynamoDbTable<VLRegisterDeviceJob> mockDdbTable;

    private VLRegisterDeviceJobDAO dao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dao = new VLRegisterDeviceJobDAO(mockDdbTable);
    }

    @Test
    void save_ValidJob_SavesToDynamoDB() {
        VLRegisterDeviceJob job = VLRegisterDeviceJob.builder()
            .jobId("test-job-id")
            .deviceId("test-device-id")
            .status("PENDING")
            .build();

        dao.save(job);

        verify(mockDdbTable).putItem(job);
    }

    @Test
    void load_ExistingJobId_RetrievesFromDynamoDB() {
        String jobId = "test-job-id";
        VLRegisterDeviceJob expectedJob = VLRegisterDeviceJob.builder()
            .jobId(jobId)
            .build();
        when(mockDdbTable.getItem(any(Key.class))).thenReturn(expectedJob);

        dao.load(jobId);

        verify(mockDdbTable).getItem(Key.builder()
            .partitionValue(jobId)
            .build());
    }
}
