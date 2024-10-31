
import type { Role } from 'aws-cdk-lib/aws-iam';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import type { Construct } from 'constructs';
import {
    AWSRegion,
    createLambdaRole,
    VideoAnalyticsAsyncWorkflowResource,
    AWSRegionUtils
} from 'video_analytics_common_construct';
import {
    EXECUTION_COUNTER,
    EXECUTION_RESULT_IN_STATUS,
    EXPORT_RESULT_PATH,
    FAILED_DYNAMIC_CAUSE,
    FAILED_FORWARD_CAUSE,
    IS_COMPLETE_CONDITION,
    JOB_ID,
    LAST_PROCESSED_TIMESTAMP,
    UPDATED_TIMESTAMP,
    VIDEO_EXPORT_ACCOUNT_JOB_ID_GSI,
    VIDEO_EXPORT_ACCOUNT_JOB_ID_GSI_PK,
    VIDEO_EXPORT_JOB_GSI_NAME,
    VIDEO_EXPORT_JOB_GSI_PK,
    VIDEO_EXPORT_JOB_GSI_SK,
    VIDEO_EXPORT_JOB_PARTITION_KEY,
    VIDEO_EXPORT_JOB_RESULT_TABLE_NAME,
    VIDEO_EXPORT_JOB_TABLE_NAME,
    VIDEO_EXPORT_SFN_STATE_MACHINE_NAME,
    WAIT_TIME_BETWEEN_LOOP,
    PARTITION_KEY_PATH,
    SORT_KEY_PATH,
    LAMBDA_ASSET_PATH
} from '../const';
import type { WorkflowStackProps } from '../workflowStack/workflowResources';
import { ProjectionType } from 'aws-cdk-lib/aws-dynamodb';
import { Code, Function, Runtime } from 'aws-cdk-lib/aws-lambda';
import { Arn, Duration, Size, Stack, CfnOutput } from 'aws-cdk-lib';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { LambdaInvoke } from 'aws-cdk-lib/aws-stepfunctions-tasks';
import {
    Choice,
    Condition,
    Fail,
    JsonPath,
    Pass,
    Result,
    StateMachine,
    Succeed,
    TaskInput,
    Wait,
    WaitTime
} from 'aws-cdk-lib/aws-stepfunctions';
import type { CfnResource } from 'aws-cdk-lib';

/**
 * A video_analytics VideoExport workflow resource
 */
export class VideoExportWorkflow extends VideoAnalyticsAsyncWorkflowResource {
    // workflow job Table
    name = VIDEO_EXPORT_JOB_TABLE_NAME;
    partitionKeyName = VIDEO_EXPORT_JOB_PARTITION_KEY;
    sortKeyName = JOB_ID;
    gsiProps = [
        {
            indexName: VIDEO_EXPORT_JOB_GSI_NAME,
            partitionKey: VIDEO_EXPORT_JOB_GSI_PK,
            sortKey: VIDEO_EXPORT_JOB_GSI_SK,
            projectionType: ProjectionType.ALL
        },
        {
            indexName: VIDEO_EXPORT_ACCOUNT_JOB_ID_GSI,
            partitionKey: VIDEO_EXPORT_ACCOUNT_JOB_ID_GSI_PK,
            sortKey: JOB_ID,
            projectionType: ProjectionType.ALL
        }
    ];
    private readonly dynamoDbStatement = new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
            'dynamodb:GetRecords',
            'dynamodb:GetItem',
            'dynamodb:Query',
            'dynamodb:PutItem',
            'dynamodb:UpdateItem'
        ],
        resources: [
            Arn.format(
                {
                    service: 'dynamodb',
                    resource: 'table',
                    resourceName: VIDEO_EXPORT_JOB_RESULT_TABLE_NAME
                },
                Stack.of(this)
            )
        ]
    });

    private kmsStatement = new PolicyStatement({
        effect: Effect.ALLOW,
        actions: ['kms:Decrypt', 'kms:Encrypt', 'kms:ReEncrypt*'],
        resources: [Arn.format({ service: 'kms', resource: 'key/*' }, Stack.of(this))]
    });

    private kvsStatement = new PolicyStatement({
        effect: Effect.ALLOW,
        actions: ['kinesisvideo:GetClip', 'kinesisvideo:GetDataEndpoint', 'kinesisvideo:ListFragments'],
        resources: [Arn.format({ service: 'kinesisvideo', resource: '*' }, Stack.of(this))]
    });

    private readonly region: AWSRegion;
    private readonly airportCode: string;
    private readonly role: Role;

    constructor(scope: Construct, id: string, props: WorkflowStackProps) {
        super(scope, id);
        this.role = createLambdaRole(this, 'StartVideoExportRole', [
            this.dynamoDbStatement,
            this.kmsStatement,
            this.kmsStatement,
            this.kvsStatement
        ]);
        this.region = props.region;
        this.airportCode = AWSRegionUtils.getAirportCode(props.region).toLowerCase();
    }

    createStepFunction(): void {
        const exportVideoAndUploadLambda = new Function(this, 'ExportVideoAndUploadLambda', {
            // TODO: Update lambda asset path once code compiled jar is available
            code: Code.fromAsset(LAMBDA_ASSET_PATH),
            description: 'Lambda to handle export Video and upload to S3',
            runtime: Runtime.JAVA_17,
            handler:
                // TODO: Update handler to match new lambda handler path
                'com.amazon.awsvideoanalyticsvlcontrolplane.videologistics.videoExport.ProcessVideoExportHandler::handleRequest',
            memorySize: 2048,
            role: this.role,
            ephemeralStorageSize: Size.gibibytes(10),
            timeout: Duration.minutes(15),
            logGroup: new LogGroup(this, 'ExportVideoAndUploadLambdaLogGroup', {
                retention: RetentionDays.TEN_YEARS,
                logGroupName: 'ExportVideoAndUploadLambdaLogGroup'
            })
        });

        const updateJobStatusLambda = new Function(this, 'UpdateJobStatusLambda', {
            // TODO: Update lambda asset path once code compiled jar is available
            code: Code.fromAsset(LAMBDA_ASSET_PATH),
            description: 'Lambda to handle updating the status of the video export job',
            runtime: Runtime.JAVA_17,
            handler:
                // TODO: Update handler to match new lambda handler path
                'com.amazon.awsvideoanalyticsvlcontrolplane.videologistics.videoExport.UpdateVideoExportJobStatusHandler::handleRequest',
            memorySize: 2048,
            role: this.role,
            timeout: Duration.minutes(2),
            logGroup: new LogGroup(this, 'UpdateJobStatusLambdaLogGroup', {
                retention: RetentionDays.TEN_YEARS,
                logGroupName: 'UpdateJobStatusLambdaLogGroup'
            })
        });

        // Define Step Functions tasks
        const exportVideoAndUpload = new LambdaInvoke(this, 'ExportVideoAndUpload-Execution', {
            lambdaFunction: exportVideoAndUploadLambda,
            payload: TaskInput.fromObject({
                jobId: JsonPath.stringAt(SORT_KEY_PATH),
                customerIDDeviceID: JsonPath.stringAt(PARTITION_KEY_PATH),
                counter: JsonPath.numberAt(EXECUTION_COUNTER)
            }),
            resultPath: EXPORT_RESULT_PATH
        });

        exportVideoAndUpload.addRetry({
            errors: ['States.TaskFailed'],
            interval: Duration.seconds(5),
            maxAttempts: 3,
            backoffRate: 2
        });

        // update job status lambda for breaking the loop or failed
        const updateJobStatusBreakLoop = new LambdaInvoke(this, 'UpdateJobStatus-BreakLoop', {
            lambdaFunction: updateJobStatusLambda,
            payload: TaskInput.fromObject({
                jobId: JsonPath.stringAt(SORT_KEY_PATH),
                customerIDDeviceID: JsonPath.stringAt(PARTITION_KEY_PATH),
                counter: JsonPath.numberAt(EXECUTION_COUNTER),
                executionResult: JsonPath.stringAt(EXECUTION_RESULT_IN_STATUS)
            })
        });

        updateJobStatusBreakLoop.addCatch(
            new Fail(this, 'FailedJobUpdateStatusFailed', {
                error: 'UpdateStatusJob Failed For Uncompleted Export Job',
                causePath: FAILED_DYNAMIC_CAUSE //Dynamically resolve the cause path given error happened in UpdateJobStatus
            })
        );

        updateJobStatusBreakLoop.next(
            new Fail(this, 'FailedExecutionInLoop', {
                error: 'Execution in Loop failed',
                causePath: FAILED_FORWARD_CAUSE // Reference the previous breakLoop execution failure cause
            })
        );

        exportVideoAndUpload.addCatch(updateJobStatusBreakLoop, {
            errors: ['States.ALL'],
            resultPath: EXECUTION_RESULT_IN_STATUS
        });

        const updateJobStatusCompleted = new LambdaInvoke(this, 'UpdateJobStatus-JobCompleted', {
            lambdaFunction: updateJobStatusLambda,
            payload: TaskInput.fromObject({
                jobId: JsonPath.stringAt(SORT_KEY_PATH),
                customerIDDeviceID: JsonPath.stringAt(PARTITION_KEY_PATH),
                counter: JsonPath.numberAt(EXECUTION_COUNTER)
            })
        });

        updateJobStatusCompleted.addCatch(
            new Fail(this, 'CompleteJobUpdateStatusFailed', {
                error: 'UpdateStatusJob Failed For Completed Export Job',
                causePath: FAILED_DYNAMIC_CAUSE //Dynamically resolve the cause path given error happened in UpdateJobStatus
            })
        );

        // Pass state to initialize the counter
        const initializeCounter = new Pass(this, 'InitializeCounter', {
            parameters: {
                Counter: 0,
                // workflow kicks off with a Pass state for increment state, which doesn't include sync path context directly
                // Hence adding it as a parameter
                sortKey: JsonPath.stringAt(SORT_KEY_PATH),
                partitionKey: JsonPath.stringAt(PARTITION_KEY_PATH)
            }
        });

        // Pass state doesn't include sync path context directly, so we need to pass it in as a parameter
        const incrementCounter = new Pass(this, 'IncrementCounter', {
            parameters: {
                'Counter.$': 'States.MathAdd($.Counter, 1)', // increment the counter using MathAdd
                'lastProcessedTimestamp.$': UPDATED_TIMESTAMP,
                sortKey: JsonPath.stringAt(SORT_KEY_PATH),
                partitionKey: JsonPath.stringAt(PARTITION_KEY_PATH)
            }
        });

        const checkJobComplete = new Choice(this, 'CheckJobComplete')
            .when(
                Condition.booleanEquals(IS_COMPLETE_CONDITION, true),
                updateJobStatusCompleted.next(new Succeed(this, 'JobComplete'))
            )
            .otherwise(
                new Pass(this, 'UpdateLastProcessedTimestamp', {
                    parameters: {
                        'lastProcessedTimestamp.$': UPDATED_TIMESTAMP
                    },
                    resultPath: LAST_PROCESSED_TIMESTAMP
                }).next(
                    new Wait(this, 'WaitBeforeNextAttempt', {
                        time: WaitTime.secondsPath(WAIT_TIME_BETWEEN_LOOP)
                    })
                        .next(incrementCounter)
                        .next(exportVideoAndUpload)
                )
            );

        const stateMachineDefinition = initializeCounter
            .next(exportVideoAndUpload)
            .next(checkJobComplete);

        this.stateMachine = new StateMachine(this, 'VideoExportStateMachine', {
            stateMachineName: VIDEO_EXPORT_SFN_STATE_MACHINE_NAME,
            definition: stateMachineDefinition,
            timeout: Duration.minutes(685)
        });

        // Output the ARN of the State Machine to have workflow Stack fall through
        // TODO have this removed once the VideoExport workflows are completed and stable
        const tempOutput = new CfnOutput(
            this,
            'TemporarilyOutputRefVideoExportStartVideoExportStateMachineF56DA0E178A1F62F',
            {
                value: this.stateMachine.stateMachineArn,
                exportName: `${Stack.of(this).stackName
                    }:ExportsOutputRefVideoExportStartVideoExportStateMachineF56DA0E178A1F62F`
            }
        );
    }

    postWorkflowCreationCallback(): void {
        this.dynamoDbStatement.addResources(this.workflow.table.tableArn);
        const encryptionKey = this.workflow.table.encryptionKey;
        if (encryptionKey) {
            this.kmsStatement.addResources(encryptionKey.keyArn);
        }
    }
}