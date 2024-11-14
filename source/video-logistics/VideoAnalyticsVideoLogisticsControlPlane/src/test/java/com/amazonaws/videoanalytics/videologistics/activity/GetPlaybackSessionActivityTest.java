package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.videologistics.GetPlaybackSessionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ResourceNotFoundExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.Status;
import com.amazonaws.videoanalytics.videologistics.SourceType;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.schema.PlaybackSession.PlaybackSession;
import com.amazonaws.videoanalytics.videologistics.schema.PlaybackSession.StreamSource;
import com.amazonaws.videoanalytics.videologistics.schema.Source;

import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.MISSING_RESOURCE;
import static com.amazonaws.videoanalytics.videologistics.schema.util.GuidanceVLConstants.CLIENT_ID;
import static com.amazonaws.videoanalytics.videologistics.schema.util.GuidanceVLConstants.DEVICE_ID;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.END_TIMESTAMP_DATE;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.HLS;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.MOCK_AWS_REGION;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.PLAYBACK_SIGNALING_CHANNEL_ARN;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.SESSION_ID;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.START_TIMESTAMP_DATE;
import static java.util.Map.entry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.amazonaws.videoanalytics.videologistics.dao.PlaybackSessionDAO;

public class GetPlaybackSessionActivityTest {
    @Mock
    private PlaybackSessionDAO playbackSessionDAO;
    @Mock
    private LambdaLogger logger;
    @Mock
    private Context context;

    @InjectMocks
    private GetPlaybackSessionActivity getPlaybackSessionActivity;

    @Captor
    ArgumentCaptor<PlaybackSession> playbackSessionArgumentCaptor = ArgumentCaptor.forClass(PlaybackSession.class);

    private final Map<String, Object> lambdaProxyRequest = Map.ofEntries(
        entry(PROXY_LAMBDA_REQUEST_PATH_PARAMETERS_KEY, Map.ofEntries(
            entry("sessionId", SESSION_ID)
        ))
    );

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    public void handleRequest_WhenValidRequest_ReturnsResponse() throws IOException {
        Source source = Source.builder()
                .expirationTime(END_TIMESTAMP_DATE)
                .clientId(CLIENT_ID)
                .hlsStreamingURL(HLS)
                .startTime(START_TIMESTAMP_DATE)
                .signalingChannelURL(PLAYBACK_SIGNALING_CHANNEL_ARN).build();
        StreamSource streamSource = StreamSource.builder().streamSessionType(SourceType.HLS).source(source).build();
        List<StreamSource> streamSources = new ArrayList<>();

        streamSources.add(streamSource);

        PlaybackSession playbackSession = PlaybackSession.builder()
                .sessionStatus(Status.COMPLETED.toString())
                .createdAt(START_TIMESTAMP_DATE.toInstant())
                .lastUpdated(END_TIMESTAMP_DATE.toInstant())
                .sessionId(SESSION_ID)
                .startTime(START_TIMESTAMP_DATE)
                .endTime(END_TIMESTAMP_DATE)
                .deviceId(DEVICE_ID)
                .streamSource(streamSources)
                .build();

        when(playbackSessionDAO.load(any())).thenReturn(playbackSession);

        Map<String, Object> response = getPlaybackSessionActivity.handleRequest(lambdaProxyRequest, context);
        System.out.println(response);
        GetPlaybackSessionResponseContent getPlaybackSessionResponse = GetPlaybackSessionResponseContent.fromJson(parseBody(response));

        assertEquals(Status.COMPLETED, getPlaybackSessionResponse.getStatus());
        assertEquals(SESSION_ID, getPlaybackSessionResponse.getSessionId());
        assertEquals(DEVICE_ID, getPlaybackSessionResponse.getDeviceId());
        assertEquals(START_TIMESTAMP_DATE, getPlaybackSessionResponse.getStartTime());
        assertEquals(END_TIMESTAMP_DATE, getPlaybackSessionResponse.getEndTime());
        assertEquals(CLIENT_ID, getPlaybackSessionResponse.getStreamSources().get(0).getSource().getClientId());
        assertEquals(HLS, getPlaybackSessionResponse.getStreamSources().get(0).getSource().gethLSStreamingURL());
        assertEquals(PLAYBACK_SIGNALING_CHANNEL_ARN, getPlaybackSessionResponse.getStreamSources().get(0).getSource().getSignalingChannelURL());
    }

    @Test
    public void handleRequest_WhenNullRequest_ThrowsValidationException() throws IOException {
        Map<String, Object> response = getPlaybackSessionActivity.handleRequest(null, context);
        assertEquals(response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 400);
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.fromJson(parseBody(response));
        assertEquals(exception.getMessage(), INVALID_INPUT_EXCEPTION);
    }

    @Test
    public void handleRequest_WhenSessionIdNotFound_ThrowsResourceNotFoundException() throws IOException {
        when(playbackSessionDAO.load(any())).thenReturn(null);
        Map<String, Object> response = getPlaybackSessionActivity.handleRequest(lambdaProxyRequest, context);
        assertEquals(response.get(PROXY_LAMBDA_RESPONSE_STATUS_CODE_KEY), 404);
        ResourceNotFoundExceptionResponseContent exception = ResourceNotFoundExceptionResponseContent.fromJson(parseBody(response));
        assertEquals(exception.getMessage(), String.format(MISSING_RESOURCE, SESSION_ID));
    }

    @Test
    public void getPlaybackSessionActivity_InjectsDependencies() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        environmentVariables.set("AWS_REGION", MOCK_AWS_REGION);
        GetPlaybackSessionActivity getPlaybackSessionActivityDagger = new GetPlaybackSessionActivity();
        getPlaybackSessionActivityDagger.assertPrivateFieldNotNull();
    }
}
