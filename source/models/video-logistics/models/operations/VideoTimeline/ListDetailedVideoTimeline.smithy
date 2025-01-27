$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

use aws.apigateway#integration
use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#ValidationException
use com.amazonaws.videoanalytics#NextToken
use com.amazonaws.videoanalytics#DeviceId
use com.amazonaws.videoanalytics#AccessDeniedException
use com.amazonaws.videoanalytics#ResourceNotFoundException

@integration(
    type: "aws_proxy",
    httpMethod: "POST",
    uri: "arn:aws:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/arn:aws:lambda:${AWS::Region}:${AWS::AccountId}:function:${ListDetailedVideoTimelineActivity}/invocations",
    credentials: "arn:aws:iam::${AWS::AccountId}:role/VideoLogisticsApiGatewayRole"
)
@http(code: 200, method: "POST", uri: "/list-detailed-video-timeline")
@paginated(inputToken: "nextToken", outputToken: "nextToken")
operation ListDetailedVideoTimeline {
    input: ListDetailedVideoTimelineRequest,
    output: ListDetailedVideoTimelineResponse,
    errors: [AccessDeniedException, ValidationException, ResourceNotFoundException, InternalServerException]
}

@input
structure ListDetailedVideoTimelineRequest {
    @required
    deviceId: DeviceId,
    @required
    startTime: Timestamp,
    @required
    endTime: Timestamp,
    nextToken: NextToken
}

@output
structure ListDetailedVideoTimelineResponse {
    deviceId: DeviceId,
    startTime: Timestamp,
    endTime: Timestamp,
    detailedVideoTimeline: DetailedVideoTimeline,
    nextToken: NextToken
}
