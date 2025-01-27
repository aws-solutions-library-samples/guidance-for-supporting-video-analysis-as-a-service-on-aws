$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

use com.amazonaws.videoanalytics#AccessDeniedException
use com.amazonaws.videoanalytics#DeviceId
use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#ValidationException

use aws.apigateway#integration

@integration(httpMethod: "POST", type: "aws_proxy"
    uri: "arn:aws:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/arn:aws:lambda:${AWS::Region}:${AWS::AccountId}:function:${UpdateDeviceShadowActivity}/invocations",
    credentials: "arn:aws:iam::${AWS::AccountId}:role/DeviceManagementApiGatewayRole")
@http(code: 200, method: "POST", uri: "/update-device-shadow/{deviceId}")
@idempotent
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
