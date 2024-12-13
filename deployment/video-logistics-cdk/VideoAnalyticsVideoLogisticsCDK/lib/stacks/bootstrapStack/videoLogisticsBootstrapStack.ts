import { Stack, StackProps, RemovalPolicy } from "aws-cdk-lib";
import { Construct } from "constructs";
import { AWSRegion } from "video_analytics_common_construct";
import {
  ArnPrincipal,
  Effect,
  PolicyDocument,
  PolicyStatement,
  CfnRole,
} from "aws-cdk-lib/aws-iam";
import { CfnAlias, Key } from "aws-cdk-lib/aws-kms";
import { BlockPublicAccess, Bucket, BucketEncryption, ObjectOwnership } from "aws-cdk-lib/aws-s3";
import { CfnTopicRule } from "aws-cdk-lib/aws-iot";
import { CfnFunction, CfnPermission } from "aws-cdk-lib/aws-lambda";
import { Queue, QueueEncryption } from "aws-cdk-lib/aws-sqs";
import { TIMELINE_BUCKET_NAME } from "../const";

export interface VideoLogisticsBootstrapStackProps extends StackProps {
  readonly region: AWSRegion;
  readonly account: string;
}

/**
 * Provisions the Video Logistics resources
 */
export class VideoLogisticsBootstrapStack extends Stack {
  constructor(
    scope: Construct,
    id: string,
    props: VideoLogisticsBootstrapStackProps
  ) {
    super(scope, id, props);

    console.log("VideoLogisticsBootstrapStack constructor called");
    console.log("Props:", JSON.stringify(props));

    new Bucket(this, "VideoAnalyticsImageUploadBucket", {
      bucketName: `video-analytics-image-upload-bucket-${this.account}-${this.region}`,
      publicReadAccess: false,
      blockPublicAccess: BlockPublicAccess.BLOCK_ALL,
      removalPolicy: RemovalPolicy.RETAIN,
      objectOwnership: ObjectOwnership.OBJECT_WRITER,
      enforceSSL: true,
      serverAccessLogsPrefix: "access-logs/",
    });

    const putVideoTimelineProxyLambdaExecutionRole = new CfnRole(
      this,
      "PutVideoTimelineProxyLambdaExecutionRole",
      {
        managedPolicyArns: [
          "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole",
        ],
        assumeRolePolicyDocument: {
          Version: "2012-10-17",
          Statement: [
            {
              Action: ["sts:AssumeRole"],
              Effect: "Allow",
              Principal: {
                Service: ["lambda.amazonaws.com"],
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

    if (putVideoTimelineProxyLambdaExecutionRole == null) {
      throw new Error(
        `A combination of conditions caused 'putVideoTimelineProxyLambdaExecutionRole' to be undefined. Fixit.`
      );
    }
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
                new ArnPrincipal(
                  putVideoTimelineProxyLambdaExecutionRole.attrArn
                ),
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

    // TODO: PutVideoTimeline Proxy Lambda Function code and environment vars
    const putVideoTimelineProxyLambdaFunction = new CfnFunction(
      this,
      "PutVideoTimelineProxyLambdaFunction",
      {
        code: {
          zipFile:
            '\nimport json\nimport boto3\nimport zipfile\nimport os\ndef lambda_handler(event, context):\n  print(\'Received timestamps:\')\n  print(event)\n\n  input_data = event.copy()\n  input_data["timestamps"] = str(event["timestamps"])\n\n  \n  }\n',
        },
        handler: "index.lambda_handler",
        role: putVideoTimelineProxyLambdaExecutionRole.attrArn,
        environment: {},
        runtime: "python3.12",
        timeout: 60,
        deadLetterConfig: {
          targetArn: videoTimelineIngestionLambdaDlq.queueArn,
        },
      }
    );

    if (putVideoTimelineProxyLambdaFunction == null) {
      throw new Error(
        `A combination of conditions caused 'putVideoTimelineProxyLambdaFunction' to be undefined. Fixit.`
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
          sql: "SELECT *, topic(2) as deviceId FROM 'videoanalytics/+/timeline'",
          awsIotSqlVersion: "2016-03-23",
          actions: [
            {
              lambda: {
                functionArn: putVideoTimelineProxyLambdaFunction.attrArn,
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

    new CfnPermission(this, "PutVideoTimelineProxyLambdaFunctionPermission", {
      action: "lambda:InvokeFunction",
      functionName: putVideoTimelineProxyLambdaFunction.attrArn,
      principal: "iot.amazonaws.com",
      sourceArn: videoTimelineIngestionIoTToLambdaRule.attrArn,
    });
  }
}
