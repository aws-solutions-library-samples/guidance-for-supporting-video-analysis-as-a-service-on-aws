$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

use com.amazonaws.videoanalytics#AccessDeniedException
use com.amazonaws.videoanalytics#ConflictException
use com.amazonaws.videoanalytics#DeviceId
use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#JobId
use com.amazonaws.videoanalytics#ResourceNotFoundException
use com.amazonaws.videoanalytics#ValidationException

use aws.apigateway#integration

@integration(
    type: "aws_proxy",
    httpMethod: "POST",
    uri: "arn:aws:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/arn:aws:lambda:${AWS::Region}:${AWS::AccountId}:function:${StartCreateDeviceActivity}/invocations",
    credentials: "arn:aws:iam::${AWS::AccountId}:role/ApiGatewayRole"
)
@http(code: 200, method: "POST", uri: "/start-create-device/{deviceId}")
@idempotent
operation StartCreateDevice {
    input: StartCreateDeviceRequest,
    output: StartCreateDeviceResponse,
    errors: [InternalServerException, ValidationException, ResourceNotFoundException,
             ConflictException, AccessDeniedException]
}

@input
structure StartCreateDeviceRequest {
    @required
    @httpLabel
    deviceId: DeviceId,
    @required 
    certificateId: CertificateId
}

@output
structure StartCreateDeviceResponse {
    jobId: JobId
}
