import { Arn, Duration, Stack, StackProps } from 'aws-cdk-lib';
import type { Role } from 'aws-cdk-lib/aws-iam';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Code, Function, Runtime, Tracing } from 'aws-cdk-lib/aws-lambda';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import {
  Fail,
  JsonPath,
  LogLevel,
  StateMachine,
  Succeed,
  TaskInput
} from 'aws-cdk-lib/aws-stepfunctions';
import { LambdaInvoke } from 'aws-cdk-lib/aws-stepfunctions-tasks';
import type { Construct } from 'constructs';
import {
  AWSRegion,
  AWSRegionUtils,
  createLambdaRole,
  VideoAnalyticsAsyncWorkflowResource
} from 'video_analytics_common_construct';
import {
  ERROR_MESSAGE_PATH,
  LAMBDA_ASSET_PATH,
  PARTITION_KEY_PATH,
  RESULT_PATH,
  RESULT_PATH_ERROR
} from '../const';

export interface WorkflowStackProps extends StackProps {
  region: AWSRegion;
  account: string;
}

class RegisterDeviceWorkflow extends VideoAnalyticsAsyncWorkflowResource {
  partitionKeyName = 'JobId';
  name = 'VLRegisterDeviceJobTable';
  private fvlRegisterDeviceJobTablePolicy = new PolicyStatement({
    effect: Effect.ALLOW,
    actions: [
      'dynamodb:GetRecords',
      'dynamodb:GetItem',
      'dynamodb:Query',
      'dynamodb:PutItem',
      'dynamodb:UpdateItem'
    ],
    resources: [Arn.format({ 
      service: 'dynamodb',
      resource: 'table',
      resourceName: 'VLRegisterDeviceJobTable'
    }, Stack.of(this))]
  });

  private fvlRegisterDeviceJobTableCleanupPolicy = new PolicyStatement({
    effect: Effect.ALLOW,
    actions: ['dynamodb:GetItem', 'dynamodb:PutItem'],
    resources: [Arn.format({ 
      service: 'dynamodb',
      resource: 'table',
      resourceName: 'VLRegisterDeviceJobTable'
    }, Stack.of(this))]
  });

  private kvsResourceCreationPolicy = new PolicyStatement({
    effect: Effect.ALLOW,
    actions: ['kinesisvideo:CreateStream', 'kinesisvideo:CreateSignalingChannel'],
    resources: [Arn.format({ service: 'kinesisvideo', resource: '*' }, Stack.of(this))]
  });

  private kvsResourceDeletionPolicy = new PolicyStatement({
    effect: Effect.ALLOW,
    actions: ['kinesisvideo:DeleteStream', 'kinesisvideo:DeleteSignalingChannel'],
    resources: [Arn.format({ service: 'kinesisvideo', resource: '*' }, Stack.of(this))]
  });

  private kmsPolicy = new PolicyStatement({
    effect: Effect.ALLOW,
    actions: ['kms:Decrypt', 'kms:Encrypt', 'kms:ReEncrypt*', 'kms:GenerateDataKey*'],
    resources: [Arn.format({ service: 'kms', resource: 'key/*' }, Stack.of(this))]
  });

  private kmsEncryptionPolicy = new PolicyStatement({
    effect: Effect.ALLOW,
    actions: ['kms:Decrypt', 'kms:GenerateDataKey*', 'kms:ReEncrypt*'],
    resources: [Arn.format({ service: 'kms', resource: 'key/*' }, Stack.of(this))]
  });

  private kvsRole: Role;
  private failureHandlerRole: Role;
  private region: AWSRegion;
  private airportCode: string;
  private ddbClientSideEncryptionEnvironment: { [key: string]: string };

  constructor(scope: Construct, id: string, props: WorkflowStackProps) {
    super(scope, id);
    this.region = props.region;
    this.airportCode = AWSRegionUtils.getAirportCode(this.region).toLowerCase();

    this.kvsRole = createLambdaRole(this, 'KVSStreamCreatorLambdaRole', [
      this.fvlRegisterDeviceJobTablePolicy,
      this.kvsResourceCreationPolicy,
      this.kmsPolicy,
      this.kmsEncryptionPolicy
    ]);

    this.failureHandlerRole = createLambdaRole(this, 'FailAndCleanupFvlRegHandlerRole', [
      this.fvlRegisterDeviceJobTableCleanupPolicy,
      this.kvsResourceDeletionPolicy,
      this.kmsPolicy
    ]);
  }

  createStepFunction(): void {
    const failState = new Fail(this, 'Fail');
    const successState = new Succeed(this, 'Successful');

    const kvsLambda = new Function(this, 'KvsCreateLambda', {
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      description: 'Lambda responsible for invocation of StepFunction',
      runtime: Runtime.JAVA_17,
      tracing: Tracing.ACTIVE,
      handler: 'com.amazonaws.videoanalytics.videologistics.workflow.KVSResourceCreateLambda::handleRequest',
      memorySize: 2048,
      role: this.kvsRole,
      environment: {
        tableName: this.name,
        airportCode: this.airportCode,
        ...this.ddbClientSideEncryptionEnvironment
      },
      timeout: Duration.minutes(12),
      logGroup: new LogGroup(this, 'KvsCreateLambdaLogGroup', {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: 'KvsCreateLambdaLogGroup'
      })
    });

    const failAndCleanupVlRegHandler = new Function(this, 'FailAndCleanupVlRegHandler', {
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      description:
        'Lambda responsible for cleaning up KVS resources and updating the failure status in ddb.',
      runtime: Runtime.JAVA_17,
      tracing: Tracing.ACTIVE,
      handler:
        'com.amazonaws.videoanalytics.videologistics.workflow.FailAndCleanupVLDeviceRegistrationHandler::handleRequest',
      memorySize: 2048,
      role: this.failureHandlerRole,
      environment: {
        tableName: this.name,
        airportCode: this.airportCode,
        ...this.ddbClientSideEncryptionEnvironment
      },
      timeout: Duration.minutes(12),
      logGroup: new LogGroup(this, 'FailAndCleanupFvlRegHandlerLogGroup', {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: 'FailAndCleanupFvlRegHandlerLogGroup'
      })
    });

    const kvsCreateState = new LambdaInvoke(this, 'Create KVS ARN', {
      lambdaFunction: kvsLambda,
      payload: TaskInput.fromObject({
        JobId: JsonPath.stringAt(PARTITION_KEY_PATH),
      }),
      resultPath: RESULT_PATH
    });

    const failAndCleanupVLRegistrationState = new LambdaInvoke(
      this,
      'FailAndCleanupVLRegStatus',
      {
        lambdaFunction: failAndCleanupVlRegHandler,
        payload: TaskInput.fromObject({
          jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
          FailureReason: JsonPath.stringAt(ERROR_MESSAGE_PATH)
        }),
        resultPath: RESULT_PATH
      }
    );

    kvsCreateState.next(successState);
    kvsCreateState.addCatch(failAndCleanupVLRegistrationState, {
      resultPath: RESULT_PATH_ERROR
    });

    //Failure step, End of workflow if it fails
    failAndCleanupVLRegistrationState.next(failState);
    failAndCleanupVLRegistrationState.addRetry({
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2
    });

    this.stateMachine = new StateMachine(this, 'VLRegisterDeviceStateMachine', {
      logs: {
        destination: new LogGroup(this, "VLRegisterDeviceStateMachineLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "VLRegisterDeviceStateMachineLogGroup"
        }),
        level: LogLevel.ALL,
      },
      definition: kvsCreateState,
      tracingEnabled: true
    });
  }

  postWorkflowCreationCallback() {
    this.fvlRegisterDeviceJobTablePolicy.addResources(this.workflow.table.tableArn);
    this.fvlRegisterDeviceJobTableCleanupPolicy.addResources(this.workflow.table.tableArn);
    this.kvsResourceCreationPolicy.addResources(
      Arn.format({ service: 'kinesisvideo', resource: '*' }, this.workflow.table.stack)
    );
    this.kvsResourceDeletionPolicy.addResources(
      Arn.format({ service: 'kinesisvideo', resource: '*' }, this.workflow.table.stack)
    );
    const encryptionKey = this.workflow.table.encryptionKey;
    if (encryptionKey !== undefined) {
      this.kmsPolicy.addResources(encryptionKey.keyArn);
    }
  }
}

/**
 * Fetch all the workflow resources to create
 */
export function getWorkflowResources(
  scope: Construct,
  props: WorkflowStackProps
): VideoAnalyticsAsyncWorkflowResource[] {
  return [
    new RegisterDeviceWorkflow(scope, 'RegisterDeviceWorkflow', props),
  ];
}
