package com.amazonaws.videoanalytics.devicemanagement.workflow.update;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.devicemanagement.dagger.AWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.DaggerAWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dependency.ddb.DDBService;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.KVS_DEVICE_IOT_POLICY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.WorkflowConstants.DEVICE_ENABLED_MESSAGE;

public class AttachKvsAccessToCertHandler implements RequestHandler<Map<String, String>, Void> {
    private static final String ENTERING_MESSAGE = "Entering " + AttachKvsAccessToCertHandler.class.getName() + " method.";
    private static final String JOB_ID = "jobId";
    
    private final IotService iotService;
    private final DDBService ddbService;

    @Inject
    public AttachKvsAccessToCertHandler(IotService iotService, DDBService ddbService) {
        this.iotService = iotService;
        this.ddbService = ddbService;
    }

    @Override
    public Void handleRequest(final Map<String, String> requestParams, final Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log(ENTERING_MESSAGE);
        
        // Query DynamoDB
        CreateDevice createdDevice = ddbService.getCreateDevice(requestParams.get(JOB_ID));

        List<String> principals = iotService.listThingPrincipals(createdDevice.getDeviceId()).principals();
        
        // Log whether certificate was found
        if (principals.isEmpty()) {
            logger.log("No certificate found for device: " + createdDevice.getDeviceId());
        } else {
            logger.log("Found certificate for device: " + createdDevice.getDeviceId() + ", principal: " + principals.get(0));
        }

        // there's only 1 cert for the device
        if (!principals.isEmpty()) {
            // Attach KVS policy to the certificate
            iotService.attachPolicy(KVS_DEVICE_IOT_POLICY, principals.get(0));

            // notify the device
            String deviceId = createdDevice.getDeviceId();
            String currentDeviceState = createdDevice.getCurrentDeviceState();
            iotService.messageDeviceProvisioningShadow(deviceId, DEVICE_ENABLED_MESSAGE);
            iotService.clearDeviceProvisioningShadowField(deviceId, currentDeviceState.toLowerCase());
        }

        ddbService.saveCreateDevice(createdDevice);
        return null;
    }
}

