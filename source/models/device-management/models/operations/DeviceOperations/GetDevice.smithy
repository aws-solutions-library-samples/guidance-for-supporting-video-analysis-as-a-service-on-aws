$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

use com.amazonaws.videoanalytics#AccessDeniedException
use com.amazonaws.videoanalytics#DeviceId
use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#KeyValueMap
use com.amazonaws.videoanalytics#ValidationException

use aws.apigateway#integration

// For Lambda function invocations, the httpMethod must be POST.
// https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-integration.html
@integration(httpMethod: "POST", type: "aws_proxy"
    uri: "arn:aws:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/arn:aws:lambda:${AWS::Region}:${AWS::AccountId}:function:${GetDeviceActivity}/invocations",
    credentials: "arn:aws:iam::${AWS::AccountId}:role/DeviceManagementApiGatewayRole")
@http(code: 200, method: "POST", uri: "/get-device/{deviceId}")
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
    deviceType: String,
    deviceMetaData: DeviceMetaData,
    deviceCapabilities: KeyValueMap,
    deviceSettings: KeyValueMap,
    @timestampFormat("date-time")
    createdAt: Timestamp
}
