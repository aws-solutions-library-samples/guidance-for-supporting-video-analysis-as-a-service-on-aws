$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

structure VideoTimeline {
    @range(min: 0, max: 1)
    cloudDensity: Float,
    @range(min: 0, max: 1)
    deviceDensity: Float
}

list VideoTimelineList {
    member: VideoTimeline
}

enum VideoDensityLocation {
    CLOUD
    DEVICE
}

enum TimeIncrementUnits {
    SECONDS,
    MINUTES,
    HOURS,
    DAYS
}

structure DetailedVideoTimeline {
    cloud: Timelines,
    device: Timelines
}

structure Timeline {
    startTime: Timestamp,
    endTime: Timestamp
}

list Timelines {
    member: Timeline
}