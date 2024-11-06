$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

use aws.apigateway#integration

@integration(httpMethod: "POST", type: "aws_proxy"
    uri: "arn:aws:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/arn:aws:lambda:${AWS::Region}:${AWS::AccountId}:function:${UpdateDeviceShadowActivity}/invocations",
    credentials: "arn:aws:iam::${AWS::AccountId}:role/ApiGatewayRole")
@http(code: 200, method: "POST", uri: "/update-device-shadow/{deviceId}")
@idempotent
//TODO: Add idempotency token and validation
operation UpdateDeviceShadow {
    input: UpdateDeviceShadowRequest,
    output: UpdateDeviceShadowResponse,
    errors: [AccessDeniedException, InternalServerException, ValidationException]
}

@input
structure UpdateDeviceShadowRequest {
    @required
    @httpLabel
    deviceId: DeviceId,
    shadowPayload: ShadowMap
}

@output
structure UpdateDeviceShadowResponse {
    deviceId: DeviceId
}
