package com.amazonaws.videoanalytics.devicemanagement.dao;

import lombok.extern.log4j.Log4j2;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import com.amazonaws.videoanalytics.devicemanagement.schema.UpdateDevice;

import javax.inject.Inject;

import static com.amazonaws.services.lambda.runtime.LambdaRuntime.getLogger;

@Log4j2
public class StartUpdateDeviceDAO {
    private final LambdaLogger logger = getLogger();
    private final DynamoDbTable<UpdateDevice> ddbTable;

    @Inject
    public StartUpdateDeviceDAO(final DynamoDbTable<UpdateDevice> ddbTable) {
        this.ddbTable = ddbTable;
    }

    public void save(final UpdateDevice updateDevice) {
        logger.log("Starting save for updateDevice");
        this.ddbTable.putItem(updateDevice);
    }

    public UpdateDevice load(final String jobId) {
        logger.log(String.format("Loading UpdateDevice session %s", jobId));
        return this.ddbTable.getItem(Key.builder()
                .partitionValue(jobId)
                .build());
    }
}
