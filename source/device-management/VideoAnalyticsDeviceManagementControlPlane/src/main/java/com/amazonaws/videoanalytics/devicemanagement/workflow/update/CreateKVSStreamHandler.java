package com.amazonaws.videoanalytics.devicemanagement.workflow.update;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.dependency.ddb.DDBService;
import com.amazonaws.videoanalytics.devicemanagement.dagger.AWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.DaggerAWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import com.amazonaws.videoanalytics.devicemanagement.utils.annotations.ExcludeFromJacocoGeneratedReport;

import javax.inject.Inject;
import java.util.Map;

public class CreateKVSStreamHandler implements RequestHandler<Map<String, Object>, Void> {
    private static final String JOB_ID = "jobId";
    private final String ENTERING_MESSAGE = "Entering " + this.getClass().getName() + " method.";
    
    private final DDBService ddbService;
    private final StartCreateDeviceDAO startCreateDeviceDAO;

    @Inject
    public CreateKVSStreamHandler(DDBService ddbService, StartCreateDeviceDAO startCreateDeviceDAO) {
        this.ddbService = ddbService;
        this.startCreateDeviceDAO = startCreateDeviceDAO;
    }

    @ExcludeFromJacocoGeneratedReport
    public CreateKVSStreamHandler() {
        AWSVideoAnalyticsDMControlPlaneComponent component = DaggerAWSVideoAnalyticsDMControlPlaneComponent.create();
        component.inject(this);
        this.ddbService = component.ddbService();
        this.startCreateDeviceDAO = component.startCreateDeviceDAO();
    }

    @Override
    public Void handleRequest(final Map<String, Object> requestParams, final Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log(ENTERING_MESSAGE);

        // Get the job ID from request parameters
        String jobId = (String) requestParams.get(JOB_ID);
        
        // Load the create device record
        CreateDevice createdDevice = startCreateDeviceDAO.load(jobId);
        
        // TODO: Implement VL Client KVS stream creation & register device logic here
        
        // Update the device record if needed
        startCreateDeviceDAO.save(createdDevice);

        return null;
    }
}
