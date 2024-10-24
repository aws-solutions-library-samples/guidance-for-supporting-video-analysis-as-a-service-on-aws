$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "PUT", uri: "/update-device/{deviceId}")
@idempotent
//TODO: Add idempotency token and validation
operation UpdateDevice {
    input: UpdateDeviceRequest,
    output: UpdateDeviceResponse
}

@input
structure UpdateDeviceRequest {
    @required
    @httpLabel
    deviceId: DeviceId,
    attributePayload: KeyValueMap,
    shadowPayload: ShadowMap
}

@output
structure UpdateDeviceResponse {
    deviceName: String,
    deviceId: DeviceId,
    deviceGroupId: DeviceGroupId,
    deviceType: String,
    deviceMetaData: DeviceMetaData,
    deviceCapabilities: KeyValueMap,
    deviceSettings: KeyValueMap,
    createdAt: Timestamp
}
