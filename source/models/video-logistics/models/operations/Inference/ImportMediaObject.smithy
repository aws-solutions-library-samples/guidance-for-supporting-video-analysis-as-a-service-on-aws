$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

use aws.apigateway#integration
use com.amazonaws.videoanalytics#DeviceId
use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#ValidationException

@integration(
    type: "aws_proxy",
    httpMethod: "POST",
    uri: "arn:aws:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/arn:aws:lambda:${AWS::Region}:${AWS::AccountId}:function:${ImportMediaObjectActivity}/invocations",
    credentials: "arn:aws:iam::${AWS::AccountId}:role/VideoLogisticsApiGatewayRole"
)
@http(code: 200, method: "POST", uri: "/import-media-object")
@idempotent
operation ImportMediaObject {
    input: ImportMediaObjectRequest,
    errors: [ValidationException, InternalServerException]
}

@input
structure ImportMediaObjectRequest {
    @required
    deviceId: DeviceId
    @required
    mediaObject: MediaObjectBlob
}
