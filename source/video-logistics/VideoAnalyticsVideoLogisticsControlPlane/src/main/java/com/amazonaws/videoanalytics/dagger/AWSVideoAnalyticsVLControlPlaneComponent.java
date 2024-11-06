package com.amazonaws.videoanalytics.dagger;

import com.amazonaws.videoanalytics.activity.CreateLivestreamSessionActivity;
import com.amazonaws.videoanalytics.client.KvsClient.KvsClientFactory;
import com.amazonaws.videoanalytics.client.KvsSignalingClient.KvsSignalingClientFactory;
import com.amazonaws.videoanalytics.dagger.modules.AWSModule;
import com.amazonaws.videoanalytics.dagger.modules.AWSVideoAnalyticsConfigurationModule;
import com.amazonaws.videoanalytics.dao.LivestreamSessionDAO;
import com.amazonaws.videoanalytics.utils.DateTime;
import com.amazonaws.videoanalytics.utils.GuidanceUUIDGenerator;
import com.amazonaws.videoanalytics.utils.KVSWebRTCUtils;
import com.amazonaws.videoanalytics.validator.DeviceValidator;
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
