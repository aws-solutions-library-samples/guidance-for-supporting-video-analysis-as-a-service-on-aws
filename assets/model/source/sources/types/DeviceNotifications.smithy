$version: "2.0"

namespace com.amazonaws.videoanalytics

// Rule ID follows the pattern of IoT topic rule name: https://docs.aws.amazon.com/iot/latest/apireference/API_CreateTopicRule.html#API_CreateTopicRule_RequestParameters
@pattern("^[a-zA-Z0-9_]+$")
@length(min: 1, max: 128)
string RuleId

enum EventCategory {
    CONNECTIVITY_STATUS
    STATE
    AI_EVENT
}

structure Sns {
    // https://docs.aws.amazon.com/sns/latest/api/API_CreateTopic.html#API_CreateTopic_RequestParameters
    @pattern("^arn:aws[a-z0-9\\-]*:sns:[a-z0-9\\-]+:\\d{12}:([\\w\\-]{1,256})$")
    targetArn: String
    roleArn: IAMRoleArn
}

structure Destination {
    @required
    sns: Sns
}

structure Condition {
    connectivityStatus: ConnectivityStatusList,
    state: DeviceStateList,
    aiEvent: AiEventList
}

@uniqueItems
list DeviceStateList {
    member: DeviceState
}

enum ConnectivityStatus {
    ON
    OFF
}

@uniqueItems
list ConnectivityStatusList {
    member: ConnectivityStatus
}

@pattern("^arn:(aws[a-zA-Z0-9-]*):iam::(\\d{12})?:(role((\\u002F)|(\\u002F[\\u0021-\\u007F]+\\u002F))[\\w+=,.@-]+)$")
@length(min: 1, max: 2048)
string IAMRoleArn

enum AiEvent {
    INTRUSION
    CROSSLINE
    MOTION
}

@uniqueItems
list AiEventList {
    member: AiEvent
}
