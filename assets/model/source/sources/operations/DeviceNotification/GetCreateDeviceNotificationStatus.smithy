$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "GET", uri: "/get-create-device-notification-status/{jobId}")
@idempotent
operation GetCreateDeviceNotificationStatus {
    input: GetCreateDeviceNotificationStatusRequest,
    output: GetCreateDeviceNotificationStatusResponse,
    errors: [InternalServerException, ValidationException],
}

@input
structure GetCreateDeviceNotificationStatusRequest {
    @required
    @httpLabel
    jobId: JobId
}

@output
structure GetCreateDeviceNotificationStatusResponse {
    jobId: JobId,
    deviceId: DeviceId,
    ruleId: RuleId,
    errorMessage: String,
    status: Status,
    createTime: Timestamp,
    modifiedTime: Timestamp,
}
