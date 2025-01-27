package com.amazonaws.videoanalytics.devicemanagement.dao;

import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import javax.inject.Inject;

import static com.amazonaws.services.lambda.runtime.LambdaRuntime.getLogger;

@Log4j2
public class StartCreateDeviceDAO {
    private final LambdaLogger logger = getLogger();
    private final DynamoDbTable<CreateDevice> ddbTable;

    @Inject
    public StartCreateDeviceDAO(final DynamoDbTable<CreateDevice> ddbTable) {
        this.ddbTable = ddbTable;
    }

    public void save(final CreateDevice createDevice) {
        logger.log("Starting save for CreateDevice");
        ddbTable.putItem(createDevice);
    }

    public CreateDevice load(final String jobId) {
        logger.log(String.format("Loading CreateDevice session %s", jobId));
        return ddbTable.getItem(Key.builder()
                .partitionValue(jobId)
                .build());
    }

    
}