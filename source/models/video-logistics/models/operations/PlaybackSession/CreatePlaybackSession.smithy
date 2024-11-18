$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

use com.amazonaws.videoanalytics#AccessDeniedException
use com.amazonaws.videoanalytics#DeviceId
use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#ResourceNotFoundException
use com.amazonaws.videoanalytics#ValidationException

@http(code: 200, method: "POST", uri: "/create-playback-session")
@idempotent
operation CreatePlaybackSession {
    input: CreatePlaybackSessionRequest,
    output: CreatePlaybackSessionResponse,
    errors: [AccessDeniedException, ValidationException, ResourceNotFoundException, InternalServerException]
}

@input
structure CreatePlaybackSessionRequest {
    @required 
    deviceId: DeviceId,
    @required
    @timestampFormat("date-time")
    startTime: Timestamp,
    @required
    @timestampFormat("date-time")
    endTime: Timestamp
}

@output
structure CreatePlaybackSessionResponse {
    streamSources: StreamSources
}
