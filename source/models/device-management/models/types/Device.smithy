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

structure DeviceTypePayload {
    deviceTypeId: DeviceTypeId,
    removeDeviceType: Boolean
}

@length(min: 1, max: 128)
string DeviceTypeId

structure CommandPayload {
    @required
    command: Command,
    @range(min: 0, max: 10)
    retries: Integer,
    @range(min: 1, max: 10080)
    timeout: Integer,
    s3Uri: S3Path
}

enum Command {
    REBOOT
    FACTORY_RESET
    SD_CARD_FORMAT
    UPDATE
}
