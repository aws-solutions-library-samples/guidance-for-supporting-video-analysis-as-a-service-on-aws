$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

list StreamSources {
    member: StreamSource
}

structure StreamSource {
    sourceType: SourceType,
    source: SourceInfo,
    startTime: Timestamp
}

enum SourceType{
    HLS,
    WEBRTC
}

structure SourceInfo {
    hLSStreamingURL: String,
    expirationTime: Timestamp,
    peerConnectionState: PeerConnectionState,
    SignalingChannelURL: String,
    clientId: String
}

enum PeerConnectionState {
    PENDING = "Pending",
    CONNECTING = "Connecting",
    CONNECTED = "Connected",
    DISCONNECTED = "Disconnected",
    FAILED = "Failed",
}
