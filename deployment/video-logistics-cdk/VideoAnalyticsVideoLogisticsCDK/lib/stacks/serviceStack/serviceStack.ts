import { Duration, Fn, Stack, StackProps } from "aws-cdk-lib";
import { MethodLoggingLevel, SpecRestApi } from 'aws-cdk-lib/aws-apigateway';
import { CfnFunction } from "aws-cdk-lib/aws-cloudfront";
import { 
  ArnPrincipal,
  CfnRole,
  Effect,
  PolicyDocument,
  PolicyStatement,
  Role,
  ServicePrincipal
} from "aws-cdk-lib/aws-iam";
import { CfnAlias, Key } from "aws-cdk-lib/aws-kms";
import { Function, Runtime, Code, CfnPermission } from "aws-cdk-lib/aws-lambda";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import { Asset } from 'aws-cdk-lib/aws-s3-assets'
import { Construct } from "constructs";
import { CfnTopicRule } from "aws-cdk-lib/aws-iot";
import { Queue, QueueEncryption } from "aws-cdk-lib/aws-sqs";
import { AWSRegion, createApiGateway, createLambdaRole, DEVICE_MANAGEMENT_API_NAME, VIDEO_LOGISTICS_API_NAME } from "video_analytics_common_construct";
import { TIMELINE_BUCKET_NAME, VIDEO_TIMELINE_TABLE_NAME, RAW_VIDEO_TIMELINE_TABLE_NAME } from "../const";

import {
    VL_ACTIVITY_JAVA_PATH_PREFIX,
    LAMBDA_ASSET_PATH,
    OPEN_API_SPEC_PATH,
  } from "../const";

export interface ServiceStackProps extends StackProps {
  region: AWSRegion;
  account: string;
}

/**
 * Service stack for generating DDB stream, DDB stream handler lambda, DLQ, lambda role
 */
export class ServiceStack extends Stack {
  public readonly restApi: SpecRestApi;

  constructor(scope: Construct, id: string, props: ServiceStackProps) {
    super(scope, id, props);

    console.log("ServiceStack constructor called");
    console.log("Props:", JSON.stringify(props));

    // roleName must match the roleName defined in Smithy model
    const apiGatewayRole = new Role(this, 'apiGatewayRole', {
      roleName: 'VideoLogisticsApiGatewayRole',
      assumedBy: new ServicePrincipal('apigateway.amazonaws.com'),
    });

    const createLivestreamSessionRole = createLambdaRole(this, "CreateLivestreamSessionRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "kinesisvideo:DescribeSignalingChannel",
          "kinesisvideo:GetSignalingChannelEndpoint",
          "kinesisvideo:GetIceServerConfig"
        ],
        resources: [
          `arn:aws:kinesisvideo:${props.region}:${props.account}:channel/*`,
        ],
      })
    ]);

    const createLivestreamSessionLambda = new Function(this, "CreateLivestreamSessionActivity", {
      runtime: Runtime.JAVA_17,
      //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.CreateLivestreamSessionActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
          ACCOUNT_ID: this.account
      },
      role: createLivestreamSessionRole,
      logGroup: new LogGroup(this, "CreateLivestreamSessionActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/CreateLivestreamSessionActivity",
      }),
    });

    const createPlaybackSessionRole = createLambdaRole(this, "CreatePlaybackSessionRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "kinesisvideo:GetDataEndpoint",
          "kinesisvideo:GetHLSStreamingSessionURL"
        ],
        resources: [
          `arn:aws:kinesisvideo:${props.region}:${props.account}:stream/*`,
        ],
      })
    ]);

    const createPlaybackSessionLambda = new Function(this, "CreatePlaybackSessionActivity", {
      runtime: Runtime.JAVA_17,
      //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.CreatePlaybackSessionActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
          ACCOUNT_ID: this.account
      },
      role: createPlaybackSessionRole,
      logGroup: new LogGroup(this, "CreatePlaybackSessionActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/CreatePlaybackSessionActivity",
      }),
    });

    const createSnapshotUploadPathRole = createLambdaRole(this, "CreateSnapshotUploadPathRole", [
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
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          's3:PutObject',
        ],
        resources: [
          `arn:aws:s3:::video-analytics-image-upload-bucket-${this.account}-${this.region}/*`
        ]
      })
    ]);

    const createSnapshotUploadPathLambda = new Function(this, "CreateSnapshotUploadPathActivity", {
      runtime: Runtime.JAVA_17,
      //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.CreateSnapshotUploadPathActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
          ACCOUNT_ID: this.account,
          DEVICE_MANAGEMENT_API_NAME: DEVICE_MANAGEMENT_API_NAME
      },
      role: createSnapshotUploadPathRole,
      logGroup: new LogGroup(this, "CreateSnapshotUploadPathActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/CreateSnapshotUploadPathActivity",
      }),
    });

    // Snapshot Resources
    const snapIoTRuleDlqRole = new CfnRole(this, "SnapIoTRuleDLQRole", {
      assumeRolePolicyDocument: {
        Version: "2012-10-17",
        Statement: [
          {
            Action: ["sts:AssumeRole"],
            Effect: "Allow",
            Principal: {
              Service: ["iot.amazonaws.com"],
            },
          },
        ],
      },
      policies: [
        {
          policyName: "SnapIoTRuleDLQPolicy",
          policyDocument: {
            Version: "2012-10-17",
            Statement: [
              {
                Sid: "SnapIoTRuleDLQAccess",
                Effect: "Allow",
                Action: ["sqs:SendMessage"],
                Resource: "*",
              },
            ],
          },
        },
      ],
    });

    const snapLambdaDlqRole = new CfnRole(this, "SnapLambdaDLQRole", {
      assumeRolePolicyDocument: {
        Version: "2012-10-17",
        Statement: [
          {
            Action: ["sts:AssumeRole"],
            Effect: "Allow",
            Principal: {
              Service: ["iot.amazonaws.com"],
            },
          },
        ],
      },
      policies: [
        {
          policyName: "SnapLambdaDLQPolicy",
          policyDocument: {
            Version: "2012-10-17",
            Statement: [
              {
                Sid: "SnapLambdaDLQAccess",
                Effect: "Allow",
                Action: ["sqs:SendMessage"],
                Resource: "*",
              },
            ],
          },
        },
      ],
    });

    if (snapIoTRuleDlqRole == null) {
      throw new Error(
        `A combination of conditions caused 'snapIoTRuleDlqRole' to be undefined. Fixit.`
      );
    }
    if (snapLambdaDlqRole == null) {
      throw new Error(
        `A combination of conditions caused 'snapLambdaDlqRole' to be undefined. Fixit.`
      );
    }

    const snapDlqKmsKey = new Key(this, "SnapDLQKmsKey", {
      description: "Kms key for snapshot DLQ.",
      enableKeyRotation: true,
      policy: new PolicyDocument({
        statements: [
          new PolicyStatement({
            effect: Effect.ALLOW,
            principals: [
              new ArnPrincipal(
                `arn:${this.partition}:iam::${this.account}:root`
              ),
            ],
            actions: ["kms:*"],
            resources: ["*"],
          }),
          new PolicyStatement({
            effect: Effect.ALLOW,
            principals: [
              new ArnPrincipal(snapLambdaDlqRole.attrArn),
              new ArnPrincipal(snapIoTRuleDlqRole.attrArn),
              new ArnPrincipal(createSnapshotUploadPathLambda.role?.roleArn!),
            ],
            actions: [
              "kms:Generate*",
              "kms:DescribeKey",
              "kms:Encrypt",
              "kms:ReEncrypt*",
              "kms:Decrypt",
            ],
            resources: ["*"],
          }),
        ],
      }),
    });

    if (snapDlqKmsKey == null) {
      throw new Error(
        `A combination of conditions caused 'snapDlqKmsKey' to be undefined. Fixit.`
      );
    }
    new CfnAlias(this, "SnapDLQKmsKeyAlias", {
      aliasName: "alias/SnapDLQKmsKey",
      targetKeyId: snapDlqKmsKey.keyId,
    });

    const snapIoTRuleDlq = new Queue(this, "SnapIoTRuleDLQ", {
      queueName: "SnapIoTRuleDLQ",
      enforceSSL: true,
      encryption: QueueEncryption.KMS,
      encryptionMasterKey: snapDlqKmsKey,
    });

    const snapLambdaDlq = new Queue(this, "SnapLambdaDLQ", {
      queueName: "SnapLambdaDLQ",
      enforceSSL: true,
      encryption: QueueEncryption.KMS,
      encryptionMasterKey: snapDlqKmsKey,
    });

    if (snapLambdaDlq == null) {
      throw new Error(
        `A combination of conditions caused 'snapLambdaDlq' to be undefined. Fixit.`
      );
    }

    if (snapIoTRuleDlq == null) {
      throw new Error(
        `A combination of conditions caused 'snapIoTRuleDlq' to be undefined. Fixit.`
      );
    }

    const snapIoTToLambdaRule = new CfnTopicRule(this, "snapIoTToLambdaRule", {
      topicRulePayload: {
        description: "Forward snapshots to destination.",
        ruleDisabled: false,
        sql: "SELECT checksum as body.checksum, contentLength as body.contentLength, topic(2) as body.deviceId FROM 'videoanalytics/+/snapshot'",
        awsIotSqlVersion: "2016-03-23",
        actions: [
          {
            lambda: {
              functionArn: createSnapshotUploadPathLambda.functionArn,
            },
          },
        ],
        errorAction: {
          sqs: {
            queueUrl: snapIoTRuleDlq.queueUrl,
            roleArn: snapIoTRuleDlqRole.attrArn,
            useBase64: false,
          },
        },
      },
    });

    if (snapIoTToLambdaRule == null) {
      throw new Error(
        `A combination of conditions caused 'snapIoTToLambdaRule' to be undefined. Fixit.`
      );
    }
    new CfnPermission(this, "GetSnapshotProxyLambdaFunctionPermission", {
      action: "lambda:InvokeFunction",
      functionName: createSnapshotUploadPathLambda.functionArn,
      principal: "iot.amazonaws.com",
      sourceArn: snapIoTToLambdaRule.attrArn,
    });
    
    const getVLRegisterDeviceStatusRole = createLambdaRole(this, "getVLRegisterDeviceStatusRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "dynamodb:Scan",
          "dynamodb:GetItem", 
          "dynamodb:UpdateItem",
          "dynamodb:Query",
          "dynamodb:BatchGetItem"
        ],
        resources: [
          `arn:aws:dynamodb:${props.region}:${props.account}:table/VLRegisterDeviceJobTable`
        ],
      })
    ]);

    const getVLRegisterDeviceStatusLambda = new Function(this, "GetVLRegisterDeviceStatusActivity", {
      runtime: Runtime.JAVA_17,
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.GetVLRegisterDeviceStatusActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
          ACCOUNT_ID: this.account
      },
      role: getVLRegisterDeviceStatusRole,
      logGroup: new LogGroup(this, "GetVLRegisterDeviceStatusActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/GetVLRegisterDeviceStatusActivity",
      }),
    });

    const startVLRegisterDeviceRole = createLambdaRole(this, "StartVLRegisterDeviceRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "dynamodb:PutItem",
          "dynamodb:UpdateItem"
        ],
        resources: [
          `arn:aws:dynamodb:${props.region}:${props.account}:table/VLRegisterDeviceJobTable`
        ],
      })
    ]);

    const startVLRegisterDeviceLambda = new Function(this, "StartVLRegisterDeviceActivity", {
      runtime: Runtime.JAVA_17,
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.StartVLRegisterDeviceActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
          ACCOUNT_ID: this.account
      },
      role: startVLRegisterDeviceRole,
      logGroup: new LogGroup(this, "StartVLRegisterDeviceActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/StartVLRegisterDeviceActivity",
      }),
    });

    apiGatewayRole.addToPolicy(new PolicyStatement({
      resources: ['*'],
      actions: ['lambda:InvokeFunction']
    }));

    const importMediaObjectRole = createLambdaRole(this, "ImportMediaObjectRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: ['kinesis:PutRecord'],
        resources: ['*']
      })
    ]);

    const importMediaObjectLambda = new Function(this, "ImportMediaObjectActivity", {
      runtime: Runtime.JAVA_17,
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.ImportMediaObjectActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
          ACCOUNT_ID: this.account
      },
      role: importMediaObjectRole,
      logGroup: new LogGroup(this, "ImportMediaObjectActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/ImportMediaObjectActivity",
      }),
    });

    apiGatewayRole.addToPolicy(new PolicyStatement({
      resources: ['*'],
      actions: ['lambda:InvokeFunction']
    }));

    const videoTimelineBaseRole = [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: ['s3:GetObject', 's3:PutObject', 's3:DeleteObject', 's3:List*'],
        resources: [
          `arn:aws:s3:::${TIMELINE_BUCKET_NAME}-${props.region}-${props.account}`,
          `arn:aws:s3:::${TIMELINE_BUCKET_NAME}-${props.region}-${props.account}/*`
        ]
      }),
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "kms:GenerateDataKey",
          "kms:Decrypt"
        ],
        resources: ["*"]  
      }),
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "dynamodb:Scan",
          "dynamodb:GetItem", 
          "dynamodb:UpdateItem",
          "dynamodb:Query",
          "dynamodb:BatchGetItem",
          "dynamodb:PutItem",
          "dynamodb:DescribeTable"
        ],
        resources: [
          `arn:aws:dynamodb:${props.region}:${props.account}:table/${VIDEO_TIMELINE_TABLE_NAME}`,
          `arn:aws:dynamodb:${props.region}:${props.account}:table/${RAW_VIDEO_TIMELINE_TABLE_NAME}`,
          `arn:aws:dynamodb:${props.region}:${props.account}:table/${VIDEO_TIMELINE_TABLE_NAME}/*`,
          `arn:aws:dynamodb:${props.region}:${props.account}:table/${RAW_VIDEO_TIMELINE_TABLE_NAME}/*`
        ],
      })
    ];

    const listDetailedVideoTimelineRole = createLambdaRole(this, "ListDetailedVideoTimelineRole", videoTimelineBaseRole);
    const listVideoTimelinesRole = createLambdaRole(this, "ListVideoTimelinesRole", videoTimelineBaseRole);
    const putVideoTimelineRole = createLambdaRole(this, "PutVideoTimelineRole", videoTimelineBaseRole);

    const listDetailedVideoTimelineLambda = new Function(this, "ListDetailedVideoTimelineActivity", {
      runtime: Runtime.JAVA_17,
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.ListDetailedVideoTimelineActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
          AWS_ACCOUNT_ID: this.account
      },
      role: listDetailedVideoTimelineRole,
      logGroup: new LogGroup(this, "ListDetailedVideoTimelineActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/ListDetailedVideoTimelineActivity",
      }),
    });

    const listVideoTimelinesLambda = new Function(this, "ListVideoTimelinesActivity", {
      runtime: Runtime.JAVA_17,
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.ListVideoTimelinesActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
        AWS_ACCOUNT_ID: this.account
      },
      role: listVideoTimelinesRole,
      logGroup: new LogGroup(this, "ListVideoTimelinesActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/ListVideoTimelinesActivity",
      }),
    });

    const putVideoTimelineLambda = new Function(this, "PutVideoTimelineActivity", {
      runtime: Runtime.JAVA_17,
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.PutVideoTimelineActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
        AWS_ACCOUNT_ID: this.account
      },
      role: putVideoTimelineRole,
      logGroup: new LogGroup(this, "PutVideoTimelineActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/PutVideoTimelineActivity",
      }),
    });

    // Add the CFN logical ID overrides
    // Overriding CFN logical IDs for OpenAPI spec transformation
    // This must match the variables defined in the Smithy model
    const createLivestreamSessionCfnLambda = createLivestreamSessionLambda.node.defaultChild as CfnFunction;
    createLivestreamSessionCfnLambda.overrideLogicalId("CreateLivestreamSessionActivity");
    const createPlaybackSessionCfnLambda = createPlaybackSessionLambda.node.defaultChild as CfnFunction;
    createPlaybackSessionCfnLambda.overrideLogicalId("CreatePlaybackSessionActivity");
    const createSnapshotUploadPathCfnLambda = createSnapshotUploadPathLambda.node.defaultChild as CfnFunction;
    createSnapshotUploadPathCfnLambda.overrideLogicalId("CreateSnapshotUploadPathActivity");
    const startVLRegisterDeviceCfnLambda = startVLRegisterDeviceLambda.node.defaultChild as CfnFunction;
    startVLRegisterDeviceCfnLambda.overrideLogicalId("StartVLRegisterDeviceActivity");
    const getVLRegisterDeviceStatusCfnLambda = getVLRegisterDeviceStatusLambda.node.defaultChild as CfnFunction;
    getVLRegisterDeviceStatusCfnLambda.overrideLogicalId("GetVLRegisterDeviceStatusActivity");
    const importMediaObjectCfnLambda = importMediaObjectLambda.node.defaultChild as CfnFunction;
    importMediaObjectCfnLambda.overrideLogicalId("ImportMediaObjectActivity");
    const listDetailedVideoTimelineCfnLambda = listDetailedVideoTimelineLambda.node.defaultChild as CfnFunction;
    listDetailedVideoTimelineCfnLambda.overrideLogicalId("ListDetailedVideoTimelineActivity");
    const listVideoTimelinesCfnLambda = listVideoTimelinesLambda.node.defaultChild as CfnFunction;
    listVideoTimelinesCfnLambda.overrideLogicalId("ListVideoTimelinesActivity");
    const putVideoTimelineCfnLambda = putVideoTimelineLambda.node.defaultChild as CfnFunction;
    putVideoTimelineCfnLambda.overrideLogicalId("PutVideoTimelineActivity");

    // Upload spec to S3
    const originalSpec = new Asset(this, "openApiFile", {
      // manually added file at this location
      path: OPEN_API_SPEC_PATH
    });

    // Pulls the content back into the template. Being inline, this will now respect CF references within the file.
    const transformMap = {
      "Location": originalSpec.s3ObjectUrl,
    };

    const transformedOpenApiSpec = Fn.transform("AWS::Include", transformMap);

    this.restApi = createApiGateway(this, 
      VIDEO_LOGISTICS_API_NAME,
      transformedOpenApiSpec,
      this.account,
      this.region,
      {
        loggingLevel: MethodLoggingLevel.OFF, 
        dataTraceEnabled: false,
        tracingEnabled: true
      }
    );
  }
}
