import {
    AccountRootPrincipal,
    Effect,
    ManagedPolicy,
    PolicyStatement,
    Role,
    ServicePrincipal
  } from 'aws-cdk-lib/aws-iam';
  import type { Construct } from 'constructs';
  import type { IFunction } from 'aws-cdk-lib/aws-lambda';
  import { Code, Function, Runtime, StartingPosition } from 'aws-cdk-lib/aws-lambda';
  import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
  import { Stream, StreamEncryption, StreamMode } from 'aws-cdk-lib/aws-kinesis';
  import { Duration, Fn, StackProps, Stack } from 'aws-cdk-lib';
  import { KinesisEventSource, SqsDlq } from 'aws-cdk-lib/aws-lambda-event-sources';
  import { Queue, QueueEncryption } from 'aws-cdk-lib/aws-sqs';
  import { Key } from 'aws-cdk-lib/aws-kms';
  import { AWSRegion } from 'video_analytics_common_construct';
  import {
    LAMBDA_ASSET_PATH,
    VL_INFERENCE_JAVA_PATH_PREFIX
  } from '../const';
  
  export interface BulkInferenceStackProps extends StackProps {
    region: AWSRegion;
    opensearchEndpoint: string;
    account: string;
  }
  
  /**
   *This stack will create the kds and trigger the bulkInference Lambda and inject the
   * Inferences into the open search.
   */
  export class BulkInferenceStack extends Stack {
    region: AWSRegion;
    public readonly bulkInferenceLambda: IFunction;
  
    constructor(scope: Construct, id: string, props: BulkInferenceStackProps) {
      super(scope, id, props);
  
      const encryptionKey = new Key(this, 'BulkInferenceKey', {
        enableKeyRotation: true,
        admins: [new AccountRootPrincipal()]
        // No need to add policy as setting the Lambda event source below
        // will add the required permissions to the Lambda role
      });

      const bulkInferenceKDS = new Stream(this, 'BulkInferenceKDS', {
        streamName: 'BulkInferenceKDS',
        streamMode: StreamMode.ON_DEMAND,
        encryption: StreamEncryption.KMS,
        encryptionKey: encryptionKey,
        retentionPeriod: Duration.days(7)
      });

      const bulkInferenceLambdaRoleArn = Fn.importValue('BulkInferenceLambdaRoleArn');
      const lambdaRole = Role.fromRoleArn(this, 'ImportedBulkInferenceLambdaRole', bulkInferenceLambdaRoleArn);
  
      this.bulkInferenceLambda = new Function(this, 'BulkInferenceLambda', {
        code: Code.fromAsset(LAMBDA_ASSET_PATH),
        description: 'Lambda responsible for Put inference in Open Search ',
        runtime: Runtime.JAVA_17,
        handler: `${VL_INFERENCE_JAVA_PATH_PREFIX}.BulkInferenceLambda::handleRequest`,
        memorySize: 2048,
        role: lambdaRole,
        environment: {
          AWSRegion: props.region,
          ACCOUNT_ID: this.account,
          opensearchEndpoint: props.opensearchEndpoint
        },
        timeout: Duration.minutes(12),
        logGroup: new LogGroup(this, 'BulkInferenceLambdaLogGroup', {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: 'BulkInferenceLambdaLogGroup'
        })
      });
  
      const deadLetter = new Queue(this, 'BulkInferenceDLQ', {
        encryption: QueueEncryption.KMS,
        encryptionMasterKey: encryptionKey
      });
  
      this.bulkInferenceLambda.addEventSource(
        new KinesisEventSource(bulkInferenceKDS, {
          batchSize: 200,
          maxBatchingWindow: Duration.seconds(3),
          maxRecordAge: Duration.days(7),
          parallelizationFactor: 5,
          enabled: true,
          bisectBatchOnError: false,
          reportBatchItemFailures: true,
          onFailure: new SqsDlq(deadLetter),
          retryAttempts: 1,
          startingPosition: StartingPosition.TRIM_HORIZON
        })
      );
    }
  }