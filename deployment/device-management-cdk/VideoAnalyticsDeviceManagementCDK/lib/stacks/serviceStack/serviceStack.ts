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
    DM_ACTIVITY_JAVA_PATH_PREFIX,
    LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT,
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
      roleName: 'ApiGatewayRole',
      assumedBy: new ServicePrincipal('apigateway.amazonaws.com'),
    });

    const getDeviceRole = createLambdaRole(this, "GetDeviceRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:DescribeThing",
          "iot:ListThingGroupsForThing",
          "iot:GetThingShadow"
        ],
        resources: [
          `arn:aws:iot:${props.region}:${props.account}:thing/*`,
        ],
      }),
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:SearchIndex"
        ],
        resources: [`arn:aws:iot:${props.region}:${props.account}:index/AWS_Things`],
      })
    ]);

    const getDeviceLambda = new Function(this, "GetDeviceActivity", {
      runtime: Runtime.JAVA_17,
      //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
      handler: `${DM_ACTIVITY_JAVA_PATH_PREFIX}.GetDeviceActivity::handleRequest`,
      code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
          ACCOUNT_ID: this.account,
          LAMBDA_ROLE_ARN: getDeviceRole.roleArn,
      },
      role: getDeviceRole,
      logGroup: new LogGroup(this, "GetDeviceActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/GetDeviceActivity",
      }),
    });

    const getDeviceShadowRole = createLambdaRole(this, "GetDeviceShadowRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:GetThingShadow"
        ],
        resources: [
          `arn:aws:iot:${props.region}:${props.account}:thing/*`,
        ],
      })
    ]);

    const getDeviceShadowLambda = new Function(this, "GetDeviceShadowActivity", {
      runtime: Runtime.JAVA_17,
      //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
      handler: `${DM_ACTIVITY_JAVA_PATH_PREFIX}.GetDeviceShadowActivity::handleRequest`,
      code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
          ACCOUNT_ID: this.account,
          LAMBDA_ROLE_ARN: getDeviceShadowRole.roleArn,
      },
      role: getDeviceShadowRole,
      logGroup: new LogGroup(this, "GetDeviceShadowActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/GetDeviceShadowActivity",
      }),
    });

    apiGatewayRole.addToPolicy(new PolicyStatement({
      resources: ['*'],
      actions: ['lambda:InvokeFunction']
    }));

    // Overriding CFN logical IDs for OpenAPI spec transformation
    // This must match the variables defined in the Smithy model
    const getDeviceCfnLambda = getDeviceLambda.node.defaultChild as CfnFunction;
    getDeviceCfnLambda.overrideLogicalId("GetDeviceActivity");
    const getDeviceShadowCfnLambda = getDeviceShadowLambda.node.defaultChild as CfnFunction;
    getDeviceShadowCfnLambda.overrideLogicalId("GetDeviceShadowActivity");

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
      "VideoAnalyticsDeviceManagementAPIGateway",
      transformedOpenApiSpec,
      this.account,
      this.region
    );
  }
}
