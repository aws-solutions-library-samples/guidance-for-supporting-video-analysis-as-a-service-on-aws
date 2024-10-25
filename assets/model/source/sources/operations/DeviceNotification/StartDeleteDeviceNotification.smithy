$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "POST", uri: "/start-delete-device-notification")
@idempotent
operation StartDeleteDeviceNotification {
    input: StartDeleteDeviceNotificationRequest,
    output: StartDeleteDeviceNotificationResponse,
    errors: [AccessDeniedException, InternalServerException, ValidationException]
}

@input
structure StartDeleteDeviceNotificationRequest {
    @required
    ruleId: RuleId,
    @required
    deviceId: DeviceId
}

@output
structure StartDeleteDeviceNotificationResponse {
    jobId: JobId
}
