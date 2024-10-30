$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

@http(code: 200, method: "POST", uri: "/start-get-device-capabilities")
@idempotent
operation StartGetDeviceCapabilities {
    input: StartGetDeviceCapabilitiesRequest,
    output: StartGetDeviceCapabilitiesResponse,
    errors: [InternalServerException, ValidationException, ResourceNotFoundException]
}

@input
structure StartGetDeviceCapabilitiesRequest {
    @required deviceId: DeviceId
}

@output
structure StartGetDeviceCapabilitiesResponse {
    jobId: JobId
}

