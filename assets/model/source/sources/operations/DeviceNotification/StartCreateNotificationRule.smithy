$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "POST", uri: "/start-create-notification-rule")
@idempotent
operation StartCreateNotificationRule {
    input: StartCreateNotificationRuleRequest,
    output: StartCreateNotificationRuleResponse,
    errors: [AccessDeniedException, InternalServerException, ValidationException]
}

@input
structure StartCreateNotificationRuleRequest {
    @required
    ruleId: RuleId,
    @required
    eventCategory: EventCategory,
    ruleDisabled: Boolean,
    @required
    condition: Condition,
    @required
    destination: Destination,
    errorDestination: Destination,
}

@output
structure StartCreateNotificationRuleResponse {
    jobId: JobId
}
