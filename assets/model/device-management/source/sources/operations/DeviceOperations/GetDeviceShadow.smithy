$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

use com.amazonaws.videoanalytics#AccessDeniedException
use com.amazonaws.videoanalytics#DeviceId
use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#ValidationException

use aws.apigateway#integration

@integration(httpMethod: "POST", type: "aws_proxy"
    uri: "arn:aws:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/arn:aws:lambda:${AWS::Region}:${AWS::AccountId}:function:${GetDeviceShadowActivity}/invocations",
    credentials: "arn:aws:iam::${AWS::AccountId}:role/ApiGatewayRole")
@http(code: 200, method: "POST", uri: "/get-device-shadow/{deviceId}")
operation GetDeviceShadow {
    input: GetDeviceShadowRequest,
    output: GetDeviceShadowResponse,
    errors: [AccessDeniedException, InternalServerException, ValidationException]
}

@input
structure GetDeviceShadowRequest {
    @required
    @httpLabel
    deviceId: DeviceId,
    shadowName: ShadowName
}

@output
structure GetDeviceShadowResponse {
    shadowPayload: ShadowMap
}
