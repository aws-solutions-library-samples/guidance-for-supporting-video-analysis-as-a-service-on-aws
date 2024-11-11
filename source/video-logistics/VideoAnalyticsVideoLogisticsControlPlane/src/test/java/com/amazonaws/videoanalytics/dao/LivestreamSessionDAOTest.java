package com.amazonaws.videoanalytics.videologistics.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amazonaws.videoanalytics.helper.ddb.DynamoDbEnhancedLocalExtension;
import com.amazonaws.videoanalytics.videologistics.schema.LivestreamSession.LivestreamSession;
import com.amazonaws.videoanalytics.videologistics.schema.Source;
import com.amazonaws.videoanalytics.videologistics.schema.status.InternalStreamingSessionStatus;
import com.amazonaws.videoanalytics.videologistics.schema.status.WebRTCConnectionStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

@ExtendWith(DynamoDbEnhancedLocalExtension.class)
@RequiredArgsConstructor
public class LivestreamSessionDAOTest {
    private static final String SESSION_ID = "s234";

    private final DynamoDbTable<LivestreamSession> playbackSessionTable;
    private LivestreamSessionDAO playbackSessionDAO;

    @BeforeEach
    public void setup() {
        playbackSessionDAO = new LivestreamSessionDAO(playbackSessionTable);
    }

    @Test
    public void saveTest() throws Exception {
        LivestreamSession playbackSession = LivestreamSession.builder()
                .sessionId(SESSION_ID)
                .sessionStatus(InternalStreamingSessionStatus.COMPLETED.toString())
                .build();
        playbackSessionDAO.save(playbackSession);
        LivestreamSession persistedLivestreamSession = playbackSessionDAO.load(SESSION_ID);
        Assertions.assertEquals(SESSION_ID, persistedLivestreamSession.getSessionId());
        Assertions.assertEquals(InternalStreamingSessionStatus.COMPLETED.toString(), persistedLivestreamSession.getSessionStatus());
    }

    @Test
    public void saveTestInvalidSessionStatus() {
        LivestreamSession playbackSession = LivestreamSession.builder()
                .sessionId(SESSION_ID)
                .sessionStatus("INVALID")
                .build();
        Assertions.assertThrows(RuntimeException.class, () -> {
            playbackSessionDAO.save(playbackSession);
        });
    }

    @Test
    public void safeUpdateDeviceWebRTCStatusUpdatesRecordForValidStatusTransition() {
        LivestreamSession playbackSession = LivestreamSession.builder()
                .sessionId(SESSION_ID)
                .source(null)
                .source(sampleSource("CONNECTING"))
                .sessionStatus(InternalStreamingSessionStatus.RUNNING.toString())
                .build();
        playbackSessionDAO.save(playbackSession);

        playbackSessionDAO.safeUpdateDeviceWebRTCStatus(
                SESSION_ID, WebRTCConnectionStatus.CONNECTED);

        // Status should be updated.
        LivestreamSession updatedSession = playbackSessionDAO.load(SESSION_ID);
        assertEquals("CONNECTED", updatedSession.getSource().getPeerConnectionState());
    }

    @Test
    public void safeUpdateDeviceWebRTCStatusIgnoresInvalidStatusTransition() {
        LivestreamSession playbackSession = LivestreamSession.builder()
                .sessionId(SESSION_ID)
                .source(sampleSource("CONNECTED"))
                .sessionStatus(InternalStreamingSessionStatus.RUNNING.toString())
                .build();
        playbackSessionDAO.save(playbackSession);

        playbackSessionDAO.safeUpdateDeviceWebRTCStatus(
                SESSION_ID, WebRTCConnectionStatus.CONNECTING);

        // Status should not be updated.
        LivestreamSession updatedSession = playbackSessionDAO.load(SESSION_ID);
        assertEquals("CONNECTED", updatedSession.getSource().getPeerConnectionState());
    }

    @Test
    public void safeUpdateDeviceWebRTCStatusThrowsRuntimeExceptionWhenRecordDoesNotExist() {
        assertThrows(RuntimeException.class, () -> {
            playbackSessionDAO.safeUpdateDeviceWebRTCStatus(
                    SESSION_ID, WebRTCConnectionStatus.CONNECTING);
        });
    }

    @Test
    public void safeUpdateDeviceWebRTCStatusThrowsExceptionWhenWebRTCSourceDoesNotExist() {
        LivestreamSession playbackSession = LivestreamSession.builder()
                .sessionId(SESSION_ID)
                .sessionStatus(InternalStreamingSessionStatus.RUNNING.toString())
                .build();
        playbackSessionDAO.save(playbackSession);

        assertThrows(RuntimeException.class, () -> {
            playbackSessionDAO.safeUpdateDeviceWebRTCStatus(
                    SESSION_ID, WebRTCConnectionStatus.CONNECTING);
        });
    }

    private Source sampleSource(String webRTCState) {
        return Source.builder()
                .peerConnectionState(webRTCState)
                .build();
    }
}
