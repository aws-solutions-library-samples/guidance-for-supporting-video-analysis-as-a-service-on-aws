package com.amazonaws.videoanalytics.videologistics.dagger.modules;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.ACCOUNT_ID;

import javax.inject.Named;
import javax.inject.Singleton;

import com.amazonaws.videoanalytics.videologistics.client.opensearch.OpenSearchClientFactory;
import com.amazonaws.videoanalytics.videologistics.client.opensearch.OpenSearchClientProvider;
import com.amazonaws.videoanalytics.videologistics.client.s3.ImageUploader;
import com.amazonaws.videoanalytics.videologistics.client.s3.ThumbnailS3PresignerFactory;
import com.amazonaws.videoanalytics.videologistics.dao.VLRegisterDeviceJobDAO;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.RawVideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.VideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.inference.InferenceDeserializer;
import com.amazonaws.videoanalytics.videologistics.inference.InferenceSerializer;
import com.amazonaws.videoanalytics.videologistics.inference.SchemaRepository;
import com.amazonaws.videoanalytics.videologistics.schema.SchemaConst;
import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.AggregateVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.RawVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.timeline.BatchTimelineMapper;
import com.amazonaws.videoanalytics.videologistics.timeline.DetailedVideoTimelineGenerator;
import com.amazonaws.videoanalytics.videologistics.timeline.TimelineKDSMetadataSerDe;
import com.amazonaws.videoanalytics.videologistics.timeline.TimestampListDeserializer;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineAggregator;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineUtils;
import com.amazonaws.videoanalytics.videologistics.utils.S3BucketRegionalizer;
import com.amazonaws.videoanalytics.videologistics.validator.InferenceValidator;
import com.fasterxml.jackson.databind.ObjectMapper;

import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;

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
    public InferenceDeserializer provideInferenceDeserializer(final ObjectMapper objectMapper) {
        return new InferenceDeserializer(objectMapper);
    }

    @Provides
    @Singleton
    public DynamoDbTable<RawVideoTimeline> provideRawVideoTimelineTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(SchemaConst.RAW_VIDEO_TIMELINE_TABLE_NAME,
                TableSchema.fromBean(RawVideoTimeline.class));
    }

    @Provides
    @Singleton
    public OpenSearchClientProvider getOpenSearchClientProvider(final OpenSearchClientFactory openSearchClientFactory) {
        return new OpenSearchClientProvider(openSearchClientFactory);
    }

    @Provides
    @Singleton
    public VideoTimelineUtils provideVideoTimelineUtils() {
        return new VideoTimelineUtils();
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

    @Provides
    @Singleton
    public TimestampListDeserializer provideTimestampListDeserializer(final ObjectMapper objectMapper) {
        return new TimestampListDeserializer(objectMapper);
    }

    @Provides
    @Singleton
    public BatchTimelineMapper provideBatchTimelineMapper(final ObjectMapper objectMapper) {
        return new BatchTimelineMapper(objectMapper);
    }

    @Provides
    @Singleton
    public S3BucketRegionalizer provideS3BucketRegionalizer(
            final Region region,
            @Named(ACCOUNT_ID) final String serviceAccountId) {
        return new S3BucketRegionalizer(region, serviceAccountId);
    }

    @Provides
    @Singleton
    public RawVideoTimelineDAO provideRawVideoTimelineDAO(
            final DynamoDbTable<RawVideoTimeline> rawVideoTimelineTable,
            final VideoTimelineUtils videoTimelineUtils) {
        return new RawVideoTimelineDAO(rawVideoTimelineTable, videoTimelineUtils);
    }

    @Provides
    @Singleton
    public VideoTimelineAggregator provideVideoTimelineAggregator(
            final VideoTimelineUtils videoTimelineUtils) {
        return new VideoTimelineAggregator(videoTimelineUtils);
    }

    @Provides
    @Singleton
    public VideoTimelineDAO provideVideoTimelineDAO(
            final DynamoDbEnhancedClient ddbClient,
            final DynamoDbTable<AggregateVideoTimeline> videoTimelineTable,
            final VideoTimelineUtils videoTimelineUtils,
            final VideoTimelineAggregator videoTimelineAggregator) {
        return new VideoTimelineDAO(ddbClient, videoTimelineTable, videoTimelineUtils, videoTimelineAggregator);
    }

    @Provides
    @Singleton
    public TimelineKDSMetadataSerDe provideTimelineKDSMetadataSerDe(
            final ObjectMapper objectMapper) {
        return new TimelineKDSMetadataSerDe(objectMapper);
    }

    @Provides
    @Singleton
    public DetailedVideoTimelineGenerator provideDetailedVideoTimelineGenerator(RawVideoTimelineDAO rawVideoTimelineDAO) {
        return new DetailedVideoTimelineGenerator(rawVideoTimelineDAO);
    }
}
