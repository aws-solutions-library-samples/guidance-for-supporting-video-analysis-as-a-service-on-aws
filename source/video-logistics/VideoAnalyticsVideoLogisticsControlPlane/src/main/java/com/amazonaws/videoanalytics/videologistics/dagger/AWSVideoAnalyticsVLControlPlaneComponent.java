package com.amazonaws.videoanalytics.videologistics.dagger;

import com.amazonaws.videoanalytics.videologistics.activity.CreateLivestreamSessionActivity;
import com.amazonaws.videoanalytics.videologistics.client.KvsClient.KvsClientFactory;
import com.amazonaws.videoanalytics.videologistics.client.KvsSignalingClient.KvsSignalingClientFactory;
import com.amazonaws.videoanalytics.videologistics.dagger.modules.AWSModule;
import com.amazonaws.videoanalytics.videologistics.dagger.modules.AWSVideoAnalyticsConfigurationModule;
import com.amazonaws.videoanalytics.videologistics.dao.LivestreamSessionDAO;
import com.amazonaws.videoanalytics.videologistics.utils.DateTime;
import com.amazonaws.videoanalytics.videologistics.utils.GuidanceUUIDGenerator;
import com.amazonaws.videoanalytics.videologistics.utils.KVSWebRTCUtils;
import com.amazonaws.videoanalytics.videologistics.validator.DeviceValidator;
import dagger.Component;
import software.amazon.awssdk.http.SdkHttpClient;
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
    LivestreamSessionDAO getLivestreamSessionDAO();
    KvsClientFactory getKvsClientFactory();
    KvsSignalingClientFactory getKvsSignalingClientFactory();
    DeviceValidator getDeviceValidator();
    GuidanceUUIDGenerator getGuidanceUUIDGenerator();
    DateTime getDateTime();
    KVSWebRTCUtils getKVSWebRTCUtils();
    Region getRegion();
}
