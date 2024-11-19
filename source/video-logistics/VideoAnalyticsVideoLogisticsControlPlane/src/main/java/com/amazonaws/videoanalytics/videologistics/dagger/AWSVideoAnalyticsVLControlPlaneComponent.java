package com.amazonaws.videoanalytics.videologistics.dagger;

import com.amazonaws.videoanalytics.videologistics.activity.CreateLivestreamSessionActivity;
import com.amazonaws.videoanalytics.videologistics.activity.CreatePlaybackSessionActivity;
import com.amazonaws.videoanalytics.videologistics.activity.CreateSnapshotUploadPathActivity;
import com.amazonaws.videoanalytics.videologistics.dagger.modules.AWSModule;
import com.amazonaws.videoanalytics.videologistics.dagger.modules.AWSVideoAnalyticsConfigurationModule;
import com.amazonaws.videoanalytics.videologistics.dependency.kvs.KvsService;
import com.amazonaws.videoanalytics.videologistics.utils.GuidanceUUIDGenerator;
import com.amazonaws.videoanalytics.videologistics.utils.KVSWebRTCUtils;
import com.amazonaws.videoanalytics.videologistics.validator.DeviceValidator;

import dagger.Component;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.regions.Region;

import javax.inject.Singleton;

@Component(
        modules = {
                AWSModule.class,
                AWSVideoAnalyticsConfigurationModule.class
        }
)
@Singleton
public interface AWSVideoAnalyticsVLControlPlaneComponent {
    void inject(CreateLivestreamSessionActivity lambda);
    void inject(CreatePlaybackSessionActivity lambda);
    void inject(CreateSnapshotUploadPathActivity lambda);

    KvsService getKvsService();
    DeviceValidator getDeviceValidator();
    GuidanceUUIDGenerator getGuidanceUUIDGenerator();
    KVSWebRTCUtils getKVSWebRTCUtils();
    S3Presigner getS3Presigner();
    Region getRegion();
}
