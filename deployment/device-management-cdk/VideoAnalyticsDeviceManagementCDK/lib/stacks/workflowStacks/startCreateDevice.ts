import { Duration } from "aws-cdk-lib";
import {
  Effect,
  PolicyDocument,
  PolicyStatement, Role, ServicePrincipal
} from "aws-cdk-lib/aws-iam";
import * as iot from 'aws-cdk-lib/aws-iot';
import { Code, Function, Runtime, Tracing } from "aws-cdk-lib/aws-lambda";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import {
  Fail,
  JsonPath,
  LogLevel,
  StateMachine,
  Succeed,
  TaskInput
} from "aws-cdk-lib/aws-stepfunctions";
import { LambdaInvoke } from "aws-cdk-lib/aws-stepfunctions-tasks";
import { Construct } from "constructs";
import { AWSRegion, VIDEO_LOGISTICS_API_NAME } from "video_analytics_common_construct";
import {
  VideoAnalyticsAsyncWorkflowResource,
  createLambdaRole,
} from "video_analytics_common_construct/";
import {
  DM_WORKFLOW_JAVA_PATH_PREFIX,
  ERROR_MESSAGE_PATH,
  LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT,
  PARTITION_KEY_PATH,
  RESULT_PATH,
  RESULT_PATH_ERROR,
} from "../const";
import { WorkflowStackProps } from "./workflowStack";

export class StartCreateDevice extends VideoAnalyticsAsyncWorkflowResource {
  partitionKeyName = "JobId";
  name = "CreateDeviceTable";

  private readonly dynamoDbStatement: PolicyStatement;
  private readonly kmsStatement: PolicyStatement;
  private readonly region: AWSRegion;
  private readonly attachKvsAccessToCertRole: Role;
  private readonly createDeviceRole: Role;
  private readonly createKvsStreamRole: Role;
  private readonly setLoggerConfigRole: Role;
  private readonly failCreateDeviceRole: Role;
  private readonly account: string;

  constructor(scope: Construct, id: string, props: WorkflowStackProps) {
    super(scope, id);
    this.account = props.account;
    this.region = props.region;

    this.dynamoDbStatement = new PolicyStatement({
      effect: Effect.ALLOW,
      actions: [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem"
      ],
      resources: ["*"], // This will be updated in postWorkflowCreationCallback
    });

    this.kmsStatement = new PolicyStatement({
      effect: Effect.ALLOW,
      actions: ["kms:Decrypt", "kms:GenerateDataKey"],
      resources: ["*"], // This will be updated in postWorkflowCreationCallback
    });

    this.attachKvsAccessToCertRole = createLambdaRole(this, "AttachKvsAccessToCertRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:AttachPolicy",
          "iot:ListThingPrincipals",
          "iot:UpdateThingShadow"
        ],
        resources: [
          `arn:aws:iot:${props.region}:${props.account}:cert/*`,
          `arn:aws:iot:${props.region}:${props.account}:policy/*`,
          `arn:aws:iot:${props.region}:${props.account}:thing/*`,
        ],
      }),
      this.dynamoDbStatement,
      this.kmsStatement,
    ]);

    this.failCreateDeviceRole = createLambdaRole(this, "FailCreateDeviceRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:UpdateCertificate"
        ],
        resources: [
          `arn:aws:iot:${props.region}:${props.account}:cert/*`
        ],
      }),
      this.dynamoDbStatement,
      this.kmsStatement,
    ]);

    this.createDeviceRole = createLambdaRole(this, "CreateDeviceRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:AddThingToThingGroup",
          "iot:AttachThingPrincipal",
          "iot:CreateThing",
          "iot:DescribeCertificate",
          "iot:DescribeThing",
          "iot:DescribeThingGroup",
          "iot:ListThingGroupsForThing",
          "iot:UpdateCertificate"
        ],
        resources: [
          `arn:aws:iot:${props.region}:${props.account}:cert/*`,
          `arn:aws:iot:${props.region}:${props.account}:thinggroup/*`,
          `arn:aws:iot:${props.region}:${props.account}:thing/*`,
        ],
      }),
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:RegisterThing"
        ],
        resources: ["*"],
      }),
      this.dynamoDbStatement,
      this.kmsStatement,
    ]);

    this.createKvsStreamRole = createLambdaRole(this, "CreateKvsStreamRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          'apigateway:GET',
          'execute-api:Invoke'
        ],
        resources: [
          `arn:aws:apigateway:${this.region}::/restapis`,
          `arn:aws:apigateway:${this.region}::/restapis/*`,
          `arn:aws:execute-api:${this.region}:${this.account}:*/*/*/*`
        ]
      }),
      this.dynamoDbStatement,
      this.kmsStatement,
    ]);

    this.setLoggerConfigRole = createLambdaRole(this, "SetLoggerConfigRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:UpdateThingShadow"
        ],
        resources: [
          `arn:aws:iot:${props.region}:${props.account}:thing/*`
        ],
      }),
      this.dynamoDbStatement,
      this.kmsStatement,
    ]);

    // Create a role specifically for IoT provisioning
    const provisioningRole = new Role(this, 'IoTProvisioningRole', {
      assumedBy: new ServicePrincipal('iot.amazonaws.com'),
      description: 'Role for IoT provisioning template',
      inlinePolicies: {
        'ProvisioningPolicy': new PolicyDocument({
          statements: [
            new PolicyStatement({
              effect: Effect.ALLOW,
              actions: [
                'iot:CreateThing',
                'iot:AddThingToThingGroup',
                'iot:CreateCertificateFromCsr',
                'iot:AttachThingPrincipal',
                'iot:AttachPolicy'
              ],
              resources: ['*']
            })
          ]
        })
      }
    });

    // Create the provisioning template using the new role
    const provisioningTemplate = new iot.CfnProvisioningTemplate(this, 'DeviceProvisioningTemplate', {
      templateName: 'VideoAnalyticsDeviceTemplate',
      description: 'Template for provisioning video analytics devices',
      enabled: true,
      provisioningRoleArn: provisioningRole.roleArn,  // Use the new role
      templateBody: JSON.stringify({
        Parameters: {
          ThingName: {
            Type: "String"
          }
        },
        Resources: {
          thing: {
            Type: "AWS::IoT::Thing",
            Properties: {
              ThingName: {
                Ref: "ThingName"
              },
              ThingGroups: [
                "SpecialGroup_EnabledState"
              ]
            }
          },
          certificate: {
            Type: "AWS::IoT::Certificate",
            Properties: {
              CertificateId: {
                Ref: "AWS::IoT::Certificate::Id"
              },
              Status: "ACTIVE"
            }
          }
        }
      })
    });
  }

  createStepFunction(): StateMachine {
    const createDeviceLambda = new Function(this, "CreateDeviceLambda", {
      runtime: Runtime.JAVA_17,
      tracing: Tracing.ACTIVE,
      handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}.createdevice.CreateDeviceHandler::handleRequest`,
      code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
        ACCOUNT_ID: this.account,
        LAMBDA_ROLE_ARN: this.createDeviceRole.roleArn,
      },
      role: this.createDeviceRole,
      logGroup: new LogGroup(this, "CreateDeviceLambdaLogGroup", {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: "/aws/lambda/CreateDeviceLambda",
      }),
    });

    // handler to create KVS Stream, FVL API used to create stream.
    const createKVSStreamLambda = new Function(this, "CreateKVSStreamLambda", {
      code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
      description: "Lambda to create KVS Stream using FVL API.",
      runtime: Runtime.JAVA_17,
      tracing: Tracing.ACTIVE,
      handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}.createdevice.CreateKVSStreamHandler::handleRequest`,
      memorySize: 512,
      role: this.createKvsStreamRole,
      environment: {
        AccountId: this.account.toString(),
        LambdaRoleArn: this.createKvsStreamRole.roleArn,
        // this would be dynamically resolved in the Lambda for API Gateway endpoint resolving
        VIDEO_LOGISTICS_API_NAME: VIDEO_LOGISTICS_API_NAME
      },
      timeout: Duration.minutes(5),
      logGroup: new LogGroup(this, "CreateKVSStreamLambdaLogGroup", {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: "CreateKVSStreamLambdaLogGroup",
      }),
    });

    // handler to attach an IoT policy which allows KVS access to vsaas cert
    const attachKvsAccessToCertLambda = new Function(
      this,
      "AttachKvsAccessToCertLambda",
      {
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description: "Lambda responsible for attaching cert to KVS Roles policy.",
        runtime: Runtime.JAVA_17,
        tracing: Tracing.ACTIVE,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}.createdevice.AttachKvsAccessToCertHandler::handleRequest`,
        memorySize: 512,
        role: this.attachKvsAccessToCertRole,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.attachKvsAccessToCertRole.roleArn,
        },
        timeout: Duration.minutes(5),
        logGroup: new LogGroup(this, "AttachKvsAccessToCertLambdaLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "AttachKvsAccessToCertLambdaLogGroup",
        }),
      }
    );

    const fvlWorkflowCheckerLambda = new Function(
      this,
      "FVLWorkflowCheckerLambda",
      {
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        description:
          "Lambda responsible for checking to make sure fvl device registration workflow completes.",
        runtime: Runtime.JAVA_17,
        tracing: Tracing.ACTIVE,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}.createdevice.VideoLogisticsWorkflowCheckerHandler::handleRequest`,
        memorySize: 512,
        // same API GW permissions as createKvsStreamLambda
        role: this.createKvsStreamRole,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.createKvsStreamRole.roleArn,
          // this would be dynamically resolved in the Lambda for API Gateway endpoint resolving
          VIDEO_LOGISTICS_API_NAME: VIDEO_LOGISTICS_API_NAME
        },
        timeout: Duration.minutes(5),
        logGroup: new LogGroup(this, "VLWorkflowCheckerLambdaLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "VLWorkflowCheckerLambdaLogGroup",
        }),
      }
    );

    const failCreateDeviceLambda = new Function(
      this,
      "FailCreateDeviceLambda",
      {
        runtime: Runtime.JAVA_17,
        tracing: Tracing.ACTIVE,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}.createdevice.FailCreateDeviceHandler::handleRequest`,
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        memorySize: 512,
        timeout: Duration.minutes(5),
        environment: {
          ACCOUNT_ID: this.account,
          LAMBDA_ROLE_ARN: this.failCreateDeviceRole.roleArn,
        },
        role: this.failCreateDeviceRole,
        logGroup: new LogGroup(this, "FailCreateDeviceLambdaLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/FailCreateDeviceLambda",
        }),
      }
    );

    const setLoggerConfigLambda = new Function(
      this, 
      "SetLoggerConfigLambda",
      {
        runtime: Runtime.JAVA_17,
        tracing: Tracing.ACTIVE,
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}.createdevice.SetLoggerConfigHandler::handleRequest`,
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        memorySize: 512,
        timeout: Duration.minutes(5),
        environment: {
          ACCOUNT_ID: this.account,
          LAMBDA_ROLE_ARN: this.setLoggerConfigRole.roleArn,
        },
        role: this.setLoggerConfigRole,
        logGroup: new LogGroup(this, "SetLoggerConfigLambdaLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/SetLoggerConfigLambda",
        }),
      }
    );

    const createDeviceState = new LambdaInvoke(this, "CreateDevice", {
      lambdaFunction: createDeviceLambda,
      payload: TaskInput.fromObject({
        jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
      }),
      resultPath: RESULT_PATH,
    });
    const setLoggerConfigState = new LambdaInvoke(this, "SetLoggerConfig", {
      lambdaFunction: setLoggerConfigLambda,
      payload: TaskInput.fromObject({
        jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
      }),
      resultPath: RESULT_PATH,
    });

    const createKVSStreamState = new LambdaInvoke(this, "CreateKVSStream", {
      lambdaFunction: createKVSStreamLambda,
      payload: TaskInput.fromObject({
        jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
      }),
      resultPath: RESULT_PATH,
    });

    const fvlWorkflowCheckerState = new LambdaInvoke(this, "FvlWorkflowChecker", {
      lambdaFunction: fvlWorkflowCheckerLambda,
      payload: TaskInput.fromObject({
        jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
      }),
      resultPath: RESULT_PATH,
    });

    const attachKvsAccessToCertState = new LambdaInvoke(this, "AttachKvsAccessToCert", {
      lambdaFunction: attachKvsAccessToCertLambda,
      payload: TaskInput.fromObject({
        jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
      }),
      resultPath: RESULT_PATH,
    });

    const failCreateDeviceState = new LambdaInvoke(this, "FailCreateDevice", {
      lambdaFunction: failCreateDeviceLambda,
      payload: TaskInput.fromObject({
        jobId: JsonPath.stringAt(PARTITION_KEY_PATH),
        failureReason: JsonPath.stringAt(ERROR_MESSAGE_PATH),
      }),
      resultPath: RESULT_PATH,
    });

    const failState = new Fail(this, "Fail");
    const successState = new Succeed(this, "Successful");

    createDeviceState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazonaws.videoanalytics.devicemanagement.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    setLoggerConfigState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazonaws.videoanalytics.devicemanagement.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });
    failCreateDeviceState.addRetry({
      //TODO: Update this once the model code is updated
      errors: [
        "com.amazonaws.videoanalytics.devicemanagement.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 5,
      backoffRate: 2,
    });

    createDeviceState.next(setLoggerConfigState);
    createDeviceState.addCatch(failCreateDeviceState, {
      resultPath: RESULT_PATH_ERROR,
    });

    setLoggerConfigState.next(createKVSStreamState);
    setLoggerConfigState.addCatch(failCreateDeviceState, {
      resultPath: RESULT_PATH_ERROR,
    });

    createKVSStreamState.next(fvlWorkflowCheckerState);
    createKVSStreamState.addCatch(failCreateDeviceState, {
      resultPath: RESULT_PATH_ERROR,
    });

    fvlWorkflowCheckerState.next(attachKvsAccessToCertState);
    // Add retry for FVL workflow checker with backoff
    fvlWorkflowCheckerState.addRetry({
      errors: [
        "com.amazonaws.videoanalytics.devicemanagement.exceptions.RetryableException",
      ],
      interval: Duration.seconds(2),
      maxAttempts: 34,
      backoffRate: 2,
    });
    fvlWorkflowCheckerState.addCatch(failCreateDeviceState, {
      resultPath: RESULT_PATH_ERROR,
    });

    attachKvsAccessToCertState.next(successState);
    attachKvsAccessToCertState.addCatch(failCreateDeviceState, {
      resultPath: RESULT_PATH_ERROR,
    });

    failCreateDeviceState.next(failState);

    this.stateMachine = new StateMachine(
      this,
      "StartCreateDeviceStateMachine",
      {
        logs: {
          destination: new LogGroup(this, "StartCreateDeviceStateMachineLogGroup", {
            retention: RetentionDays.TEN_YEARS,
            logGroupName: "StartCreateDeviceStateMachineLogGroup"
          }),
          level: LogLevel.ALL,
        },
        definition: createDeviceState,
        tracingEnabled: true
      }
    );

    return this.stateMachine;
  }

  // Link to dynamoDB, resources must be finalized here in workflows.
  postWorkflowCreationCallback() {
    if (this.workflow && this.workflow.table) {
      // Update DynamoDB permissions
      this.dynamoDbStatement.addResources(this.workflow.table.tableArn);

      // Update KMS permissions if encryption key exists
      const encryptionKey = this.workflow.table.encryptionKey;
      if (encryptionKey) {
        this.kmsStatement.addResources(encryptionKey.keyArn);
      }
    } else {
      throw new Error("Workflow or workflow table is not defined");
    }
  }
}
