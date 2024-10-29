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
} from "video_analytics_common_construct/";
import { AWSRegion } from "video_analytics_common_construct/lib/serviceConstructs/util";
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
  LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT,
  DM_WORKFLOW_JAVA_PATH_PREFIX,
} from "../const";

/**
 *
 */
export class StartUpdateNotificationRule extends VideoAnalyticsAsyncWorkflowResource {
  partitionKeyName = "JobId";
  name = "UpdateNotificationRuleTable";

  private readonly region: AWSRegion;
  private readonly airportCode: string;
  private readonly role: Role;
  private readonly account: string;
  private readonly dynamoDbStatement: PolicyStatement;
  private readonly kmsStatement: PolicyStatement;

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
    this.role = createLambdaRole(this, "StartUpdateNotificationRuleRole", [
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

    const updateIotRuleLambda = new Function(this, "UpdateIotRuleLambda", {
      //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
      code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
      description: "Lambda responsible for updating an IoT Rule",
      runtime: LAMBDA_RUNTIME,
      handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}workflow.updatenotifrule.UpdateIotRuleHandler::handleRequest`,
      memorySize: LAMBDA_MEMORY_SIZE_KB,
      role: this.role,
      environment: {
        AccountId: this.account.toString(),
        LambdaRoleArn: this.role.roleArn,
      },
      timeout: LAMBDA_TIMEOUT_MINUTES,
      logGroup: new LogGroup(this, "UpdateIotRuleLambdaLogGroup", {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: "UpdateIotRuleLambdaLogGroup",
      }),
    });

    const updateUpdateNotificationRuleStatusStateLambda = new Function(
      this,
      "UpdateUpdateNotificationRuleStatusLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for updating the state of the Update Notification Rule Job",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}workflow.updatenotifrule.UpdateUpdateNotificationRuleStatusStateHandler::handleRequest`,
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(
          this,
          "UpdateUpdateNotificationRuleStatusLambdaLogGroup",
          {
            retention: RetentionDays.TEN_YEARS,
            logGroupName: "UpdateUpdateNotificationRuleStatusLambdaLogGroup",
          }
        ),
      }
    );

    const failUpdateNotificationRuleStatusStateLambda = new Function(
      this,
      "FailUpdateNotificationRuleStatusLambda",
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description: "Lambda responsible for marking as failed",
        runtime: LAMBDA_RUNTIME,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}workflow.updatenotifrule.FailUpdateNotificationRuleStatusStateHandler::handleRequest`,
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(
          this,
          "FailUpdateNotificationRuleStatusLambdaLogGroup",
          {
            retention: RetentionDays.TEN_YEARS,
            logGroupName: "FailUpdateNotificationRuleStatusLambdaLogGroup",
          }
        ),
      }
    );

    /**
     * Define Lambda Invokers for step function here.
     */
    const updateIotRuleState = new LambdaInvoke(this, "UpdateIotRule", {
      lambdaFunction: updateIotRuleLambda,
      payload: TaskInput.fromObject({
        jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
      }),
      resultPath: RESULT_PATH,
    });

    const updateCreateNotificationRuleStatusState = new LambdaInvoke(
      this,
      "UpdateCreateNotificationRuleStatus",
      {
        lambdaFunction: updateUpdateNotificationRuleStatusStateLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    const failCreateNotificationRuleStatusState = new LambdaInvoke(
      this,
      "FailCreateNotificationRuleStatus",
      {
        lambdaFunction: failUpdateNotificationRuleStatusStateLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
          failureReason: JsonPath.stringAt(ERROR_MESSAGE_PATH),
        }),
        resultPath: RESULT_PATH,
      }
    );

    /**
     * Add workflow steps here.  A catch is added to every state to handle error conditions.
     */

    // Step 1 : Update IoT rule
    updateIotRuleState.next(updateCreateNotificationRuleStatusState);
    updateIotRuleState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    updateIotRuleState.addCatch(failCreateNotificationRuleStatusState, {
      resultPath: RESULT_PATH_ERROR,
    });

    // Step 2 : Update the Notification Rule Update Job state to successful.
    // End of workflow if it is successful.
    updateCreateNotificationRuleStatusState.next(successState);
    updateCreateNotificationRuleStatusState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    updateCreateNotificationRuleStatusState.addCatch(
      failCreateNotificationRuleStatusState,
      {
        resultPath: RESULT_PATH_ERROR,
      }
    );

    // Failure step, End of workflow if it fails
    failCreateNotificationRuleStatusState.next(failState);

    /**
     *  Define state machine used by this step function.  This triggers the first lambda in the workflow.
     *  This must remain at the end of this function after the workflow is set up.
     */
    const updateNotificationRuleStateMachine = new StateMachine(
      this,
      "StartUpdateNotificationRuleStateMachine",
      {
        definition: updateIotRuleState,
      }
    );
    this.stateMachine = updateNotificationRuleStateMachine;
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

  finalizeSetup() {
    super.finalizeSetup();
  }
}
