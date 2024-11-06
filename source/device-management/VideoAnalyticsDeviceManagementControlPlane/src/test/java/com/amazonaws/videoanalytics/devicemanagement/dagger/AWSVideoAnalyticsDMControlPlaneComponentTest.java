package com.amazonaws.videoanalytics.devicemanagement.dagger;

import org.junit.jupiter.api.Test;

import com.amazonaws.videoanalytics.devicemanagement.activity.GetDeviceActivity;
import com.amazonaws.videoanalytics.devicemanagement.activity.GetDeviceShadowActivity;
import com.amazonaws.videoanalytics.devicemanagement.activity.UpdateDeviceShadowActivity;

public class AWSVideoAnalyticsDMControlPlaneComponentTest {
    @Test
    public void inject_WhenGetDeviceActivity_InjectsDependencies() {
        GetDeviceActivity getDeviceActivity = new GetDeviceActivity();
        AWSVideoAnalyticsDMControlPlaneComponent component = DaggerAWSVideoAnalyticsDMControlPlaneComponent.create();
        component.inject(getDeviceActivity);
        getDeviceActivity.assertPrivateFieldNotNull();
    }

    @Test
    public void inject_WhenGetDeviceShadowActivity_InjectsDependencies() {
        GetDeviceShadowActivity getDeviceShadowActivity = new GetDeviceShadowActivity();
        AWSVideoAnalyticsDMControlPlaneComponent component = DaggerAWSVideoAnalyticsDMControlPlaneComponent.create();
        component.inject(getDeviceShadowActivity);
        getDeviceShadowActivity.assertPrivateFieldNotNull();
    }

    @Test
    public void inject_WhenUpdateDeviceShadowActivity_InjectsDependencies() {
        UpdateDeviceShadowActivity updateDeviceShadowActivity = new UpdateDeviceShadowActivity();
        AWSVideoAnalyticsDMControlPlaneComponent component = DaggerAWSVideoAnalyticsDMControlPlaneComponent.create();
        component.inject(updateDeviceShadowActivity);
        updateDeviceShadowActivity.assertPrivateFieldNotNull();
    }
}
