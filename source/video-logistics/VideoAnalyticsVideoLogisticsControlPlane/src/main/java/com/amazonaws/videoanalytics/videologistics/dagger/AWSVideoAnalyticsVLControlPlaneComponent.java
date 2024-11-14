package com.amazonaws.videoanalytics.videologistics.dagger;

import com.amazonaws.videoanalytics.videologistics.activity.CreateLivestreamSessionActivity;
import com.amazonaws.videoanalytics.videologistics.activity.CreatePlaybackSessionActivity;
import com.amazonaws.videoanalytics.videologistics.activity.GetPlaybackSessionActivity;
import com.amazonaws.videoanalytics.videologistics.dagger.modules.AWSModule;
import com.amazonaws.videoanalytics.videologistics.dagger.modules.AWSVideoAnalyticsConfigurationModule;
import com.amazonaws.videoanalytics.videologistics.dependency.kvs.KvsService;
import com.amazonaws.videoanalytics.videologistics.utils.DateTime;
import com.amazonaws.videoanalytics.videologistics.utils.GuidanceUUIDGenerator;
import com.amazonaws.videoanalytics.videologistics.utils.KVSWebRTCUtils;
import com.amazonaws.videoanalytics.videologistics.validator.DeviceValidator;

import dagger.Component;

import javax.inject.Singleton;

import com.amazonaws.videoanalytics.videologistics.dao.PlaybackSessionDAO;

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
    void inject(GetPlaybackSessionActivity lambda);

    KvsService getKvsService();
    DeviceValidator getDeviceValidator();
    GuidanceUUIDGenerator getGuidanceUUIDGenerator();
    DateTime getDateTime();
    KVSWebRTCUtils getKVSWebRTCUtils();
    PlaybackSessionDAO getPlaybackSessionDAO();
}
