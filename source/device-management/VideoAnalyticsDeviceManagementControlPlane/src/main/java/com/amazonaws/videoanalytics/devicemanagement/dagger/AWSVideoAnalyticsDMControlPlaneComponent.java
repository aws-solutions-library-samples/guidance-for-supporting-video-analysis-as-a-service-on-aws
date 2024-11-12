package com.amazonaws.videoanalytics.devicemanagement.dagger;

import com.amazonaws.videoanalytics.devicemanagement.dagger.modules.AWSVideoAnalyticsConfigurationModule;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.videoanalytics.devicemanagement.dagger.modules.AWSVideoAnalyticsControlPlaneModule;
import com.amazonaws.videoanalytics.devicemanagement.dagger.modules.AWSModule;
import com.amazonaws.videoanalytics.devicemanagement.workflow.WorkflowManager;

import com.amazonaws.videoanalytics.devicemanagement.activity.GetDeviceActivity;
import com.amazonaws.videoanalytics.devicemanagement.activity.GetDeviceShadowActivity;
import com.amazonaws.videoanalytics.devicemanagement.activity.UpdateDeviceShadowActivity;
import com.amazonaws.videoanalytics.devicemanagement.activity.StartCreateDeviceActivity;

import dagger.Component;

import javax.inject.Singleton;

@Component(
    modules = {
        AWSModule.class,
        AWSVideoAnalyticsConfigurationModule.class,
        AWSVideoAnalyticsControlPlaneModule.class
    }
)

@Singleton
public interface AWSVideoAnalyticsDMControlPlaneComponent {
    void inject(GetDeviceActivity lambda);
    void inject(GetDeviceShadowActivity lambda);
    void inject(StartCreateDeviceActivity lambda);       
    void inject(UpdateDeviceShadowActivity lambda);

    IotService iotService();
    WorkflowManager workflowManager();
}
