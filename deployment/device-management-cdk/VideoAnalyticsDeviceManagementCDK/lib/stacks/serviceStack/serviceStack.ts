import { Duration, Fn, Stack, StackProps } from "aws-cdk-lib";
import { SpecRestApi, MethodLoggingLevel } from 'aws-cdk-lib/aws-apigateway';
import { CfnFunction } from "aws-cdk-lib/aws-cloudfront";
import { Effect, PolicyStatement, Role, ServicePrincipal, ManagedPolicy } from "aws-cdk-lib/aws-iam";
import { Function, Runtime, Code } from "aws-cdk-lib/aws-lambda";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import { Asset } from 'aws-cdk-lib/aws-s3-assets'
import { Construct } from "constructs";
import { AWSRegion, createApiGateway, createLambdaRole } from "video_analytics_common_construct";

import {
    DM_ACTIVITY_JAVA_PATH_PREFIX,
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
      roleName: 'DeviceManagementApiGatewayRole',
      assumedBy: new ServicePrincipal('apigateway.amazonaws.com'),
      managedPolicies: [
        ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonAPIGatewayPushToCloudWatchLogs')
      ]
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
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
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
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
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
      //TODO: Update this if any changes are made to the lambda handler path or asset built jar location
      handler: `${DM_ACTIVITY_JAVA_PATH_PREFIX}.UpdateDeviceShadowActivity::handleRequest`,
      code: Code.fromAsset(LAMBDA_ASSET_PATH),
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

    // Create base role for the create device activity related lambdas
    const createDeviceActivityLambdaRole = createLambdaRole(this, "CreateDeviceActivityLambdaRole", [
      // DynamoDB permissions
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "dynamodb:GetRecords",
          "dynamodb:GetItem",
          "dynamodb:Query",
          "dynamodb:PutItem",
          "dynamodb:UpdateItem",
          "dynamodb:Scan",
        ],
        resources: [`arn:aws:dynamodb:${props.region}:${props.account}:table/*`],
      }),
      // KMS permissions
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: ["kms:Decrypt", "kms:Encrypt", "kms:ReEncrypt*"],
        resources: [`arn:aws:kms:${props.region}:${props.account}:key/*`],
      }),
      // IoT permissions
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "iot:CreateThing",
          "iot:CreateKeysAndCertificate",
          "iot:AttachThingPrincipal",
          "iot:AttachPolicy",
          "iot:CreatePolicy",
          "iot:DescribeThing",
          "iot:UpdateThing",
          "iot:ListThingGroupsForThing",
          "iot:DescribeThingGroup",
          "iot:AddThingToThingGroup",
          "iot:DescribeCertificate"
        ],
        resources: [
          `arn:aws:iot:${props.region}:${props.account}:thing/*`,
          `arn:aws:iot:${props.region}:${props.account}:cert/*`,
          `arn:aws:iot:${props.region}:${props.account}:policy/*`
        ]
      }),
      // KVS permissions
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "kinesisvideo:CreateStream",
          "kinesisvideo:DescribeStream",
          "kinesisvideo:TagStream"
        ],
        resources: [`arn:aws:kinesisvideo:${props.region}:${props.account}:stream/*`],
      }),
      // API Gateway permissions
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          'execute-api:Invoke',
          'execute-api:ManageConnections'
        ],
        resources: [`arn:aws:execute-api:${props.region}:${props.account}:*`]
      }),
      // Add Step Function execution permissions
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          'states:StartExecution',
          'states:DescribeExecution',
          'states:StopExecution'
        ],
        resources: [`arn:aws:states:${props.region}:${props.account}:stateMachine:*`]
      })
    ]);

    const getCreateDeviceStatusLambda = new Function(this, "GetCreateDeviceStatusActivity", {
      runtime: Runtime.JAVA_17,
      handler: `${DM_ACTIVITY_JAVA_PATH_PREFIX}.GetCreateDeviceStatusActivity::handleRequest`,
      code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
        ACCOUNT_ID: this.account,
        LAMBDA_ROLE_ARN: createDeviceActivityLambdaRole.roleArn,
        AWS_LAMBDA_LOG_LEVEL: "DEBUG"
      },
      role: createDeviceActivityLambdaRole,
      logGroup: new LogGroup(this, "GetCreateDeviceStatusActivityLogGroup", {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: "/aws/lambda/GetCreateDeviceStatusActivity",
      }),
    });

    const startCreateDeviceSyncPathLambda = new Function(this, "StartCreateDeviceActivity", {
      runtime: Runtime.JAVA_17,
      handler: `${DM_ACTIVITY_JAVA_PATH_PREFIX}.StartCreateDeviceActivity::handleRequest`,
      code: Code.fromAsset(`${LAMBDA_ASSET_PATH_TO_DEVICE_MANAGEMENT}`),
      memorySize: 512,
      timeout: Duration.minutes(5),
      environment: {
        ACCOUNT_ID: this.account,
        LAMBDA_ROLE_ARN: createDeviceActivityLambdaRole.roleArn,
      },
      role: createDeviceActivityLambdaRole,
      logGroup: new LogGroup(this, "StartCreateDeviceActivityLogGroup", {
        retention: RetentionDays.TEN_YEARS,
        logGroupName: "/aws/lambda/StartCreateDeviceActivity",
      }),
    });

    // Add Lambda invoke permissions
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
    const updateDeviceShadowCfnLambda = updateDeviceShadowLambda.node.defaultChild as CfnFunction;
    updateDeviceShadowCfnLambda.overrideLogicalId("UpdateDeviceShadowActivity");

    const getCreateDeviceStatusCfnLambda = getCreateDeviceStatusLambda.node.defaultChild as CfnFunction;
    getCreateDeviceStatusCfnLambda.overrideLogicalId("GetCreateDeviceStatusActivity");

    const startCreateDeviceCfnLambda = startCreateDeviceSyncPathLambda.node.defaultChild as CfnFunction;
    startCreateDeviceCfnLambda.overrideLogicalId("StartCreateDeviceActivity");

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

    // Create API Gateway with stage settings
    this.restApi = createApiGateway(this, 
      "VideoAnalyticsDeviceManagementAPIGateway",
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
