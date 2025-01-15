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
import { CfnTopicRule } from "aws-cdk-lib/aws-iot";
import { CfnAlias, Key } from "aws-cdk-lib/aws-kms";
import { CfnPermission, Code, Function, Runtime, Tracing } from "aws-cdk-lib/aws-lambda";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import { Asset } from 'aws-cdk-lib/aws-s3-assets';
import { Queue, QueueEncryption } from "aws-cdk-lib/aws-sqs";
import { Construct } from "constructs";
import * as fs from 'fs';
import { AWSRegion, createApiGateway, createLambdaRole, DEVICE_MANAGEMENT_API_NAME, VIDEO_LOGISTICS_API_NAME } from "video_analytics_common_construct";
import {
  LAMBDA_ASSET_PATH,
  OPEN_API_SPEC_PATH, RAW_VIDEO_TIMELINE_TABLE_NAME, TIMELINE_BUCKET_NAME, VIDEO_TIMELINE_TABLE_NAME, VL_ACTIVITY_JAVA_PATH_PREFIX
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
      }),
      // permission to validate device exists
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          'apigateway:GET',
          'execute-api:Invoke'
        ],
        resources: [
          `arn:aws:apigateway:${this.region}::/restapis`,
          `arn:aws:apigateway:${this.region}::/restapis/*`,
          `arn:aws:execute-api:${this.region}:${this.account}:*/*/POST/get-device/*`
        ]
      })
    ]);

    const createLivestreamSessionLambda = new Function(this, "CreateLivestreamSessionActivity", {
      runtime: Runtime.JAVA_17,
      tracing: Tracing.ACTIVE,
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.CreateLivestreamSessionActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
          ACCOUNT_ID: this.account,
          DEVICE_MANAGEMENT_API_NAME: DEVICE_MANAGEMENT_API_NAME
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
      }),
      // permission to validate device exists
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          'apigateway:GET',
          'execute-api:Invoke'
        ],
        resources: [
          `arn:aws:apigateway:${this.region}::/restapis`,
          `arn:aws:apigateway:${this.region}::/restapis/*`,
          `arn:aws:execute-api:${this.region}:${this.account}:*/*/POST/get-device/*`
        ]
      })
    ]);

    const createPlaybackSessionLambda = new Function(this, "CreatePlaybackSessionActivity", {
      runtime: Runtime.JAVA_17,
      tracing: Tracing.ACTIVE,
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.CreatePlaybackSessionActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
          ACCOUNT_ID: this.account,
          DEVICE_MANAGEMENT_API_NAME: DEVICE_MANAGEMENT_API_NAME
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
          `arn:aws:execute-api:${this.region}:${this.account}:*/*/POST/update-device-shadow/*`
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
      tracing: Tracing.ACTIVE,
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
    new CfnPermission(this, "CreateSnapshotUploadPathLambdaFunctionPermission", {
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
      tracing: Tracing.ACTIVE,
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
      }),
      // permission to validate device exists
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          'apigateway:GET',
          'execute-api:Invoke'
        ],
        resources: [
          `arn:aws:apigateway:${this.region}::/restapis`,
          `arn:aws:apigateway:${this.region}::/restapis/*`,
          `arn:aws:execute-api:${this.region}:${this.account}:*/*/POST/get-device/*`
        ]
      })
    ]);

    const startVLRegisterDeviceLambda = new Function(this, "StartVLRegisterDeviceActivity", {
      runtime: Runtime.JAVA_17,
      tracing: Tracing.ACTIVE,
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.StartVLRegisterDeviceActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
          ACCOUNT_ID: this.account,
          DEVICE_MANAGEMENT_API_NAME: DEVICE_MANAGEMENT_API_NAME
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

    // Set KMS permission for lambda so kinesis:PutRecord doesn't return 400
    importMediaObjectRole.addToPolicy(
      new PolicyStatement({
        effect: Effect.ALLOW,
        resources: [
          `arn:aws:kms:${this.region}:${this.account}:key/*`
        ],
        actions: ['kms:Encrypt', 'kms:Decrypt', 'kms:ReEncrypt*', 'kms:GenerateDataKey']
      })
    );

    const importMediaObjectLambda = new Function(this, "ImportMediaObjectActivity", {
      runtime: Runtime.JAVA_17,
      tracing: Tracing.ACTIVE,
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
      tracing: Tracing.ACTIVE,
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.ListDetailedVideoTimelineActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
        ACCOUNT_ID: this.account
      },
      role: listDetailedVideoTimelineRole,
      logGroup: new LogGroup(this, "ListDetailedVideoTimelineActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/ListDetailedVideoTimelineActivity",
      }),
    });

    const listVideoTimelinesLambda = new Function(this, "ListVideoTimelinesActivity", {
      runtime: Runtime.JAVA_17,
      tracing: Tracing.ACTIVE,
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.ListVideoTimelinesActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
        ACCOUNT_ID: this.account
      },
      role: listVideoTimelinesRole,
      logGroup: new LogGroup(this, "ListVideoTimelinesActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/ListVideoTimelinesActivity",
      }),
    });

    const putVideoTimelineLambda = new Function(this, "PutVideoTimelineActivity", {
      runtime: Runtime.JAVA_17,
      tracing: Tracing.ACTIVE,
      handler: `${VL_ACTIVITY_JAVA_PATH_PREFIX}.PutVideoTimelineActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
        ACCOUNT_ID: this.account
      },
      role: putVideoTimelineRole,
      logGroup: new LogGroup(this, "PutVideoTimelineActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/PutVideoTimelineActivity",
      }),
    });
    
    // Timeline resources
    const videoTimelineIngestionIoTRuleDlqRole = new CfnRole(
      this,
      "VideoTimelineIngestionIoTRuleDLQRole",
      {
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
            policyName: "VideoTimelineIngestionIoTRuleDLQPolicy",
            policyDocument: {
              Version: "2012-10-17",
              Statement: [
                {
                  Sid: "VideoTimelineIoTRuleDLQAccess",
                  Effect: "Allow",
                  Action: ["sqs:SendMessage"],
                  Resource: "*",
                },
              ],
            },
          },
        ],
      }
    );

    const videoTimelineIngestionLambdaDlqRole = new CfnRole(
      this,
      "VideoTimelineIngestionLambdaDLQRole",
      {
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
            policyName: "VideoTimelineIngestionLambdaDLQPolicy",
            policyDocument: {
              Version: "2012-10-17",
              Statement: [
                {
                  Sid: "VideoTimelineLambdaDLQAccess",
                  Effect: "Allow",
                  Action: ["sqs:SendMessage"],
                  Resource: "*",
                },
              ],
            },
          },
        ],
      }
    );

    if (videoTimelineIngestionIoTRuleDlqRole == null) {
      throw new Error(
        `A combination of conditions caused 'videoTimelineIngestionIoTRuleDlqRole' to be undefined. Fixit.`
      );
    }
    if (videoTimelineIngestionLambdaDlqRole == null) {
      throw new Error(
        `A combination of conditions caused 'videoTimelineIngestionLambdaDlqRole' to be undefined. Fixit.`
      );
    }
    const videoTimelineIngestionDlqKmsKey = new Key(
      this,
      "VideoTimelineIngestionDLQKmsKey",
      {
        description: "Kms key for Video Timeline ingestion DLQ",
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
                new ArnPrincipal(videoTimelineIngestionLambdaDlqRole.attrArn),
                new ArnPrincipal(videoTimelineIngestionIoTRuleDlqRole.attrArn),
                new ArnPrincipal(putVideoTimelineLambda.role?.roleArn!),
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
      }
    );

    if (videoTimelineIngestionDlqKmsKey == null) {
      throw new Error(
        `A combination of conditions caused 'videoTimelineIngestionDlqKmsKey' to be undefined. Fixit.`
      );
    }
    new CfnAlias(this, "VideoTimelineIngestionDLQKmsKeyAlias", {
      aliasName: "alias/VideoTimelineIngestionDLQKmsKey",
      targetKeyId: videoTimelineIngestionDlqKmsKey.keyId,
    });

    const videoTimelineIngestionIoTRuleDlq = new Queue(
      this,
      "VideoTimelineIngestionIoTRuleDLQ",
      {
        queueName: "VideoTimelineIngestionIoTRuleDLQ",
        enforceSSL: true,
        encryption: QueueEncryption.KMS,
        encryptionMasterKey: videoTimelineIngestionDlqKmsKey,
      }
    );

    const videoTimelineIngestionLambdaDlq = new Queue(
      this,
      "VideoTimelineIngestionLambdaDLQ",
      {
        queueName: "VideoTimelineIngestionLambdaDLQ",
        enforceSSL: true,
        encryption: QueueEncryption.KMS,
        encryptionMasterKey: videoTimelineIngestionDlqKmsKey,
      }
    );

    if (videoTimelineIngestionLambdaDlq == null) {
      throw new Error(
        `A combination of conditions caused 'videoTimelineIngestionLambdaDlq' to be undefined. Fixit.`
      );
    }
    if (videoTimelineIngestionIoTRuleDlq == null) {
      throw new Error(
        `A combination of conditions caused 'videoTimelineIngestionIoTRuleDlq' to be undefined. Fixit.`
      );
    }

    const videoTimelineIngestionIoTToLambdaRule = new CfnTopicRule(
      this,
      "VideoTimelineIngestionIoTToLambdaRule",
      {
        topicRulePayload: {
          description: "Forward Video Timeline to destination.",
          ruleDisabled: false,
          sql: "SELECT location as body.location, timestamps as body.timestamps, topic(2) as body.deviceId FROM 'videoanalytics/+/timeline'",
          awsIotSqlVersion: "2016-03-23",
          actions: [
            {
              lambda: {
                functionArn: putVideoTimelineLambda.functionArn,
              },
            },
          ],
          errorAction: {
            sqs: {
              queueUrl: videoTimelineIngestionIoTRuleDlq.queueUrl,
              roleArn: videoTimelineIngestionIoTRuleDlqRole.attrArn,
              useBase64: false,
            },
          },
        },
      }
    );

    if (videoTimelineIngestionIoTToLambdaRule == null) {
      throw new Error(
        `A combination of conditions caused 'videoTimelineIngestionIoTToLambdaRule' to be undefined. Fixit.`
      );
    }

    new CfnPermission(this, "PutVideoTimelineLambdaFunctionPermission", {
      action: "lambda:InvokeFunction",
      functionName: putVideoTimelineLambda.functionArn,
      principal: "iot.amazonaws.com",
      sourceArn: videoTimelineIngestionIoTToLambdaRule.attrArn,
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

    // configure auth type for all methods (workaround since Smithy does not support x-amazon-apigateway-auth trait)
    // create-snapshot-upload-path and put-video-timeline are not invoked through API GW
    const APIS = [
      "/create-livestream-session",
      "/create-playback-session",
      "/get-vl-register-device-status/{jobId}",
      "/import-media-object",
      "/list-detailed-video-timeline",
      "/list-video-timelines",
      "/start-vl-register-device/{deviceId}"
    ]
    const data = JSON.parse(fs.readFileSync(OPEN_API_SPEC_PATH, 'utf8'));
    for (const api of APIS) {
      data["paths"][api]["post"]["x-amazon-apigateway-auth"] = {
        "type": "AWS_IAM"
      };
    }
    fs.writeFileSync(OPEN_API_SPEC_PATH, JSON.stringify(data, null, 4));

    // Upload spec to S3
    const originalSpec = new Asset(this, "openApiFile", {
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
