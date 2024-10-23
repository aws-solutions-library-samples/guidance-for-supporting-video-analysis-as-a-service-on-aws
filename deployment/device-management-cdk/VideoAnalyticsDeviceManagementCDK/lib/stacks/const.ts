import type { Table } from 'aws-cdk-lib/aws-dynamodb';
import { Runtime } from 'aws-cdk-lib/aws-lambda';
import { Duration } from 'aws-cdk-lib';
// State paths
export const PARTITION_KEY_PATH = '$.partitionKey';
export const SORT_KEY_PATH = '$.sortKey';
export const RESULT_PATH_ERROR = '$.error';
export const ERROR_MESSAGE_PATH = '$.error.Cause';
export const RESULT_PATH = '$.output';
// TODO: Update this once the DM's package name is updated
export const DM_CONTROL_PLANE_PACKAGE = 'AWSVideoAnalyticsDMControlPlane-1.0';
export const LAMBDA_TIMEOUT_MINUTES = Duration.minutes(12);
export const LAMBDA_RUNTIME = Runtime.JAVA_17;
export const LAMBDA_MEMORY_SIZE_KB = 2048;
// TODO: REFACTOR once DM workflow's lambda paths are updated
export const DM_WORKFLOW_JAVA_PATH_PREFIX = 'com.amazon.awsvideoanalyticsdmcontrolplane.workflow.';
// Messages to Edge devices: This allows the reuse of lambdas in our workflows.
export const enum MESSAGE_TO_DEVICE {
    CREATE_KEYS_MESSAGE = 'createCredentials',
    CREDENTIALS_ARE_ACTIVATED_MESSAGE = 'credentialsActivated'
}
export const IS_REGISTERED_PATH = '$.isRegistered';
export const IS_COMMAND_PATH = '$.isCommand';
export const IS_SOFTWARE_UPDATE_PATH = '$.isSoftwareUpdate';

// TODO: REFACTOR once DM workflow's lambda paths are updated
export const LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT = './lambda/deviceManagement';
