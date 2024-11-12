$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

use com.amazonaws.videoanalytics#AccessDeniedException
use com.amazonaws.videoanalytics#ConflictException
use com.amazonaws.videoanalytics#DeviceId
use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#ResourceNotFoundException
use com.amazonaws.videoanalytics#ValidationException

@http(code: 200, method: "POST", uri: "/create-livestream-session")
@idempotent
operation CreateLivestreamSession {
    input: CreateLivestreamSessionRequest,
    output: CreateLivestreamSessionResponse,
    errors: [AccessDeniedException, ConflictException, ValidationException, ResourceNotFoundException, InternalServerException]
}

@input
structure CreateLivestreamSessionRequest {
    @required deviceId: DeviceId
    clientId: ClientId,
}

@output
structure CreateLivestreamSessionResponse {
    sessionId: SessionId,
    clientId: ClientId,
    iceServers: IceServerList
    signalingChannelURL: String,
}
