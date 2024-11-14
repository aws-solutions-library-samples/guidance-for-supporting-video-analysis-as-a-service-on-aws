package com.amazonaws.videoanalytics.videologistics.dao;

import com.amazonaws.videoanalytics.videologistics.SourceType;
import com.amazonaws.videoanalytics.videologistics.Status;
import com.amazonaws.videoanalytics.videologistics.schema.PlaybackSession.PlaybackSession;
import com.amazonaws.videoanalytics.videologistics.schema.Source;
import com.amazonaws.videoanalytics.videologistics.schema.PlaybackSession.StreamSource;
import com.amazonaws.videoanalytics.videologistics.schema.status.WebRTCConnectionStatus;
import com.google.common.collect.ImmutableList;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PlaybackSessionDAOTest {
    private static final String SESSION_ID = "s234";

    @Mock
    private DynamoDbTable<PlaybackSession> playbackSessionTable;

    private PlaybackSessionDAO playbackSessionDAO;

    @BeforeEach
    public void setup() {
        playbackSessionDAO = new PlaybackSessionDAO(playbackSessionTable);
    }

    @Test
    public void save_WhenGetItem_ReturnsSavedRecord() throws Exception {
        PlaybackSession playbackSession = PlaybackSession.builder()
                .sessionId(SESSION_ID)
                .sessionStatus(Status.COMPLETED.toString())
                .build();

        when(playbackSessionTable.getItem(any(Key.class))).thenReturn(playbackSession);
        
        playbackSessionDAO.save(playbackSession);
        PlaybackSession persistedPlaybackSession = playbackSessionDAO.load(SESSION_ID);
        Assertions.assertEquals(SESSION_ID, persistedPlaybackSession.getSessionId());
        Assertions.assertEquals(Status.COMPLETED.toString(), persistedPlaybackSession.getSessionStatus());
    }

    @Test
    public void save_WhenInvalidSessionStatus_ThrowsRuntimeException() {
        PlaybackSession playbackSession = PlaybackSession.builder()
                .sessionId(SESSION_ID)
                .sessionStatus("INVALID")
                .build();

        Assertions.assertThrows(RuntimeException.class, () -> {
            playbackSessionDAO.save(playbackSession);
        });
    }

    @Test
    public void safeUpdateDeviceWebRTCStatus_WhenValidStatusTransition_UpdatesRecord() {
        PlaybackSession playbackSession = PlaybackSession.builder()
                .sessionId(SESSION_ID)
                .streamSource(sampleStreamSource("CONNECTING"))
                .sessionStatus(Status.RUNNING.toString())
                .build();

        playbackSessionDAO.save(playbackSession);

        PlaybackSession finalPlaybackSession = PlaybackSession.builder()
                .sessionId(SESSION_ID)
                .streamSource(sampleStreamSource("CONNECTED"))
                .sessionStatus(Status.RUNNING.toString())
                .build();

        when(playbackSessionTable.getItem(any(Key.class))).thenReturn(playbackSession).thenReturn(finalPlaybackSession);

        playbackSessionDAO.safeUpdateDeviceWebRTCStatus(
                SESSION_ID, WebRTCConnectionStatus.CONNECTED);

        // Status should be updated.
        PlaybackSession updatedSession = playbackSessionDAO.load(SESSION_ID);
        assertEquals("CONNECTED", updatedSession.getStreamSource().get(1).getSource().getPeerConnectionState());
    }

    @Test
    public void safeUpdateDeviceWebRTCStatus_WhenInvalidStatusTransition_DoesNothing() {
        PlaybackSession playbackSession = PlaybackSession.builder()
                .sessionId(SESSION_ID)
                .streamSource(sampleStreamSource("CONNECTED"))
                .sessionStatus(Status.RUNNING.toString())
                .build();

        playbackSessionDAO.save(playbackSession);

        when(playbackSessionTable.getItem(any(Key.class))).thenReturn(playbackSession);

        playbackSessionDAO.safeUpdateDeviceWebRTCStatus(
                SESSION_ID, WebRTCConnectionStatus.CONNECTING);
        
        when(playbackSessionTable.getItem(any(Key.class))).thenReturn(playbackSession);

        // Status should not be updated.
        PlaybackSession updatedSession = playbackSessionDAO.load(SESSION_ID);
        assertEquals("CONNECTED", updatedSession.getStreamSource().get(1).getSource().getPeerConnectionState());
    }

    @Test
    public void safeUpdateDeviceWebRTCStatus_WhenRecordDoesNotExist_ThrowsException() {
        assertThrows(RuntimeException.class, () -> {
            playbackSessionDAO.safeUpdateDeviceWebRTCStatus(
                    SESSION_ID, WebRTCConnectionStatus.CONNECTING);
        });
    }

    @Test
    public void safeUpdateDeviceWebRTCStatus_WhenWebRTCSourceDoesNotExist_ThrowsException() {
        // No stream source at all
        PlaybackSession playbackSession = PlaybackSession.builder()
                .sessionId(SESSION_ID)
                .sessionStatus(Status.RUNNING.toString())
                .build();
        playbackSessionDAO.save(playbackSession);

        assertThrows(RuntimeException.class, () -> {
            playbackSessionDAO.safeUpdateDeviceWebRTCStatus(
                    SESSION_ID, WebRTCConnectionStatus.CONNECTING);
        });

        // Has a stream source but it is not WebRTC
        playbackSession = PlaybackSession.builder()
                .sessionId(SESSION_ID)
                .streamSource(ImmutableList.of(StreamSource.builder()
                        .streamSessionType(SourceType.HLS)
                        .build()))
                .sessionStatus(Status.RUNNING.toString())
                .build();
        playbackSessionDAO.save(playbackSession);

        assertThrows(RuntimeException.class, () -> {
            playbackSessionDAO.safeUpdateDeviceWebRTCStatus(
                    SESSION_ID, WebRTCConnectionStatus.CONNECTING);
        });
    }

    private List<StreamSource> sampleStreamSource(String webRTCState) {
        return ImmutableList.of(
                StreamSource.builder()
                        .streamSessionType(SourceType.HLS)
                        .build(),
                StreamSource.builder()
                        .streamSessionType(SourceType.WEBRTC)
                        .source(Source.builder()
                                .peerConnectionState(webRTCState)
                                .build())
                        .build()
        );
    }
}
