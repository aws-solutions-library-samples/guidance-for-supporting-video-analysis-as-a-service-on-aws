$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

@http(code: 200, method: "POST", uri: "/create-livestream-session")
@idempotent
operation CreateLivestreamSession {
    input: CreateLivestreamSessionRequest,
    output: CreateLivestreamSessionResponse,
    errors: [ValidationException, ResourceNotFoundException, InternalServerException]
}

@input
structure CreateLivestreamSessionRequest {
    @required deviceId: DeviceId
    clientId: ClientId,
    temporaryUseSyncProcessing: Boolean
}

@output
structure CreateLivestreamSessionResponse {
    sessionId: SessionId,
    clientId: ClientId,
    iceServers: IceServerList
    signalingChannelURL: String,
}
