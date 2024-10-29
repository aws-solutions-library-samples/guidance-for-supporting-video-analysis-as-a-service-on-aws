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

@pattern("date-time")
string Timestamp
