package com.amazonaws.videoanalytics.videologistics.dagger.modules;

import dagger.Module;
import dagger.Provides;
import com.amazonaws.videoanalytics.videologistics.dao.VLRegisterDeviceJobDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import com.amazonaws.videoanalytics.videologistics.schema.SchemaConst;
import com.amazonaws.videoanalytics.videologistics.inference.SchemaRepository;
import com.amazonaws.videoanalytics.videologistics.inference.InferenceSerializer;
import com.amazonaws.videoanalytics.videologistics.validator.InferenceValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.AggregateVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.RawVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineUtils;

import javax.inject.Singleton;

@Module
public class AWSVideoAnalyticsVLControlPlaneModule {

    @Provides
    @Singleton
    public DynamoDbTable<VLRegisterDeviceJob> provideVLRegisterDeviceJobTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(SchemaConst.VL_REGISTER_DEVICE_JOB_TABLE_NAME,
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

    @Provides
    @Singleton
    public SchemaRepository provideSchemaRepository(final ObjectMapper objectMapper) {
        return new SchemaRepository(objectMapper);
    }

    @Provides
    @Singleton
    public InferenceValidator provideInferenceValidator(final ObjectMapper objectMapper,
                                                        final SchemaRepository schemaRepository) {
        return new InferenceValidator(objectMapper, schemaRepository);
    }

    @Provides
    @Singleton
    public InferenceSerializer provideInferenceSerializer(final ObjectMapper objectMapper) {
        return new InferenceSerializer(objectMapper);
    }

    @Provides
    @Singleton
    public DynamoDbTable<AggregateVideoTimeline> provideAggregateVideoTimelineTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(SchemaConst.VIDEO_TIMELINE_TABLE_NAME,
                TableSchema.fromBean(AggregateVideoTimeline.class));
    }

    @Provides
    @Singleton
    public DynamoDbTable<RawVideoTimeline> provideRawVideoTimelineTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(SchemaConst.RAW_VIDEO_TIMELINE_TABLE_NAME,
                TableSchema.fromBean(RawVideoTimeline.class));
    }

    @Provides
    @Singleton
    public VideoTimelineUtils provideVideoTimelineUtils() {
        return new VideoTimelineUtils();
    }
}
