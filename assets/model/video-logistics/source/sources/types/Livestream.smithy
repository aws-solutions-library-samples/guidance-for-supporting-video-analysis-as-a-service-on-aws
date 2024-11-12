$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

use com.amazonaws.videoanalytics#StringList

list IceServerList {
    member: IceServer
}

structure IceServer {
    password: String,
    ttl: Float,
    uris: StringList,
    username: String
}

@pattern("^[a-z0-9\\-]+$")
@length(min: 1, max: 128)
string SessionId

// https://docs.aws.amazon.com/kinesisvideostreams-webrtc-dg/latest/devguide/kvswebrtc-websocket-apis-1.html#kvswebrtc-websocket-apis-1-request
@pattern("^(?!((A|a)(W|w)(S|s))_.*)[a-zA-Z0-9_.\\-]")
@length(min: 1, max: 256)
string ClientId
