import { Duration, Fn, Stack, StackProps } from "aws-cdk-lib";
import { MethodLoggingLevel, SpecRestApi } from 'aws-cdk-lib/aws-apigateway';
import { CfnFunction } from "aws-cdk-lib/aws-cloudfront";
import { Effect, ManagedPolicy, PolicyStatement, Role, ServicePrincipal } from "aws-cdk-lib/aws-iam";
import { Code, Function, Runtime, Tracing } from "aws-cdk-lib/aws-lambda";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import { Asset } from 'aws-cdk-lib/aws-s3-assets';
import { Construct } from "constructs";
import * as fs from 'fs';
import { AWSRegion, createApiGateway, createLambdaRole, DEVICE_MANAGEMENT_API_NAME } from "video_analytics_common_construct";
import {
  DM_ACTIVITY_JAVA_PATH_PREFIX,
  LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT,
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
      roleName: 'DeviceManagementApiGatewayRole',
      assumedBy: new ServicePrincipal('apigateway.amazonaws.com'),
      managedPolicies: [
        ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonAPIGatewayPushToCloudWatchLogs')
      ]
    });

    // Add Lambda invoke permissions
    apiGatewayRole.addToPolicy(new PolicyStatement({
      resources: [`arn:aws:lambda:${props.region}:${props.account}:function:*`],
      actions: ['lambda:InvokeFunction']
    }));

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
      tracing: Tracing.ACTIVE,
      handler: `${DM_ACTIVITY_JAVA_PATH_PREFIX}.GetDeviceActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT),
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

    getDeviceLambda.addPermission('getDeviceApiGatewayPermission', {
      principal: new ServicePrincipal('apigateway.amazonaws.com'),
    })

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
      tracing: Tracing.ACTIVE,
      handler: `${DM_ACTIVITY_JAVA_PATH_PREFIX}.GetDeviceShadowActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT),
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

    getDeviceShadowLambda.addPermission('getDeviceShadowApiGatewayPermission', {
      principal: new ServicePrincipal('apigateway.amazonaws.com'),
    })

    const updateDeviceShadowRole = createLambdaRole(this, "UpdateDeviceShadowRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:UpdateThingShadow"
        ],
        resources: [
          `arn:aws:iot:${props.region}:${props.account}:thing/*`,
        ],
      })
    ]);

    const updateDeviceShadowLambda = new Function(this, "UpdateDeviceShadowActivity", {
      runtime: Runtime.JAVA_17,
      tracing: Tracing.ACTIVE,
      handler: `${DM_ACTIVITY_JAVA_PATH_PREFIX}.UpdateDeviceShadowActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
          ACCOUNT_ID: this.account,
          LAMBDA_ROLE_ARN: updateDeviceShadowRole.roleArn,
      },
      role: updateDeviceShadowRole,
      logGroup: new LogGroup(this, "UpdateDeviceShadowActivityLogGroup", {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: "/aws/lambda/UpdateDeviceShadowActivity",
      }),
    });

    updateDeviceShadowLambda.addPermission('updateDeviceShadowApiGatewayPermission', {
      principal: new ServicePrincipal('apigateway.amazonaws.com'),
    })

    // Create StartCreateDeviceActivity role with minimal permissions
    const StartCreateDeviceSyncPathRole = createLambdaRole(this, "StartCreateDeviceSyncPathRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:DescribeThing",
          "iot:DescribeCertificate",
        ],
        resources: [
          `arn:aws:iot:${props.region}:${props.account}:thing/*`,
          `arn:aws:iot:${props.region}:${props.account}:cert/*`,
        ],
      }),
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: ["dynamodb:PutItem"],
        resources: [`arn:aws:dynamodb:${props.region}:${props.account}:table/CreateDeviceTable`],
      }),
    ]);

    // Create GetCreateDeviceStatusActivity role with minimal permissions
    const getCreateDeviceStatusRole = createLambdaRole(this, "GetCreateDeviceStatusRole", [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: ["dynamodb:GetItem"],
        resources: [`arn:aws:dynamodb:${props.region}:${props.account}:table/CreateDeviceTable`],
      }),
    ]);

    // StartCreateDeviceActivity Lambda
    const startCreateDeviceLambda = new Function(this, "StartCreateDeviceActivity", {
      runtime: Runtime.JAVA_17,
      tracing: Tracing.ACTIVE,
      handler: `${DM_ACTIVITY_JAVA_PATH_PREFIX}.StartCreateDeviceActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
        ACCOUNT_ID: this.account,
        LAMBDA_ROLE_ARN: StartCreateDeviceSyncPathRole.roleArn,
      },
      role: StartCreateDeviceSyncPathRole,
      logGroup: new LogGroup(this, "StartCreateDeviceActivityLogGroup", {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: "/aws/lambda/StartCreateDeviceActivity",
      }),
    });

    startCreateDeviceLambda.addPermission('startCreateDeviceApiGatewayPermission', {
      principal: new ServicePrincipal('apigateway.amazonaws.com'),
    })

    // GetCreateDeviceStatusActivity Lambda
    const getCreateDeviceStatusLambda = new Function(this, "GetCreateDeviceStatusActivity", {
      runtime: Runtime.JAVA_17,
      tracing: Tracing.ACTIVE,
      handler: `${DM_ACTIVITY_JAVA_PATH_PREFIX}.GetCreateDeviceStatusActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
        ACCOUNT_ID: this.account,
        LAMBDA_ROLE_ARN: getCreateDeviceStatusRole.roleArn,
      },
      role: getCreateDeviceStatusRole,
      logGroup: new LogGroup(this, "GetCreateDeviceStatusActivityLogGroup", {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: "/aws/lambda/GetCreateDeviceStatusActivity",
      }),
    });

    getCreateDeviceStatusLambda.addPermission('getCreateDeviceApiGatewayPermission', {
      principal: new ServicePrincipal('apigateway.amazonaws.com'),
    })

    // Overriding CFN logical IDs for OpenAPI spec transformation
    // This must match the variables defined in the Smithy model
    const getDeviceCfnLambda = getDeviceLambda.node.defaultChild as CfnFunction;
    getDeviceCfnLambda.overrideLogicalId("GetDeviceActivity");
    const getDeviceShadowCfnLambda = getDeviceShadowLambda.node.defaultChild as CfnFunction;
    getDeviceShadowCfnLambda.overrideLogicalId("GetDeviceShadowActivity");
    const updateDeviceShadowCfnLambda = updateDeviceShadowLambda.node.defaultChild as CfnFunction;
    updateDeviceShadowCfnLambda.overrideLogicalId("UpdateDeviceShadowActivity");

    const getCreateDeviceStatusCfnLambda = getCreateDeviceStatusLambda.node.defaultChild as CfnFunction;
    getCreateDeviceStatusCfnLambda.overrideLogicalId("GetCreateDeviceStatusActivity");

    const startCreateDeviceCfnLambda = startCreateDeviceLambda.node.defaultChild as CfnFunction;
    startCreateDeviceCfnLambda.overrideLogicalId("StartCreateDeviceActivity");

    // configure auth type for all methods (workaround since Smithy does not support x-amazon-apigateway-auth trait)
    const APIS = [
      "/get-create-device-status/{jobId}",
      "/get-device-shadow/{deviceId}",
      "/get-device/{deviceId}",
      "/start-create-device/{deviceId}",
      "/update-device-shadow/{deviceId}"
    ]
    const data = JSON.parse(fs.readFileSync(OPEN_API_SPEC_PATH, 'utf8'));
    for (const api of APIS) {
      data["paths"][api]["post"]["x-amazon-apigateway-auth"] = {
        "type": "AWS_IAM"
      };
    }
    fs.writeFileSync(OPEN_API_SPEC_PATH, JSON.stringify(data, null, 4));

    // Upload spec to S3
    const originalSpec = new Asset(this, "openApiFile", {
      path: OPEN_API_SPEC_PATH
    });

    // Pulls the content back into the template. Being inline, this will now respect CF references within the file.
    const transformMap = {
      "Location": originalSpec.s3ObjectUrl,
    };

    const transformedOpenApiSpec = Fn.transform("AWS::Include", transformMap);

    // Create API Gateway with stage settings
    this.restApi = createApiGateway(this, 
      DEVICE_MANAGEMENT_API_NAME,
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
