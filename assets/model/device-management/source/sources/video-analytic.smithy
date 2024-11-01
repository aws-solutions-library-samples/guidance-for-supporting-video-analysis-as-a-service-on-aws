$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

use aws.protocols#restJson1


@restJson1
@title("Video Analytic Guidance Solution - Device Management")
service VideoAnalytic {
    version: "2024-10-18"
    resources: [
        Device,
        DeviceNotification,
        DeviceOperations
    ]
}
