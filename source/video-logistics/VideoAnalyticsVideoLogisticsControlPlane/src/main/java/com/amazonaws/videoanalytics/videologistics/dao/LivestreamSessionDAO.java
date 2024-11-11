package com.amazonaws.videoanalytics.videologistics.dao;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.videologistics.schema.LivestreamSession.LivestreamSession;
import com.amazonaws.videoanalytics.videologistics.schema.Source;
import com.amazonaws.videoanalytics.videologistics.schema.status.InternalStreamingSessionStatus;
import com.amazonaws.videoanalytics.videologistics.schema.status.WebRTCConnectionStatus;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import javax.inject.Inject;

import static com.amazonaws.services.lambda.runtime.LambdaRuntime.getLogger;

public class LivestreamSessionDAO {
    private final LambdaLogger logger = getLogger();
    private final DynamoDbTable<LivestreamSession> livestreamSessionTable;

    @Inject
    public LivestreamSessionDAO(final DynamoDbTable<LivestreamSession> livestreamSessionTable) {
        this.livestreamSessionTable = livestreamSessionTable;
    }

    /**
     * Saves the livestream session object.
     */
    public void save(final LivestreamSession livestreamSession) {
        logger.log("Starting save for livestreamSession");
        // Session status should always map to InternalSession status for DAO
        // this call throws an error if it does not
        try {
            final InternalStreamingSessionStatus status =
                    InternalStreamingSessionStatus.valueOf(livestreamSession.getSessionStatus());
            this.livestreamSessionTable.putItem(livestreamSession);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(String.format("Invalid session status. Please make sure it is one of the defined" +
                    "InternalSession statuses."));
        }
    }

    public LivestreamSession load(final String sessionId) {
        logger.log(String.format("Loading livestream session %s ", sessionId));
        return this.livestreamSessionTable.getItem(Key.builder()
                .partitionValue(sessionId)
                .build());
    }

    /**
     * Safely update the WebRTC status i.e. only update the status if it is a valid state transition
     * Note that this method expects a LivestreamSession record to exist in the table. If not, it throws
     * a RuntimeException.
     */
    public void safeUpdateDeviceWebRTCStatus(final String sessionId,
                                             final WebRTCConnectionStatus targetStatus) {
        final LivestreamSession livestreamSession = this.load(sessionId);
        if (livestreamSession == null) {
            throw new RuntimeException(String.format("LivestreamSession with sessionId: %s" +
                    " was not found. This is unexpected.", sessionId));
        }

        final Source sourceInfo = livestreamSession.getSource();
        final WebRTCConnectionStatus from = WebRTCConnectionStatus.valueOf(sourceInfo.getPeerConnectionState());

        logger.log(String.format("Requested DeviceWebRTCStatus transition from %s to %s.", from, targetStatus));
        if (!from.isValidTransitionTo(targetStatus)) {
            logger.log(String.format("Invalid DeviceWebRTCStatus transition from %s to %s. " +
                    "Assuming this is a stale update and ignoring update request.", from, targetStatus));
            return;
        }

        sourceInfo.setPeerConnectionState(targetStatus.name());
        this.save(livestreamSession);
    }
}
