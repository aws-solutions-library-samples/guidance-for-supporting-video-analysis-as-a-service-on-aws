$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "GET", uri: "/get-delete-device-notification-status/{jobId}")
@readonly
operation GetDeleteDeviceNotificationStatus {
    input: GetDeleteDeviceNotificationStatusRequest,
    output: GetDeleteDeviceNotificationStatusResponse,
    errors: [InternalServerException, ValidationException]
}

@input
structure GetDeleteDeviceNotificationStatusRequest {
    @required
    @httpLabel
    jobId: JobId
}

@output
structure GetDeleteDeviceNotificationStatusResponse {
    jobId: JobId,
    deviceId: DeviceId,
    ruleId: RuleId,
    errorMessage: String,
    status: Status,
    createTime: Timestamp,
    modifiedTime: Timestamp,
}
