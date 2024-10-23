$version: "2.0"

namespace com.amazonaws.videoanalytics


@http(code: 200, method: "PUT", uri: "/get-device-internal/{deviceId}")
@idempotent
operation GetDeviceInternal {
    input: GetDeviceInternalRequest,
    output: GetDeviceInternalResponse,
    errors: [ValidationException, ResourceNotFoundException, AccessDeniedException, InternalServerException]
}

@input
structure GetDeviceInternalRequest {
    @required
    @httpLabel
    deviceId: DeviceId
}

@output
structure GetDeviceInternalResponse {
    deviceName: String,
    deviceId: DeviceId,
    deviceGroupIds: DeviceGroupIdList,
    deviceType: String,
    deviceMetaData: DeviceMetaData,
    deviceCapabilities: KeyValueMap,
    deviceSettings: KeyValueMap,
    deviceState: DeviceState,
    createdAt: Timestamp
}
