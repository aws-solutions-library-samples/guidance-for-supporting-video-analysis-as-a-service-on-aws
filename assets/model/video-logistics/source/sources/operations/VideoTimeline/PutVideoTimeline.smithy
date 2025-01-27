$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

use aws.apigateway#integration
use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#ValidationException
use com.amazonaws.videoanalytics#DeviceId

@integration(
    type: "aws_proxy",
    httpMethod: "POST",
    uri: "arn:aws:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/arn:aws:lambda:${AWS::Region}:${AWS::AccountId}:function:${PutVideoTimelineActivity}/invocations",
    credentials: "arn:aws:iam::${AWS::AccountId}:role/VideoLogisticsApiGatewayRole"
)
@http(code: 200, method: "POST", uri: "/put-video-timeline")
@idempotent
operation PutVideoTimeline {
    input: PutVideoTimelineRequest,
    errors: [ValidationException, InternalServerException]
}

@input
structure PutVideoTimelineRequest {
    @required
    deviceId: DeviceId,
    @required
    location: VideoDensityLocation,
    @required
    timestamps: Document,
    durationInMillis: Long
}


