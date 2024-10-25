$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "POST", uri: "/start-update-notification-rule")
@idempotent
operation StartUpdateNotificationRule {
    input: StartUpdateNotificationRuleRequest,
    output: StartUpdateNotificationRuleResponse,
    errors: [AccessDeniedException, InternalServerException, ValidationException]
}

@input
structure StartUpdateNotificationRuleRequest {
    @required
    ruleId: RuleId,
    eventCategory: EventCategory,
    ruleDisabled: Boolean,
    condition: Condition,
    destination: Destination,
    errorDestination: Destination,
}

@output
structure StartUpdateNotificationRuleResponse {
    jobId: JobId
}
