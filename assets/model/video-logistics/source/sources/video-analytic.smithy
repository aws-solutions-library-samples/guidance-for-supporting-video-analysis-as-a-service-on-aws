$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

use aws.protocols#restJson1


@restJson1
@title("Video Analytic Guidance Solution - Video Logistics")
service VideoAnalytic {
    version: "2024-10-18"
    resources: [
        LivestreamSession,
        PlaybackSession,
        RegisterDevice,
        Snapshot,
        Inference,
        VideoTimelineOps
    ]
}
