package com.amazonaws.videoanalytics.videologistics.dagger;

import com.amazonaws.videoanalytics.videologistics.activity.CreateLivestreamSessionActivity;
import com.amazonaws.videoanalytics.videologistics.activity.CreatePlaybackSessionActivity;
import com.amazonaws.videoanalytics.videologistics.dagger.modules.AWSModule;
import com.amazonaws.videoanalytics.videologistics.dagger.modules.AWSVideoAnalyticsConfigurationModule;
import com.amazonaws.videoanalytics.videologistics.dependency.kvs.KvsService;
import com.amazonaws.videoanalytics.videologistics.utils.GuidanceUUIDGenerator;
import com.amazonaws.videoanalytics.videologistics.utils.KVSWebRTCUtils;
import com.amazonaws.videoanalytics.videologistics.validator.DeviceValidator;

import dagger.Component;

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

    KvsService getKvsService();
    DeviceValidator getDeviceValidator();
    GuidanceUUIDGenerator getGuidanceUUIDGenerator();
    KVSWebRTCUtils getKVSWebRTCUtils();
}
