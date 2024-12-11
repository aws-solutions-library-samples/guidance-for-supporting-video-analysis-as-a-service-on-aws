package com.amazonaws.videoanalytics.videologistics.dagger;

import com.amazonaws.videoanalytics.videologistics.activity.CreateLivestreamSessionActivity;
import com.amazonaws.videoanalytics.videologistics.activity.CreatePlaybackSessionActivity;
import com.amazonaws.videoanalytics.videologistics.activity.CreateSnapshotUploadPathActivity;
import com.amazonaws.videoanalytics.videologistics.dagger.modules.AWSModule;
import com.amazonaws.videoanalytics.videologistics.dagger.modules.AWSVideoAnalyticsConfigurationModule;
import com.amazonaws.videoanalytics.videologistics.dependency.kvs.KvsService;

import com.amazonaws.videoanalytics.videologistics.inference.BulkInferenceLambda;
import com.amazonaws.videoanalytics.videologistics.inference.ImportMediaObjectHandler;
import com.amazonaws.videoanalytics.videologistics.inference.InferenceSerializer;
import com.amazonaws.videoanalytics.videologistics.inference.InferenceDeserializer;

import com.amazonaws.videoanalytics.videologistics.client.opensearch.OpenSearchClientProvider;
import com.amazonaws.videoanalytics.videologistics.client.s3.ThumbnailS3PresignerFactory;
import com.amazonaws.videoanalytics.videologistics.client.s3.ImageUploader;

import com.amazonaws.videoanalytics.videologistics.validator.InferenceValidator;
import com.amazonaws.videoanalytics.videologistics.utils.GuidanceUUIDGenerator;
import com.amazonaws.videoanalytics.videologistics.utils.KVSWebRTCUtils;
import com.amazonaws.videoanalytics.videologistics.validator.DeviceValidator;
import com.amazonaws.videoanalytics.videologistics.activity.StartVLRegisterDeviceActivity;
import com.amazonaws.videoanalytics.videologistics.activity.GetVLRegisterDeviceStatusActivity;
import com.amazonaws.videoanalytics.videologistics.activity.ImportMediaObjectActivity;
import com.amazonaws.videoanalytics.videologistics.dagger.modules.AWSVideoAnalyticsVLControlPlaneModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.videoanalytics.videologistics.dao.VLRegisterDeviceJobDAO;
import com.amazonaws.videoanalytics.videologistics.workflow.KVSResourceCreateLambda;
import com.amazonaws.videoanalytics.videologistics.workflow.FailAndCleanupVLDeviceRegistrationHandler;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.ACCOUNT_ID;
import com.amazonaws.videoanalytics.videologistics.dependency.apig.ApigService;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.VideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.RawVideoTimelineDAO;

import dagger.Component;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.regions.Region;

import javax.inject.Named;
import javax.inject.Singleton;

import software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;

import com.amazonaws.videoanalytics.videologistics.client.s3.S3Proxy;
import com.amazonaws.videoanalytics.videologistics.timeline.TimestampListDeserializer;
import com.amazonaws.videoanalytics.videologistics.timeline.BatchTimelineMapper;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineUtils;
import com.amazonaws.videoanalytics.videologistics.utils.S3BucketRegionalizer;
import com.amazonaws.videoanalytics.videologistics.timeline.PutVideoTimelineHandler;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineAggregator;
import com.amazonaws.videoanalytics.videologistics.timeline.TimelineKDSMetadataSerDe;

import com.amazonaws.videoanalytics.videologistics.activity.PutVideoTimelineActivity;
import com.amazonaws.videoanalytics.videologistics.activity.ListVideoTimelinesActivity;
@Component(
        modules = {
                AWSModule.class,
                AWSVideoAnalyticsConfigurationModule.class,
                AWSVideoAnalyticsVLControlPlaneModule.class
        }
)
@Singleton
public interface AWSVideoAnalyticsVLControlPlaneComponent {
    void inject(CreateLivestreamSessionActivity lambda);
    void inject(CreatePlaybackSessionActivity lambda);
    void inject(StartVLRegisterDeviceActivity lambda);
    void inject(GetVLRegisterDeviceStatusActivity lambda);
    void inject(KVSResourceCreateLambda lambda);
    void inject(FailAndCleanupVLDeviceRegistrationHandler lambda);
    void inject(CreateSnapshotUploadPathActivity lambda);
    void inject(BulkInferenceLambda lambda);
    void inject(ImportMediaObjectActivity lambda);
    void inject(PutVideoTimelineActivity lambda);
    void inject(ListVideoTimelinesActivity lambda);

    KvsService getKvsService();
    DeviceValidator getDeviceValidator();
    GuidanceUUIDGenerator getGuidanceUUIDGenerator();
    KVSWebRTCUtils getKVSWebRTCUtils();
    VLRegisterDeviceJobDAO getVLRegisterDeviceJobDAO();
    ObjectMapper getObjectMapper();
    KinesisVideoClient getKinesisVideoClient();
    S3Presigner getS3Presigner();
    Region getRegion();
    ImportMediaObjectHandler getImportMediaObjectHandler();
    VideoTimelineDAO getVideoTimelineDAO();
    RawVideoTimelineDAO getRawVideoTimelineDAO();
    OpenSearchClientProvider getOpenSearchClientProvider();
    InferenceSerializer getInferenceSerializer();
    InferenceDeserializer getInferenceDeserializer();
    ThumbnailS3PresignerFactory getThumbnailS3PresignerFactory();
    ImageUploader getImageUploader();
    @Named(ACCOUNT_ID) String getAccountId();
    ApigService apigService();
    S3Proxy getS3Proxy();
    TimestampListDeserializer getTimestampListDeserializer();
    BatchTimelineMapper getBatchTimelineMapper();
    S3BucketRegionalizer getS3BucketRegionalizer();
    VideoTimelineUtils getVideoTimelineUtils();
    PutVideoTimelineHandler getPutVideoTimelineHandler();
    VideoTimelineAggregator getVideoTimelineAggregator();
    TimelineKDSMetadataSerDe getTimelineKDSMetadataSerDe();
    KinesisClient getKinesisClient();
}
