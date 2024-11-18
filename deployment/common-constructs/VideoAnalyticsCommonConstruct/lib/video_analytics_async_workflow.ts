import { Construct } from 'constructs';
import { Stack } from 'aws-cdk-lib';
import * as path from 'path';
import {
  Table, AttributeType, BillingMode, StreamViewType, ProjectionType
} from 'aws-cdk-lib/aws-dynamodb';
import {
  AccountRootPrincipal, Role, ServicePrincipal, PolicyStatement, ManagedPolicy, Effect
} from 'aws-cdk-lib/aws-iam';
import {
  Function, IFunction, Runtime, StartingPosition, Code
} from 'aws-cdk-lib/aws-lambda';
import { Queue, QueueEncryption } from 'aws-cdk-lib/aws-sqs';
import { DynamoEventSource, SqsDlq } from 'aws-cdk-lib/aws-lambda-event-sources';
import { StateMachine } from 'aws-cdk-lib/aws-stepfunctions';
import { Key } from 'aws-cdk-lib/aws-kms';
import * as fs from 'fs';
import { Bucket } from 'aws-cdk-lib/aws-s3';

import {
  DEFAULT_LAMBDA_PACKAGE_NAME,
  DEFAULT_LAMBDA_HANDLER_DIRECTORY,
  JOB_ID,
  LAMBDA_MANAGED_POLICY_NAME,
  LAMBDA_SERVICE_PRINCIPAL
} from './serviceConstructs/const';
import { createGSI, createTable } from './serviceConstructs';



export abstract class VideoAnalyticsAsyncWorkflowResource extends Construct {
  protected name: string;
  getName(): string {
    return this.name;
  }
  public stateMachine?: StateMachine;
  // optional State Machine for initiating delete workflow
  public stateMachineForDelete?: StateMachine;
  // optional State Machine for initiating finalize workflow
  public stateMachineForFinalize?: StateMachine;
  protected partitionKeyName?: string;
  protected sortKeyName?: string;
  protected sortKeyAttributeType?: AttributeType;
  protected gsiProps?: GsiProps[];
  public workflow: Workflow;
  protected workflowLambdaProps?: WorkflowLambdaProps;

  protected constructor(scope: Construct, id: string) {
    super(scope, id);
  }

  /**
   * Creates a Step function state machine
   */
  abstract createStepFunction(): void;

  /**
   * Create a DDB table that the workflow created in {@link createStepFunction()} uses to store and manage
   * workflow state. It also sets a DDB stream for the table and connects the stream to a stream processor
   * lambda. This lambda creates workflow executions using the state machine defined in {@link createStepFunction()}.
   */
  createWorkflow(): void {
    this.workflow = new Workflow(this, `Workflow-${this.name}`, {
      tableName: this.name,
      ...(this.stateMachine && {
        stateMachineArn: this.stateMachine.stateMachineArn
      }),
      partitionKeyName: this.partitionKeyName ? this.partitionKeyName : JOB_ID,
      sortKeyName: this.sortKeyName,
      gsiPropsList: this.gsiProps,
      workflowLambdaProps: this.workflowLambdaProps,
      ...(this.stateMachineForDelete && {
        stateMachineForDeleteArn: this.stateMachineForDelete.stateMachineArn
      }),
      ...(this.stateMachineForFinalize && {
        stateMachineForFinalizeArn: this.stateMachineForFinalize.stateMachineArn
      }),
      ...(this.sortKeyAttributeType && { sortKeyAttributeType: this.sortKeyAttributeType })
    });
    this.postWorkflowCreationCallback();
  }

  /**
   * Can be used to scope down IAM roles to resources created in {@link createStepFunction()} and
   * {@link createWorkflow()}
   */
  abstract postWorkflowCreationCallback(): void;

  /**
   * Creates a table and corresponding Step function state machine
   * see README.md for more details on the workflow.
   * Can be overriden to create additional resources for the workflow.
   */
  public finalizeSetup(): void {
    this.createStepFunction();
    this.createWorkflow();
  }
}

/**
 * tableName: DDB Table name
 * stateMachineArn: StepFunction ARN which lambda will be invoking.
 *
 */
export interface WorkflowProps {
  tableName: string;
  stateMachineArn?: string;
  stateMachineForDeleteArn?: string;
  stateMachineForFinalizeArn?: string;

  partitionKeyName: string;
  sortKeyName: string | undefined;
  sortKeyAttributeType?: AttributeType | undefined;
  gsiPropsList: GsiProps[] | undefined;

  workflowLambdaProps?: WorkflowLambdaProps;
}

export interface GsiProps {
  indexName: string;
  partitionKey: string;
  sortKey: string;
  projectionType?: ProjectionType;
  nonKeyAttributes?: string[] | undefined
}

/**
 * Props used to modify which lambda function is used to process Dynamo events and trigger the workflow
 */
export interface WorkflowLambdaProps {
  lambdaPackageName: string;
  lambdaHandlerDirectory: string;
}

/**
 * Creates a DDB table for a Video Analytics job.
 * It also sets up a lambda to processes the stream from the table which in turn invokes a
 * Step function workflow to execute jobs in the stream.
 */
export class Workflow extends Construct {
  readonly lambda: IFunction;
  readonly deadLetter: Queue;
  readonly table: Table;
  readonly ddbStreamProcessorLambdaRole: Role;

  constructor(scope: Construct, id: string, props: WorkflowProps) {
    super(scope, id);

    this.ddbStreamProcessorLambdaRole = new Role(this, 'DDBStreamProcessorLambdaRole', {
      assumedBy: new ServicePrincipal(LAMBDA_SERVICE_PRINCIPAL)
    });
    this.ddbStreamProcessorLambdaRole.addManagedPolicy(
      ManagedPolicy.fromAwsManagedPolicyName(LAMBDA_MANAGED_POLICY_NAME)
    );

    this.ddbStreamProcessorLambdaRole.addToPolicy(
      new PolicyStatement({
        effect: Effect.ALLOW,
        resources: [
          ...(props.stateMachineArn ? [props.stateMachineArn] : []),
          ...(props.stateMachineForDeleteArn ? [props.stateMachineForDeleteArn] : []),
          ...(props.stateMachineForFinalizeArn ? [props.stateMachineForFinalizeArn] : []),
        ],
        actions: [
          'states:StartExecution'
        ]
      })
    );

    const environment: { [p: string]: string } = {
      ...(props.stateMachineArn && {
        STATE_MACHINE_ARN: props.stateMachineArn
      }),
      PARTITION_KEY: props.partitionKeyName,
      SORT_KEY: props.sortKeyName || '',
      ...(props.stateMachineForDeleteArn && {
        STATE_MACHINE_FOR_DELETE_ARN: props.stateMachineForDeleteArn
      }),
      ...(props.stateMachineForFinalizeArn && {
        STATE_MACHINE_FOR_FINALIZE_ARN: props.stateMachineForFinalizeArn
      })
    };

    const lambdaName = props.workflowLambdaProps?.lambdaPackageName ?? DEFAULT_LAMBDA_PACKAGE_NAME;
    const lambdaDirectory =
      props.workflowLambdaProps?.lambdaHandlerDirectory ?? DEFAULT_LAMBDA_HANDLER_DIRECTORY;

    const lambdaAssetPath = path.join(__dirname, '../../../', 'lambda-built', lambdaName);
    let code: Code;
    if (fs.existsSync(lambdaAssetPath)) {
      code = Code.fromAsset(lambdaAssetPath);
    } else {
      code = Code.fromAsset('../../../../guidance-for-video-analytics-infrastructure-on-aws/assets/lambda-built/common-construct-assets/VideoAnalyticsWorkflowHandler-1.0-beta.jar');
    }

    this.lambda = new Function(this, `${props.tableName}-StreamProcessor`, {
      code: code,
      description: 'Lambda responsible for invocation of StepFunction',
      runtime: Runtime.JAVA_17,
      handler: 'com.amazonaws.videoanalytics.workflow.lambda.TriggerStepFunctionLambda::handleRequest', // TODO: Update this to the correct handler
      role: this.ddbStreamProcessorLambdaRole,
      memorySize: 1028,
      environment: environment
    });
    const timeToLiveAttribute = undefined;

    const deploymentStack = Stack.of(this);

    this.table = createTable(
      deploymentStack,
      props.tableName,
      props.partitionKeyName,
      props.sortKeyName,
      timeToLiveAttribute,
      StreamViewType.NEW_AND_OLD_IMAGES,
      BillingMode.PAY_PER_REQUEST,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      props.sortKeyAttributeType
    );

    if (props.gsiPropsList) {
      for (const gsiProps of props.gsiPropsList) {
        createGSI(
          this.table,
          gsiProps.indexName,
          gsiProps.partitionKey,
          gsiProps.sortKey,
          BillingMode.PAY_PER_REQUEST,
          gsiProps.projectionType,
          gsiProps.nonKeyAttributes
        );
      }
    }

    const dlqEncryptionKey = new Key(this, `${props.tableName}StreamDlqEncryptionKey`, {
      enableKeyRotation: true,
      admins: [new AccountRootPrincipal()]
      // No need to add policy as setting the DLQ for the Lambda function below
      // will add required permissions to the Lambda role
    });

    this.deadLetter = new Queue(this, `${props.tableName}-StreamDLQ`, {
      encryption: QueueEncryption.KMS,
      encryptionMasterKey: dlqEncryptionKey
    });

    this.lambda.addEventSource(
      new DynamoEventSource(this.table, {
        startingPosition: StartingPosition.LATEST,
        batchSize: 10,
        enabled: true,
        bisectBatchOnError: true,
        onFailure: new SqsDlq(this.deadLetter),
        retryAttempts: 2
      })
    );
  }
}
