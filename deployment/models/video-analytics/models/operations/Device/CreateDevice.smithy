$version: "2.0"

namespace com.aws.videoanalytic

@http(code: 200, method: "POST", uri: "/create-device")
@idempotent
operation CreateDevice {
    input: CreateDeviceRequest,
    output: CreateDeviceResponse,
    errors: []
}

@input
structure CreateDeviceRequest {
    deviceName: String,
    @required uniqueDeviceIdentifier: String
    @required certificateId: CertificateId
}

@output
structure CreateDeviceResponse {
    deviceId: DeviceId
}
