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
