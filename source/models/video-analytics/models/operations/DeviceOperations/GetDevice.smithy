$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "GET", uri: "/get-device/{deviceId}")
@readonly
operation GetDevice {
    input: GetDeviceRequest
    output: GetDeviceResponse,
    errors: [AccessDeniedException, InternalServerException, ValidationException]
}

@input
structure GetDeviceRequest {
    @required
    @httpLabel
    deviceId: DeviceId
}

@output
structure GetDeviceResponse {
    deviceName: String,
    deviceId: DeviceId,
    deviceGroupIds: DeviceGroupIdList,
    deviceType: String,
    deviceMetaData: DeviceMetaData,
    deviceCapabilities: KeyValueMap,
    deviceSettings: KeyValueMap,
    createdAt: Timestamp
}
