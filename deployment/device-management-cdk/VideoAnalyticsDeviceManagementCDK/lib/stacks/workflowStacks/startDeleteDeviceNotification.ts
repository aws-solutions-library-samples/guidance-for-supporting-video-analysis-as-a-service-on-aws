import type { Role } from 'aws-cdk-lib/aws-iam';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Chain, Fail, JsonPath, StateMachine, Succeed, TaskInput } from 'aws-cdk-lib/aws-stepfunctions';
import {
  Workflow,
  WorkflowProps,
  VideoAnalyticsAsyncWorkflowResource,
  createLambdaRole, AWSRegionUtils,
  AWSRegion
} from 'video_analytics_common_construct/';
import { Function, Runtime, Code } from 'aws-cdk-lib/aws-lambda';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { LambdaInvoke } from 'aws-cdk-lib/aws-stepfunctions-tasks';
import { Duration } from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { WorkflowStackProps } from './workflowStack';
import {
  DM_CONTROL_PLANE_PACKAGE,
  ERROR_MESSAGE_PATH,
  LAMBDA_MEMORY_SIZE_KB,
  LAMBDA_RUNTIME,
  LAMBDA_TIMEOUT_MINUTES,
  PARTITION_KEY_PATH,
  RESULT_PATH,
  RESULT_PATH_ERROR,
  LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT,
  DM_WORKFLOW_JAVA_PATH_PREFIX
} from '../const';

/**
 *
 */
export class StartDeleteDeviceNotification extends VideoAnalyticsAsyncWorkflowResource {
  partitionKeyName = 'JobId';
  name = 'DeleteDeviceNotificationTable';

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
        'dynamodb:GetRecords',
        'dynamodb:GetItem',
        'dynamodb:Query',
        'dynamodb:PutItem',
        'dynamodb:UpdateItem'
      ],
      resources: ['*'] // This will be updated in postWorkflowCreationCallback
    });

    this.kmsStatement = new PolicyStatement({
      effect: Effect.ALLOW,
      actions: ['kms:Decrypt', 'kms:Encrypt', 'kms:ReEncrypt*'],
      resources: ['*'] // This will be updated in postWorkflowCreationCallback
    });

    this.role = createLambdaRole(this, 'StartDeleteDeviceNotificationRole', [
      this.dynamoDbStatement,
      this.kmsStatement,
    ]);
    this.region = props.region;
    this.airportCode = AWSRegionUtils.getAirportCode(props.region).toLowerCase();
    this.account = props.account;
  }

  createStepFunction(): void {
    const failState = new Fail(this, 'Fail');
    const successState = new Succeed(this, 'Successful');

    /*
     * Define Lambdas for Step function here.
     * Order matters here, first lambdas are defined, then lambda invokers,
     * then the state machine and other resources, then define the workflow steps.
     */
    const detachNotificationPermissionFromDeviceCertLambda = new Function(
      this,
      'DetachNotificationPermissionFromDeviceCertLambda',
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          'Lambda responsible for detaching notification permission from the device cert',
        runtime: LAMBDA_RUNTIME,
        handler:
          `${DM_WORKFLOW_JAVA_PATH_PREFIX}deletedevicenotif.DetachNotificationPermissionFromDeviceCertHandler::handleRequest`,
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(this, 'DetachNotificationPermissionFromDeviceCertLambdaLogGroup', {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: 'DetachNotificationPermissionFromDeviceCertLambdaLogGroup'
        })
      }
    );

    const removeNotificationTopicFromShadowLambda = new Function(
      this,
      'RemoveNotificationTopicFromShadowLambda',
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description: 'Lambda responsible for removing notification topic from the shadow',
        runtime: LAMBDA_RUNTIME,
        handler:
          `${DM_WORKFLOW_JAVA_PATH_PREFIX}deletedevicenotif.RemoveNotificationTopicFromShadowHandler::handleRequest`,
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(this, 'RemoveNotificationTopicFromShadowLambdaLogGroup', {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: 'RemoveNotificationTopicFromShadowLambdaLogGroup'
        })
      }
    );

    const updateDeleteDeviceNotificationStatusStateLambda = new Function(
      this,
      'UpdateDeleteDeviceNotificationStatusLambda',
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          'Lambda responsible for updating the state of the Delete Device Notification Job',
        runtime: LAMBDA_RUNTIME,
        handler:
          `${DM_WORKFLOW_JAVA_PATH_PREFIX}deletedevicenotif.UpdateDeleteDeviceNotificationStatusStateHandler::handleRequest`,
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(this, 'UpdateDeleteDeviceNotificationStatusLambdaLogGroup', {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: 'UpdateDeleteDeviceNotificationStatusLambdaLogGroup'
        })
      }
    );

    const failDeleteDeviceNotificationStatusStateLambda = new Function(
      this,
      'FailDeleteDeviceNotificationStatusLambda',
      {
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description: 'Lambda responsible for marking as failed',
        runtime: LAMBDA_RUNTIME,
        handler:
          `${DM_WORKFLOW_JAVA_PATH_PREFIX}deletedevicenotif.FailDeleteDeviceNotificationStatusStateHandler::handleRequest`,
        memorySize: LAMBDA_MEMORY_SIZE_KB,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
        },
        timeout: LAMBDA_TIMEOUT_MINUTES,
        logGroup: new LogGroup(this, 'FailDeleteDeviceNotificationStatusLambdaLogGroup', {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: 'FailDeleteDeviceNotificationStatusLambdaLogGroup'
        })
      }
    );

    /**
     * Define Lambda Invokers for step function here.
     */
    const detachNotificationPermissionFromDeviceCertState = new LambdaInvoke(
      this,
      'DetachNotificationPermissionFromDeviceCert',
      {
        lambdaFunction: detachNotificationPermissionFromDeviceCertLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH)
        }),
        resultPath: RESULT_PATH
      }
    );

    const removeNotificationTopicFromShadowState = new LambdaInvoke(
      this,
      'RemoveNotificationTopicFromShadow',
      {
        lambdaFunction: removeNotificationTopicFromShadowLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH)
        }),
        resultPath: RESULT_PATH
      }
    );

    const updateDeleteDeviceNotificationStatusState = new LambdaInvoke(
      this,
      'UpdateDeleteDeviceNotificationStatus',
      {
        lambdaFunction: updateDeleteDeviceNotificationStatusStateLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH)
        }),
        resultPath: RESULT_PATH
      }
    );

    const failDeleteDeviceNotificationStatusState = new LambdaInvoke(
      this,
      'FailDeleteDeviceNotificationStatus',
      {
        lambdaFunction: failDeleteDeviceNotificationStatusStateLambda,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
          failureReason: JsonPath.stringAt(ERROR_MESSAGE_PATH)
        }),
        resultPath: RESULT_PATH
      }
    );

    /**
     * Add workflow steps here.  A catch is added to every state to handle error conditions.
     */

    // Step 1 : Detach notification permision from device cert
    detachNotificationPermissionFromDeviceCertState.next(removeNotificationTopicFromShadowState);
    detachNotificationPermissionFromDeviceCertState.addRetry({
      //TODO: Update this once the model code is updated
      errors: ['com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException'],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2
    });
    detachNotificationPermissionFromDeviceCertState.addCatch(
      failDeleteDeviceNotificationStatusState,
      {
        resultPath: RESULT_PATH_ERROR
      }
    );

    // Step 2 : Remove notification topic from shadow
    removeNotificationTopicFromShadowState.next(updateDeleteDeviceNotificationStatusState);
    removeNotificationTopicFromShadowState.addRetry({
      //TODO: Update this once the model code is updated
      errors: ['com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException'],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2
    });
    removeNotificationTopicFromShadowState.addCatch(failDeleteDeviceNotificationStatusState, {
      resultPath: RESULT_PATH_ERROR
    });

    // Step 3 : Update the Notification Rule Creation Job state to successful.
    // End of workflow if it is successful.
    updateDeleteDeviceNotificationStatusState.next(successState);
    updateDeleteDeviceNotificationStatusState.addRetry({
      //TODO: Update this once the model code is updated
      errors: ['com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException'],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2
    });
    updateDeleteDeviceNotificationStatusState.addCatch(failDeleteDeviceNotificationStatusState, {
      resultPath: RESULT_PATH_ERROR
    });

    // Failure step, End of workflow if it fails
    failDeleteDeviceNotificationStatusState.next(failState);

    /**
     *  Define state machine used by this step function.  This triggers the first lambda in the workflow.
     *  This must remain at the end of this function after the workflow is set up.
     */
    const deleteNotificationRuleStateMachine = new StateMachine(
      this,
      'StartDeleteDeviceNotificationStateMachine',
      {
        definition: detachNotificationPermissionFromDeviceCertState
      }
    );
    this.stateMachine = deleteNotificationRuleStateMachine;
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
      throw new Error('Workflow or workflow table is not defined');
    }
  }

  finalizeSetup() {
    super.finalizeSetup();
  }
}
