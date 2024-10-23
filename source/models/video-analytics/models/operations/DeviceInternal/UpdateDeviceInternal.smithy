$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "PUT", uri: "/update-device-internal/{deviceId}")
@idempotent
//TODO: Add idempotency token and validation
operation UpdateDeviceInternal {
    input: UpdateDeviceInternalRequest,
    output: UpdateDeviceInternalResponse
}

@input
structure UpdateDeviceInternalRequest {
    @required
    @httpLabel
    deviceId: DeviceId,
    attributePayload: KeyValueMap,
    shadowPayload: ShadowMap
}

@output
structure UpdateDeviceInternalResponse {
    deviceName: String,
    deviceId: DeviceId,
    deviceGroupId: DeviceGroupId,
    deviceType: String,
    deviceMetaData: DeviceMetaData,
    deviceCapabilities: KeyValueMap,
    deviceSettings: KeyValueMap,
    createdAt: Timestamp
}
