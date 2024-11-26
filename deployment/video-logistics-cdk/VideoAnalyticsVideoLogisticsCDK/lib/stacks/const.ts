export const FORWARDING_RULES_BUCKET_NAME = "videoanalytics-fwd-rules";
export const FORWARDING_RULES_ACCESS_LOGS_BUCKET_NAME =
  "videoanalytics-fwd-rules-access-logs";
export const FORWARDING_RULES_SQS_QUEUE_NAME =
  "aws-ip-videoanalytics-VLControlPlane-forwardingRulesQueue";
export const FORWARDING_RULES_TABLE_NAME = "ForwardingRulesTable";
export const FORWARDING_RULES_PK_NAME = "CustomerAccountId";
export const FORWARDING_RULES_SK_NAME = "ForwardingRulesName";
export const VIDEO_EXPORT_JOB_TABLE_NAME = "VideoExportJobTable";
export const VIDEO_EXPORT_JOB_RESULT_TABLE_NAME = "VideoExportResultTable";
export const VIDEO_EXPORT_SFN_STATE_MACHINE_NAME = "VideoExportStateMachine";
export const VIDEO_EXPORT_JOB_GSI_NAME = "DeviceJobStatus-Index";
export const VIDEO_EXPORT_JOB_GSI_PK = "CustomerAccountId_DeviceId";
export const VIDEO_EXPORT_JOB_GSI_SK = "JobStatus";
export const JOB_ID = "JobId";
export const EXPORT_FILE_START_TIME = "ExportFileStartTime";
export const VIDEO_EXPORT_JOB_PARTITION_KEY = "CustomerAccountId_DeviceId";
export const JOB_STATUS = "JobStatus";
export const VIDEO_EXPORT_RESULTS_GSI_1ST_NAME = "JobIDStatus-Index";
export const VIDEO_EXPORT_RESULTS_GSI_1ST_SK = "SegmentJobStatus";
export const VIDEO_EXPORT_RESULTS_GSI_2ND_NAME = "JobIDEndTime-Index";
export const VIDEO_EXPORT_RESULTS_GSI_2ND_SK = "ExportFileEndTime";
export const VIDEO_EXPORT_ACCOUNT_JOB_ID_GSI = "AccountIDJobID-Index";
export const VIDEO_EXPORT_ACCOUNT_JOB_ID_GSI_PK = "CustomerAccountId";
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
export const SHOULD_STREAM_FROM_DEVICE_PATH = "$.shouldStreamFromDevice";
export const PARTITION_KEY_PATH = "$.partitionKey";
export const SORT_KEY_PATH = "$.sortKey";
export const RESULT_PATH_ERROR = "$.error";
export const ERROR_MESSAGE_PATH = "$.error.Cause";
export const RESULT_PATH = "$.output";
export const enum PLAYBACK_SESSION_STATUS {
  FAILED = "FAILED",
  COMPLETED = "COMPLETED",
}
export const RAW_VIDEO_TIMELINE_TABLE_NAME = "RawVideoTimelineTable";
export const VIDEO_TIMELINE_TABLE_NAME = "VideoTimelineTable";
export const RAW_VIDEO_TIMELINE_PK_NAME = "CustomerId_DeviceId";
export const VIDEO_TIMELINE_PK_NAME = "CustomerId_DeviceId_TimeUnit";
export const VIDEO_TIMELINE_SK_NAME = "UnitTimestamp";
export const VIDEO_TIMELINE_TTL_ATTRIBUTE_NAME = "ExpirationTimestamp";
export const RAW_VIDEO_TIMELINE_SORT_KEY_NAME = "Timestamp";
export const TIMELINE_BUCKET_NAME = "videoanalytics-timeline-bucket";
// TODO: Update handler to match new lambda handler path
export const DENSITY_UPDATE_LAMBDA_HANDLER_PATH =
  "com.amazon.awsvideoanalyticsvlcontrolplane.timeline.VideoDensityUpdateLambda::handleRequest";
export const EXPORT_LAMBDA_HANDLER_PATH =
  "com.amazon.awsvideoanalyticsvlcontrolplane.timeline.VideoTimelineS3ExportLambda::handleRequest";
export const LAMBDA_SERVICE_PRINCIPAL = 'lambda.amazonaws.com';
export const LAMBDA_MANAGED_POLICY_NAME = 'service-role/AWSLambdaBasicExecutionRole';
// TODO: Update handler to match new lambda handler path
export const TIMELINE_FORWARDER_HANDLER_PATH =
  'com.amazon.awsvideoanalyticsvlcontrolplane.timeline.TimelineForwarderLambda::handleRequest';
export const LAMBDA_PACKAGE_NAME = 'AWSVideoAnalyticsVLControlPlane-1.0';
  "com.amazon.awsvideoanalyticsvlcontrolplane.timeline.TimelineForwarderLambda::handleRequest";

export const VL_ACTIVITY_JAVA_PATH_PREFIX =
  "com.amazonaws.videoanalytics.videologistics.activity";
export const VL_WORKFLOW_JAVA_PATH_PREFIX =
  "com.amazonaws.videoanalytics.videologistics.workflow";
export const LAMBDA_ASSET_PATH =
  "../../../assets/lambda-built/video-logistics-assets/VideoAnalyticsVideoLogisticsControlPlane-1.0-beta.jar";
export const OPEN_API_SPEC_PATH = "../../../assets/model/video-logistics/openapi-conversion/openapi/VideoAnalytic.openapi.json";