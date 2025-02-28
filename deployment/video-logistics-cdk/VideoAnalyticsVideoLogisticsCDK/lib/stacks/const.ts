// Step function state parameters
export const EXECUTION_COUNTER = "$.Counter";
export const EXPORT_RESULT_PATH = "$.exportResult";
export const EXECUTION_RESULT_IN_STATUS = "$.executionResult";
export const UPDATED_TIMESTAMP =
  "$.exportResult.Payload.newLastProcessedTimestamp";
export const IS_COMPLETE_CONDITION = "$.exportResult.Payload.isJobComplete";
export const LAST_PROCESSED_TIMESTAMP = "$.lastProcessedTimestamp";
export const WAIT_TIME_BETWEEN_LOOP = "$.exportResult.Payload.waitTime";
export const FAILED_DYNAMIC_CAUSE = "$.Cause";
// used for when execution Lambda failed cause forwarding after statusUpdate
export const FAILED_FORWARD_CAUSE = "$.Payload.Cause";
export const PARTITION_KEY_PATH = "$.partitionKey";
export const SORT_KEY_PATH = "$.sortKey";
export const RESULT_PATH_ERROR = "$.error";
export const ERROR_MESSAGE_PATH = "$.error.Cause";
export const RESULT_PATH = "$.output";
export const RAW_VIDEO_TIMELINE_TABLE_NAME = "RawVideoTimelineTable";
export const VIDEO_TIMELINE_TABLE_NAME = "VideoTimelineTable";
export const RAW_VIDEO_TIMELINE_PK_NAME = "DeviceId";
export const VIDEO_TIMELINE_PK_NAME = "DeviceId_TimeUnit";
export const VIDEO_TIMELINE_SK_NAME = "UnitTimestamp";
export const VIDEO_TIMELINE_TTL_ATTRIBUTE_NAME = "ExpirationTimestamp";
export const RAW_VIDEO_TIMELINE_SORT_KEY_NAME = "Timestamp";
export const TIMELINE_BUCKET_NAME = "videoanalytics-timeline-bucket";
export const DENSITY_UPDATE_LAMBDA_HANDLER_PATH =
  "com.amazonaws.videoanalytics.videologistics.timeline.VideoDensityUpdateLambda::handleRequest";
export const EXPORT_LAMBDA_HANDLER_PATH =
  "com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineS3ExportLambda::handleRequest";
export const LAMBDA_SERVICE_PRINCIPAL = 'lambda.amazonaws.com';
export const LAMBDA_MANAGED_POLICY_NAME = 'service-role/AWSLambdaBasicExecutionRole';
export const TIMELINE_FORWARDER_HANDLER_PATH =
  'com.amazon.awsvideoanalyticsvlcontrolplane.timeline.TimelineForwarderLambda::handleRequest';
export const LAMBDA_PACKAGE_NAME = 'AWSVideoAnalyticsVLControlPlane-1.0';

export const VL_ACTIVITY_JAVA_PATH_PREFIX =
  "com.amazonaws.videoanalytics.videologistics.activity";
export const VL_INFERENCE_JAVA_PATH_PREFIX =
  "com.amazonaws.videoanalytics.videologistics.inference";
export const VL_WORKFLOW_JAVA_PATH_PREFIX =
  "com.amazonaws.videoanalytics.videologistics.workflow";
export const LAMBDA_ASSET_PATH =
  "../../../assets/lambda-built/video-logistics-assets/VideoAnalyticsVideoLogisticsControlPlane-1.0-beta.jar";
export const OPEN_API_SPEC_PATH = "../../../assets/model/video-logistics/openapi-conversion/openapi/VideoAnalytic.openapi.json";
export const OPEN_SEARCH_SERVICE_NAME = "es.amazonaws.com";
