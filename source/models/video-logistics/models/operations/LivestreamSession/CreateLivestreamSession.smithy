$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

use aws.apigateway#integration
use com.amazonaws.videoanalytics#AccessDeniedException
use com.amazonaws.videoanalytics#ConflictException
use com.amazonaws.videoanalytics#DeviceId
use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#ResourceNotFoundException
use com.amazonaws.videoanalytics#ValidationException

@integration(
    type: "aws_proxy",
    httpMethod: "POST",
    uri: "arn:aws:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/arn:aws:lambda:${AWS::Region}:${AWS::AccountId}:function:${CreateLivestreamSessionActivity}/invocations",
    credentials: "arn:aws:iam::${AWS::AccountId}:role/VideoLogisticsApiGatewayRole"
)
@http(code: 200, method: "POST", uri: "/create-livestream-session")
@idempotent
operation CreateLivestreamSession {
    input: CreateLivestreamSessionRequest,
    output: CreateLivestreamSessionResponse,
    errors: [AccessDeniedException, ConflictException, ValidationException, ResourceNotFoundException, InternalServerException]
}

@input
structure CreateLivestreamSessionRequest {
    @required deviceId: DeviceId
    clientId: ClientId,
}

@output
structure CreateLivestreamSessionResponse {
    sessionId: SessionId,
    clientId: ClientId,
    iceServers: IceServerList
    signalingChannelURL: String,
}
