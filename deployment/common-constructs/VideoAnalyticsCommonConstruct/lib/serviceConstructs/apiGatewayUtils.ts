import { SpecRestApi, ApiDefinition, MethodLoggingLevel } from 'aws-cdk-lib/aws-apigateway';
import { Construct } from 'constructs';

/**
 * Create an API Gateway with required stack and openApiSecFilePath. Others are optional.
 */
export function createApiGateway(
  scope: Construct,
  apiGatewayName: string,
  openApiSpecFilePath: string
) {
  return new SpecRestApi(scope, apiGatewayName, {
    apiDefinition: ApiDefinition.fromAsset(openApiSpecFilePath),
    deployOptions: {
      loggingLevel: MethodLoggingLevel.INFO,
      dataTraceEnabled: true
    }
  });
}