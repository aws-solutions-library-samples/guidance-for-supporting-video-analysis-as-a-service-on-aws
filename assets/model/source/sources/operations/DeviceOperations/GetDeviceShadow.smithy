$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "POST", uri: "/get-device-shadow-/{deviceId}")
operation GetDeviceShadow {
    input: GetDeviceShadowRequest,
    output: GetDeviceShadowResponse
}

@input
structure GetDeviceShadowRequest {
    @required
    @httpLabel
    deviceId: DeviceId,
    shadowName: String
}

@output
structure GetDeviceShadowResponse {
    shadowPayload: ShadowMap
}
