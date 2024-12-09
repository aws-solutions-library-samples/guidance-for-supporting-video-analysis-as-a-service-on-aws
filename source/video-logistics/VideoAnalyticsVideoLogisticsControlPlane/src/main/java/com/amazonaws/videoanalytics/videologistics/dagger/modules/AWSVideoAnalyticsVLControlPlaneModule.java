package com.amazonaws.videoanalytics.videologistics.dagger.modules;

import dagger.Module;
import dagger.Provides;
import com.amazonaws.videoanalytics.videologistics.dao.VLRegisterDeviceJobDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import com.amazonaws.videoanalytics.videologistics.schema.SchemaConst;
import com.amazonaws.videoanalytics.videologistics.inference.SchemaRepository;
import com.amazonaws.videoanalytics.videologistics.inference.InferenceSerializer;
import com.amazonaws.videoanalytics.videologistics.inference.InferenceDeserializer;
import com.amazonaws.videoanalytics.videologistics.validator.InferenceValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import com.amazonaws.videoanalytics.videologistics.client.opensearch.OpenSearchClientFactory;
import com.amazonaws.videoanalytics.videologistics.client.opensearch.OpenSearchClientProvider;
import com.amazonaws.videoanalytics.videologistics.client.s3.ThumbnailS3PresignerFactory;
import com.amazonaws.videoanalytics.videologistics.client.s3.ImageUploader;

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
    public InferenceDeserializer provideInferenceDeserializer(final ObjectMapper objectMapper) {
        return new InferenceDeserializer(objectMapper);
    }

    @Provides
    @Singleton
    public OpenSearchClientProvider getOpenSearchClientProvider(final OpenSearchClientFactory openSearchClientFactory) {
        return new OpenSearchClientProvider(openSearchClientFactory);
    }

    @Provides
    @Singleton
    public ThumbnailS3PresignerFactory provideThumbnailS3PresignerFactory() {
        return new ThumbnailS3PresignerFactory();
    }

    @Provides
    @Singleton
    public ImageUploader provideImageUploader() {
        return new ImageUploader();
    }
}
