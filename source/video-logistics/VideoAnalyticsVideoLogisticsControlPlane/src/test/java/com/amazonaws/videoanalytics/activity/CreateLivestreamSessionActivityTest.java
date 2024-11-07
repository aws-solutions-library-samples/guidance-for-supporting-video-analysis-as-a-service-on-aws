package com.amazonaws.videoanalytics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.videoanalytics.client.KvsClient.KvsClientFactory;
import com.amazonaws.videoanalytics.client.KvsClient.KvsClientWrapper;
import com.amazonaws.videoanalytics.client.KvsSignalingClient.KvsSignalingClientFactory;
import com.amazonaws.videoanalytics.client.KvsSignalingClient.KvsSignalingClientWrapper;
import com.amazonaws.videoanalytics.dao.LivestreamSessionDAO;
import com.amazonaws.videoanalytics.schema.LivestreamSession.LivestreamSession;
import com.amazonaws.videoanalytics.utils.DateTime;
import com.amazonaws.videoanalytics.utils.GuidanceUUIDGenerator;
import com.amazonaws.videoanalytics.utils.KVSWebRTCUtils;
import com.amazonaws.videoanalytics.validator.DeviceValidator;
import com.amazonaws.videoanalytics.videologistics.CreateLivestreamSessionResponseContent;
import com.amazonaws.videoanalytics.videologistics.IceServer;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import com.amazonaws.videoanalytics.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.amazonaws.videoanalytics.exceptions.GuidanceExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.exceptions.GuidanceExceptionMessage.RESOURCE_NOT_FOUND;
import static com.amazonaws.videoanalytics.schema.util.GuidanceVLConstants.CLIENT_ID;
import static com.amazonaws.videoanalytics.schema.util.GuidanceVLConstants.DEVICE_ID;
import static com.amazonaws.videoanalytics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.utils.SchemaConst.SESSION_ID;
import static com.amazonaws.videoanalytics.utils.SchemaConst.WORKFLOW_NAME;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

public class CreateLivestreamSessionActivityTest {

    private final static Date CURRENT_TIME = new Date();

    @Mock
    GuidanceUUIDGenerator guidanceUUIDGenerator;
    @Mock
    DateTime dateTime;
    @Captor
    ArgumentCaptor<LivestreamSession> livestreamSessionArgumentCaptor;
    @Mock
    private LivestreamSessionDAO livestreamSessionDAO;
    @Mock
    private DeviceValidator deviceValidator;
    @Mock
    private KvsClientFactory kvsClientFactory;
    @Mock
    private KvsSignalingClientFactory kvsSignalingClientFactory;
    @Mock
    private KVSWebRTCUtils kvsWebRTCUtils;
    @Mock
    private KvsClientWrapper kvsClientWrapper;
    @Mock
    private AwsCredentialsProvider credentialsProvider;
    @Mock
    private KvsSignalingClientWrapper kvsSignalingClientWrapper;
    @Mock
    private List<IceServer> iceServerListList;

    @Mock
    private Context context;
    private String region = "us-west-2";

    private ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private CreateLivestreamSessionActivity createLivestreamSessionActivity;

    private final Map<String, Object> lambdaProxyRequest = Map.ofEntries(
            entry("pathParameters", Map.ofEntries(
                    entry("deviceId", DEVICE_ID),
                    entry("clientId", CLIENT_ID)
            ))
    );

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(dateTime.getTime()).thenReturn(CURRENT_TIME);
        when(guidanceUUIDGenerator.generateUUIDRandom()).thenReturn(SESSION_ID).thenReturn(WORKFLOW_NAME);
        doNothing().when(deviceValidator).validateDeviceExists(DEVICE_ID);
    }

    @Test
    public void createLivestreamSessionActivityTest() throws IOException {
        when(kvsClientFactory.create(any())).thenReturn(kvsClientWrapper);
        when(kvsWebRTCUtils.sign(any(), any(), any(), any(), any())).thenReturn("presignedUrl");
        when(kvsSignalingClientFactory.create(any(), any())).thenReturn(kvsSignalingClientWrapper);
        when(kvsSignalingClientWrapper.getSyncIceServerConfigs(any(), any())).thenReturn(iceServerListList);
        Map<String, Object> response = createLivestreamSessionActivity.handleRequest(lambdaProxyRequest, context);
        CreateLivestreamSessionResponseContent createLivestreamSessionResponse =
                (CreateLivestreamSessionResponseContent) response.get("body");
        assertEquals(CLIENT_ID, createLivestreamSessionResponse.getClientId());
        assertEquals("presignedUrl", createLivestreamSessionResponse.getSignalingChannelURL());
        assertEquals(iceServerListList, createLivestreamSessionResponse.getIceServers());
    }

    @Test
    public void nullCheckForCreateLivestreamSessionActivityTest() throws IOException {
        Map<String, Object> response = createLivestreamSessionActivity.handleRequest(null, context);
        assertEquals(response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(response));
        assertEquals(exception.getMessage(), INVALID_INPUT_EXCEPTION);
    }

    @Test
    public void createLivestreamSessionActivityDeviceNotFound() {
        doThrow(new ResourceNotFoundException(RESOURCE_NOT_FOUND)).when(deviceValidator).validateDeviceExists(DEVICE_ID);
        final Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            createLivestreamSessionActivity.handleRequest(lambdaProxyRequest, context);
        });

        assertTrue(exception.getMessage().contains(RESOURCE_NOT_FOUND));
    }
}
