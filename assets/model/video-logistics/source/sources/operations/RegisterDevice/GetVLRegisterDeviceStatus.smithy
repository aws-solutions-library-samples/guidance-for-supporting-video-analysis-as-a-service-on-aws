$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

use aws.apigateway#integration
use com.amazonaws.videoanalytics#AccessDeniedException
use com.amazonaws.videoanalytics#JobId
use com.amazonaws.videoanalytics#DeviceId
use com.amazonaws.videoanalytics#KVSStreamARNs
use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#ResourceNotFoundException
use com.amazonaws.videoanalytics#ValidationException
use com.amazonaws.videoanalytics#Status

@integration(
    type: "aws_proxy",
    httpMethod: "POST",
    uri: "arn:aws:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/arn:aws:lambda:${AWS::Region}:${AWS::AccountId}:function:${GetVLRegisterDeviceStatusActivity}/invocations",
    credentials: "arn:aws:iam::${AWS::AccountId}:role/VideoLogisticsApiGatewayRole"
)
@http(code: 200, method: "POST", uri: "/get-vl-register-device-status/{jobId}")
@readonly
operation GetVLRegisterDeviceStatus {
    input: GetVLRegisterDeviceStatusRequest,
    output: GetVLRegisterDeviceStatusResponse,
    errors: [AccessDeniedException, ValidationException, ResourceNotFoundException, InternalServerException]
}

@input
structure GetVLRegisterDeviceStatusRequest {
    @required
    @httpLabel
    jobId: JobId
}

@output
structure GetVLRegisterDeviceStatusResponse {
    jobId: JobId,
    deviceId: DeviceId,
    status: Status,
    @timestampFormat("date-time")
    createTime: Timestamp,
    @timestampFormat("date-time")
    modifiedTime: Timestamp,
    kvsStreamArns: KVSStreamARNs
}


