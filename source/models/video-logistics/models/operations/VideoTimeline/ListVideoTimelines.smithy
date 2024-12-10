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
    uri: "arn:aws:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/arn:aws:lambda:${AWS::Region}:${AWS::AccountId}:function:${ListVideoTimelinesActivity}/invocations",
    credentials: "arn:aws:iam::${AWS::AccountId}:role/VideoLogisticsApiGatewayRole"
)
@http(code: 200, method: "POST", uri: "/list-video-timelines")
@paginated(inputToken: "nextToken", outputToken: "nextToken")
operation ListVideoTimelines {
    input: ListVideoTimelinesRequest,
    output: ListVideoTimelinesResponse,
    errors: [ValidationException, InternalServerException]
}

@input
structure ListVideoTimelinesRequest {
    @required
    deviceId: DeviceId,
    @required
    startTime: Timestamp,
    @required
    endTime: Timestamp,
    @required
    timeIncrement: Integer,
    @required
    timeIncrementUnits: TimeIncrementUnits,
    nextToken: NextToken
}

@output
structure ListVideoTimelinesResponse {
    deviceId: DeviceId,
    startTime: Timestamp,
    endTime: Timestamp,
    timeIncrement: Integer,
    timeIncrementUnits: TimeIncrementUnits,
    videoTimelines: VideoTimelineList,
    nextToken: NextToken
}