$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "POST", uri: "/get-device-shadow-internal/{deviceId}")
operation GetDeviceShadowInternal {
    input: GetDeviceShadowInternalRequest,
    output: GetDeviceShadowInternalResponse
}

@input
structure GetDeviceShadowInternalRequest {
    @required
    @httpLabel
    deviceId: DeviceId,
    shadowName: String
}

@output
structure GetDeviceShadowInternalResponse {
    shadowPayload: ShadowMap
}
