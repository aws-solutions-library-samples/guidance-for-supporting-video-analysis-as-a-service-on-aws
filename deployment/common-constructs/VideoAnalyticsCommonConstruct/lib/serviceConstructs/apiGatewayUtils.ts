import { IResolvable } from 'aws-cdk-lib';
import { SpecRestApi, ApiDefinition, MethodLoggingLevel } from 'aws-cdk-lib/aws-apigateway';
import { AnyPrincipal, PolicyDocument, PolicyStatement } from 'aws-cdk-lib/aws-iam';
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
        // Tried to set to AccountPrincipal, but I was getting authorization errors
        // Can test again and try to scope down later
        principals: [new AnyPrincipal()],
      })],
    })
  });
}
