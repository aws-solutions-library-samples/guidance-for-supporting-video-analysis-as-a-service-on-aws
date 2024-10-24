$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "GET", uri: "/get-update-device-status/{jobId}")
@readonly
operation GetUpdateDeviceStatus {
    input: GetUpdateDeviceStatusRequest,
    output: GetUpdateDeviceStatusResponse,
    errors: [InternalServerException, ValidationException]

}

@input
structure GetUpdateDeviceStatusRequest {
    @required
    @httpLabel
    jobId: JobId
}

@output
structure GetUpdateDeviceStatusResponse {
    jobId: JobId,
    deviceId: DeviceId,
    status: Status,
    createTime: Timestamp,
    modifiedTime: Timestamp,
    errorMessage: String
}
