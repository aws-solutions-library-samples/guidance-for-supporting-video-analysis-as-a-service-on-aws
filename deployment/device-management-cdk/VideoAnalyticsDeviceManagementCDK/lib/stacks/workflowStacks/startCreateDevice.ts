import type { Role } from 'aws-cdk-lib/aws-iam';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Chain, Fail, JsonPath, StateMachine, Succeed, TaskInput } from 'aws-cdk-lib/aws-stepfunctions';
import { Workflow, WorkflowProps, VideoAnalyticsAsyncWorkflowResource, createLambdaRole, AWSRegionUtils } from 'video_analytics_common_construct/';
import { AWSRegion } from 'video_analytics_common_construct/lib/serviceConstructs/util';
import { Function, Runtime, Code } from 'aws-cdk-lib/aws-lambda';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { LambdaInvoke } from 'aws-cdk-lib/aws-stepfunctions-tasks';
import { Duration } from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { WorkflowStackProps } from './workflowStack';
import { PARTITION_KEY_PATH, RESULT_PATH, RESULT_PATH_ERROR, ERROR_MESSAGE_PATH, DM_WORKFLOW_JAVA_PATH_PREFIX, LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT } from '../const';
import * as iam from 'aws-cdk-lib/aws-iam';

export class StartCreateDevice extends VideoAnalyticsAsyncWorkflowResource {

    partitionKeyName = 'JobId';
    name = 'CreateDeviceTable';

    private readonly dynamoDbStatement: PolicyStatement;
    private readonly kmsStatement: PolicyStatement;
    private readonly region: AWSRegion;
    private readonly airportCode: string;
    private readonly role: Role;
    private readonly account: string;
    private dynamoDbPlaceholderArn: string;
    private kmsPlaceholderArn: string;

    constructor(scope: Construct, id: string, props: WorkflowStackProps) {
        super(scope, id);
        this.account = props.account;
        this.region = props.region;

        this.airportCode = AWSRegionUtils.getAirportCode(props.region).toLowerCase();
        /// Create placeholder ARNs
        this.dynamoDbPlaceholderArn = `arn:aws:dynamodb:${this.region}:${this.account}:table/*`;
        this.kmsPlaceholderArn = `arn:aws:kms:${this.region}:${this.account}:key/*`;
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

        this.role = createLambdaRole(this, 'StartCreateDeviceRole', [
            new PolicyStatement({
                effect: Effect.ALLOW,
                actions: [
                    // create device
                    'iot:DescribeThingGroup',
                    'iot:AddThingToThingGroup'
                ],
                resources: [`arn:aws:iot:${props.region}:${props.account}:thinggroup/*`]
            }),
            new PolicyStatement({
                effect: Effect.ALLOW,
                actions: [
                    // create device
                    'iot:DescribeCertificate',
                    'iot:AttachThingPrincipal',
                    // failure handler
                    'iot:UpdateCertificate'
                ],
                resources: [`arn:aws:iot:${props.region}:${props.account}:cert/*`]
            }),
            this.dynamoDbStatement,
            this.kmsStatement
        ]);

    }

    createStepFunction(): StateMachine {
        const createDeviceLambda = new Function(this, 'CreateDeviceLambda', {
            runtime: Runtime.JAVA_17,
            //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
            handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}createdevice.CreateDeviceHandler::handleRequest`,
            code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
            memorySize: 512,
            timeout: Duration.minutes(5),
            environment: {
                // TODO: make a note that STAGE would not be used in the relative lambdas
                ACCOUNT_ID: this.account,
                LAMBDA_ROLE_ARN: this.role.roleArn
            },
            role: this.role,
            logGroup: new LogGroup(this, 'CreateDeviceLambdaLogGroup', {
                retention: RetentionDays.TEN_YEARS,
                logGroupName: '/aws/lambda/CreateDeviceLambda'
            })
        });

        const setLoggerConfigLambda = new Function(this, 'SetLoggerConfigLambda', {
            runtime: Runtime.JAVA_17,
            //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
            handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}createdevice.SetLoggerConfigHandler::handleRequest`,
            code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
            memorySize: 512,
            timeout: Duration.minutes(5),
            environment: {
                ACCOUNT_ID: this.account,
                LAMBDA_ROLE_ARN: this.role.roleArn,
            },
            role: this.role,
            logGroup: new LogGroup(this, 'SetLoggerConfigLambdaLogGroup', {
                retention: RetentionDays.TEN_YEARS,
                logGroupName: '/aws/lambda/SetLoggerConfigLambda'
            })
        });

        const failCreateDeviceLambda = new Function(this, 'FailCreateDeviceLambda', {
            runtime: Runtime.JAVA_17,
            //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
            handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}createdevice.FailCreateDeviceHandler::handleRequest`,
            code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
            memorySize: 512,
            timeout: Duration.minutes(5),
            environment: {
                ACCOUNT_ID: this.account,
                LAMBDA_ROLE_ARN: this.role.roleArn
            },
            role: this.role,
            logGroup: new LogGroup(this, 'FailCreateDeviceLambdaLogGroup', {
                retention: RetentionDays.TEN_YEARS,
                logGroupName: '/aws/lambda/FailCreateDeviceLambda'
            })
        });

        const createDeviceState = new LambdaInvoke(this, 'CreateDevice', {
            lambdaFunction: createDeviceLambda,
            payload: TaskInput.fromObject({
                jobId: JsonPath.stringAt(PARTITION_KEY_PATH)
            }),
            resultPath: RESULT_PATH
        });
        const setLoggerConfigState = new LambdaInvoke(this, 'SetLoggerConfig', {
            lambdaFunction: setLoggerConfigLambda,
            payload: TaskInput.fromObject({
                jobId: JsonPath.stringAt(PARTITION_KEY_PATH)
            }),
            resultPath: RESULT_PATH
        });
        const failCreateDeviceState = new LambdaInvoke(this, 'FailCreateDevice', {
            lambdaFunction: failCreateDeviceLambda,
            payload: TaskInput.fromObject({
                jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
                failureReason: JsonPath.stringAt(ERROR_MESSAGE_PATH)
            }),
            resultPath: RESULT_PATH
        });

        const failState = new Fail(this, 'Fail');
        const successState = new Succeed(this, 'Successful');

        createDeviceState.addRetry({
            //TODO: Update this once the model code is updated
            errors: ['com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException'],
            interval: Duration.seconds(2),
            maxAttempts: 5,
            backoffRate: 2
        });
        setLoggerConfigState.addRetry({
            //TODO: Update this once the model code is updated
            errors: ['com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException'],
            interval: Duration.seconds(2),
            maxAttempts: 5,
            backoffRate: 2
        });
        failCreateDeviceState.addRetry({
            //TODO: Update this once the model code is updated
            errors: ['com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException'],
            interval: Duration.seconds(2),
            maxAttempts: 5,
            backoffRate: 2
        });

        createDeviceState.next(setLoggerConfigState);
        createDeviceState.addCatch(failCreateDeviceState, { resultPath: RESULT_PATH_ERROR });

        setLoggerConfigState.next(successState);
        setLoggerConfigState.addCatch(failCreateDeviceState, { resultPath: RESULT_PATH_ERROR });

        failCreateDeviceState.next(failState);

        this.stateMachine = new StateMachine(this, 'StartCreateDeviceStateMachine', {
            definition: createDeviceState
        });
        return this.stateMachine;
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
            throw new Error('Workflow or workflow table is not defined');
        }
    }

}   
