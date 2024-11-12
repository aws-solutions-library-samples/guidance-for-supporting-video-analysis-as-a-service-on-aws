package com.amazonaws.videoanalytics.devicemanagement.workflow.createdevice;

import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import com.amazonaws.videoanalytics.devicemanagement.dagger.DaggerAWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.dagger.AWSVideoAnalyticsDMControlPlaneComponent;
import com.amazonaws.videoanalytics.devicemanagement.exceptions.ExceptionTranslator;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.services.iot.model.IotException;
import javax.inject.Inject;
import java.util.Map;
import static com.amazonaws.videoanalytics.devicemanagement.utils.WorkflowConstants.*;


public class CreateDeviceHandler implements RequestHandler<Map<String, String>, Void> {
    private final String ENTERING_MESSAGE = "Entering " + this.getClass().getName() + " method.";
    private static final String JOB_ID = "jobId";
    private final IotService iotService;
    private final StartCreateDeviceDAO startCreateDeviceDAO;
    
    @Inject
    public CreateDeviceHandler(IotService iotService, StartCreateDeviceDAO startCreateDeviceDAO) {
        this.iotService = iotService;
        this.startCreateDeviceDAO = startCreateDeviceDAO;
    }
    @Override
    public Void handleRequest(Map<String, String> requestParams, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log(ENTERING_MESSAGE);
        CreateDevice createDevice = startCreateDeviceDAO.load(requestParams.get(JOB_ID));
        try {
            iotService.workflowRegisterDevice(createDevice.getCertificateId(), createDevice.getDeviceId());
        } catch (IotException e) {
            logger.log(e.getMessage());
            ExceptionTranslator.translateIotExceptionToRetryable(e);
        }
        startCreateDeviceDAO.save(createDevice);
        return null;
    }
}