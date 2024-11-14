package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.GetPlaybackSessionResponseContent;
import com.amazonaws.videoanalytics.videologistics.PeerConnectionState;
import com.amazonaws.videoanalytics.videologistics.ResourceNotFoundExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.Status;
import com.amazonaws.videoanalytics.videologistics.StreamSource;
import com.amazonaws.videoanalytics.videologistics.SourceInfo;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.schema.Source;
import com.amazonaws.videoanalytics.videologistics.schema.PlaybackSession.PlaybackSession;
import com.amazonaws.videoanalytics.videologistics.utils.annotations.ExcludeFromJacocoGeneratedReport;

import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dao.PlaybackSessionDAO;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.MISSING_RESOURCE;

import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parsePathParameter;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;

/**
 * Class for handling the request for GetPlaybackSession API.
 */
public class GetPlaybackSessionActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final PlaybackSessionDAO playbackSessionDAO;

    @Inject
    public GetPlaybackSessionActivity(final PlaybackSessionDAO playbackSessionDAO) {
        this.playbackSessionDAO = playbackSessionDAO;
    }

    public GetPlaybackSessionActivity() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.playbackSessionDAO = component.getPlaybackSessionDAO();
    }

    // used for unit tests
    @ExcludeFromJacocoGeneratedReport
    public void assertPrivateFieldNotNull() {
        if (playbackSessionDAO == null) {
            throw new AssertionError("private field is null");
        }
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Entered GetPlaybackSession method");

        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .build();

        if (Objects.isNull(input)) {
            return serializeResponse(400, exception.toJson());
        }

        String sessionId;
        try {
            sessionId = parsePathParameter(input, "sessionId");
        } catch (Exception e) {
            logger.log(e.toString());
            return serializeResponse(400, exception.toJson());
        }

        PlaybackSession playbackSession = playbackSessionDAO.load(sessionId);

        if (playbackSession == null) {
            ResourceNotFoundExceptionResponseContent resourceNotFoundException = ResourceNotFoundExceptionResponseContent.builder()
                    .message(String.format(MISSING_RESOURCE, sessionId))
                    .build();
            return serializeResponse(404, resourceNotFoundException.toJson());
        }

        GetPlaybackSessionResponseContent response = GetPlaybackSessionResponseContent.builder()
                .status(Status.fromValue(playbackSession.getSessionStatus()))
                .createdAt(Date.from(playbackSession.getCreatedAt()))
                .lastUpdatedAt(Date.from(playbackSession.getLastUpdated()))
                .sessionId(playbackSession.getSessionId())
                .deviceId(playbackSession.getDeviceId())
                .startTime(playbackSession.getStartTime())
                .endTime(playbackSession.getEndTime())
                .build();

        // We are returning only the error message and error code if the playback workflow has failed
        // so we don't expose the signaling channel URLs to the caller
        if (playbackSession.getSessionStatus().equals(Status.FAILED.toString())) {
            response.setErrorMessage(playbackSession.getErrorMessage());
            return serializeResponse(200, response.toJson());
        }

        // Do not return stream sources information if the status is currently not completed
        // so we don't expose the signaling channel URLs to the caller
        if (!playbackSession.getSessionStatus().equals(Status.COMPLETED.toString())) {
            return serializeResponse(200, response.toJson());
        }

        List<StreamSource> streamSources = new ArrayList<>();
        if (playbackSession.getStreamSource() != null) {
            for (int i = 0; i < playbackSession.getStreamSource().size(); i++) {
                Source source = playbackSession.getStreamSource().get(i).getSource();
                SourceInfo sourceInfo = SourceInfo
                        .builder().clientId(source.getClientId())
                        .signalingChannelURL(source.getSignalingChannelURL())
                        .expirationTime(source.getExpirationTime())
                        .hLSStreamingURL(source.getHlsStreamingURL())
                        .build();
                if (source.getPeerConnectionState() != null) {
                    sourceInfo.setPeerConnectionState(PeerConnectionState.fromValue(source.getPeerConnectionState()));
                }

                StreamSource streamSource = new StreamSource();
                streamSource.setSourceType(playbackSession.getStreamSource().get(i).getStreamSessionType());
                streamSource.setSource(sourceInfo);
                streamSource.setStartTime(source.getStartTime());

                streamSources.add(streamSource);
                response.setStreamSources(streamSources);
            }
        }

        System.out.println("response: " + response);

        return serializeResponse(200, response.toJson());
    }
}
