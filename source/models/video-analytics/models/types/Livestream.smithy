$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

list IceServerList {
    member: IceServer
}

structure IceServer {
    password: String,
    ttl: Float,
    uris: StringList,
    username: String
}
