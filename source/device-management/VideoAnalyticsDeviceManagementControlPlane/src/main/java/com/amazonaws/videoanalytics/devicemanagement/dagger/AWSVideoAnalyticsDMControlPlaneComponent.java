package com.amazonaws.videoanalytics.devicemanagement.dagger;
import com.amazonaws.videoanalytics.devicemanagement.dagger.modules.AWSVideoAnalyticsConfigurationModule;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.videoanalytics.devicemanagement.dagger.modules.AWSVideoAnalyticsControlPlaneModule;
import com.amazonaws.videoanalytics.devicemanagement.dagger.modules.AWSModule;
import com.amazonaws.videoanalytics.devicemanagement.workflow.WorkflowManager;
import com.amazonaws.videoanalytics.devicemanagement.workflow.createdevice.CreateKVSStreamHandler;
import com.amazonaws.videoanalytics.devicemanagement.workflow.createdevice.AttachKvsAccessToCertHandler;
import com.amazonaws.videoanalytics.devicemanagement.dependency.ddb.DDBService;
import com.amazonaws.videoanalytics.devicemanagement.activity.GetDeviceActivity;
import com.amazonaws.videoanalytics.devicemanagement.activity.GetDeviceShadowActivity;
import com.amazonaws.videoanalytics.devicemanagement.activity.UpdateDeviceShadowActivity;
import com.amazonaws.videoanalytics.devicemanagement.activity.StartCreateDeviceActivity;
import com.amazonaws.videoanalytics.devicemanagement.activity.GetCreateDeviceStatusActivity;
import com.amazonaws.videoanalytics.devicemanagement.workflow.createdevice.VideoLogisticsWorkflowCheckerHandler;
import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.workflow.createdevice.SetLoggerConfigHandler;
import com.amazonaws.videoanalytics.devicemanagement.workflow.createdevice.CreateDeviceHandler;
import com.amazonaws.videoanalytics.devicemanagement.workflow.createdevice.FailCreateDeviceHandler;
import com.amazonaws.videoanalytics.devicemanagement.dependency.apig.ApigService;
import dagger.Component;

import javax.inject.Singleton;
import com.fasterxml.jackson.databind.ObjectMapper;

@Singleton
@Component(
    modules = {
        AWSModule.class,
        AWSVideoAnalyticsConfigurationModule.class,
        AWSVideoAnalyticsControlPlaneModule.class
    }
)
public interface AWSVideoAnalyticsDMControlPlaneComponent {
    void inject(GetDeviceActivity activity);
    void inject(GetDeviceShadowActivity activity);
    void inject(StartCreateDeviceActivity activity);
    void inject(UpdateDeviceShadowActivity activity);
    void inject(GetCreateDeviceStatusActivity activity);
    void inject(CreateKVSStreamHandler handler);
    void inject(AttachKvsAccessToCertHandler handler);
    void inject(VideoLogisticsWorkflowCheckerHandler handler);
    void inject(SetLoggerConfigHandler handler);
    void inject(CreateDeviceHandler handler);
    void inject(FailCreateDeviceHandler handler);
    

    IotService iotService();
    WorkflowManager workflowManager();
    DDBService ddbService();
    StartCreateDeviceDAO startCreateDeviceDAO();
    ObjectMapper objectMapper();
    ApigService apigService();
}