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
import software.amazon.awssdk.services.iot.model.CertificateStatus;
import com.amazonaws.videoanalytics.devicemanagement.Status;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Inject;
import java.util.Map;
import static com.amazonaws.videoanalytics.devicemanagement.utils.WorkflowConstants.*;
import com.amazonaws.videoanalytics.devicemanagement.utils.annotations.ExcludeFromJacocoGeneratedReport;

public class SetLoggerConfigHandler implements RequestHandler<Map<String, String>, Void> {

    private final String ENTERING_MESSAGE = "Entering " + this.getClass().getName() + " method.";
    private static final String JOB_ID = "jobId";

    private final IotService iotService;
    private final StartCreateDeviceDAO startCreateDeviceDAO;

    @Inject
    public SetLoggerConfigHandler(IotService iotService, StartCreateDeviceDAO startCreateDeviceDAO) {
        this.iotService = iotService;
        this.startCreateDeviceDAO = startCreateDeviceDAO;
    }

    @ExcludeFromJacocoGeneratedReport
    public SetLoggerConfigHandler() {
        AWSVideoAnalyticsDMControlPlaneComponent component = DaggerAWSVideoAnalyticsDMControlPlaneComponent.create();
        component.inject(this);
        this.iotService = component.iotService();
        this.startCreateDeviceDAO = component.startCreateDeviceDAO();
    }

    @Override
    public Void handleRequest(Map<String, String> requestParams, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log(ENTERING_MESSAGE);

        CreateDevice createDevice = startCreateDeviceDAO.load((String) requestParams.get(JOB_ID));

        try {
            iotService.publishLogConfigurationToProvisioningShadow(createDevice.getDeviceId());
        } catch (IotException e) {
            logger.log("Failed to publish to provisioning shadow.");
            ExceptionTranslator.translateIotExceptionToRetryable(e);
        }
        

        createDevice.setJobStatus(Status.COMPLETED.toString());
        startCreateDeviceDAO.save(createDevice);
        
        return null;
    }

}