package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.videologistics.CreatePlaybackSessionResponseContent;
import com.amazonaws.videoanalytics.videologistics.SourceInfo;
import com.amazonaws.videoanalytics.videologistics.SourceType;
import com.amazonaws.videoanalytics.videologistics.StreamSource;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.dependency.kvs.KvsService;
import com.amazonaws.videoanalytics.videologistics.validator.DeviceValidator;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.NoDataRetentionException;

import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Map;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.END_TIME_WITHIN_A_DAY;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.NO_DATA_RETENTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.START_TIME_GREATER_THAN_OR_EQUAL_TO_END_TIME;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_BODY_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.DEVICE_ID;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.END_TIMESTAMP;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.HLS_STREAMING_URL;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.MOCK_AWS_REGION;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.START_TIMESTAMP;

import static java.util.Map.entry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

public class CreatePlaybackSessionActivityTest {
    @Mock
    private DeviceValidator deviceValidator;
    @Mock
    private KvsService kvsService;
    @Mock
    private LambdaLogger logger;
    @Mock
    private Context context;

    @InjectMocks
    private CreatePlaybackSessionActivity createPlaybackSessionActivity;

    private final Map<String, Object> lambdaProxyRequest = Map.ofEntries(
        entry(PROXY_LAMBDA_BODY_KEY, "{\"deviceId\": \"" + DEVICE_ID + "\", \"startTime\": \""+ START_TIMESTAMP +"\", \"endTime\": \""+ END_TIMESTAMP + "\"}")
    );

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(context.getLogger()).thenReturn(logger);
        doNothing().when(deviceValidator).validateDeviceExists(DEVICE_ID);
    }

    @Test
    public void handleRequest_WhenValidRequest_ReturnsResponse() throws IOException {
        SourceInfo source = SourceInfo.builder()
                .hLSStreamingURL(HLS_STREAMING_URL)
                .build();
        StreamSource streamSource = StreamSource.builder()
                .sourceType(SourceType.HLS)
                .source(source)
                .build();
        when(kvsService.getStreamingSessionURL(any(), any(), any())).thenReturn(streamSource);
        Map<String, Object> response = createPlaybackSessionActivity.handleRequest(lambdaProxyRequest, context);

        CreatePlaybackSessionResponseContent createPlaybackSessionResponse = 
            CreatePlaybackSessionResponseContent.fromJson(parseBody(response));
        StreamSource responseStreamSource = createPlaybackSessionResponse.getStreamSources().get(0);
        assertEquals(SourceType.HLS, responseStreamSource.getSourceType());
        assertEquals(HLS_STREAMING_URL, responseStreamSource.getSource().gethLSStreamingURL());
    }

    @Test
    public void handleRequest_WhenNullRequest_ThrowsValidationException() throws IOException {
        Map<String, Object> response = createPlaybackSessionActivity.handleRequest(null, context);
        assertEquals(response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(response));
        assertEquals(exception.getMessage(), INVALID_INPUT_EXCEPTION);
    }

    @Test
    public void handleRequest_WhenEmptyRequest_ThrowsValidationException() throws IOException {
        Map<String, Object> lambdaProxyRequestEmpty = Map.ofEntries();
        Map<String, Object> responseMap = createPlaybackSessionActivity.handleRequest(lambdaProxyRequestEmpty, context);
        assertEquals(responseMap.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(responseMap));
        assertEquals(exception.getMessage(), INVALID_INPUT_EXCEPTION);
    }

    @Test
    public void handleRequest_WhenLengthGreaterThanADay_ThrowsValidationException() throws IOException {
        String endTimeStamp = "2023-02-18T16:26:01Z";
        Map<String, Object> lambdaProxyRequestLengthGreaterThanADay = Map.ofEntries(
            entry(PROXY_LAMBDA_BODY_KEY, "{\"deviceId\": \"" + DEVICE_ID + "\", \"startTime\": \""+ START_TIMESTAMP +"\", \"endTime\": \""+ endTimeStamp + "\"}")
        );

        Map<String, Object> response = createPlaybackSessionActivity.handleRequest(lambdaProxyRequestLengthGreaterThanADay, context);
        assertEquals(response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(response));
        assertEquals(exception.getMessage(), END_TIME_WITHIN_A_DAY);
    }

    @Test
    public void handleRequest_WhenStartAfterEnd_ThrowsValidationException() throws IOException {
        Map<String, Object> lambdaProxyRequestStartAfterEnd = Map.ofEntries(
            entry(PROXY_LAMBDA_BODY_KEY, "{\"deviceId\": \"" + DEVICE_ID + "\", \"startTime\": \""+ END_TIMESTAMP +"\", \"endTime\": \""+ START_TIMESTAMP + "\"}")
        );

        Map<String, Object> response = createPlaybackSessionActivity.handleRequest(lambdaProxyRequestStartAfterEnd, context);
        assertEquals(response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(response));
        assertEquals(exception.getMessage(), START_TIME_GREATER_THAN_OR_EQUAL_TO_END_TIME);
    }

    @Test
    public void handleRequest_WhenNoDataRetention_ThrowsValidationException() throws IOException {
        when(kvsService.getStreamingSessionURL(any(), any(), any())).thenThrow(NoDataRetentionException.builder().build());
        Map<String, Object> response = createPlaybackSessionActivity.handleRequest(lambdaProxyRequest, context);

        assertEquals(response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(response));
        assertEquals(exception.getMessage(), NO_DATA_RETENTION);
    }

    @Test
    public void createPlaybackSessionActivity_InjectsDependencies() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        environmentVariables.set("AWS_REGION", MOCK_AWS_REGION);
        CreatePlaybackSessionActivity createPlaybackSessionActivityDagger = new CreatePlaybackSessionActivity();
        createPlaybackSessionActivityDagger.assertPrivateFieldNotNull();
    }
}
