$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "POST", uri: "/start-create-device")
@idempotent
operation StartCreateDevice {
    input: StartCreateDeviceRequest,
    output: StartCreateDeviceResponse,
    errors: [InternalServerException, ValidationException, ResourceNotFoundException,
             ConflictException, AccessDeniedException]
}

@input
structure StartCreateDeviceRequest {
    @required deviceId: DeviceId
    @required certificateId: CertificateId
}

@output
structure StartCreateDeviceResponse {
    jobId: JobId
}
