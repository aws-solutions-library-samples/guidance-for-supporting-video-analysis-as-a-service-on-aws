$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

use com.amazonaws.videoanalytics#S3Path

// Certificate ID follows the pattern of the same in IoT: https://docs.aws.amazon.com/iot/latest/apireference/API_CreateKeysAndCertificate.html#iot-CreateKeysAndCertificate-response-certificateId
@pattern("^(0x)?[a-fA-F0-9]+$")
@length(min: 1, max: 64)
string CertificateId

// Shadow Name has the same pattern as IoT shadow name: https://docs.aws.amazon.com/iot/latest/apireference/API_iotdata_GetThingShadow.html#API_iotdata_GetThingShadow_RequestParameters
@pattern("^[a-zA-Z0-9:_\\-]+$")
@length(min: 1, max: 64)
string ShadowName

structure ShadowMap {
    shadowName: ShadowName,
    stateDocument: Document
}
