package com.amazonaws.videoanalytics.devicemanagement.workflow.update;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.devicemanagement.dependency.ddb.DDBService;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.ListThingPrincipalsResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.KVS_DEVICE_IOT_POLICY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.WorkflowConstants.DEVICE_ENABLED_MESSAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AttachKvsAccessToCertHandlerTest {
    private static final String JOB_ID = "jobId";
    private static final String DEVICE_ID = "testDevice";
    private static final String DEVICE_STATE = "CREATED";
    private static final String PRINCIPAL = "arn:aws:iot:region:account:cert/123";

    @Mock
    private IotService iotService;

    @Mock
    private DDBService ddbService;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    private AttachKvsAccessToCertHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(context.getLogger()).thenReturn(logger);
        handler = new AttachKvsAccessToCertHandler(iotService, ddbService);
    }

    @Test
    void handleRequest_withValidDevice_shouldAttachPolicyAndUpdateDevice() {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(JOB_ID, "test-job-id");

        CreateDevice device = new CreateDevice();
        device.setDeviceId(DEVICE_ID);

        when(ddbService.getCreateDevice(any())).thenReturn(device);
        when(iotService.listThingPrincipals(DEVICE_ID))
            .thenReturn(ListThingPrincipalsResponse.builder()
                .principals(Collections.singletonList(PRINCIPAL))
                .build());

        handler.handleRequest(requestParams, context);

        verify(ddbService).getCreateDevice(requestParams.get(JOB_ID));
        verify(iotService).listThingPrincipals(DEVICE_ID);
        verify(iotService).attachPolicy(KVS_DEVICE_IOT_POLICY, PRINCIPAL);
        verify(iotService).messageDeviceProvisioningShadow(eq(DEVICE_ID), eq(DEVICE_ENABLED_MESSAGE));
        verify(ddbService).saveCreateDevice(device);
    }
}
