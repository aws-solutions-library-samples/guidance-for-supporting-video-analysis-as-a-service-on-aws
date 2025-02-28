import { Runtime } from "aws-cdk-lib/aws-lambda";
import { Duration } from "aws-cdk-lib";
// State paths
export const PARTITION_KEY_PATH = "$.partitionKey";
export const SORT_KEY_PATH = "$.sortKey";
export const RESULT_PATH_ERROR = "$.error";
export const ERROR_MESSAGE_PATH = "$.error.Cause";
export const RESULT_PATH = "$.output";
export const DM_CONTROL_PLANE_PACKAGE = "AWSVideoAnalyticsDMControlPlane-1.0";
export const LAMBDA_TIMEOUT_MINUTES = Duration.minutes(12);
export const LAMBDA_RUNTIME = Runtime.JAVA_17;
export const LAMBDA_MEMORY_SIZE_KB = 2048;
export const DM_ACTIVITY_JAVA_PATH_PREFIX =
  "com.amazonaws.videoanalytics.devicemanagement.activity";
export const DM_WORKFLOW_JAVA_PATH_PREFIX =
  "com.amazonaws.videoanalytics.devicemanagement.workflow";
export const IS_REGISTERED_PATH = "$.isRegistered";
export const IS_COMMAND_PATH = "$.isCommand";
export const IS_SOFTWARE_UPDATE_PATH = "$.isSoftwareUpdate";
export const LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT =
  "../../../assets/lambda-built/device-management-assets/VideoAnalyticsDeviceManagementControlPlane-1.0-beta.jar";

export const IOT_CONNECTED_THING_NAME = `\${iot:Connection.Thing.ThingName}`;

export const IOT_CREDENTIAL_THING_NAME = `\${credentials-iot:ThingName}`;
export const OPEN_API_SPEC_PATH = "../../../assets/model/device-management/openapi-conversion/openapi/VideoAnalytic.openapi.json";
