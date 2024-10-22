$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "GET", uri: "/get-create-notification-rule-status/{jobId}")
@readonly
operation GetCreateNotificationRuleStatus {
    input: GetCreateNotificationRuleStatusRequest,
    output: GetCreateNotificationRuleStatusResponse,
    errors: [InternalServerException, ValidationException]
}

@input
structure GetCreateNotificationRuleStatusRequest {
    @required
    @httpLabel
    jobId: JobId
}

@output
structure GetCreateNotificationRuleStatusResponse {
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
