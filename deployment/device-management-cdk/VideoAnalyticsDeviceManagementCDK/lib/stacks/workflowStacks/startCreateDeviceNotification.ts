import type { Role } from 'aws-cdk-lib/aws-iam';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Fail, JsonPath, StateMachine, Succeed, TaskInput } from 'aws-cdk-lib/aws-stepfunctions';
import {
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
    ERROR_MESSAGE_PATH,
    PARTITION_KEY_PATH,
    RESULT_PATH,
    RESULT_PATH_ERROR,
    LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT,
} from '../const';

/**
 *
 */
export class StartCreateDeviceNotification extends VideoAnalyticsAsyncWorkflowResource {
    partitionKeyName = 'JobId';
    name = 'CreateDeviceNotificationTable';

    private readonly dynamoDbStatement: PolicyStatement;
    private readonly kmsStatement: PolicyStatement;

    private readonly region: AWSRegion;
    private readonly airportCode: string;
    private readonly role: Role;
    private readonly account: string;

    constructor(scope: Construct, id: string, props: WorkflowStackProps) {
        super(scope, id);

        this.region = props.region;
        this.airportCode = AWSRegionUtils.getAirportCode(props.region).toLowerCase();
        this.account = props.account;
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

        this.role = createLambdaRole(this, 'StartCreateDeviceNotificationRole', [
            this.dynamoDbStatement,
            this.kmsStatement
        ]);
    }

    createStepFunction(): void {
        const failState = new Fail(this, 'Fail');
        const successState = new Succeed(this, 'Successful');

        /*
         * Define Lambdas for Step function here.
         * Order matters here, first lambdas are defined, then lambda invokers,
         * then the state machine and other resources, then define the workflow steps.
         */

        const attachNotificationPermissionToDeviceCertLambda = new Function(
            this,
            'AttachNotificationPermissionToDeviceCertLambda',
            {
                //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
                code: Code.fromAsset(LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT),
                description: 'Lambda responsible for attaching notification permission to the device cert',
                runtime: Runtime.JAVA_17,
                //TODO: Update this if any changes are made to the lambda handler path
                handler: 'com.amazon.awsvideoanalyticsdmcontrolplane.workflow.createdevicenotif.AttachNotificationPermissionToDeviceCertHandler::handleRequest',
                memorySize: 2048,
                role: this.role,
                environment: {
                    ACCOUNT_ID: this.account,
                    LAMBDA_ROLE_ARN: this.role.roleArn,
                },
                timeout: Duration.minutes(12),
                logGroup: new LogGroup(this, 'AttachNotificationPermissionToDeviceCertLambdaLogGroup', {
                    retention: RetentionDays.TEN_YEARS,
                    logGroupName: '/aws/lambda/AttachNotificationPermissionToDeviceCertLambda'
                })
            }
        );

        const addNotificationTopicToShadowLambda = new Function(
            this,
            'AddNotificationTopicToShadowLambda',
            {
                //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
                code: Code.fromAsset(LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT),
                description: 'Lambda responsible for adding notification topic to the shadow',
                runtime: Runtime.JAVA_17,
                //TODO: Update this lambda to use the correct code path relative to the package path
                handler: 'com.amazon.awsvideoanalyticsdmcontrolplane.workflow.createdevicenotif.AddNotificationTopicToShadowHandler::handleRequest',
                memorySize: 2048,
                role: this.role,
                environment: {
                    ACCOUNT_ID: this.account,
                    LAMBDA_ROLE_ARN: this.role.roleArn,
                },
                timeout: Duration.minutes(12),
                logGroup: new LogGroup(this, 'AddNotificationTopicToShadowLambdaLogGroup', {
                    retention: RetentionDays.TEN_YEARS,
                    logGroupName: '/aws/lambda/AddNotificationTopicToShadowLambda'
                })
            }
        );

        const updateCreateDeviceNotificationStatusStateLambda = new Function(
            this,
            'UpdateCreateDeviceNotificationStatusLambda',
            {
                //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
                code: Code.fromAsset(LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT),
                description: 'Lambda responsible for updating the state of the Create Device Notification Job',
                runtime: Runtime.JAVA_17,
                //TODO: Update this lambda to use the correct code path relative to the package path
                handler: 'com.amazon.awsvideoanalyticsdmcontrolplane.workflow.createdevicenotif.UpdateCreateDeviceNotificationStatusStateHandler::handleRequest',
                memorySize: 2048,
                role: this.role,
                environment: {
                    ACCOUNT_ID: this.account,
                    LAMBDA_ROLE_ARN: this.role.roleArn,
                },
                timeout: Duration.minutes(12),
                logGroup: new LogGroup(this, 'UpdateCreateDeviceNotificationStatusLambdaLogGroup', {
                    retention: RetentionDays.TEN_YEARS,
                    logGroupName: '/aws/lambda/UpdateCreateDeviceNotificationStatusLambda'
                })
            }
        );

        const failCreateDeviceNotificationStatusStateLambda = new Function(
            this,
            'FailCreateDeviceNotificationStatusLambda',
            {
                //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
                code: Code.fromAsset(LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT),
                description: 'Lambda responsible for marking as failed',
                runtime: Runtime.JAVA_17,
                //TODO: Update this lambda to use the correct code path relative to the package path
                handler: 'com.amazon.awsvideoanalyticsdmcontrolplane.workflow.createdevicenotif.FailCreateDeviceNotificationStatusStateHandler::handleRequest',
                memorySize: 2048,
                role: this.role,
                environment: {
                    ACCOUNT_ID: this.account,
                    LAMBDA_ROLE_ARN: this.role.roleArn,
                },
                timeout: Duration.minutes(12),
                logGroup: new LogGroup(this, 'FailCreateDeviceNotificationStatusLambdaLogGroup', {
                    retention: RetentionDays.TEN_YEARS,
                    logGroupName: '/aws/lambda/FailCreateDeviceNotificationStatusLambda'
                })
            }
        );

        /**
         * Define Lambda Invokers for step function here.
         */
        const attachNotificationPermissionToDeviceCertState = new LambdaInvoke(
            this,
            'AttachNotificationPermissionToDeviceCert',
            {
                lambdaFunction: attachNotificationPermissionToDeviceCertLambda,
                payload: TaskInput.fromObject({
                    jobId: JsonPath.stringAt(PARTITION_KEY_PATH)
                }),
                resultPath: RESULT_PATH
            }
        );

        const addNotificationTopicToShadowState = new LambdaInvoke(
            this,
            'AddNotificationTopicToShadow',
            {
                lambdaFunction: addNotificationTopicToShadowLambda,
                payload: TaskInput.fromObject({
                    jobId: JsonPath.stringAt(PARTITION_KEY_PATH)
                }),
                resultPath: RESULT_PATH
            }
        );

        const updateCreateDeviceNotificationStatusState = new LambdaInvoke(
            this,
            'UpdateCreateDeviceNotificationStatus',
            {
                lambdaFunction: updateCreateDeviceNotificationStatusStateLambda,
                payload: TaskInput.fromObject({
                    jobId: JsonPath.stringAt(PARTITION_KEY_PATH)
                }),
                resultPath: RESULT_PATH
            }
        );

        const failCreateDeviceNotificationStatusState = new LambdaInvoke(
            this,
            'FailCreateDeviceNotificationStatus',
            {
                lambdaFunction: failCreateDeviceNotificationStatusStateLambda,
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

        // Step 1 : Attach notification permision to device cert
        attachNotificationPermissionToDeviceCertState.next(addNotificationTopicToShadowState);
        attachNotificationPermissionToDeviceCertState.addRetry({
            errors: ['com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException'],
            interval: Duration.seconds(2),
            maxAttempts: 5,
            backoffRate: 2
        });
        attachNotificationPermissionToDeviceCertState.addCatch(
            failCreateDeviceNotificationStatusState,
            {
                resultPath: RESULT_PATH_ERROR
            }
        );

        // Step 2 : Add notification topic to shadow
        addNotificationTopicToShadowState.next(updateCreateDeviceNotificationStatusState);
        addNotificationTopicToShadowState.addRetry({
            errors: ['com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException'],
            interval: Duration.seconds(2),
            maxAttempts: 5,
            backoffRate: 2
        });
        addNotificationTopicToShadowState.addCatch(failCreateDeviceNotificationStatusState, {
            resultPath: RESULT_PATH_ERROR
        });

        // Step 3 : Update the Notification Rule Creation Job state to successful.
        // End of workflow if it is successful.
        updateCreateDeviceNotificationStatusState.next(successState);
        updateCreateDeviceNotificationStatusState.addRetry({
            //TODO: Update this once the model code is updated
            errors: ['com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException'],
            interval: Duration.seconds(2),
            maxAttempts: 5,
            backoffRate: 2
        });
        updateCreateDeviceNotificationStatusState.addCatch(failCreateDeviceNotificationStatusState, {
            resultPath: RESULT_PATH_ERROR
        });

        // Failure step, End of workflow if it fails
        failCreateDeviceNotificationStatusState.next(failState);

        /**
         *  Define state machine used by this step function.  This triggers the first lambda in the workflow.
         *  This must remain at the end of this function after the workflow is set up.
         */
        const createDeviceNotificationStateMachine = new StateMachine(
            this,
            'StartCreateDeviceNotificationStateMachine',
            {
                definition: attachNotificationPermissionToDeviceCertState
            }
        );
        this.stateMachine = createDeviceNotificationStateMachine;
    }

    // Link to dynamoDB, resources must be finalized here in workflows.
    postWorkflowCreationCallback(): void {
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
