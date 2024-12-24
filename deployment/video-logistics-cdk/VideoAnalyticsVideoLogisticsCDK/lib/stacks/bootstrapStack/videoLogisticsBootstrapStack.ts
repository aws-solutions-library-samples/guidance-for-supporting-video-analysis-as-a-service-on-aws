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
  }
}
