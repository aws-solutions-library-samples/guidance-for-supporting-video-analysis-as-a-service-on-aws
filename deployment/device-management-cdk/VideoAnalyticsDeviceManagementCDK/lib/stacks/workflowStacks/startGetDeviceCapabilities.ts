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
  LAMBDA_ASSET_PATH,
  DM_WORKFLOW_JAVA_PATH_PREFIX,
} from "../const";

/**
 *
 */
export class StartGetDeviceCapabilities extends VideoAnalyticsAsyncWorkflowResource {
  partitionKeyName = "JobId";
  name = "GetDeviceCapabilitiesTable";

  private readonly dynamoDbStatement: PolicyStatement;
  private readonly kmsStatement: PolicyStatement;
  private readonly region: AWSRegion;
  private readonly airportCode: string;
  private readonly role: Role;
  private readonly account: string;

  constructor(scope: Construct, id: string, props: WorkflowStackProps) {
    super(scope, id);
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
    this.role = createLambdaRole(this, "GetDeviceCapabilitiesRole", [
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
    const requestCapabilitiesLambda = new Function(
      this,
      "RequestCapabilitiesLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH}`),
        description: "Lambda to request device capabilities",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}workflow.index.handler`,
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          ACCOUNT_ID: this.account.toString(),
          LAMBDA_ROLE_ARN: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(this, "RequestCapabilitiesLambdaLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/RequestCapabilitiesLambda",
        }),
      }
    );

    const getCapabilitiesLambda = new Function(this, "GetCapabilitiesLambda", {
      //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
      code: Code.fromAsset(`${LAMBDA_ASSET_PATH}`),
      description: "Lambda to get device capabilities",
      runtime: LAMBDA_RUNTIME,
      handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}workflow.index.handler`,
      memorySize: LAMBDA_MEMORY_SIZE_KB,
      role: this.role,
      environment: {
        ACCOUNT_ID: this.account.toString(),
        LAMBDA_ROLE_ARN: this.role.roleArn,
      },
      timeout: LAMBDA_TIMEOUT_MINUTES,
      logGroup: new LogGroup(this, "GetCapabilitiesLambdaLogGroup", {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: "/aws/lambda/GetCapabilitiesLambda",
      }),
    });

    const failGetCapabilitiesLambda = new Function(
      this,
      "FailGetCapabilitiesLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH}`),
        description: "Lambda to handle get device capabilities failure",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}workflow.index.handler`,
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          ACCOUNT_ID: this.account.toString(),
          LAMBDA_ROLE_ARN: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(this, "FailGetCapabilitiesLambdaLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/FailGetCapabilitiesLambda",
        }),
      }
    );

    const requestCapabilitiesState = new LambdaInvoke(
      this,
      "RequestCapabilities",
      {
        lambdaFunction: requestCapabilitiesLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );
    const getCapabilitiesState = new LambdaInvoke(this, "GetCapabilities", {
      lambdaFunction: getCapabilitiesLambda,
      payload: TaskInput.fromObject({
        jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
      }),
      resultPath: RESULT_PATH,
    });
    const failGetCapabilitiesState = new LambdaInvoke(
      this,
      "FailGetCapabilities",
      {
        lambdaFunction: failGetCapabilitiesLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
          failureReason: JsonPath.stringAt(ERROR_MESSAGE_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    const successState = new Succeed(this, "Successful");
    const failState = new Fail(this, "Fail");

    requestCapabilitiesState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    getCapabilitiesState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 34,
      backoffRate: 2,
    });
    failGetCapabilitiesState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    requestCapabilitiesState.addCatch(failGetCapabilitiesState, {
      resultPath: RESULT_PATH_ERROR,
    });
    getCapabilitiesState.addCatch(failGetCapabilitiesState, {
      resultPath: RESULT_PATH_ERROR,
    });

    failGetCapabilitiesState.next(failState);
    requestCapabilitiesState.next(getCapabilitiesState).next(successState);

    this.stateMachine = new StateMachine(
      this,
      "GetDeviceCapabilitiesStateMachine",
      {
        definition: requestCapabilitiesState,
      }
    );
  }

  // Link to dynamoDB, resources must be finalized here in workflows.
  postWorkflowCreationCallback() {
    if (this.workflow && this.workflow.table) {
      this.dynamoDbStatement.addResources(this.workflow.table.tableArn);
      const encryptionKey = this.workflow.table.encryptionKey;
      if (encryptionKey) {
        this.kmsStatement.addResources(encryptionKey.keyArn);
      }
    } else {
      throw new Error("Workflow or workflow table is not defined");
    }
  }
}
