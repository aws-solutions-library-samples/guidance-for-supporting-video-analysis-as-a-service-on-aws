package com.amazonaws.videoanalytics.devicemanagement.activity;

import java.io.IOException;

import com.amazonaws.videoanalytics.devicemanagement.ShadowMap;
import com.amazonaws.videoanalytics.devicemanagement.GetDeviceShadowResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.dependency.iot.IotService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.iot.model.ThrottlingException;

import java.text.ParseException;
import java.util.Map;
import static java.util.Map.entry;

import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.exceptions.VideoAnalyticsExceptionMessage.THROTTLING_EXCEPTION;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_BODY_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DEVICE_ID;
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
        entry("pathParameters", Map.ofEntries(
            entry("deviceId", DEVICE_ID)
        )),
        entry("body", "{\"shadowName\": \"" + SHADOW_NAME + "\"}")
    );

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    public void getDeviceShadowActivity_WhenValidRequest_ReturnsResponse() throws IOException {
        JSONObject stateDocument = new JSONObject().put("shadowName", "true");
        GetDeviceShadowResponseContent responseFromIotService = GetDeviceShadowResponseContent
                .builder()
                .shadowPayload(ShadowMap.builder()
                    .shadowName(SHADOW_NAME)
                    .stateDocument(stateDocument.toMap())
                    .build())
                .build();

        when(iotService.getDeviceShadow(DEVICE_ID, SHADOW_NAME)).thenReturn(responseFromIotService);
        Map<String, Object> response = getDeviceShadowActivity.handleRequest(lambdaProxyRequest, context);
        ObjectMapper mapper = new ObjectMapper();
        GetDeviceShadowResponseContent getDeviceShadowResponse = GetDeviceShadowResponseContent.fromJson(mapper.convertValue(response.get("body"), String.class));
        System.out.println(getDeviceShadowResponse);
        assertEquals(SHADOW_NAME, getDeviceShadowResponse.getShadowPayload().getShadowName());
        assertEquals(stateDocument.toMap(), getDeviceShadowResponse.getShadowPayload().getStateDocument());
    }

    @Test
    public void getDeviceShadowActivity_WhenValidRequestNoShadowName_ReturnsResponse() throws IOException {
        JSONObject stateDocument = new JSONObject().put("shadowName", "false");
        GetDeviceShadowResponseContent responseFromIotService = GetDeviceShadowResponseContent
                .builder()
                .shadowPayload(ShadowMap.builder()
                    .shadowName(null)
                    .stateDocument(stateDocument.toMap())
                    .build())
                .build();

        when(iotService.getDeviceShadow(DEVICE_ID, SHADOW_NAME)).thenReturn(responseFromIotService);
        Map<String, Object> response = getDeviceShadowActivity.handleRequest(lambdaProxyRequest, context);
        ObjectMapper mapper = new ObjectMapper();
        GetDeviceShadowResponseContent getDeviceShadowResponse = GetDeviceShadowResponseContent.fromJson(mapper.convertValue(response.get("body"), String.class));
        assertEquals(null, getDeviceShadowResponse.getShadowPayload().getShadowName());
        assertEquals(stateDocument.toMap(), getDeviceShadowResponse.getShadowPayload().getStateDocument());
    }

    @Test
    public void getDeviceShadowActivity_WhenEmptyDeviceId_ThrowsInvalidInputException() {
        Map<String, Object> lambdaProxyRequestEmptyDeviceId = Map.ofEntries(
            entry("pathParameters", Map.ofEntries(
                entry("deviceId", ""),
                entry("body", "{\"shadowName\": \"" + SHADOW_NAME + "\"}")
            ))
        );
        Map<String, Object> responseMap = getDeviceShadowActivity.handleRequest(lambdaProxyRequestEmptyDeviceId, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_BODY_KEY), INVALID_INPUT_EXCEPTION);
    }

    @Test
    public void getDeviceShadowActivity_WhenThrottlingException_ThrowsThrottlingException() throws ParseException, JsonProcessingException {
        when(iotService.getDeviceShadow(DEVICE_ID, SHADOW_NAME)).thenThrow(ThrottlingException.builder().build());
        Map<String, Object> responseMap = getDeviceShadowActivity.handleRequest(lambdaProxyRequest, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_BODY_KEY), THROTTLING_EXCEPTION);
    }

    @Test
    public void getDeviceShadowActivity_WhenRuntimeException_ThrowsInternalException() throws ParseException, JsonProcessingException {
        when(iotService.getDeviceShadow(DEVICE_ID, SHADOW_NAME)).thenThrow(RuntimeException.class);
        Map<String, Object> responseMap = getDeviceShadowActivity.handleRequest(lambdaProxyRequest, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 500);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_BODY_KEY), INTERNAL_SERVER_EXCEPTION);
    }
}
