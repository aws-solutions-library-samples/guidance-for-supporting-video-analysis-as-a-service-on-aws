$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

@http(code: 200, method: "POST", uri: "/create-playback-session")
@idempotent
operation CreatePlaybackSession {
    input: CreatePlaybackSessionRequest,
    output: CreatePlaybackSessionResponse,
    errors: [AccessDeniedException, ValidationException, ResourceNotFoundException, InternalServerException]
}

@input
structure CreatePlaybackSessionRequest {
    @required deviceId: DeviceId,
    @required startTime: Timestamp,
    @required endTime: Timestamp,
    temporaryIsWebRTCConnection: Boolean
}

@output
structure CreatePlaybackSessionResponse {
    sessionId: SessionId
}