package com.amazonaws.videoanalytics.devicemanagement.dagger.modules;

import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import com.amazonaws.videoanalytics.devicemanagement.schema.SchemaConst;
import com.amazonaws.videoanalytics.devicemanagement.workflow.WorkflowManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;

import javax.inject.Singleton;

@Module
public class AWSVideoAnalyticsControlPlaneModule {
    
    @Provides
    @Singleton
    public DynamoDbTable<CreateDevice> provideCreateDeviceTable(final DynamoDbEnhancedClient ddbClient) {
        return ddbClient.table(SchemaConst.CREATE_DEVICE_TABLE_NAME, TableSchema.fromBean(CreateDevice.class));
    }

    @Provides
    @Singleton
    public StartCreateDeviceDAO provideStartCreateDeviceDAO(DynamoDbTable<CreateDevice> createDeviceTable) {
        return new StartCreateDeviceDAO(createDeviceTable);
    }

    @Provides
    @Singleton
    public IotService provideIotService(IotClient iotClient, IotDataPlaneClient iotDataPlaneClient) {
        return new IotService(iotClient, iotDataPlaneClient);
    }

    @Provides
    @Singleton
    public WorkflowManager provideWorkflowManager(StartCreateDeviceDAO startCreateDeviceDAO, IotService iotService) {
        return new WorkflowManager(startCreateDeviceDAO, iotService);
    }

    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        return new ObjectMapper();
    }
}
