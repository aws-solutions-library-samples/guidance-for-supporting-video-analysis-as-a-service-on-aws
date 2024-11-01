import { Duration, Fn, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import { AttributeType, BillingMode, StreamViewType, Table } from 'aws-cdk-lib/aws-dynamodb';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';
import { 
  AWSRegion,
  createGSI,
  createTable,
  LAMBDA_MANAGED_POLICY_NAME, 
  LAMBDA_SERVICE_PRINCIPAL
} from 'video_analytics_common_construct';
import {
  EXPORT_FILE_START_TIME,
  JOB_ID,
  VIDEO_EXPORT_JOB_RESULT_TABLE_NAME,
  VIDEO_EXPORT_RESULTS_GSI_1ST_NAME,
  VIDEO_EXPORT_RESULTS_GSI_1ST_SK,
  VIDEO_EXPORT_RESULTS_GSI_2ND_NAME,
  VIDEO_EXPORT_RESULTS_GSI_2ND_SK
} from '../const';
import { Arn } from 'aws-cdk-lib';

export interface VideoExportStackProps extends StackProps {
  region: AWSRegion;
}

/**
 * Additional resources for VideoExport API setups, results DB related
 */
export class VideoExportStack extends Stack {
  region: AWSRegion;
  kmsPolicy: PolicyStatement;
  tablePolicy: PolicyStatement;
  //TODO add in UpdateStatusLambda and JobExportLambda as props
  private airportCode: string;
  constructor(scope: Construct, id: string, props: VideoExportStackProps) {
    super(scope, id, props);
    this.kmsPolicy = new PolicyStatement({
      effect: Effect.ALLOW,
      actions: ['kms:Decrypt', 'kms:Encrypt', 'kms:ReEncrypt*', 'kms:GenerateDataKey*'],
      resources: [Arn.format({ 
        service: 'kms',
        resource: 'key/*'
      }, Stack.of(this))]
    });
    this.tablePolicy = new PolicyStatement({
      effect: Effect.ALLOW,
      actions: [
        'dynamodb:GetRecords',
        'dynamodb:GetItem',
        'dynamodb:Query',
        'dynamodb:PutItem',
        'dynamodb:UpdateItem',
        'dynamodb:BatchWriteItem'
      ],
      resources: [
        // Base table ARN
        Arn.format({ 
          service: 'dynamodb',
          resource: 'table',
          resourceName: VIDEO_EXPORT_JOB_RESULT_TABLE_NAME
        }, Stack.of(this)),
        // Include GSI ARNs
        Arn.format({ 
          service: 'dynamodb',
          resource: 'table',
          resourceName: `${VIDEO_EXPORT_JOB_RESULT_TABLE_NAME}/index/*`
        }, Stack.of(this))
      ]
    });

    // VideoExport Results table
    const videoExportResultsTable = createVideoExportRelatedTable(
      this,
      VIDEO_EXPORT_JOB_RESULT_TABLE_NAME,
      JOB_ID,
      EXPORT_FILE_START_TIME
    );
    this.tablePolicy.addResources(videoExportResultsTable.tableArn);

    // GSI 1. JobIDStatus
    createGSI(
      videoExportResultsTable,
      VIDEO_EXPORT_RESULTS_GSI_1ST_NAME,
      JOB_ID,
      VIDEO_EXPORT_RESULTS_GSI_1ST_SK,
      BillingMode.PAY_PER_REQUEST,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined
    );

    // GSI 2. JobIDEndTime
    createGSI(
      videoExportResultsTable,
      VIDEO_EXPORT_RESULTS_GSI_2ND_NAME,
      JOB_ID,
      VIDEO_EXPORT_RESULTS_GSI_2ND_SK,
      BillingMode.PAY_PER_REQUEST,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined
    );

    const resultsTableEncryptKey = videoExportResultsTable.encryptionKey;
    if (resultsTableEncryptKey) {
      this.kmsPolicy.addResources(resultsTableEncryptKey.keyArn);
    }
  }
}

/**
 * Util function for adding Video Export related Table
 */
export function createVideoExportRelatedTable(
  stack: Stack,
  tableName: string,
  partitionKeyName: string,
  sortKeyName?: string
): Table {
  return createTable(
    stack,
    tableName,
    partitionKeyName,
    sortKeyName,
    undefined,
    StreamViewType.NEW_AND_OLD_IMAGES,
    BillingMode.PAY_PER_REQUEST
  );
}