$version: "2.0"

namespace com.amazonaws.videoanalytics

// Rule ID follows the pattern of IoT topic rule name: https://docs.aws.amazon.com/iot/latest/apireference/API_CreateTopicRule.html#API_CreateTopicRule_RequestParameters
@pattern("^[a-zA-Z0-9_]+$")
@length(min: 1, max: 128)
string RuleId
