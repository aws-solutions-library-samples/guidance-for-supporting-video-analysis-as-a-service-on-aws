$version: "2.0"

namespace com.aws.videoanalytic

@http(code: 200, method: "POST", uri: "/delete-device")
@idempotent
operation DeleteDevice {
    input: DeleteDeviceRequest,
    output: DeleteDeviceResponse,
    errors: []
}

@input
structure DeleteDeviceRequest {
    @required
    deviceId: DeviceId
}

@output
structure DeleteDeviceResponse {
}
