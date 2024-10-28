import type { Role } from 'aws-cdk-lib/aws-iam';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import type { Construct } from 'constructs';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { Duration } from 'aws-cdk-lib';
import { LambdaInvoke } from 'aws-cdk-lib/aws-stepfunctions-tasks';
import { Fail, Succeed, JsonPath, StateMachine, TaskInput } from 'aws-cdk-lib/aws-stepfunctions';
import { Function, Code } from 'aws-cdk-lib/aws-lambda';
import {
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
import {
    VideoAnalyticsAsyncWorkflowResource,
    createLambdaRole, AWSRegionUtils,
    AWSRegion
} from 'video_analytics_common_construct/';
import { WorkflowStackProps } from './workflowStack';

/**
 *
 */
export class StartCreateNotificationRule extends VideoAnalyticsAsyncWorkflowResource {
    partitionKeyName = 'JobId';
    name = 'CreateNotificationRuleTable';


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
        this.role = createLambdaRole(this, 'StartCreateNotificationRuleRole', [
            this.dynamoDbStatement,
            this.kmsStatement
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

        const createIotRuleLambda = new Function(this, 'CreateIotRuleLambda', {
            code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
            description: 'Lambda responsible for creating an IoT Rule',
            runtime: LAMBDA_RUNTIME,
            handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}createnotifrule.CreateIotRuleHandler::handleRequest`,
            memorySize: LAMBDA_MEMORY_SIZE_KB,
            role: this.role,
            environment: {
                ACCOUNT_ID: this.account,
                LAMBDA_ROLE_ARN: this.role.roleArn,
            },
            timeout: LAMBDA_TIMEOUT_MINUTES,
            logGroup: new LogGroup(this, 'CreateIotRuleLambdaLogGroup', {
                retention: RetentionDays.TEN_YEARS,
                logGroupName: '/aws/lambda/CreateIotRuleLambda'
            })
        });

        const updateCreateNotificationRuleStatusStateLambda = new Function(
            this,
            'UpdateCreateNotificationRuleStatusLambda',
            {
                //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
                code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
                description:
                    'Lambda responsible for updating the state of the Create Notification Rule Job',
                runtime: LAMBDA_RUNTIME,
                handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}createnotifrule.UpdateCreateNotificationRuleStatusStateHandler::handleRequest`,
                memorySize: LAMBDA_MEMORY_SIZE_KB,
                role: this.role,
                environment: {
                    ACCOUNT_ID: this.account,
                    LAMBDA_ROLE_ARN: this.role.roleArn,
                },
                timeout: LAMBDA_TIMEOUT_MINUTES,
                logGroup: new LogGroup(this, 'UpdateCreateNotificationRuleStatusLambdaLogGroup', {
                    retention: RetentionDays.TEN_YEARS,
                    logGroupName: '/aws/lambda/UpdateCreateNotificationRuleStatusLambda'
                })
            }
        );

        const failCreateNotificationRuleStatusStateLambda = new Function(
            this,
            'FailCreateNotificationRuleStatusLambda',
            {
                //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
                code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
                description: 'Lambda responsible for marking as failed',
                runtime: LAMBDA_RUNTIME,
                handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}createnotifrule.FailCreateNotificationRuleStatusStateHandler::handleRequest`,
                memorySize: LAMBDA_MEMORY_SIZE_KB,
                role: this.role,
                environment: {
                    ACCOUNT_ID: this.account,
                    LAMBDA_ROLE_ARN: this.role.roleArn,
                },
                timeout: LAMBDA_TIMEOUT_MINUTES,
                logGroup: new LogGroup(this, 'FailCreateNotificationRuleStatusLambdaLogGroup', {
                    retention: RetentionDays.TEN_YEARS,
                    logGroupName: '/aws/lambda/FailCreateNotificationRuleStatusLambda'
                })
            }
        );

        /**
         * Define Lambda Invokers for step function here.
         */
        const createIotRuleState = new LambdaInvoke(this, 'CreateIotRule', {
            lambdaFunction: createIotRuleLambda,
            payload: TaskInput.fromObject({
                jobId: JsonPath.stringAt(PARTITION_KEY_PATH)
            }),
            resultPath: RESULT_PATH
        });

        const updateCreateNotificationRuleStatusState = new LambdaInvoke(
            this,
            'UpdateCreateNotificationRuleStatus',
            {
                lambdaFunction: updateCreateNotificationRuleStatusStateLambda,
                payload: TaskInput.fromObject({
                    jobId: JsonPath.stringAt(PARTITION_KEY_PATH)
                }),
                resultPath: RESULT_PATH
            }
        );

        const failCreateNotificationRuleStatusState = new LambdaInvoke(
            this,
            'FailCreateNotificationRuleStatus',
            {
                lambdaFunction: failCreateNotificationRuleStatusStateLambda,
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

        // Step 1 : Create IoT rule
        createIotRuleState.next(updateCreateNotificationRuleStatusState);
        createIotRuleState.addRetry({
            //TODO: Update this error exception once Model code is updated
            errors: ['com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException'],
            interval: Duration.seconds(2),
            maxAttempts: 5,
            backoffRate: 2
        });
        createIotRuleState.addCatch(failCreateNotificationRuleStatusState, {
            resultPath: RESULT_PATH_ERROR
        });

        // Step 2 : Update the Notification Rule Creation Job state to successful.
        // End of workflow if it is successful.
        updateCreateNotificationRuleStatusState.next(successState);
        updateCreateNotificationRuleStatusState.addRetry({
            //TODO: Update this once the model code is updated
            errors: ['com.amazon.awsvideoanalyticsdmcontrolplane.exceptions.RetryableException'],
            interval: Duration.seconds(2),
            maxAttempts: 5,
            backoffRate: 2
        });
        updateCreateNotificationRuleStatusState.addCatch(failCreateNotificationRuleStatusState, {
            resultPath: RESULT_PATH_ERROR
        });

        // Failure step, End of workflow if it fails
        failCreateNotificationRuleStatusState.next(failState);

        /**
         *  Define state machine used by this step function.  This triggers the first lambda in the workflow.
         *  This must remain at the end of this function after the workflow is set up.
         */
        const createNotificationRuleStateMachine = new StateMachine(
            this,
            'StartCreateNotificationRuleStateMachine',
            {
                definition: createIotRuleState
            }
        );
        this.stateMachine = createNotificationRuleStateMachine;
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
