$version: "2.0"

namespace com.amazonaws.videoanalytics

// Certificate ID follows the pattern of the same in IoT: https://docs.aws.amazon.com/iot/latest/apireference/API_CreateKeysAndCertificate.html#iot-CreateKeysAndCertificate-response-certificateId
@pattern("^(0x)?[a-fA-F0-9]+$")
@length(min: 1, max: 64)
string CertificateId

// Device ID has the same pattern as IoT thing name: https://docs.aws.amazon.com/iot/latest/apireference/API_DescribeThing.html#API_DescribeThing_RequestParameters
@pattern("^[a-zA-Z0-9:_\\-]+$")
@length(min: 1, max: 128)
string DeviceId

@pattern("^[a-z0-9\\-]+$")
@length(min: 1, max: 128)
string JobId

enum Status {
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
