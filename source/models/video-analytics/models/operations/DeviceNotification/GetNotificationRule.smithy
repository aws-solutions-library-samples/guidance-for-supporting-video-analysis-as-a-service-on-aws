$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "GET", uri: "/get-notification-rule/{ruleId}")
@readonly
operation GetNotificationRule {
    input: GetNotificationRuleRequest,
    output: GetNotificationRuleResponse,
    errors: [AccessDeniedException, InternalServerException, ValidationException]
}

@input
structure GetNotificationRuleRequest {
    @required
    @httpLabel
    ruleId: RuleId
}

@output
structure GetNotificationRuleResponse {
    jobId: JobId,
    ruleId: RuleId,
    eventCategory: EventCategory,
    condition: Condition,
    destination: Destination,
    errorDestination: Destination,
    ruleDisabled: Boolean
}
