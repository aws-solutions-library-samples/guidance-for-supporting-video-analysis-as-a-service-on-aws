$version: "2.0"

namespace com.amazonaws.videoanalytics

resource Device {
    operations: [
        GetCreateDeviceStatus,
        DeleteDevice,
        GetUpdateDeviceStatus,
        StartCreateDevice,
        StartGetDeviceCapabilities,
        StartUpdateDevice
    ]
}