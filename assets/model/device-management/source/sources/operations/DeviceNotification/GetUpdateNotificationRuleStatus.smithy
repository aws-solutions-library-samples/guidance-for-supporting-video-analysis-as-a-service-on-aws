$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

@http(code: 200, method: "GET", uri: "/get-update-notification-rule-status/{jobId}")
@readonly
operation GetUpdateNotificationRuleStatus {
    input: GetUpdateNotificationRuleStatusRequest,
    output: GetUpdateNotificationRuleStatusResponse,
    errors: [InternalServerException, ValidationException]
}

@input
structure GetUpdateNotificationRuleStatusRequest {
    @required
    @httpLabel
    jobId: JobId
}

@output
structure GetUpdateNotificationRuleStatusResponse {
    jobId: JobId,
    ruleId: RuleId,
    eventCategory: EventCategory,
    condition: Condition,
    destination: Destination,
    errorDestination: Destination,
    ruleDisabled: Boolean,
    errorMessage: String,
    status: Status,
    createTime: Timestamp,
    modifiedTime: Timestamp,
}
