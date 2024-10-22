$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "POST", uri: "/delete-device")
@idempotent
operation DeleteDevice {
    input: DeleteDeviceRequest,
    output: DeleteDeviceResponse,
    errors: [AccessDeniedException, InternalServerException, ValidationException]
}

@input
structure DeleteDeviceRequest {
    @required
    deviceId: DeviceId
}

@output
structure DeleteDeviceResponse {
}
