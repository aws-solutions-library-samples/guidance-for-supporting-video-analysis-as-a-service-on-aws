$version: "2.0"

namespace com.amazonaws.videoanalytics

structure KeyPair {
    privateKey: String,
    publicKey: String
}

map KeyValueMap {
    // Using the patterns for AWS resource tag keys and values: https://docs.aws.amazon.com/resourcegroupstagging/latest/APIReference/API_TagResources.html#API_TagResources_RequestParameters
    @pattern("^[\\s\\S]*$")
    @length(min: 1, max: 128)
    key: String,
    @pattern("^[\\s\\S]*$")
    @length(min: 0, max: 256)
    value: String
}

@length(min: 1, max: 100)
@uniqueItems
list StringList {
    @length(min:1, max:100)
    member: String
}

enum Status {
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@pattern("^[a-z0-9\\-]+$")
@length(min: 1, max: 128)
string JobId

// The pattern for next tokens rejects new line, carriage return, greater than and less than symbols, ampersand, backslash, and backspace
@pattern("^[^\\n\\r<>&'\"\\x08]+$")
string NextToken

// Device ID has the same pattern as IoT thing name: https://docs.aws.amazon.com/iot/latest/apireference/API_DescribeThing.html#API_DescribeThing_RequestParameters
@pattern("^[a-zA-Z0-9:_\\-]+$")
@length(min: 1, max: 128)
string DeviceId

@pattern("^s3://[a-z0-9][a-z0-9\\.-]{1,61}[a-z0-9](/.*)?$")
@length(min: 1, max: 2048)
string S3Path

structure KVSStreamARNs {
    kvsStreamARNForPlayback: String
}