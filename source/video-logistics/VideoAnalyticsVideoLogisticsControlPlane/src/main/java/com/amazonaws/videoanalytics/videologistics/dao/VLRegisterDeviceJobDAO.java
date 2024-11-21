package com.amazonaws.videoanalytics.videologistics.dao;

import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import javax.inject.Inject;

import static com.amazonaws.services.lambda.runtime.LambdaRuntime.getLogger;

@Log4j2
public class VLRegisterDeviceJobDAO {
    private final LambdaLogger logger = getLogger();
    private final DynamoDbTable<VLRegisterDeviceJob> ddbTable;

    @Inject
    public VLRegisterDeviceJobDAO(final DynamoDbTable<VLRegisterDeviceJob> ddbTable) {
        this.ddbTable = ddbTable;
    }

    public void save(final VLRegisterDeviceJob job) {
        logger.log("Starting save for VLRegisterDeviceJob");
        ddbTable.putItem(job);
    }   

    public VLRegisterDeviceJob load(final String jobId) {
        logger.log(String.format("Loading VLRegisterDeviceJob session %s", jobId));
        return ddbTable.getItem(Key.builder()
                .partitionValue(jobId)
                .build());
    }
}
