import { Duration, Fn, Stack, StackProps, CfnOutput } from "aws-cdk-lib";
import { MethodLoggingLevel, SpecRestApi } from 'aws-cdk-lib/aws-apigateway';
import { CfnFunction } from "aws-cdk-lib/aws-cloudfront";
import { Effect, PolicyStatement, Role, ServicePrincipal } from "aws-cdk-lib/aws-iam";
import { Function, Runtime, Code } from "aws-cdk-lib/aws-lambda";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import { Asset } from 'aws-cdk-lib/aws-s3-assets'
import { Construct } from "constructs";
import { AWSRegion, createApiGateway, createLambdaRole, VIDEO_LOGISTICS_API_NAME } from "video_analytics_common_construct";

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
          "s3:PutObject",
          "iot:UpdateThing",
          "iot:UpdateThingShadow"
        ],
        resources: [
          `arn:aws:s3:${props.region}:${props.account}:fwd-rules/*`,
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
