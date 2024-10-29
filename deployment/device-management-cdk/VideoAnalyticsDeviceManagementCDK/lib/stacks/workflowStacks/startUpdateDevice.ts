import type { Role } from "aws-cdk-lib/aws-iam";
import { Effect, PolicyStatement } from "aws-cdk-lib/aws-iam";
import {
  Fail,
  JsonPath,
  StateMachine,
  Succeed,
  TaskInput,
} from "aws-cdk-lib/aws-stepfunctions";
import {
  VideoAnalyticsAsyncWorkflowResource,
  createLambdaRole,
  AWSRegionUtils,
  AWSRegion,
} from "video_analytics_common_construct/";
import { Function, Code } from "aws-cdk-lib/aws-lambda";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import { LambdaInvoke } from "aws-cdk-lib/aws-stepfunctions-tasks";
import { Duration } from "aws-cdk-lib";
import { Construct } from "constructs";
import { WorkflowStackProps } from "./workflowStack";
import {
  ERROR_MESSAGE_PATH,
  LAMBDA_MEMORY_SIZE_KB,
  LAMBDA_RUNTIME,
  LAMBDA_TIMEOUT_MINUTES,
  PARTITION_KEY_PATH,
  RESULT_PATH,
  RESULT_PATH_ERROR,
  IS_COMMAND_PATH,
  LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT,
  IS_SOFTWARE_UPDATE_PATH,
  IS_REGISTERED_PATH,
  DM_WORKFLOW_JAVA_PATH_PREFIX,
} from "../const";
import { Choice, Condition } from "aws-cdk-lib/aws-stepfunctions";

/**
 *
 */
export class StartUpdateDevice extends VideoAnalyticsAsyncWorkflowResource {
  partitionKeyName = "JobId";
  name = "UpdateDeviceTable";
  private dynamoDbStatement: PolicyStatement;
  private kmsStatement: PolicyStatement;

  private readonly region: AWSRegion;
  private readonly airportCode: string;
  private readonly role: Role;
  private readonly account: string;

  constructor(scope: Construct, id: string, props: WorkflowStackProps) {
    super(scope, id);
    // Add these lines to set the statements
    this.dynamoDbStatement = new PolicyStatement({
      effect: Effect.ALLOW,
      actions: [
        "dynamodb:GetRecords",
        "dynamodb:GetItem",
        "dynamodb:Query",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
      ],
      resources: ["*"], // This will be updated in postWorkflowCreationCallback
    });

    this.kmsStatement = new PolicyStatement({
      effect: Effect.ALLOW,
      actions: ["kms:Decrypt", "kms:Encrypt", "kms:ReEncrypt*"],
      resources: ["*"], // This will be updated in postWorkflowCreationCallback
    });
    this.role = createLambdaRole(this, "StartCreateDeviceNotificationRole", [
      this.dynamoDbStatement,
      this.kmsStatement,
    ]);

    this.region = props.region;
    this.airportCode = AWSRegionUtils.getAirportCode(
      props.region
    ).toLowerCase();
    this.account = props.account;
  }

  createStepFunction(): void {
    const failState = new Fail(this, "Fail");
    const successState = new Succeed(this, "Successful");

    /*
     * Define Lambdas for Step function here.
     * Order matters here, first lambdas are defined, then lambda invokers,
     * then the state machine and other resources, then define the workflow steps.
     */

    const determineUpdateTypeLambda = new Function(
      this,
      "DetermineUpdateTypeLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for determining if device update is commandPayload type or not. " +
          "If it is command operation, proceed to sendCommand",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.DetermineUpdateTypeHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logRetention: RetentionDays.TEN_YEARS,
      }
    );

    const determineCommandTypeLambda = new Function(
      this,
      "DetermineCommandTypeLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for determining what commandType a given commandPayload is. " +
          "If it is an update operation, proceed to createDownloadFileIotJob",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.DetermineCommandTypeHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logRetention: RetentionDays.TEN_YEARS,
      }
    );

    const createDownloadFileIotJobLambda = new Function(
      this,
      "CreateDownloadFileIotJobLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for creating an IoT Job to download a file.",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.CreateDownloadFileIotJobHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logRetention: RetentionDays.TEN_YEARS,
      }
    );

    const checkIotDownloadJobStatusLambda = new Function(
      this,
      "CheckIotDownloadJobStatusLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for checking IoT Job status for the download file job.",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.CheckIotDownloadJobStatusHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logRetention: RetentionDays.TEN_YEARS,
      }
    );

    const createInstallApplicationIotJobLambda = new Function(
      this,
      "CreateInstallApplicationIotJobLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for creating an IoT Job to install an application.",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.CreateInstallApplicationIotJobHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logRetention: RetentionDays.TEN_YEARS,
      }
    );

    const checkIotInstallApplicationJobStatusLambda = new Function(
      this,
      "CheckIotInstallApplicationJobStatusLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for checking IoT Job status for the install application job.",
        runtime: LAMBDA_RUNTIME,
        handler:
          `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.` +
          "CheckIotInstallApplicationJobStatusHandler::handleRequest",
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logRetention: RetentionDays.TEN_YEARS,
      }
    );

    const createCommandIotJobLambda = new Function(
      this,
      "CreateCommandIotJobLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for creating an IoT Job for a given device command.",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.CreateCommandIotJobHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logRetention: RetentionDays.TEN_YEARS,
      }
    );

    const checkIotJobStatusLambda = new Function(
      this,
      "CheckIotJobStatusLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description: "Lambda responsible for checking IoT Job status.",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.CheckIotJobStatusHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logRetention: RetentionDays.TEN_YEARS,
      }
    );

    const publishConfigurationsToShadowLambda = new Function(
      this,
      "PublishConfigurationsToShadowLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description: "Publish configurations to their respective shadow",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.PublishConfigurationsToShadowHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(
          this,
          "PublishConfigurationsToShadowLambdaLogGroup",
          {
            retention: RetentionDays.TEN_YEARS,
            logGroupName: "PublishConfigurationsToShadowLambdaLogGroup",
          }
        ),
      }
    );

    const messageDeviceLambda = new Function(this, "MessageDeviceLambda", {
      //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
      code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
      description: "Lambda sends message to device.",
      runtime: LAMBDA_RUNTIME,
      handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.MessageDeviceHandler::handleRequest`,
      //TODO: Update this if any changes are made to the lambda handler path
      memorySize: LAMBDA_MEMORY_SIZE_KB,
      role: this.role,
      environment: {
        AccountId: this.account.toString(),
        LambdaRoleArn: this.role.roleArn,
      },
      timeout: LAMBDA_TIMEOUT_MINUTES,
      logGroup: new LogGroup(this, "MessageDeviceLambdaLogGroup", {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: "MessageDeviceLambdaLogGroup",
      }),
    });

    const determineDeviceStateLambda = new Function(
      this,
      "DetermineIsDeviceStateLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for determine new state is registered or not. " +
          "If its registered, proceed to get device capabilities",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.DetermineDeviceStateHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(this, "DetermineIsDeviceStateLambdaLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "DetermineIsDeviceStateLambdaLogGroup",
        }),
      }
    );

    const updateUpdateStatusStateLambda = new Function(
      this,
      "UpdateUpdateStatusLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for updating the state of the Update Device Job",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.UpdateUpdateStatusStateHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(this, "UpdateUpdateStatusLambdaLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "UpdateUpdateStatusLambdaLogGroup",
        }),
      }
    );

    const updateDeviceTypeStateLambda = new Function(
      this,
      "UpdateDeviceTypeLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for updating deviceType for deviceTypePayloads.",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.DeviceTypePayloadHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(
          this,
          "UpdateDeviceTypeFromUpdateDeviceLambdaLogGroup",
          {
            retention: RetentionDays.TEN_YEARS,
            logGroupName: "UpdateDeviceTypeFromUpdateDeviceLambdaLogGroup",
          }
        ),
      }
    );

    const publishDeviceStateEventHandlerLambda = new Function(
      this,
      "PublishDeviceStateEventHandlerLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description: "Lambda responsible for publishing new device state",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.PublishDeviceStateEventHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(
          this,
          "PublishDeviceStateEventHandlerLambdaLogGroup",
          {
            retention: RetentionDays.TEN_YEARS,
            logGroupName: "PublishDeviceStateEventHandlerLambdaLogGroup",
          }
        ),
      }
    );

    // handler to create KVS Stream, FVL API used to create stream.
    const createKVSStreamLambda = new Function(this, "CreateKVSStreamLambda", {
      //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
      code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
      description: "Lambda to create KVS Stream using FVL API.",
      runtime: LAMBDA_RUNTIME,
      handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.CreateKVSStreamHandler::handleRequest`,
      //TODO: Update this if any changes are made to the lambda handler path
      memorySize: LAMBDA_MEMORY_SIZE_KB,
      role: this.role,
      environment: {
        AccountId: this.account.toString(),
        LambdaRoleArn: this.role.roleArn,
      },
      timeout: LAMBDA_TIMEOUT_MINUTES,
      logGroup: new LogGroup(this, "CreateKVSStreamLambdaLogGroup", {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: "CreateKVSStreamLambdaLogGroup",
      }),
    });

    // handler to attach an IoT policy which allows KVS access to vsaas cert
    const attachKvsAccessToCertLambda = new Function(
      this,
      "AttachKvsAccessToCertLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for attaching cert to KVS Roles policy.",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.AttachKvsAccessToCertHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(this, "AttachKvsAccessToCertLambdaLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "AttachKvsAccessToCertLambdaLogGroup",
        }),
      }
    );

    // handler to detach an IoT policy which allows KVS access to vsaas cert
    const detachKvsAccessToCertLambda = new Function(
      this,
      "DetachKvsAccessToCertLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for detaching cert to KVS Roles policy.",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.DetachKvsAccessFromCertHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(this, "DetachKvsAccessToCertLambdaLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "DetachKvsAccessToCertLambdaLogGroup",
        }),
      }
    );

    // handler to check if FVL device registration workflow completed successfully.
    const fvlWorkflowCheckerLambda = new Function(
      this,
      "FVLWorkflowCheckerLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for checking to make sure fvl device registration workflow completes.",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.FVLWorkflowCheckerHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(this, "FVLWorkflowCheckerLambdaLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "FVLWorkflowCheckerLambdaLogGroup",
        }),
      }
    );

    const updateDeviceGroupLambda = new Function(
      this,
      "UpdateDeviceGroupLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for updating device thing group if device group payload present.",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.DeviceGroupPayloadHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(this, "UpdateDeviceGroupLambdaLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "UpdateDeviceGroupLambdaLogGroup",
        }),
      }
    );

    const failUpdateStatusStateLambda = new Function(
      this,
      "FailUpdateStatusLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description: "Lambda responsible for marking as failed",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}update.FailUpdateStatusStateHandler::handleRequest`,
        //TODO: Update this if any changes are made to the lambda handler path
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(this, "FailUpdateStatusLambdaLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "FailUpdateStatusLambdaLogGroup",
        }),
      }
    );

    /**
     * Define Lambda Invokers for step function here.
     */

    const determineUpdateTypeState = new LambdaInvoke(
      this,
      "DetermineUpdateType",
      {
        lambdaFunction: determineUpdateTypeLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: IS_COMMAND_PATH,
      }
    );

    const updateTypeStateChoice = new Choice(this, "UpdateTypeStateChoice");
    const commandCondition = Condition.stringEquals(
      `${IS_COMMAND_PATH}.Payload`,
      "COMMAND"
    );

    const determineCommandTypeState = new LambdaInvoke(
      this,
      "DetermineCommandType",
      {
        lambdaFunction: determineCommandTypeLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: IS_SOFTWARE_UPDATE_PATH,
      }
    );

    const commandTypeStateChoice = new Choice(this, "CommandTypeStateChoice");
    const commandTypeConditionSoftwareUpdate = Condition.stringEquals(
      `${IS_SOFTWARE_UPDATE_PATH}.Payload`,
      "SOFTWARE_UPDATE"
    );

    const createCommandIotJobState = new LambdaInvoke(
      this,
      "CreateCommandIotJob",
      {
        lambdaFunction: createCommandIotJobLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    const checkIotJobStatusState = new LambdaInvoke(this, "CheckIotJobStatus", {
      lambdaFunction: checkIotJobStatusLambda,
      payload: TaskInput.fromObject({
        jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
      }),
      resultPath: RESULT_PATH,
    });

    const createDownloadFileIotJobState = new LambdaInvoke(
      this,
      "CreateDownloadFileIotJob",
      {
        lambdaFunction: createDownloadFileIotJobLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    const checkIotDownloadJobStatusState = new LambdaInvoke(
      this,
      "CheckIotDownloadJobStatus",
      {
        lambdaFunction: checkIotDownloadJobStatusLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    const createInstallApplicationIotJobState = new LambdaInvoke(
      this,
      "CreateInstallApplicationIotJob",
      {
        lambdaFunction: createInstallApplicationIotJobLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    const checkIotInstallApplicationJobStatusState = new LambdaInvoke(
      this,
      "CheckIotInstallApplicationJobStatus",
      {
        lambdaFunction: checkIotInstallApplicationJobStatusLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    const publishConfigurationsToShadowState = new LambdaInvoke(
      this,
      "PublishConfigurationsToShadow",
      {
        lambdaFunction: publishConfigurationsToShadowLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    const notifyDeviceState = new LambdaInvoke(this, "NotifyDevice", {
      lambdaFunction: messageDeviceLambda,
      payload: TaskInput.fromObject({
        jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
      }),
      resultPath: RESULT_PATH,
    });

    const determineDeviceState = new LambdaInvoke(
      this,
      "determineDeviceState",
      {
        lambdaFunction: determineDeviceStateLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: IS_REGISTERED_PATH,
      }
    );

    const deviceStateChoice = new Choice(this, "StateChoice");
    const enabledCondition = Condition.stringEquals(
      `${IS_REGISTERED_PATH}.Payload`,
      "ENABLED"
    );
    const disabledCondition = Condition.stringEquals(
      `${IS_REGISTERED_PATH}.Payload`,
      "DISABLED"
    );
    const createdCondition = Condition.stringEquals(
      `${IS_REGISTERED_PATH}.Payload`,
      "CREATED"
    );

    const createKVSStreamLambdaState = new LambdaInvoke(
      this,
      "CreateKVSStream",
      {
        lambdaFunction: createKVSStreamLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    const attachKvsAccessToCertLambdaState = new LambdaInvoke(
      this,
      "AttachKvsAccessToCert",
      {
        lambdaFunction: attachKvsAccessToCertLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    const detachKvsAccessToCertLambdaState = new LambdaInvoke(
      this,
      "DetachKvsAccessToCert",
      {
        lambdaFunction: detachKvsAccessToCertLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    const fvlWorkflowCheckerLambdaState = new LambdaInvoke(
      this,
      "FvlWorkflowChecker",
      {
        lambdaFunction: fvlWorkflowCheckerLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    const updateUpdateStatusState = new LambdaInvoke(
      this,
      "UpdateUpdateStatus",
      {
        lambdaFunction: updateUpdateStatusStateLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    const updateDeviceTypeState = new LambdaInvoke(this, "UpdateDeviceType", {
      lambdaFunction: updateDeviceTypeStateLambda,
      payload: TaskInput.fromObject({
        jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
      }),
      resultPath: RESULT_PATH,
    });

    const publishUpdateStatusState = new LambdaInvoke(
      this,
      "PublishUpdateStatus",
      {
        lambdaFunction: publishDeviceStateEventHandlerLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    const updateDeviceGroupState = new LambdaInvoke(this, "UpdateDeviceGroup", {
      lambdaFunction: updateDeviceGroupLambda,
      payload: TaskInput.fromObject({
        jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
      }),
      resultPath: RESULT_PATH,
    });

    const failUpdateStatusState = new LambdaInvoke(this, "FailUpdateStatus", {
      lambdaFunction: failUpdateStatusStateLambda,
      payload: TaskInput.fromObject({
        jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        failureReason: JsonPath.stringAt(ERROR_MESSAGE_PATH),
      }),
      resultPath: RESULT_PATH,
    });

    /**
     * Add workflow steps here.  A catch is added to every state to handle error conditions.
     */

    determineUpdateTypeState.next(updateTypeStateChoice);
    determineUpdateTypeState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    updateTypeStateChoice
      .when(commandCondition, determineCommandTypeState)
      .otherwise(publishConfigurationsToShadowState);

    determineCommandTypeState.next(commandTypeStateChoice);
    determineCommandTypeState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    commandTypeStateChoice
      .when(commandTypeConditionSoftwareUpdate, createDownloadFileIotJobState)
      .otherwise(createCommandIotJobState);

    createDownloadFileIotJobState.next(checkIotDownloadJobStatusState);
    createDownloadFileIotJobState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    createDownloadFileIotJobState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    checkIotDownloadJobStatusState.next(createInstallApplicationIotJobState);
    checkIotDownloadJobStatusState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(60),
      maxAttempts: 5,
      backoffRate: 2,
    });
    checkIotDownloadJobStatusState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    createInstallApplicationIotJobState.next(
      checkIotInstallApplicationJobStatusState
    );
    createInstallApplicationIotJobState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    createInstallApplicationIotJobState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    checkIotInstallApplicationJobStatusState.next(successState);
    checkIotInstallApplicationJobStatusState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(60),
      maxAttempts: 5,
      backoffRate: 2,
    });
    checkIotInstallApplicationJobStatusState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    createCommandIotJobState.next(checkIotJobStatusState);
    createCommandIotJobState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    createCommandIotJobState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    checkIotJobStatusState.next(successState);
    checkIotJobStatusState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(60),
      maxAttempts: 5,
      backoffRate: 2,
    });
    checkIotJobStatusState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    publishConfigurationsToShadowState.next(notifyDeviceState);
    publishConfigurationsToShadowState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    publishConfigurationsToShadowState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    // Notify device that update is starting
    notifyDeviceState.next(determineDeviceState);
    notifyDeviceState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    notifyDeviceState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    determineDeviceState.next(deviceStateChoice);
    determineDeviceState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    deviceStateChoice
      .when(enabledCondition, createKVSStreamLambdaState)
      .when(disabledCondition, detachKvsAccessToCertLambdaState)
      .when(createdCondition, detachKvsAccessToCertLambdaState)
      .otherwise(publishUpdateStatusState);

    detachKvsAccessToCertLambdaState.next(publishUpdateStatusState);
    detachKvsAccessToCertLambdaState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    createKVSStreamLambdaState.next(fvlWorkflowCheckerLambdaState);
    createKVSStreamLambdaState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    fvlWorkflowCheckerLambdaState.next(attachKvsAccessToCertLambdaState);
    //TODO: Determine event driven connection between the workflows.  For now will poll (~20min) with backoff.
    fvlWorkflowCheckerLambdaState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 34,
      backoffRate: 2,
    });
    fvlWorkflowCheckerLambdaState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    attachKvsAccessToCertLambdaState.next(publishUpdateStatusState);
    attachKvsAccessToCertLambdaState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    // Publish MQTT message to device state notification topic
    publishUpdateStatusState.next(updateUpdateStatusState);
    publishUpdateStatusState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    publishUpdateStatusState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    // Update the Update Device Job state to successful.
    // End of workflow if it is successful.
    updateUpdateStatusState.next(updateDeviceTypeState);
    updateUpdateStatusState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    updateUpdateStatusState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    // Update the deviceType value to deviceTypePayload value
    updateDeviceTypeState.next(updateDeviceGroupState);
    updateDeviceTypeState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    updateDeviceTypeState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    updateDeviceGroupState.next(successState);
    updateDeviceGroupState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    updateDeviceGroupState.addCatch(failUpdateStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    // Failure step, End of workflow if it fails
    failUpdateStatusState.next(failState);

    /**
     *  Define state machine used by this step function.  This triggers the first lambda in the workflow.
     *  This must remain at the end of this function after the workflow is set up.
     */
    const deviceUpdateStateMachine = new StateMachine(
      this,
      "StartUpdateDeviceStateMachine",
      {
        definition: determineUpdateTypeState,
      }
    );
    this.stateMachine = deviceUpdateStateMachine;
  }

  // Link to dynamoDB, resources must be finalized here in workflows.
  postWorkflowCreationCallback() {
    if (this.workflow && this.workflow.table) {
      if (this.dynamoDbStatement) {
        this.dynamoDbStatement.addResources(this.workflow.table.tableArn);
      }
      const encryptionKey = this.workflow.table.encryptionKey;
      if (encryptionKey && this.kmsStatement) {
        this.kmsStatement.addResources(encryptionKey.keyArn);
      }
    } else {
      throw new Error("Workflow or workflow table is not defined");
    }
  }

  finalizeSetup() {
    super.finalizeSetup();
  }
}
