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

public class FailCreateDeviceHandler implements RequestHandler<Map<String, String>, Void> {
    private final String ENTERING_MESSAGE = "Entering " + this.getClass().getName() + " method.";
    private static final String FAILURE_REASON = "failureReason";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String EXCEPTION_MESSAGE = "Error parsing error message cause from step function %s";

    private final IotService iotService;
    private final StartCreateDeviceDAO startCreateDeviceDAO;
    private final ObjectMapper objectMapper;

    @Inject
    public FailCreateDeviceHandler(IotService iotService, StartCreateDeviceDAO startCreateDeviceDAO, ObjectMapper objectMapper) {
        this.iotService = iotService;
        this.startCreateDeviceDAO = startCreateDeviceDAO;
        this.objectMapper = objectMapper;
    }

    @ExcludeFromJacocoGeneratedReport
    public FailCreateDeviceHandler() {
        AWSVideoAnalyticsDMControlPlaneComponent component = DaggerAWSVideoAnalyticsDMControlPlaneComponent.create();
        component.inject(this);
        this.iotService = component.iotService();
        this.startCreateDeviceDAO = component.startCreateDeviceDAO();
        this.objectMapper = component.objectMapper();
    }

    @Override
    public Void handleRequest(Map<String, String> requestParams, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log(ENTERING_MESSAGE);

        CreateDevice createDevice = startCreateDeviceDAO.load(requestParams.get(JOB_ID));

        try {
            iotService.updateCertificate(createDevice.getCertificateId(), CertificateStatus.INACTIVE);
        } catch (IotException e) {
            logger.log(e.getMessage());
            ExceptionTranslator.translateIotExceptionToRetryable(e);
        }

        if (requestParams.containsKey(FAILURE_REASON)) {
            try {
                String failureReason = String.valueOf(objectMapper.readValue(
                        requestParams.get(FAILURE_REASON), Map.class).get(ERROR_MESSAGE));
                createDevice.setErrorMessage(failureReason);
            } catch (JsonProcessingException e) {
                logger.log(String.format(EXCEPTION_MESSAGE, e));
            }
        }

        createDevice.setJobStatus(Status.FAILED.toString());
        startCreateDeviceDAO.save(createDevice);

        return null;
    }
}

