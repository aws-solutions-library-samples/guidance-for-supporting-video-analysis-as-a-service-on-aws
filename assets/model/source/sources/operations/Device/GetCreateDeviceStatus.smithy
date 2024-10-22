$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "GET", uri: "/get-create-device-status/{jobId}")
@idempotent
operation GetCreateDeviceStatus {
    input: GetCreateDeviceStatusRequest,
    output: GetCreateDeviceStatusResponse,
    errors: [InternalServerException, ValidationException]
}

@input
structure GetCreateDeviceStatusRequest {
    @required
    @httpLabel
    jobId: JobId
}

@output
structure GetCreateDeviceStatusResponse {
    jobId: JobId,
    deviceId: DeviceId,
    status: Status,
    createTime: Timestamp,
    modifiedTime: Timestamp,
    errorMessage: String
}


