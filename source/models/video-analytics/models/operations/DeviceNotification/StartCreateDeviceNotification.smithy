$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "POST", uri: "/start-create-device-notification")
@idempotent
operation StartCreateDeviceNotification {
    input: StartCreateDeviceNotificationRequest,
    output: StartCreateDeviceNotificationResponse,
    errors: [AccessDeniedException, InternalServerException, ValidationException]
}

@input
structure StartCreateDeviceNotificationRequest {
    @required
    ruleId: RuleId,
    @required
    deviceId: DeviceId
}

@output
structure StartCreateDeviceNotificationResponse {
    jobId: JobId
}
