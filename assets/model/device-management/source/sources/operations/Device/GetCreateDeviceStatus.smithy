$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

use aws.apigateway#integration
use com.amazonaws.videoanalytics#DeviceId
use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#JobId
use com.amazonaws.videoanalytics#Status
use com.amazonaws.videoanalytics#ValidationException

@integration(
    type: "aws_proxy",
    httpMethod: "POST",
    uri: "arn:aws:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/arn:aws:lambda:${AWS::Region}:${AWS::AccountId}:function:${GetCreateDeviceStatusActivity}/invocations",
    credentials: "arn:aws:iam::${AWS::AccountId}:role/DeviceManagementApiGatewayRole"
)
@http(code: 200, method: "POST", uri: "/get-create-device-status/{jobId}")
@idempotent
operation GetCreateDeviceStatus {
    input: GetCreateDeviceStatusRequest,
    output: GetCreateDeviceStatusResponse,
    errors: [InternalServerException, ValidationException]
}

@input
structure GetCreateDeviceStatusRequest {
    @required
    @httpLabel
    jobId: JobId
}

@output
structure GetCreateDeviceStatusResponse {
    jobId: JobId,
    deviceId: DeviceId,
    status: Status,
    @timestampFormat("date-time")
    createTime: Timestamp,
    @timestampFormat("date-time")
    modifiedTime: Timestamp,
    errorMessage: String
}


