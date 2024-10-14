import type { TableProps } from 'aws-cdk-lib/aws-dynamodb';
import {
  StreamViewType,
  AttributeType,
  BillingMode,
  ProjectionType,
  Table,
  TableClass,
  TableEncryption
} from 'aws-cdk-lib/aws-dynamodb';
import * as regionInfo from 'aws-cdk-lib/region-info';
import { Key } from 'aws-cdk-lib/aws-kms';
import { Stack } from 'aws-cdk-lib';
const AUTO_SCALING_AT_CPU_UTILIZATION = 80;
const DEFAULT_MIN_READ_CAPACITY = 1;
const DEFAULT_MAX_READ_CAPACITY = 20;
const DEFAULT_MIN_WRITE_CAPACITY = 1;
const DEFAULT_MAX_WRITE_CAPACITY = 5;



/**
 * String representing an AWS region.
 *
 * Using the AWS SDK to ensure we always have the most up-to-date list of regions.
 */

export type AWSRegion = string & keyof typeof regionInfo.RegionInfo.regions;

/**
 * Create a DDB table with required stack, table name and partition key. Others are optional.
 */
export function createTable(
  stack: Stack,
  tableName: string,
  partitionKey: string,
  sortKey: string | undefined,
  timeToLiveAttribute: string | undefined,
  stream = StreamViewType.NEW_AND_OLD_IMAGES,
  billingMode = BillingMode.PAY_PER_REQUEST,
  minReadCapacity = DEFAULT_MIN_READ_CAPACITY,
  maxReadCapacity = DEFAULT_MAX_READ_CAPACITY,
  minWriteCapacity = DEFAULT_MIN_WRITE_CAPACITY,
  maxWriteCapacity = DEFAULT_MAX_WRITE_CAPACITY,
  partitionKeyAttributeType: AttributeType = AttributeType.STRING,
  sortKeyAttributeType: AttributeType = AttributeType.STRING
) {
  const tableProps: TableProps = {
    tableName: tableName,
    partitionKey: { name: partitionKey, type: partitionKeyAttributeType },
    stream: stream,
    billingMode: billingMode,
    deletionProtection: true,
    pointInTimeRecovery: true,
    encryption: TableEncryption.AWS_MANAGED,
    tableClass: TableClass.STANDARD
  };

  if (sortKey) {
    (tableProps as any).sortKey = { name: sortKey, type: sortKeyAttributeType };
  }

  if (timeToLiveAttribute) {
    (tableProps as any).timeToLiveAttribute = timeToLiveAttribute;
  }

  const table = new Table(stack, tableName, tableProps);

  if (billingMode === BillingMode.PROVISIONED) {
    table
      .autoScaleReadCapacity({
        minCapacity: minReadCapacity,
        maxCapacity: maxReadCapacity
      })
      .scaleOnUtilization({ targetUtilizationPercent: AUTO_SCALING_AT_CPU_UTILIZATION });

    table
      .autoScaleWriteCapacity({
        minCapacity: minWriteCapacity,
        maxCapacity: maxWriteCapacity
      })
      .scaleOnUtilization({ targetUtilizationPercent: AUTO_SCALING_AT_CPU_UTILIZATION });
  }

  return table;
}

/**
 * Create GSI with required table, index name, partition key. Others are optional.
 */
export function createGSI(
  table: Table,
  indexName: string,
  partitionKey: string,
  sortKey: string | undefined = undefined,
  billingModel = BillingMode.PAY_PER_REQUEST,
  projectionType = ProjectionType.ALL,
  nonKeyAttributes: string[] | undefined = undefined,
  minReadCapacity = DEFAULT_MIN_READ_CAPACITY,
  maxReadCapacity = DEFAULT_MAX_READ_CAPACITY,
  minWriteCapacity = DEFAULT_MIN_WRITE_CAPACITY,
  maxWriteCapacity = DEFAULT_MAX_WRITE_CAPACITY,
  partitionKeyAttributeType: AttributeType = AttributeType.STRING,
  sortKeyAttributeType: AttributeType | undefined = undefined
) {
  const gsiProps: any = {
    indexName: indexName,
    partitionKey: { name: partitionKey, type: partitionKeyAttributeType },
    projectionType: projectionType,
    nonKeyAttributes: (projectionType === ProjectionType.INCLUDE) ? nonKeyAttributes : undefined,
  };

  if (sortKey && sortKeyAttributeType) {
    gsiProps.sortKey = { name: sortKey, type: sortKeyAttributeType };
  }

  table.addGlobalSecondaryIndex(gsiProps);

  if (billingModel === BillingMode.PROVISIONED) {
    table
      .autoScaleGlobalSecondaryIndexReadCapacity(indexName, {
        minCapacity: minReadCapacity,
        maxCapacity: maxReadCapacity
      })
      .scaleOnUtilization({ targetUtilizationPercent: AUTO_SCALING_AT_CPU_UTILIZATION });

    table
      .autoScaleGlobalSecondaryIndexWriteCapacity(indexName, {
        minCapacity: minWriteCapacity,
        maxCapacity: maxWriteCapacity
      })
      .scaleOnUtilization({ targetUtilizationPercent: AUTO_SCALING_AT_CPU_UTILIZATION });
  }
}
