package com.amazonaws.videoanalytics.videologistics.dao;

import com.amazonaws.videoanalytics.videologistics.SourceType;
import com.amazonaws.videoanalytics.videologistics.Status;
import com.amazonaws.videoanalytics.videologistics.schema.PlaybackSession.PlaybackSession;
import com.amazonaws.videoanalytics.videologistics.schema.Source;
import com.amazonaws.videoanalytics.videologistics.schema.PlaybackSession.StreamSource;
import com.amazonaws.videoanalytics.videologistics.schema.status.WebRTCConnectionStatus;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import javax.inject.Inject;
import java.util.Optional;

import static com.amazonaws.services.lambda.runtime.LambdaRuntime.getLogger;

@Log4j2
public class PlaybackSessionDAO {
    private final LambdaLogger logger = getLogger();
    private final DynamoDbTable<PlaybackSession> playbackSessionTable;

    @Inject
    public PlaybackSessionDAO(final DynamoDbTable<PlaybackSession> playbackSessionTable) {
        this.playbackSessionTable = playbackSessionTable;
    }

    /**
     * Saves the playback session object. 
     */
    public void save(final PlaybackSession playbackSession) {
        logger.log("Starting save for playbackSession");
        // Session status should always map to status for DAO
        // this call throws an error if it does not
        try {
            final Status status = Status.valueOf(playbackSession.getSessionStatus());
            this.playbackSessionTable.putItem(playbackSession);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(String.format("Invalid session status. Please make sure it is one of the defined" +
                    "InternalSession statuses."));
        }
    }

    public PlaybackSession load(final String sessionId) {
        logger.log(String.format("Loading playback session %s", sessionId));
        return this.playbackSessionTable.getItem(Key.builder()
                .partitionValue(sessionId)
                .build());
    }

    /**
     * Safely update the WebRTC status i.e. only update the status if it is a valid state transition
     * Note that this method expects a PlaybackSession record to exist in the table. If not, it throws
     * a RuntimeException.
     */
    public void safeUpdateDeviceWebRTCStatus(final String sessionId,
                                             final WebRTCConnectionStatus targetStatus) {
        final PlaybackSession playbackSession = this.load(sessionId);
        if (playbackSession == null) {
            throw new RuntimeException(String.format("PlaybackSession with sessionId: %s" +
                    " was not found. This is unexpected.", sessionId));
        }

        Optional<StreamSource> webRTCSource = Optional.empty();
        if (playbackSession.getStreamSource() != null) {
            webRTCSource = playbackSession.getStreamSource()
                    .stream()
                    .filter(s -> SourceType.WEBRTC.equals(s.getStreamSessionType()))
                    .findFirst();
        }

        if (webRTCSource.isEmpty()) {
            throw new RuntimeException(String.format("PlaybackSession with sessionId: %s" +
                    " was found but, does not have a WebRTC StreamSource. " +
                    "This is unexpected.", sessionId));
        }

        final Source sourceInfo = webRTCSource.get().getSource();
        final WebRTCConnectionStatus from = WebRTCConnectionStatus.valueOf(sourceInfo.getPeerConnectionState());

        logger.log(String.format("Requested DeviceWebRTCStatus transition from %s to %s.", from, targetStatus));
        if (!from.isValidTransitionTo(targetStatus)) {
            logger.log(String.format("Invalid DeviceWebRTCStatus transition from %s to %s. " +
                    "Assuming this is a stale update and ignoring update request.", from, targetStatus));
            return;
        }

        sourceInfo.setPeerConnectionState(targetStatus.name());
        this.save(playbackSession);
    }
}
