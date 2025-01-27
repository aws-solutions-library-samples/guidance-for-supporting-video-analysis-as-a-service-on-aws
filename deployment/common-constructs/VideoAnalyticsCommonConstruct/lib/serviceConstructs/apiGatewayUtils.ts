import { IResolvable } from 'aws-cdk-lib';
import { ApiDefinition, MethodLoggingLevel, SpecRestApi } from 'aws-cdk-lib/aws-apigateway';
import { AccountPrincipal, PolicyDocument, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

/**
 * Create an API Gateway with required stack and openApiSpec. Others are optional.
 */
export function createApiGateway(
  scope: Construct,
  apiGatewayName: string,
  openApiSpec: IResolvable,
  account: string,
  region: string,
  deployOptions?: {
    loggingLevel: MethodLoggingLevel;
    dataTraceEnabled: boolean;
    tracingEnabled?: boolean;
    cloudWatchRole?: boolean;
  }
) {
  return new SpecRestApi(scope, apiGatewayName, {
    apiDefinition: ApiDefinition.fromInline(openApiSpec),
    deployOptions: deployOptions || {
      loggingLevel: MethodLoggingLevel.INFO,
      dataTraceEnabled: true
    },
    policy: new PolicyDocument({
      statements: [new PolicyStatement({
        actions: ['execute-api:Invoke'],
        resources: [
          `arn:aws:execute-api:${region}:${account}:*/*/POST/*`
        ],
        principals: [new AccountPrincipal(account)],
      })],
    })
  });
}

export const VIDEO_LOGISTICS_API_NAME = "VideoAnalyticsVideoLogisticsAPIGateway";
export const DEVICE_MANAGEMENT_API_NAME = "VideoAnalyticsDeviceManagementAPIGateway";

