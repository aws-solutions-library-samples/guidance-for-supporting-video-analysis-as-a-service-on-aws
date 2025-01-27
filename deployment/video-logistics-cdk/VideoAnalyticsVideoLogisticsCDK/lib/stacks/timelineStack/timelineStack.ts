import { Duration, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';

import { AttributeType, BillingMode, StreamViewType, Table } from 'aws-cdk-lib/aws-dynamodb';
import { Rule, Schedule } from 'aws-cdk-lib/aws-events';
import { LambdaFunction } from 'aws-cdk-lib/aws-events-targets';
import {
    AccountRootPrincipal,
    Effect,
    ManagedPolicy,
    PolicyStatement,
    Role,
    ServicePrincipal
} from 'aws-cdk-lib/aws-iam';
import { Stream, StreamEncryption, StreamMode } from 'aws-cdk-lib/aws-kinesis';
import { Key } from 'aws-cdk-lib/aws-kms';
import { Code, Function, IFunction, Runtime, StartingPosition, Tracing } from 'aws-cdk-lib/aws-lambda';
import { DynamoEventSource, KinesisEventSource, SqsDlq } from 'aws-cdk-lib/aws-lambda-event-sources';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { BlockPublicAccess, Bucket, BucketEncryption, ObjectOwnership } from 'aws-cdk-lib/aws-s3';
import { Queue, QueueEncryption } from 'aws-cdk-lib/aws-sqs';


import type { Construct } from 'constructs';
import { AWSRegion, createTable } from 'video_analytics_common_construct';
import {
    DENSITY_UPDATE_LAMBDA_HANDLER_PATH,
    EXPORT_LAMBDA_HANDLER_PATH,
    LAMBDA_ASSET_PATH,
    LAMBDA_MANAGED_POLICY_NAME,
    LAMBDA_SERVICE_PRINCIPAL,
    RAW_VIDEO_TIMELINE_PK_NAME,
    RAW_VIDEO_TIMELINE_SORT_KEY_NAME,
    RAW_VIDEO_TIMELINE_TABLE_NAME,
    TIMELINE_BUCKET_NAME,
    TIMELINE_FORWARDER_HANDLER_PATH,
    VIDEO_TIMELINE_PK_NAME,
    VIDEO_TIMELINE_SK_NAME,
    VIDEO_TIMELINE_TABLE_NAME,
    VIDEO_TIMELINE_TTL_ATTRIBUTE_NAME
} from '../const';

export interface TimelineStackProps extends StackProps {
    region: AWSRegion;
}

/**
 * Lambda  function and step Functions are generated. And DLQ was created. And DDB triggering was added.
 */
export class TimelineStack extends Stack {
    region: AWSRegion;
    deadLetter: Queue;
    kmsPolicy: PolicyStatement;
    tablePolicy: PolicyStatement;
    encryptionKey: Key;
    lambdaRole: Role;
    private airportCode: string;
    public readonly videoDensityUpdateLambda: IFunction;
    public readonly videoTimelineExportLambda: IFunction;

    constructor(scope: Construct, id: string, props: TimelineStackProps) {
        super(scope, id, props);
        this.kmsPolicy = new PolicyStatement({
            effect: Effect.ALLOW,
            actions: ['kms:Decrypt', 'kms:Encrypt', 'kms:ReEncrypt*', 'kms:GenerateDataKey*']
        });
        this.tablePolicy = new PolicyStatement({
            effect: Effect.ALLOW,
            resources: ['*'],
            actions: [
                'dynamodb:GetRecords',
                'dynamodb:GetItem',
                'dynamodb:Query',
                'dynamodb:PutItem',
                'dynamodb:UpdateItem',
                'dynamodb:BatchWriteItem',
                'dynamodb:DeleteItem'
            ]
        });
        // RAW VIDEO TIMELINE TABLE RESOURCES
        const rawVideoTimelineTable = createTimelineTables(
            this,
            RAW_VIDEO_TIMELINE_TABLE_NAME,
            RAW_VIDEO_TIMELINE_PK_NAME,
            RAW_VIDEO_TIMELINE_SORT_KEY_NAME,
            AttributeType.NUMBER,
            VIDEO_TIMELINE_TTL_ATTRIBUTE_NAME
        );
        this.tablePolicy.addResources(rawVideoTimelineTable.tableArn);

        // VIDEO TIMELINE TABLE RESOURCES
        const videoTimelineTable = createTimelineTables(
            this,
            VIDEO_TIMELINE_TABLE_NAME,
            VIDEO_TIMELINE_PK_NAME,
            VIDEO_TIMELINE_SK_NAME,
            AttributeType.NUMBER,
            VIDEO_TIMELINE_TTL_ATTRIBUTE_NAME
        );
        this.tablePolicy.addResources(videoTimelineTable.tableArn);

        const rawEncryptionKey = rawVideoTimelineTable.encryptionKey;
        if (rawEncryptionKey !== undefined) {
            this.kmsPolicy.addResources(rawEncryptionKey.keyArn);
        }

        const encryptionKey = videoTimelineTable.encryptionKey;
        if (encryptionKey !== undefined) {
            this.kmsPolicy.addResources(encryptionKey.keyArn);
        }

        this.encryptionKey = new Key(this, 'TimelineKey', {
            enableKeyRotation: true,
            admins: [new AccountRootPrincipal()]
            // No need to add policy as setting the Lambda event source below
            // will add the required permissions to the Lambda role
        });

        this.kmsPolicy.addResources(this.encryptionKey.keyArn);
        // TimelineForwarderLAMBDA + KDS resources
        const timelineKDS = new Stream(this, 'TimelineKDS', {
            streamName: 'TimelineKDS',
            streamMode: StreamMode.ON_DEMAND,
            encryption: StreamEncryption.KMS,
            encryptionKey: this.encryptionKey,
            retentionPeriod: Duration.days(7)
        });

        const forwarderLambdaRole = new Role(this, 'TimelineForwarderLambdaRole', {
            roleName: 'TmelineForwarderLambdaRole',
            assumedBy: new ServicePrincipal('lambda.amazonaws.com'),
            description: 'Allows lambda to forward DDB requests to KDS'
        });

        forwarderLambdaRole.addManagedPolicy(
            ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')
        );

        const timelineForwarderLambda = new Function(this, 'TimelineForwarderLambda', {
            code: Code.fromAsset(LAMBDA_ASSET_PATH),
            description: 'Lambda responsible for forwarding lambda to KDS ',
            runtime: Runtime.JAVA_17,
            tracing: Tracing.ACTIVE,
            handler: TIMELINE_FORWARDER_HANDLER_PATH,
            memorySize: 2048,
            role: forwarderLambdaRole,
            environment: {
                table: RAW_VIDEO_TIMELINE_TABLE_NAME,
                airportCode: this.airportCode,
                rawPartitionKey: RAW_VIDEO_TIMELINE_PK_NAME,
                rawSortKey: RAW_VIDEO_TIMELINE_SORT_KEY_NAME
            },
            timeout: Duration.minutes(12),
            logGroup: new LogGroup(this, 'TimelineForwarderLambdaLogGroup', {
                retention: RetentionDays.TEN_YEARS,
                logGroupName: 'TimelineForwarderLambdaLogGroup'
            })
        });
        const forwarderDeadLetter = new Queue(this, 'TimelineForwarderLambdaDLQ', {
            encryption: QueueEncryption.KMS,
            encryptionMasterKey: this.encryptionKey
        });

        timelineForwarderLambda.addEventSource(
            new DynamoEventSource(rawVideoTimelineTable, {
                startingPosition: StartingPosition.LATEST,
                batchSize: 10,
                enabled: true,
                bisectBatchOnError: true,
                onFailure: new SqsDlq(forwarderDeadLetter),
                retryAttempts: 2
            })
        );

        forwarderLambdaRole.addToPolicy(this.kmsPolicy);
        // Set KDS permission for lambda
        forwarderLambdaRole.addToPolicy(
            new PolicyStatement({
                effect: Effect.ALLOW,
                actions: ['kinesis:PutRecord'],
                resources: ['*']
            })
        );
        forwarderLambdaRole.addToPolicy(this.tablePolicy);

        // VideoDensityUpdateLambda
        this.lambdaRole = new Role(this, 'VideoTimelineLambdaRole', {
            assumedBy: new ServicePrincipal(LAMBDA_SERVICE_PRINCIPAL)
        });
        this.lambdaRole.addManagedPolicy(
            ManagedPolicy.fromAwsManagedPolicyName(LAMBDA_MANAGED_POLICY_NAME)
        );
        this.videoDensityUpdateLambda = new Function(this, 'VideoDensityUpdateLambda', {
            code: Code.fromAsset(LAMBDA_ASSET_PATH),
            description: 'Lambda responsible for aggregation of video timelines',
            runtime: Runtime.JAVA_17,
            tracing: Tracing.ACTIVE,
            handler: DENSITY_UPDATE_LAMBDA_HANDLER_PATH,
            memorySize: 2048,
            role: this.lambdaRole,
            environment: {
                table: RAW_VIDEO_TIMELINE_TABLE_NAME,
                airportCode: this.airportCode,
                rawPartitionKey: RAW_VIDEO_TIMELINE_PK_NAME,
                rawSortKey: RAW_VIDEO_TIMELINE_SORT_KEY_NAME,
            },
            timeout: Duration.minutes(12),
            logGroup: new LogGroup(this, 'VideoTimelineLambdaLogGroup', {
                retention: RetentionDays.TEN_YEARS,
                logGroupName: 'VideoTimelineLambdaLogGroup'
            })
        });
        this.lambdaRole.addToPolicy(this.kmsPolicy);
        this.lambdaRole.addToPolicy(this.tablePolicy);

        const dlqEncryptionKey = new Key(this, 'TimelineStreamDlqEncryptionKey', {
            enableKeyRotation: true,
            admins: [new AccountRootPrincipal()]
        });

        this.deadLetter = new Queue(this, `${RAW_VIDEO_TIMELINE_TABLE_NAME}-StreamDLQ`, {
            encryption: QueueEncryption.KMS,
            encryptionMasterKey: dlqEncryptionKey
        });

        this.videoDensityUpdateLambda.addEventSource(
            new KinesisEventSource(timelineKDS, {
                batchSize: 200,
                maxBatchingWindow: Duration.seconds(3),
                maxRecordAge: Duration.days(7),
                parallelizationFactor: 5,
                enabled: true,
                bisectBatchOnError: false,
                reportBatchItemFailures: true,
                onFailure: new SqsDlq(this.deadLetter),
                retryAttempts: 1,
                startingPosition: StartingPosition.TRIM_HORIZON
            })
        );

        // S3ExporterScheduledLambda
        const exporterPeriodInMinutes = 1;
        const bucketName = `${TIMELINE_BUCKET_NAME}-${this.region}-${this.account}`;
        const role = getExportLambdaRole(
            this,
            rawVideoTimelineTable,
        );

        this.videoTimelineExportLambda = createExportLambda(this, role, props);

        const eventRule = new Rule(this, 'S3Exporter-XMinutesRule', {
            // https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-cron-expressions.html
            schedule: Schedule.cron({ minute: `0/${exporterPeriodInMinutes}` })
        });

        eventRule.addTarget(
            new LambdaFunction(this.videoTimelineExportLambda, {
                retryAttempts: 1,
                maxEventAge: Duration.minutes(exporterPeriodInMinutes)
            })
        );

        const bucketEncryptionKey = new Key(this, 'TimelineBucketEncryptionKey', {
            enableKeyRotation: true,
            admins: [new AccountRootPrincipal()]
        });

        // S3 bucket to store batched timeline information
        role.addToPolicy(
            new PolicyStatement({
                effect: Effect.ALLOW,
                resources: [bucketEncryptionKey.keyArn],
                actions: ['kms:Decrypt']
            })
        );

        // S3 Bucket for server access logging for timeline bucket
        const serverAccessLoggingBucket: Bucket = new Bucket(this, `${bucketName}--ServerAccessLogs`, {
            publicReadAccess: false,
            blockPublicAccess: BlockPublicAccess.BLOCK_ALL,
            removalPolicy: RemovalPolicy.RETAIN,
            objectOwnership: ObjectOwnership.OBJECT_WRITER,
            versioned: true,
            enforceSSL: true
        });

        // S3 bucket to store batched timeline information
        const bucket = new Bucket(this, 'TimelineBucket', {
            bucketName: bucketName,
            blockPublicAccess: BlockPublicAccess.BLOCK_ALL,
            removalPolicy: RemovalPolicy.RETAIN,
            enforceSSL: true,
            encryption: BucketEncryption.KMS,
            encryptionKey: bucketEncryptionKey,
            versioned: true,
            serverAccessLogsBucket: serverAccessLoggingBucket,
            lifecycleRules: [
                {
                    enabled: true,
                    expiration: Duration.days(90),
                    noncurrentVersionExpiration: Duration.days(90),
                    id: 'ExpireAfterNinetyDays'
                }
            ]
        });
    }
}

function createTimelineTables(
    stack: Stack,
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

function getExportLambdaRole(
    stack: Stack,
    table: Table,
) {
    const s3Policy = new PolicyStatement({
        effect: Effect.ALLOW,
        resources: [
            `arn:aws:s3:::${TIMELINE_BUCKET_NAME}-${stack.region}-${stack.account}`,
            `arn:aws:s3:::${TIMELINE_BUCKET_NAME}-${stack.region}-${stack.account}/*`
        ],
        actions: ['s3:GetObject', 's3:PutObject', 's3:DeleteObject', 's3:List*']
    });

    const ddbPolicy = new PolicyStatement({
        effect: Effect.ALLOW,
        resources: [table.tableArn],
        actions: ['dynamodb:GetItem', 'dynamodb:PutItem', 'dynamodb:UpdateItem']
    });

    const lambdaRole = new Role(stack, 'S3ExporterLambdaRole', {
        roleName: 'S3ExporterLambdaRole',
        assumedBy: new ServicePrincipal('lambda.amazonaws.com'),
        description: 'Allows lambda to export timeline objects in S3 to DDB'
    });

    lambdaRole.addToPolicy(s3Policy);
    if (table.encryptionKey) {
        const kmsPolicy = new PolicyStatement({
            effect: Effect.ALLOW,
            resources: [table.encryptionKey.keyArn],
            actions: ['kms:Encrypt', 'kms:Decrypt', 'kms:GenerateDataKey']
        });
        lambdaRole.addToPolicy(kmsPolicy);
    }

    lambdaRole.addToPolicy(ddbPolicy);
    lambdaRole.addManagedPolicy(
        ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')
    );

    return lambdaRole;
}

function createExportLambda(stack: Stack, role: Role, props: TimelineStackProps) {
    return new Function(stack, 'VideoTimelineS3ExportLambda', {
        code: Code.fromAsset(LAMBDA_ASSET_PATH),
        description: 'Lambda responsible for exporting timeline information from S3 to DDB',
        runtime: Runtime.JAVA_17,
        tracing: Tracing.ACTIVE,
        handler: EXPORT_LAMBDA_HANDLER_PATH,
        memorySize: 2048,
        role: role,
        environment: {
            ACCOUNT_ID: stack.account
        },
        timeout: Duration.minutes(12),
        logGroup: new LogGroup(stack, 'VideoTimelineS3ExportLambdaLogGroup', {
            retention: RetentionDays.TEN_YEARS,
            logGroupName: 'VideoTimelineS3ExportLambdaLogGroup'
        })
    });
}
