# Video Analytics Device Management CDK

This project contains the CDK code for deploying the Video Analytics Device Management infrastructure.

## Prerequisites

- AWS CLI installed and configured
- Node.js and npm installed
- AWS CDK CLI installed globally (`npm install -g aws-cdk`)

Note: The OpenAPI spec passed into `createApiGateway` must have lambda integration defined. Remove any APIs that do not have lambda integrations yet.

## Deployment Steps

1. **Clone the repository**
   ```
   git clone <repository-url>
   cd deployment/device-management-cdk/VideoAnalyticsDeviceManagementCDK
   ```

2. **Install dependencies**
   ```
   npm install
   ```

3. **Configure deployment environment**
   Set the appropriate environment variables:
   ```
   export CDK_DEPLOY_ACCOUNT=your_aws_account_id
   export CDK_DEPLOY_REGION=your_aws_region
   ```

4. **Assume AWS credentials**
   Assume AWS credentials using ADA or AWS CLI or from exported variables in Isengard bash.

5. **Synthesize the CloudFormation template**
   ```
   cdk synth
   ```

6. **Deploy the stack**
   ```
   cdk deploy WorkflowStack
   ```
   To deploy all stacks, use:
   ```
   cdk deploy --all
   ```

7. **Review and confirm the changes** when prompted

8. **Monitor the deployment** in the terminal or AWS CloudFormation console

9. **Verify the resources** in the AWS Console after deployment

## Cleanup

To remove the deployed resources:


## Development Notes

### Lambda Asset Locations
The Device Managment Application Lambda compiled jar is expected to be located at:
```
guidance-for-video-analytics-infrastructure-on-aws/assets/lambda-built/device-management-assets
```

### REST API ID
The REST API ID is exported from the Video Logistics CDK stack (../../video-logistics-cdk/VideoAnalyticsVideoLogisticsCDK/lib/stacks/serviceStack/serviceStack.ts) and can be referenced using the export name 'VideoLogisticsRestApiId'. This ID is exposed through a CloudFormation output in the service stack.

This REST API ID is required for asynchronous workflow-based APIs, such as Device Registration, which needs the Video Logistics API endpoint to create KVS (Kinesis Video Streams) streams during the device registration process.

**Note:** Due to this dependency on the Video Logistics API, the deployment order matters. The Video Logistics stack must be deployed before the Device Management stack. Noted for later reference of VL of DM api endpoint, deadlock circular dependency would become an issue, TODO! to collate into single stack for all async workflows and endpoint deployment.

