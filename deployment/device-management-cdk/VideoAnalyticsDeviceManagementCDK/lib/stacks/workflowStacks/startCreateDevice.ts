import { Role } from "aws-cdk-lib/aws-iam";
import { 
  ServicePrincipal, 
  ManagedPolicy, 
  PolicyDocument, 
  Policy,
  PolicyStatement,
  Effect
} from "aws-cdk-lib/aws-iam";
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
import { Function, Runtime, Code } from "aws-cdk-lib/aws-lambda";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import { LambdaInvoke } from "aws-cdk-lib/aws-stepfunctions-tasks";
import { Duration } from "aws-cdk-lib";
import { Construct } from "constructs";
import { WorkflowStackProps } from "./workflowStack";
import {
  PARTITION_KEY_PATH,
  RESULT_PATH,
  RESULT_PATH_ERROR,
  ERROR_MESSAGE_PATH,
  DM_WORKFLOW_JAVA_PATH_PREFIX,
  LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT,
} from "../const";
import * as fs from 'fs';
import * as path from 'path';
import * as iot from 'aws-cdk-lib/aws-iot';

export class StartCreateDevice extends VideoAnalyticsAsyncWorkflowResource {
  partitionKeyName = "JobId";
  name = "CreateDeviceTable";

  private readonly dynamoDbStatement: PolicyStatement;
  private readonly kmsStatement: PolicyStatement;
  private readonly region: AWSRegion;
  private readonly airportCode: string;
  private readonly role: Role;
  private readonly account: string;
  private dynamoDbPlaceholderArn: string;
  private kmsPlaceholderArn: string;
  private readonly setLoggerConfigRole: Role;
  private readonly setLoggerConfigLambda: Function;
  private readonly setLoggerConfigDynamoStatement: PolicyStatement;
  private readonly setLoggerConfigKmsStatement: PolicyStatement;

  constructor(scope: Construct, id: string, props: WorkflowStackProps) {
    super(scope, id);
    this.account = props.account;
    this.region = props.region;

    this.airportCode = AWSRegionUtils.getAirportCode(
      props.region
    ).toLowerCase();
    /// Create placeholder ARNs
    this.dynamoDbPlaceholderArn = `arn:aws:dynamodb:${this.region}:${this.account}:table/*`;
    this.kmsPlaceholderArn = `arn:aws:kms:${this.region}:${this.account}:key/*`;
    this.dynamoDbStatement = new PolicyStatement({
      effect: Effect.ALLOW,
      actions: [
        "dynamodb:GetRecords",
        "dynamodb:GetItem",
        "dynamodb:Query",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:Scan",
      ],
      resources: ["*"], // This will be updated in postWorkflowCreationCallback
    });

    this.kmsStatement = new PolicyStatement({
      effect: Effect.ALLOW,
      actions: ["kms:Decrypt", "kms:Encrypt", "kms:ReEncrypt*"],
      resources: ["*"], // This will be updated in postWorkflowCreationCallback
    });

    this.setLoggerConfigDynamoStatement = new PolicyStatement({
      effect: Effect.ALLOW,
      actions: [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem"
      ],
      resources: [this.dynamoDbPlaceholderArn]
    });

    this.setLoggerConfigKmsStatement = new PolicyStatement({
      effect: Effect.ALLOW,
      actions: [
        "kms:Decrypt",
        "kms:GenerateDataKey"
      ],
      resources: [this.kmsPlaceholderArn]
    });

    this.role = createLambdaRole(this, "StartCreateDeviceRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:AttachPolicy",
          "iot:DetachPolicy",
          "iot:ListAttachedPolicies",
          "iot:ListTargetsForPolicy"
        ],
        resources: [
          `arn:aws:iot:${props.region}:${props.account}:policy/*`,
          `arn:aws:iot:${props.region}:${props.account}:cert/*`
        ],
      }),
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:DescribeThingGroup",
          "iot:AddThingToThingGroup",
          "iot:CreateThing",
          "iot:DescribeThing",
          "iot:ListThingGroupsForThing",
          "iot:ListThingPrincipals",
        ],
        resources: [
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
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:DescribeCertificate",
          "iot:AttachThingPrincipal",
          "iot:UpdateCertificate",
        ],
        resources: [`arn:aws:iot:${props.region}:${props.account}:cert/*`],
      }),
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "kinesisvideo:CreateStream",
          "kinesisvideo:DescribeStream",
          "kinesisvideo:TagStream"
        ],
        resources: [`arn:aws:kinesisvideo:${props.region}:${props.account}:stream/*`],
      }),
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "states:DescribeExecution",
          "states:GetExecutionHistory"
        ],
        resources: [`arn:aws:states:${props.region}:${props.account}:execution:*:*`],
      }),
      this.dynamoDbStatement,
      this.kmsStatement,
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:GetThingShadow",
          "iot:UpdateThingShadow",
          "iot:DeleteThingShadow",
          "iotdata:GetThingShadow",
          "iotdata:UpdateThingShadow",
          "iotdata:DeleteThingShadow",
          "iotdata:Publish"
        ],
        resources: [
          `arn:aws:iot:${props.region}:${props.account}:thing/*`,
          `arn:aws:iot:${props.region}:${props.account}:endpoint/*`
        ],
      }),
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

    // Create the role for SetLoggerConfig
    this.setLoggerConfigRole = new Role(this, 'SetLoggerConfigRole', {
      assumedBy: new ServicePrincipal('lambda.amazonaws.com'),
      description: 'Role for SetLoggerConfig Lambda function',
      inlinePolicies: {
        'SetLoggerConfigPolicy': new PolicyDocument({
          statements: [
            // IoT Data Plane permissions
            new PolicyStatement({
              effect: Effect.ALLOW,
              actions: [
                "iotdata:GetThingShadow",
                "iotdata:UpdateThingShadow",
                "iotdata:DeleteThingShadow",
                "iotdata:Publish"
              ],
              resources: [
                `arn:aws:iot:${props.region}:${props.account}:thing/*`,
                `arn:aws:iot:${props.region}:${props.account}:topic/*`
              ]
            }),

            // IoT Control Plane permissions
            new PolicyStatement({
              effect: Effect.ALLOW,
              actions: [
                "iot:GetThingShadow",
                "iot:UpdateThingShadow",
                "iot:DeleteThingShadow",
                "iot:DescribeThing",
                "iot:DescribeThingGroup",
                "iot:ListThingGroupsForThing"
              ],
              resources: [
                `arn:aws:iot:${props.region}:${props.account}:thing/*`,
                `arn:aws:iot:${props.region}:${props.account}:thinggroup/*`
              ]
            }),

            // CloudWatch Logs permissions
            new PolicyStatement({
              effect: Effect.ALLOW,
              actions: [
                "logs:CreateLogGroup",
                "logs:CreateLogStream",
                "logs:PutLogEvents"
              ],
              resources: [
                `arn:aws:logs:${props.region}:${props.account}:log-group:/aws/lambda/SetLoggerConfigLambda:*`,
                `arn:aws:logs:${props.region}:${props.account}:log-group:/aws/lambda/SetLoggerConfigLambda:log-stream:*`
              ]
            }),

            this.setLoggerConfigDynamoStatement,
            this.setLoggerConfigKmsStatement
          ]
        })
      }
    });

    // Create the SetLoggerConfig Lambda
    this.setLoggerConfigLambda = new Function(this, "SetLoggerConfigLambda", {
      runtime: Runtime.JAVA_17,
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
    });
  }

  createStepFunction(): StateMachine {
    const createDeviceLambda = new Function(this, "CreateDeviceLambda", {
      runtime: Runtime.JAVA_17,
      //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
      handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}.createdevice.CreateDeviceHandler::handleRequest`,
      code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
        // TODO: make a note that STAGE would not be used in the relative lambdas
        ACCOUNT_ID: this.account,
        LAMBDA_ROLE_ARN: this.role.roleArn,
      },
      role: this.role,
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
      handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}.update.CreateKVSStreamHandler::handleRequest`,
      memorySize: 512,
      role: this.role,
      environment: {
        AccountId: this.account.toString(),
        LambdaRoleArn: this.role.roleArn,
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
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}.update.AttachKvsAccessToCertHandler::handleRequest`,
        memorySize: 512,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
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
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}.update.VideoLogicsticsWorkflowCheckerHandler::handleRequest`,
        memorySize: 512,
        role: this.role,
        environment: {
          AccountId: this.account.toString(),
          LambdaRoleArn: this.role.roleArn,
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
        //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
        handler: `${DM_WORKFLOW_JAVA_PATH_PREFIX}.createdevice.FailCreateDeviceHandler::handleRequest`,
        code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
        memorySize: 512,
        timeout: Duration.minutes(5),
        environment: {
          ACCOUNT_ID: this.account,
          LAMBDA_ROLE_ARN: this.role.roleArn,
        },
        role: this.role,
        logGroup: new LogGroup(this, "FailCreateDeviceLambdaLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/FailCreateDeviceLambda",
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
      lambdaFunction: this.setLoggerConfigLambda,
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
        definition: createDeviceState,
      }
    );
    return this.stateMachine;
  }

  // Link to dynamoDB, resources must be finalized here in workflows.
  postWorkflowCreationCallback() {
    if (this.workflow && this.workflow.table) {
      // Update DynamoDB permissions
      this.dynamoDbStatement.addResources(this.workflow.table.tableArn);
      this.setLoggerConfigDynamoStatement.addResources(this.workflow.table.tableArn);

      // Update KMS permissions if encryption key exists
      const encryptionKey = this.workflow.table.encryptionKey;
      if (encryptionKey) {
        this.kmsStatement.addResources(encryptionKey.keyArn);
        this.setLoggerConfigKmsStatement.addResources(encryptionKey.keyArn);
      }
    } else {
      throw new Error("Workflow or workflow table is not defined");
    }
  }
}
