import type { Role } from 'aws-cdk-lib/aws-iam';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import type { Construct } from 'constructs';
import { Code, Function, Runtime } from 'aws-cdk-lib/aws-lambda';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { Arn, Duration, Stack, StackProps } from 'aws-cdk-lib';
import { LambdaInvoke } from 'aws-cdk-lib/aws-stepfunctions-tasks';
import {
  Choice,
  Condition,
  Fail,
  Succeed,
  JsonPath,
  StateMachine,
  TaskInput
} from 'aws-cdk-lib/aws-stepfunctions';
import {
  ERROR_MESSAGE_PATH,
  PARTITION_KEY_PATH,
  PLAYBACK_SESSION_STATUS,
  RESULT_PATH,
  RESULT_PATH_ERROR,
  SHOULD_STREAM_FROM_DEVICE_PATH,
  SORT_KEY_PATH,
  LAMBDA_ASSET_PATH
} from '../const';
import {
  AWSRegion,
  createLambdaRole,
  VideoAnalyticsAsyncWorkflowResource,
  AWSRegionUtils
} from 'video_analytics_common_construct';
import { VideoExportWorkflow } from '../videoExportStack';

export interface WorkflowStackProps extends StackProps {
  region: AWSRegion;
  account: string;
}

class RegisterDeviceWorkflow extends VideoAnalyticsAsyncWorkflowResource {
  partitionKeyName = 'CustomerAccountId';
  sortKeyName = 'JobId';
  name = 'FVLRegisterDeviceJobTable';
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
      resourceName: 'FVLRegisterDeviceJobTable'
    }, Stack.of(this))]
  });

  private fvlRegisterDeviceJobTableCleanupPolicy = new PolicyStatement({
    effect: Effect.ALLOW,
    actions: ['dynamodb:GetItem', 'dynamodb:PutItem'],
    resources: [Arn.format({ 
      service: 'dynamodb',
      resource: 'table',
      resourceName: 'FVLRegisterDeviceJobTable'
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

  // Keeping the scope for resources broad-ish in order to allow decryption by cross account keys
  private kmsEncryptionPolicy = new PolicyStatement({
    effect: Effect.ALLOW,
    actions: ['kms:Decrypt', 'kms:GenerateDataKey*', 'kms:ReEncrypt*'],
    resources: ['arn:*:kms:*:*:key/*']
  });

  private crossAccountAssumeRolePolicy = new PolicyStatement({
    effect: Effect.ALLOW,
    actions: ['sts:AssumeRole']
  });

  private kvsRole: Role;
  private failureHandlerRole: Role;
  private region: AWSRegion;
  private airportCode: string;
  private ddbClientSideEncryptionEnvironment: { [key: string]: string };

  constructor(scope: Construct, id: string, props: WorkflowStackProps) {
    super(scope, id);
    this.crossAccountAssumeRolePolicy.addResources(
      Arn.format(
        {
          service: 'iam',
          region: '',
          account: '*',
          resource: 'role',
          resourceName: 'CrossAccountRoleForStartFVLRegisterDevice'
        },
        Stack.of(this)
      )
    );
    this.region = props.region;
    this.airportCode = AWSRegionUtils.getAirportCode(this.region).toLowerCase();

    this.kvsRole = createLambdaRole(this, 'KVSStreamCreatorLambdaRole', [
      this.fvlRegisterDeviceJobTablePolicy,
      this.crossAccountAssumeRolePolicy,
      this.kvsResourceCreationPolicy,
      this.kmsPolicy,
      this.kmsEncryptionPolicy
    ]);

    this.failureHandlerRole = createLambdaRole(this, 'FailAndCleanupFvlRegHandlerRole', [
      this.fvlRegisterDeviceJobTableCleanupPolicy,
      this.crossAccountAssumeRolePolicy,
      this.kvsResourceDeletionPolicy,
      this.kmsPolicy
    ]);
  }

  createStepFunction(): void {
    const failState = new Fail(this, 'Fail');
    const successState = new Succeed(this, 'Successful');

    const kvsLambda = new Function(this, 'KvsCreateLambda', {
      // TODO: Update lambda asset path once code compiled jar is available
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      description: 'Lambda responsible for invocation of StepFunction',
      runtime: Runtime.JAVA_17,
      // TODO: Update handler to match new lambda handler path
      handler: 'com.amazon.awsvideoanalyticsvlcontrolplane.workflow.KVSResourceCreateLambda::handleRequest',
      memorySize: 2048,
      role: this.kvsRole,
      environment: {
        tableName: this.name,
        airportCode: this.airportCode,
        ...this.ddbClientSideEncryptionEnvironment
      },
      timeout: Duration.minutes(12),
      logGroup: new LogGroup(this, 'DataForwarderLambdaLogGroup', {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: 'KvsCreateLambdaLogGroup'
      })
    });

    const failAndCleanupFvlRegHandler = new Function(this, 'FailAndCleanupFvlRegHandler', {
      // TODO: Update lambda asset path once code compiled jar is available
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      description:
        'Lambda responsible for cleaning up KVS resources and updating the failure status in ddb.',
      runtime: Runtime.JAVA_17,
      handler:
        // TODO: Update handler to match new lambda handler path
        'com.amazon.awsvideoanalyticsvlcontrolplane.workflow.FailAndCleanupFVLDeviceRegistrationHandler::handleRequest',
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
        CustomerAccountId: JsonPath.stringAt(PARTITION_KEY_PATH),
        JobId: JsonPath.stringAt(SORT_KEY_PATH)
      }),
      resultPath: RESULT_PATH
    });

    const failAndCleanupFVLRegistrationState = new LambdaInvoke(
      this,
      'FailAndCleanupFVLRegStatus',
      {
        lambdaFunction: failAndCleanupFvlRegHandler,
        payload: TaskInput.fromObject({
          CustomerAccountId: JsonPath.stringAt(PARTITION_KEY_PATH),
          JobId: JsonPath.stringAt(SORT_KEY_PATH),
          FailureReason: JsonPath.stringAt(ERROR_MESSAGE_PATH)
        }),
        resultPath: RESULT_PATH
      }
    );

    kvsCreateState.next(successState);
    kvsCreateState.addCatch(failAndCleanupFVLRegistrationState, {
      resultPath: RESULT_PATH_ERROR
    });

    //Failure step, End of workflow if it fails
    failAndCleanupFVLRegistrationState.next(failState);
    failAndCleanupFVLRegistrationState.addRetry({
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2
    });

    this.stateMachine = new StateMachine(this, 'FVLRegisterDeviceStateMachine', {
      definition: kvsCreateState
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
    this.crossAccountAssumeRolePolicy.addResources(
      `arn:aws:iam::*:role/CrossAccountRoleForStartFVLRegisterDevice-${this.airportCode}`
    );
  }
}

class ModelSchema extends VideoAnalyticsAsyncWorkflowResource {
  partitionKeyName = 'CustomerAccountId';
  sortKeyName = 'ModelSchemaId';
  name = 'ModelSchemaTable';
  private statement = new PolicyStatement({
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
      resourceName: 'ModelSchemaTable'
    }, Stack.of(this))]
  });
  private role: Role;
  private ddbClientSideEncryptionEnvironment: { [key: string]: string };

  constructor(scope: Construct, id: string, props: WorkflowStackProps) {
    super(scope, id);
    this.role = createLambdaRole(this, 'ModelSchemaRole', [
      this.statement
    ]);
  }

  createStepFunction(): void {
    const stepFunctionLambda = new Function(this, 'ModelSchemaLambda', {
      // TODO: Update lambda asset path once code compiled jar is available
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      description: 'Lambda responsible for invocation of StepFunction',
      runtime: Runtime.JAVA_17,
      // TODO: Update this to actual handler
      handler: 'com.amazon.awsvideoanalyticsvlcontrolplane.lambda.KVSResourceCreateLambda::handleRequest',
      memorySize: 2048,
      role: this.role,
      environment: {
        tableName: this.name,
        ...this.ddbClientSideEncryptionEnvironment
      },
      timeout: Duration.minutes(12),
      logGroup: new LogGroup(this, 'ModelSchemaLambdaLogGroup', {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: 'ModelSchemaLambdaLogGroup'
      })
    });
    const finalStatus = new LambdaInvoke(this, 'ModelSchemaIndexTemplateCreate', {
      lambdaFunction: stepFunctionLambda
    });

    const simpleStateMachine = new StateMachine(this, 'ModelSchemaStateMachine', {
      definition: finalStatus
    });
    this.stateMachine = simpleStateMachine;
  }

  postWorkflowCreationCallback() {
    this.statement.addResources(this.workflow.table.tableArn);
  }
}

class PlaybackSession extends VideoAnalyticsAsyncWorkflowResource {
  partitionKeyName = 'CustomerAccountId';
  sortKeyName = 'SessionId';
  name = 'PlaybackSessionTable';
  private dynamoDbStatement = new PolicyStatement({
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
      resourceName: 'PlaybackSessionTable'
    }, Stack.of(this))]
  });

  private kmsStatement = new PolicyStatement({
    effect: Effect.ALLOW,
    actions: ['kms:Decrypt', 'kms:Encrypt', 'kms:ReEncrypt*'],
    resources: [Arn.format({ service: 'kms', resource: 'key/*' }, Stack.of(this))]
  });


  private kvsStatement = new PolicyStatement({
    effect: Effect.ALLOW,
    actions: ['kinesisvideo:GetHLSStreamingSessionURL', 'kinesisvideo:GetDataEndpoint'],
    resources: ['*']
  });

  private readonly airportCode: string;
  private readonly role: Role;
  private readonly deviceCellAccountIds: string[];
  private readonly ddbClientSideEncryptionEnvironment: { [key: string]: string };

  constructor(scope: Construct, id: string, props: WorkflowStackProps) {
    super(scope, id);

    this.role = createLambdaRole(this, 'PlaybackSessionRole', [
      this.dynamoDbStatement,
      this.kmsStatement,
      this.kvsStatement,
    ]);
    this.airportCode = AWSRegionUtils.getAirportCode(props.region).toLowerCase();
  }

  createStepFunction(): void {
    const failState = new Fail(this, 'Fail');
    const successState = new Succeed(this, 'Successful');

    const updatePlaybackSessionStatusLambda = new Function(
      this,
      'UpdatePlaybackSessionStatusLambda',
      {
        // TODO: Update lambda asset path once code compiled jar is available
        code: Code.fromAsset(LAMBDA_ASSET_PATH),
        description: 'Lambda responsible for updating playback session status',
        runtime: Runtime.JAVA_17,
        handler:
          // TODO: Update handler to match new lambda handler path  
          'com.amazon.awsvideoanalyticsvlcontrolplane.playback.UpdatePlaybackSessionStatus::handleRequest',
        memorySize: 2048,
        role: this.role,
        environment: {
          ...this.ddbClientSideEncryptionEnvironment
        },
        timeout: Duration.minutes(12),
        logGroup: new LogGroup(this, 'UpdatePlaybackSessionStatusLambdaLogGroup', {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: 'UpdatePlaybackSessionStatusLambdaLogGroup'
        })
      }
    );

    // Step 2: determine if it is HLS or WebRTC path
    const determineIfCloudOrPeerToPeerLambda = new Function(
      this,
      'DetermineIfCloudOrPeerToPeerLambda',
      {
        // TODO: Update lambda asset path once code compiled jar is available
        code: Code.fromAsset(LAMBDA_ASSET_PATH),
        description: 'Lambda responsible for determining to try WebRTC or HLS for playback session',
        runtime: Runtime.JAVA_17,
        // TODO: Update this handler path
        handler:
          'com.amazon.awsvideoanalyticsvlcontrolplane.playback.PlaybackSessionDetermination::handleRequest',
        memorySize: 2048,
        role: this.role,
        environment: {
          tableName: this.name,
          ...this.ddbClientSideEncryptionEnvironment
        },
        timeout: Duration.minutes(12),
        logGroup: new LogGroup(this, 'DetermineIfCloudOrPeerToPeerLambdaLogGroup', {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: 'DetermineIfCloudOrPeerToPeerLambdaLogGroup'
        })
      }
    );

    const determineIfCloudOrPeerToPeerState = new LambdaInvoke(
      this,
      'DetermineIfCloudOrPeerToPeer',
      {
        lambdaFunction: determineIfCloudOrPeerToPeerLambda,
        payload: TaskInput.fromObject({
          customerAccountId: JsonPath.stringAt(PARTITION_KEY_PATH),
          sessionId: JsonPath.stringAt(SORT_KEY_PATH)
        }),
        resultPath: SHOULD_STREAM_FROM_DEVICE_PATH
      }
    );

    const failPlaybackSessionStatusState = new LambdaInvoke(
      this,
      'FailPlaybackSessionStatusState',
      {
        lambdaFunction: updatePlaybackSessionStatusLambda,
        payload: TaskInput.fromObject({
          customerAccountId: JsonPath.stringAt(PARTITION_KEY_PATH),
          sessionId: JsonPath.stringAt(SORT_KEY_PATH),
          sessionStatus: PLAYBACK_SESSION_STATUS.FAILED,
          failureReason: JsonPath.stringAt(ERROR_MESSAGE_PATH)
        })
      }
    );

    failPlaybackSessionStatusState.next(failState);

    // Step 3: Making the choice on which state to go to
    const playbackPeerVsCloudChoiceState = new Choice(this, 'ShouldStreamFromDevice');
    determineIfCloudOrPeerToPeerState.next(playbackPeerVsCloudChoiceState);
    determineIfCloudOrPeerToPeerState.addCatch(failPlaybackSessionStatusState, {
      resultPath: RESULT_PATH_ERROR
    });

    // Step 3a: HLS and update status table
    const hlsStreamingLambda = new Function(this, 'HLSStreamingLambda', {
      // TODO: Update lambda asset path once code compiled jar is available
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      description: 'Lambda responsible for fetching and updating the table with hls streaming URL',
      runtime: Runtime.JAVA_17,
      handler:
        // TODO: Update handler to match new lambda handler path
        'com.amazon.awsvideoanalyticsvlcontrolplane.playback.HLSStreamingSessionHandler::handleRequest',
      memorySize: 2048,
      role: this.role,
      environment: {
        tableName: this.name,
        airportCode: this.airportCode,
        ...this.ddbClientSideEncryptionEnvironment
      },
      timeout: Duration.minutes(12),
      logGroup: new LogGroup(this, 'HLSStreamingLambdaLogGroup', {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: 'HLSStreamingLambdaLogGroup'
      })
    });
    const startHLSStreamingSession = new LambdaInvoke(this, 'StartHLSStreamingSession', {
      lambdaFunction: hlsStreamingLambda,
      payload: TaskInput.fromObject({
        customerAccountId: JsonPath.stringAt(PARTITION_KEY_PATH),
        sessionId: JsonPath.stringAt(SORT_KEY_PATH)
      }),
      resultPath: RESULT_PATH
    });

    const updatePlaybackSessionStatusState = new LambdaInvoke(
      this,
      'UpdatePlaybackSessionStatusState',
      {
        lambdaFunction: updatePlaybackSessionStatusLambda,
        payload: TaskInput.fromObject({
          customerAccountId: JsonPath.stringAt(PARTITION_KEY_PATH),
          sessionId: JsonPath.stringAt(SORT_KEY_PATH),
          sessionStatus: PLAYBACK_SESSION_STATUS.COMPLETED
        })
      }
    );

    updatePlaybackSessionStatusState.next(successState);

    startHLSStreamingSession.next(updatePlaybackSessionStatusState);
    startHLSStreamingSession.addCatch(failPlaybackSessionStatusState, {
      resultPath: RESULT_PATH_ERROR
    });

    playbackPeerVsCloudChoiceState.when(
      Condition.booleanEquals(`${SHOULD_STREAM_FROM_DEVICE_PATH}.Payload`, false),
      startHLSStreamingSession
    );

    // Step 3b: Try and create WebRTC Connection
    const webRtcLambda = new Function(this, 'WebRtcLambda', {
      // TODO: Update lambda asset path once code compiled jar is available
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      description: 'Lambda responsible for handling P2P connections',
      runtime: Runtime.JAVA_17,
      // TODO: Update handler to match new lambda handler path
      handler: 'com.amazon.awsvideoanalyticsvlcontrolplane.playback.WebRTCConnectionHandler::handleRequest',
      memorySize: 2048,
      role: this.role,
      environment: {
        tableName: this.name,
        airportCode: this.airportCode,
        ...this.ddbClientSideEncryptionEnvironment
      },
      timeout: Duration.minutes(12),
      logGroup: new LogGroup(this, 'PlaybackWebRtcLambdaLogGroup', {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: 'PlaybackWebRtcLambdaLogGroup'
      })
    });

    const webRtcSessionState = new LambdaInvoke(this, 'WebRTCSessionState', {
      lambdaFunction: webRtcLambda,
      payload: TaskInput.fromObject({
        customerAccountId: JsonPath.stringAt(PARTITION_KEY_PATH),
        sessionId: JsonPath.stringAt(SORT_KEY_PATH)
      }),
      resultPath: RESULT_PATH
    });

    playbackPeerVsCloudChoiceState.when(
      Condition.booleanEquals(`${SHOULD_STREAM_FROM_DEVICE_PATH}.Payload`, true),
      webRtcSessionState
    );

    const wasWebRtcConnectionSuccessful = new Choice(this, 'WasWebRTCConnectionSuccessful');

    // Step 4a: Try HLS if P2P is not available
    // TODO: Potentially remove this fork in the workflow depending on the timeline API
    wasWebRtcConnectionSuccessful.when(
      Condition.booleanEquals(`${RESULT_PATH}.Payload`, false),
      startHLSStreamingSession
    );

    wasWebRtcConnectionSuccessful.when(
      Condition.booleanEquals(`${RESULT_PATH}.Payload`, true),
      updatePlaybackSessionStatusState
    );
    webRtcSessionState.next(wasWebRtcConnectionSuccessful);
    webRtcSessionState.addCatch(failPlaybackSessionStatusState, {
      resultPath: RESULT_PATH_ERROR
    });

    const playbackStateMachine = new StateMachine(this, 'PlaybackSessionStateMachine', {
      definition: determineIfCloudOrPeerToPeerState
    });
    this.stateMachine = playbackStateMachine;
  }

  postWorkflowCreationCallback() {
    this.dynamoDbStatement.addResources(this.workflow.table.tableArn);
    const encryptionKey = this.workflow.table.encryptionKey;
    if (encryptionKey !== undefined) {
      this.kmsStatement.addResources(encryptionKey.keyArn);
    }
  }

  finalizeSetup() {
    super.finalizeSetup();
  }
}

class LiveStream extends VideoAnalyticsAsyncWorkflowResource {
  partitionKeyName = 'CustomerAccountId';
  sortKeyName = 'SessionId';
  name = 'LivestreamSessionTable';
  private dynamoDbStatement = new PolicyStatement({
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
      resourceName: 'LivestreamSessionTable'
    }, Stack.of(this))]
  });

  private kmsStatement = new PolicyStatement({
    effect: Effect.ALLOW,
    actions: ['kms:Decrypt', 'kms:Encrypt', 'kms:ReEncrypt*'],
    resources: [Arn.format({ service: 'kms', resource: 'key/*' }, Stack.of(this))]
  });

  private kvsStatement = new PolicyStatement({
    effect: Effect.ALLOW,
    actions: ['kinesisvideo:GetHLSStreamingSessionURL', 'kinesisvideo:GetDataEndpoint'],
    resources: [Arn.format({ service: 'kinesisvideo', resource: '*' }, Stack.of(this))]
  });

  private readonly airportCode: string;
  private readonly role: Role;

  constructor(scope: Construct, id: string, props: WorkflowStackProps) {
    super(scope, id);
    this.role = createLambdaRole(this, 'LivestreamSessionRole', [
      this.dynamoDbStatement,
      this.kmsStatement,
      this.kvsStatement,

    ]);
    this.airportCode = AWSRegionUtils.getAirportCode(props.region).toLowerCase();
  }

  createStepFunction(): void {
    const failState = new Fail(this, 'Fail');
    const successState = new Succeed(this, 'Successful');

    const updateLivestreamSessionStatusLambda = new Function(
      this,
      'UpdateLivestreamSessionStatusLambda',
      {
        // TODO: Update lambda asset path once code compiled jar is available
        code: Code.fromAsset(LAMBDA_ASSET_PATH),
        description: 'Lambda responsible for updating livestream session status',
        runtime: Runtime.JAVA_17,
        handler:
          // TODO: Update handler to match new lambda handler path
          'com.amazon.awsvideoanalyticsvlcontrolplane.livestream.UpdateLivestreamSessionStatus::handleRequest',
        memorySize: 2048,
        role: this.role,
        timeout: Duration.minutes(12),
        logGroup: new LogGroup(this, 'UpdateLivestreamSessionStatusLambdaLogGroup', {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: 'UpdateLivestreamSessionStatusLambdaLogGroup'
        })
      }
    );

    const failLivestreamSessionStatusState = new LambdaInvoke(
      this,
      'FailLivestreamSessionStatusState',
      {
        lambdaFunction: updateLivestreamSessionStatusLambda,
        payload: TaskInput.fromObject({
          customerAccountId: JsonPath.stringAt(PARTITION_KEY_PATH),
          sessionId: JsonPath.stringAt(SORT_KEY_PATH),
          sessionStatus: PLAYBACK_SESSION_STATUS.FAILED,
          failureReason: JsonPath.stringAt(ERROR_MESSAGE_PATH)
        })
      }
    );

    failLivestreamSessionStatusState.next(failState);

    const updateLivestreamSessionStatusState = new LambdaInvoke(
      this,
      'UpdateLivestreamSessionStatusState',
      {
        lambdaFunction: updateLivestreamSessionStatusLambda,
        payload: TaskInput.fromObject({
          customerAccountId: JsonPath.stringAt(PARTITION_KEY_PATH),
          sessionId: JsonPath.stringAt(SORT_KEY_PATH),
          sessionStatus: PLAYBACK_SESSION_STATUS.COMPLETED
        })
      }
    );

    updateLivestreamSessionStatusState.next(successState);

    const webRtcLambda = new Function(this, 'WebRtcLambda', {
      // TODO: Update lambda asset path once code compiled jar is available
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      description: 'Lambda responsible for handling P2P connections',
      runtime: Runtime.JAVA_17,
      handler:
        // TODO: Update handler to match new lambda handler path
        'com.amazon.awsvideoanalyticsvlcontrolplane.livestream.WebRTCLivestreamConnectionHandler::handleRequest',
      memorySize: 2048,
      role: this.role,
      environment: {
        tableName: this.name,
        airportCode: this.airportCode
      },
      timeout: Duration.minutes(12),
      logGroup: new LogGroup(this, 'LivestreamWebRtcLambdaLogGroup', {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: 'LivestreamWebRtcLambdaLogGroup'
      })
    });

    const webRtcSessionState = new LambdaInvoke(this, 'WebRTCSessionState', {
      lambdaFunction: webRtcLambda,
      payload: TaskInput.fromObject({
        customerAccountId: JsonPath.stringAt(PARTITION_KEY_PATH),
        sessionId: JsonPath.stringAt(SORT_KEY_PATH)
      }),
      resultPath: RESULT_PATH
    });

    webRtcSessionState.next(updateLivestreamSessionStatusState);
    webRtcSessionState.addCatch(failLivestreamSessionStatusState, {
      resultPath: RESULT_PATH_ERROR
    });

    const livestreamStateMachine = new StateMachine(this, 'LivestreamSessionStateMachine', {
      definition: webRtcSessionState
    });
    this.stateMachine = livestreamStateMachine;
  }

  postWorkflowCreationCallback() {
    this.dynamoDbStatement.addResources(this.workflow.table.tableArn);
    const encryptionKey = this.workflow.table.encryptionKey;
    if (encryptionKey !== undefined) {
      this.kmsStatement.addResources(encryptionKey.keyArn);
    }
  }

  finalizeSetup() {
    super.finalizeSetup();
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
    new ModelSchema(scope, 'ModelSchema', props),
    new PlaybackSession(scope, 'PlaybackSession', props),
    new LiveStream(scope, 'Livestream', props),
    new VideoExportWorkflow(scope, 'VideoExport', props)
  ];
}