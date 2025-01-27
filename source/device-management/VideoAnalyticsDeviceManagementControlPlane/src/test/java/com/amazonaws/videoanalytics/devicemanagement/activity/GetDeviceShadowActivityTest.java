package com.amazonaws.videoanalytics.devicemanagement.activity;

import java.io.IOException;

import com.amazonaws.videoanalytics.devicemanagement.ShadowMap;
import com.amazonaws.videoanalytics.devicemanagement.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.GetDeviceShadowResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.json.JSONObject;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.iotdataplane.model.ThrottlingException;

import java.text.ParseException;
import java.util.Map;
import static java.util.Map.entry;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.THROTTLING_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_BODY_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DEVICE_ID;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.MOCK_AWS_REGION;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.SHADOW_NAME;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class GetDeviceShadowActivityTest {
    @InjectMocks
    GetDeviceShadowActivity getDeviceShadowActivity;
    @Mock
    private IotService iotService;
    @Mock
    private Context context;
    @Mock
    private LambdaLogger logger;

    private final Map<String, Object> lambdaProxyRequest = Map.ofEntries(
        entry(PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY, Map.ofEntries(
            entry(PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY, DEVICE_ID)
        )),
        entry(PROXY_LAMBDA_BODY_KEY, "{\"shadowName\": \"" + SHADOW_NAME + "\"}")
    );

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    public void handleRequest_WhenValidRequest_ReturnsResponse() throws IOException {
        JSONObject stateDocument = new JSONObject().put("shadowName", "true");
        GetDeviceShadowResponseContent responseFromIotService = GetDeviceShadowResponseContent
                .builder()
                .shadowPayload(ShadowMap.builder()
                    .shadowName(SHADOW_NAME)
                    .stateDocument(stateDocument.toMap())
                    .build())
                .build();

        when(iotService.getDeviceShadow(DEVICE_ID, SHADOW_NAME)).thenReturn(responseFromIotService);
        Map<String, Object> responseMap = getDeviceShadowActivity.handleRequest(lambdaProxyRequest, context);
        GetDeviceShadowResponseContent getDeviceShadowResponse = GetDeviceShadowResponseContent.fromJson(parseBody(responseMap));
        assertEquals(SHADOW_NAME, getDeviceShadowResponse.getShadowPayload().getShadowName());
        assertEquals(stateDocument.toMap(), getDeviceShadowResponse.getShadowPayload().getStateDocument());
    }

    @Test
    public void handleRequest_WhenValidRequestNoShadowName_ReturnsResponse() throws IOException {
        JSONObject stateDocument = new JSONObject().put("shadowName", "false");
        GetDeviceShadowResponseContent responseFromIotService = GetDeviceShadowResponseContent
                .builder()
                .shadowPayload(ShadowMap.builder()
                    .shadowName(null)
                    .stateDocument(stateDocument.toMap())
                    .build())
                .build();

        when(iotService.getDeviceShadow(DEVICE_ID, SHADOW_NAME)).thenReturn(responseFromIotService);
        Map<String, Object> responseMap = getDeviceShadowActivity.handleRequest(lambdaProxyRequest, context);
        GetDeviceShadowResponseContent getDeviceShadowResponse = GetDeviceShadowResponseContent.fromJson(parseBody(responseMap));
        assertEquals(null, getDeviceShadowResponse.getShadowPayload().getShadowName());
        assertEquals(stateDocument.toMap(), getDeviceShadowResponse.getShadowPayload().getStateDocument());
    }

    @Test
    public void handleRequest_WhenEmptyDeviceId_ThrowsValidationException() throws IOException {
        Map<String, Object> lambdaProxyRequestEmptyDeviceId = Map.ofEntries(
            entry(PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY, Map.ofEntries(
                entry(PROXY_LAMBDA_REQUEST_DEVICE_ID_PATH_PARAMETER_KEY, ""),
                entry(PROXY_LAMBDA_BODY_KEY, "{\"shadowName\": \"" + SHADOW_NAME + "\"}")
            ))
        );
        Map<String, Object> responseMap = getDeviceShadowActivity.handleRequest(lambdaProxyRequestEmptyDeviceId, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), INVALID_INPUT_EXCEPTION);
    }

    @Test
    public void handleRequest_WhenThrottlingException_ThrowsInternalServerException() throws IOException, ParseException, JsonProcessingException {
        when(iotService.getDeviceShadow(DEVICE_ID, SHADOW_NAME)).thenThrow(ThrottlingException.builder().build());
        Map<String, Object> responseMap = getDeviceShadowActivity.handleRequest(lambdaProxyRequest, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), THROTTLING_EXCEPTION);
    }

    @Test
    public void handleRequest_WhenRuntimeException_ThrowsInternalServerException() throws IOException, ParseException, JsonProcessingException {
        when(iotService.getDeviceShadow(DEVICE_ID, SHADOW_NAME)).thenThrow(RuntimeException.class);
        Map<String, Object> responseMap = getDeviceShadowActivity.handleRequest(lambdaProxyRequest, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        InternalServerExceptionResponseContent exception = InternalServerExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), INTERNAL_SERVER_EXCEPTION);
    }

    @Test
    public void getDeviceShadowActivity_InjectsDependencies() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        environmentVariables.set("AWS_REGION", MOCK_AWS_REGION);
        GetDeviceShadowActivity getDeviceShadowActivityDagger = new GetDeviceShadowActivity();
        getDeviceShadowActivityDagger.assertPrivateFieldNotNull();
    }
}
