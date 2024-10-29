import { AWSRegion, createTable } from "video_analytics_common_construct";
import type { Construct } from "constructs";
import * as cdk from "aws-cdk-lib";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import { Duration, StackProps } from "aws-cdk-lib";
import {
  FORWARDING_RULES_BUCKET_NAME,
  FORWARDING_RULES_PK_NAME,
  FORWARDING_RULES_SK_NAME,
  FORWARDING_RULES_SQS_QUEUE_NAME,
  FORWARDING_RULES_TABLE_NAME,
  LAMBDA_ASSET_PATH,
} from "../const";
import type { IFunction } from "aws-cdk-lib/aws-lambda";
import {
  Code,
  EventSourceMapping,
  Function,
  Runtime,
} from "aws-cdk-lib/aws-lambda";
import {
  Effect,
  ManagedPolicy,
  PolicyStatement,
  Role,
  ServicePrincipal,
} from "aws-cdk-lib/aws-iam";
import { Key } from "aws-cdk-lib/aws-kms";
import { Queue, QueueEncryption } from "aws-cdk-lib/aws-sqs";
import type { AttributeType } from "aws-cdk-lib/aws-dynamodb";
import { BillingMode, StreamViewType } from "aws-cdk-lib/aws-dynamodb";

export interface ForwardingRulesProps extends StackProps {
  region: AWSRegion;
  account: string;
}

/**
 * Stack to manage all resources needing to be created for Forwarding Rules
 */
export class ForwardingRulesStack extends cdk.Stack {
  public readonly dataForwarderLambda: IFunction;
  constructor(scope: Construct, id: string, props: ForwardingRulesProps) {
    super(scope, id, props);

    // SQS Queue for forwarding rules
    // Create a KMS key for the queue where S3 has permission to use it when sending messages
    // Ref: https://docs.aws.amazon.com/AmazonS3/latest/userguide/grant-destinations-permissions-to-s3.html
    const forwardingRulesKmsKey = new Key(
      this,
      `${FORWARDING_RULES_SQS_QUEUE_NAME}-kmsKey`,
      {
        enableKeyRotation: true,
      }
    );

    forwardingRulesKmsKey.addToResourcePolicy(
      new PolicyStatement({
        actions: ["kms:GenerateDataKey", "kms:Decrypt"],
        resources: ["*"],
        principals: [new ServicePrincipal("s3.amazonaws.com")],
      })
    );

    const deadLetterQueue = new Queue(
      this,
      `${FORWARDING_RULES_SQS_QUEUE_NAME}-StreamDLQ`,
      {
        encryption: QueueEncryption.KMS,
        encryptionMasterKey: forwardingRulesKmsKey,
      }
    );

    // Create Forwarding rules queue
    const forwardingRulesQueue = new Queue(this, "ForwardingRulesQueue", {
      queueName: FORWARDING_RULES_SQS_QUEUE_NAME,
      encryption: QueueEncryption.KMS,
      encryptionMasterKey: forwardingRulesKmsKey,
      visibilityTimeout: Duration.minutes(6),
      deadLetterQueue: {
        queue: deadLetterQueue,
        maxReceiveCount: 5,
      },
    });

    // Grant the S3 bucket permissions to write to the queue
    // Ref: https://docs.aws.amazon.com/AmazonS3/latest/userguide/grant-destinations-permissions-to-s3.html
    forwardingRulesQueue.addToResourcePolicy(
      new PolicyStatement({
        effect: Effect.ALLOW,
        principals: [new ServicePrincipal("s3.amazonaws.com")],
        actions: ["SQS:SendMessage"],
        resources: [forwardingRulesQueue.queueArn],
        conditions: {
          ArnLike: {
            "aws:SourceArn": `arn:aws:s3:*:*:${FORWARDING_RULES_BUCKET_NAME}*`,
          },
        },
      })
    );

    const lambdaRole = new Role(this, "DataForwarderLambdaRole", {
      roleName: "DataForwarderLambdaRole",
      assumedBy: new ServicePrincipal("lambda.amazonaws.com"),
      description:
        "Allows lambda to read and delete messages from the SQS queue",
    });

    // Setup consumer Lambda role with SQS permissions to read and delete messages from the queue
    lambdaRole.addManagedPolicy(
      ManagedPolicy.fromAwsManagedPolicyName(
        "service-role/AWSLambdaSQSQueueExecutionRole"
      )
    );

    lambdaRole.addToPolicy(
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: ["s3:List*", "s3:Get*", "s3:DeleteObject"],
        resources: [
          `arn:aws:s3:::${FORWARDING_RULES_BUCKET_NAME}-${this.region}*`,
          `arn:aws:s3:::${FORWARDING_RULES_BUCKET_NAME}-${this.region}*/*`,
        ],
      })
    );

    // Set KDS permission for lambda
    lambdaRole.addToPolicy(
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: ["kinesis:PutRecord"],
        resources: ["*"],
      })
    );

    this.dataForwarderLambda = new Function(this, "DataForwarderLambda", {
      // TODO: Update lambda asset path once code compiled jar is available
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
      description:
        "Lambda responsible for forwarding data in accordance with forwarding rules",
      runtime: Runtime.JAVA_17,
      handler:
        // TODO: Update handler to match new lambda handler path
        "com.amazon.awsvideoanalyticsvlcontrolplane.forwardingrules.DataForwarderLambda::handleRequest",
      memorySize: 2048,
      role: lambdaRole,
      environment: {
        AWSRegion: props.region,
        // TODO: Removed stage variable, Lambda would need to be updated
      },
      timeout: Duration.minutes(6),
      logGroup: new LogGroup(this, "DataForwarderLambdaLogGroup", {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: "DataForwarderLambdaLogGroup",
      }),
    });

    // Setup Lambda event source mapping to SQS queues publishing device state
    new EventSourceMapping(this, "EventSourceMapping", {
      target: this.dataForwarderLambda,
      batchSize: 10,
      eventSourceArn: forwardingRulesQueue.queueArn,
    });

    const tablePolicy = new PolicyStatement({
      effect: Effect.ALLOW,
      resources: ["*"],
      actions: [
        "dynamodb:GetRecords",
        "dynamodb:GetItem",
        "dynamodb:Query",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem",
      ],
    });

    const lambdaKmsPolicy = new PolicyStatement({
      effect: Effect.ALLOW,
      resources: ["*"],
      actions: [
        "kms:Encrypt",
        "kms:Decrypt",
        "kms:ReEncrypt*",
        "kms:GenerateDataKey",
      ],
    });

    const forwardingRulesTable = createForwardingRulesTable(
      this,
      FORWARDING_RULES_TABLE_NAME,
      FORWARDING_RULES_PK_NAME,
      FORWARDING_RULES_SK_NAME,
      undefined,
      undefined
    );

    tablePolicy.addResources(forwardingRulesTable.tableArn);
    const dfEncryptionKey = forwardingRulesTable.encryptionKey;
    if (dfEncryptionKey !== undefined) {
      lambdaKmsPolicy.addResources(dfEncryptionKey.keyArn);
    }

    lambdaRole.addToPolicy(tablePolicy);
    lambdaRole.addToPolicy(lambdaKmsPolicy);
  }
}

function createForwardingRulesTable(
  stack: cdk.Stack,
  tableName: string,
  partitionKeyName: string,
  sortKeyName: string | undefined,
  sortKeyAttributeType: AttributeType | undefined,
  timeToLiveAttribute: string | undefined
) {
  return createTable(
    stack,
    tableName,
    partitionKeyName,
    sortKeyName,
    timeToLiveAttribute,
    StreamViewType.NEW_AND_OLD_IMAGES,
    BillingMode.PAY_PER_REQUEST,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    sortKeyAttributeType
  );
}
