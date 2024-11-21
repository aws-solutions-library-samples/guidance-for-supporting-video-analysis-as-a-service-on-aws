package com.amazonaws.videoanalytics.videologistics.dagger.modules;

import dagger.Module;
import dagger.Provides;
import com.amazonaws.videoanalytics.videologistics.dao.VLRegisterDeviceJobDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import com.amazonaws.videoanalytics.videologistics.schema.SchemaConst;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Singleton;

@Module
public class AWSVideoAnalyticsVLControlPlaneModule {

    @Provides
    @Singleton
    public DynamoDbTable<VLRegisterDeviceJob> provideVLRegisterDeviceJobTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(SchemaConst.FVL_REGISTER_DEVICE_JOB_TABLE_NAME,
                TableSchema.fromBean(VLRegisterDeviceJob.class));
    }

    @Provides
    @Singleton
    public VLRegisterDeviceJobDAO provideVLRegisterDeviceJobDAO(final DynamoDbTable<VLRegisterDeviceJob> ddbTable) {
        return new VLRegisterDeviceJobDAO(ddbTable);
    }

    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        return new ObjectMapper();
    }
}
