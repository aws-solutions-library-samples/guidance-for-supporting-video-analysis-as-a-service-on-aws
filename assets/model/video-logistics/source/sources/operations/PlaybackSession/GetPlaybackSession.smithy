$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

use com.amazonaws.videoanalytics#ResourceNotFoundException
use com.amazonaws.videoanalytics#Status
use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#DeviceId

@http(code: 200, method: "POST", uri: "/get-playback-session/{sessionId}")
operation GetPlaybackSession {
    input: GetPlaybackSessionRequest,
    output: GetPlaybackSessionResponse,
    errors: [ResourceNotFoundException, InternalServerException]
}

@input
structure GetPlaybackSessionRequest {
    @required
    @httpLabel
    sessionId: SessionId
}

@output
structure GetPlaybackSessionResponse {
    deviceId: DeviceId,
    @timestampFormat("date-time")
    startTime: Timestamp,
    @timestampFormat("date-time")
    endTime: Timestamp,
    sessionId: SessionId,
    status: Status,
    @timestampFormat("date-time")
    createdAt: Timestamp,
    @timestampFormat("date-time")
    lastUpdatedAt: Timestamp,
    errorMessage: String,
    streamSources: StreamSources
}
