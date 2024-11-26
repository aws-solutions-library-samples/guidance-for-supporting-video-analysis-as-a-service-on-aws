import { Duration, Fn, Stack, StackProps } from "aws-cdk-lib";
import { SpecRestApi } from 'aws-cdk-lib/aws-apigateway';
import { CfnFunction } from "aws-cdk-lib/aws-cloudfront";
import { Effect, PolicyStatement, Role, ServicePrincipal } from "aws-cdk-lib/aws-iam";
import { Function, Runtime, Code } from "aws-cdk-lib/aws-lambda";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import { Asset } from 'aws-cdk-lib/aws-s3-assets'
import { Construct } from "constructs";
import { AWSRegion, createApiGateway, createLambdaRole } from "video_analytics_common_construct";

import {
    VL_ACTIVITY_JAVA_PATH_PREFIX,
    LAMBDA_ASSET_PATH,
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
          "s3:PutObject",
          "iot:UpdateThing",
          "iot:UpdateThingShadow"
        ],
        resources: [
          `arn:aws:s3:${props.region}:${props.account}:fathom-fwd-rules/*`,
          `arn:aws:iot:${props.region}:${props.account}:thing/*`
        ],
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
          ACCOUNT_ID: this.account
      },
      role: createSnapshotUploadPathRole,
      logGroup: new LogGroup(this, "CreateSnapshotUploadPathActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/CreateSnapshotUploadPathActivity",
      }),
    });

    apiGatewayRole.addToPolicy(new PolicyStatement({
      resources: ['*'],
      actions: ['lambda:InvokeFunction']
    }));

    // Overriding CFN logical IDs for OpenAPI spec transformation
    // This must match the variables defined in the Smithy model
    const createLivestreamSessionCfnLambda = createLivestreamSessionLambda.node.defaultChild as CfnFunction;
    createLivestreamSessionCfnLambda.overrideLogicalId("CreateLivestreamSessionActivity");
    const createPlaybackSessionCfnLambda = createPlaybackSessionLambda.node.defaultChild as CfnFunction;
    createPlaybackSessionCfnLambda.overrideLogicalId("CreatePlaybackSessionActivity");
    const createSnapshotUploadPathCfnLambda = createSnapshotUploadPathLambda.node.defaultChild as CfnFunction;
    createSnapshotUploadPathCfnLambda.overrideLogicalId("CreateSnapshotUploadPathActivity");

    // Upload spec to S3
    const originalSpec = new Asset(this, "openApiFile", {
      // manually added file at this location
      path: "./lib/openapi/VideoAnalytic.openapi.json"
    });

    // Pulls the content back into the template. Being inline, this will now respect CF references within the file.
    const transformMap = {
      "Location": originalSpec.s3ObjectUrl,
    };

    const transformedOpenApiSpec = Fn.transform("AWS::Include", transformMap);

    this.restApi = createApiGateway(this, 
      "VideoAnalyticsVideoLogisticsAPIGateway",
      transformedOpenApiSpec,
      this.account,
      this.region
    );
  }
}
