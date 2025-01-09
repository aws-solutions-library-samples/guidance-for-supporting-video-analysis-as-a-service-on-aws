# Guidance Title (required)

The Guidance title should be consistent with the title established first in Alchemy.

**Example:** *Guidance for Product Substitutions on AWS*

This title correlates exactly to the Guidance it’s linked to, including its corresponding sample code repository. 


## Table of Contents (required)

List the top-level sections of the README template, along with a hyperlink to the specific section.

### Required

1. [Overview](#overview-required)
    - [Cost](#cost)
2. [Prerequisites](#prerequisites-required)
    - [Operating System](#operating-system-required)
3. [Deployment Steps](#deployment-steps-required)
4. [Deployment Validation](#deployment-validation-required)
5. [Running the Guidance](#running-the-guidance-required)
6. [Next Steps](#next-steps-required)
7. [Cleanup](#cleanup-required)

***Optional***

8. [FAQ, known issues, additional considerations, and limitations](#faq-known-issues-additional-considerations-and-limitations-optional)
9. [Revisions](#revisions-optional)
10. [Notices](#notices-optional)
11. [Authors](#authors-optional)

## Overview (required)

1. Provide a brief overview explaining the what, why, or how of your Guidance. You can answer any one of the following to help you write this:

    - **Why did you build this Guidance?**
    - **What problem does this Guidance solve?**

2. Include the architecture diagram image, as well as the steps explaining the high-level overview and flow of the architecture. 
    - To add a screenshot, create an ‘assets/images’ folder in your repository and upload your screenshot to it. Then, using the relative file path, add it to your README. 

### Cost ( required )

This section is for a high-level cost estimate. Think of a likely straightforward scenario with reasonable assumptions based on the problem the Guidance is trying to solve. Provide an in-depth cost breakdown table in this section below ( you should use AWS Pricing Calculator to generate cost breakdown ).

Start this section with the following boilerplate text:

_You are responsible for the cost of the AWS services used while running this Guidance. As of <month> <year>, the cost for running this Guidance with the default settings in the <Default AWS Region (Most likely will be US East (N. Virginia)) > is approximately $<n.nn> per month for processing ( <nnnnn> records )._

Replace this amount with the approximate cost for running your Guidance in the default Region. This estimate should be per month and for processing/serving resonable number of requests/entities.

Suggest you keep this boilerplate text:
_We recommend creating a [Budget](https://docs.aws.amazon.com/cost-management/latest/userguide/budgets-managing-costs.html) through [AWS Cost Explorer](https://aws.amazon.com/aws-cost-management/aws-cost-explorer/) to help manage costs. Prices are subject to change. For full details, refer to the pricing webpage for each AWS service used in this Guidance._

### Sample Cost Table ( required )

**Note : Once you have created a sample cost table using AWS Pricing Calculator, copy the cost breakdown to below table and upload a PDF of the cost estimation on BuilderSpace. Do not add the link to the pricing calculator in the ReadMe.**

The following table provides a sample cost breakdown for deploying this Guidance with the default parameters in the US East (N. Virginia) Region for one month.

| AWS service  | Dimensions | Cost [USD] |
| ----------- | ------------ | ------------ |
| Amazon API Gateway | 1,000,000 REST API calls per month  | $ 3.50month |
| Amazon Cognito | 1,000 active users per month without advanced security feature | $ 0.00 |

## Prerequisites (required)

### Operating System (required)

This deployment has been tested on macOS and Linux operating systems. Follow these steps in order:

1. **AWS Account and CLI Setup**
   - An AWS account with administrative permissions
   - [AWS CLI version 2](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) installed and configured
   - Configure AWS CLI with your credentials:
     ```bash
     aws configure
     ```

2. **Development Tools**
   - Java Development Kit (JDK) 17 or higher
     ```bash
     # For macOS using Homebrew
     brew install openjdk@17
     
     # For Ubuntu/Debian
     sudo apt update
     sudo apt install openjdk-17-jdk
     
     # For Amazon Linux/RHEL/CentOS
     # Per: https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/amazon-linux-install.html
     sudo yum install java-17-amazon-corretto
     
     # Verify installation
     java -version
     ```
   
   - Node.js (v18 or higher) and npm
     ```bash
     # For macOS using Homebrew
     brew install node@18
     
     # For Ubuntu/Debian
     curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
     sudo apt-get install -y nodejs
     
     # For Amazon Linux/RHEL/CentOS
     curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -
     sudo yum install -y nodejs
     
     # Verify installation
     node --version
     npm --version
     ```

3. **GStreamer Libraries**
   GStreamer is required for running and testing the edge binary for video processing.

   - For macOS:
     ```bash
     # Using Homebrew
     brew install gstreamer gst-plugins-base gst-plugins-good gst-plugins-bad gst-plugins-ugly gst-libav
     
     # Verify installation
     gst-launch-1.0 --version
     ```

   - For Ubuntu/Debian:
     ```bash
     sudo apt update
     sudo apt install -y \
         gstreamer1.0-tools \
         gstreamer1.0-plugins-base \
         gstreamer1.0-plugins-good \
         gstreamer1.0-plugins-bad \
         gstreamer1.0-plugins-ugly \
         gstreamer1.0-libav
     
     # Verify installation
     gst-launch-1.0 --version
     ```

   - For Amazon Linux/RHEL/CentOS:
     ```bash
     sudo yum install -y \
         gstreamer1 \
         gstreamer1-plugins-base \
         gstreamer1-plugins-good \
         gstreamer1-plugins-bad-free \
         gstreamer1-plugins-ugly-free \
         gstreamer1-libav
     
     # Verify installation
     gst-launch-1.0 --version
     ```

   Note: Some GStreamer plugins might require additional dependencies or licenses depending on your use case.

4. **AWS CDK CLI**
   ```bash
   npm install -g aws-cdk
   
   # Verify installation
   cdk --version
   ```

5. **Rust Development Environment** (Required for Edge Components)
   - Install Rust and Cargo
     ```bash
     curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
     source $HOME/.cargo/env
     
     # Verify installation
     rustc --version
     cargo --version
     ```
   
   - Install Cross (for cross-compilation)
     ```bash
     cargo install cross --git https://github.com/cross-rs/cross
     ```

6. **Additional Tools**
   - Git
     ```bash
     # For macOS using Homebrew
     brew install git
     
     # For Ubuntu/Debian
     sudo apt install git
     
     # For Amazon Linux/RHEL/CentOS
     sudo yum install git
     
     # Verify installation
     git --version
     ```
   
   - AWS Systems Manager Session Manager plugin (for camera access)
     ```bash
     # For macOS
     curl "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/mac/sessionmanager-bundle.zip" -o "sessionmanager-bundle.zip"
     unzip sessionmanager-bundle.zip
     sudo ./sessionmanager-bundle/install -i /usr/local/sessionmanagerplugin -b /usr/local/bin/session-manager-plugin
     
     # For Linux
     curl "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/linux_64bit/session-manager-plugin.rpm" -o "session-manager-plugin.rpm"
     sudo yum install -y session-manager-plugin.rpm
     
     # Verify installation
     session-manager-plugin --version
     ```

7. **Project Setup**
   ```bash
   # Clone the repository
   git clone <repository-url>
   cd guidance-for-video-analytics-infrastructure-on-aws
   
   # Install dependencies for deployment
   cd deployment
   npm install
   
   # Build common constructs
   cd common-constructs/VideoAnalyticsCommonConstruct
   npm install
   npm run build
   
   # Return to root directory
   cd ../../..
   ```

8. **Environment Configuration**
   ```bash
   # Set required environment variables
   export AWS_REGION=<your-preferred-region>  # e.g., us-east-1
   ```

### Required AWS Account Permissions

The deployment requires an AWS account with permissions to create and manage the following services:
- AWS Lambda
- Amazon API Gateway
- Amazon S3
- Amazon DynamoDB
- AWS IoT Core
- Amazon Kinesis Video Streams
- AWS Systems Manager
- AWS IAM (for creating roles and policies)

### Network Requirements
- Outbound internet access for downloading dependencies and accessing AWS services
- If working with cameras:
  - Access to port 80 (ONVIF)
  - Access to port 554 (RTSP)
  - Proper network configuration to allow communication between edge devices and AWS cloud services

## Deployment Steps (required)

### Project Structure Overview

The deployment infrastructure is organized into two main components and follows this structure:

```
deployment/
├── package.json
├── package-lock.json
├── tsconfig.json
├── node_modules/
│   ├── aws-cdk-lib/
│   ├── constructs/
│   └── ... (other shared dependencies)
├── common-constructs/
│   └── VideoAnalyticsCommonConstruct/
│       ├── package.json
│       ├── tsconfig.json
│       ├── lib/
│       │   └── ... (TypeScript Common constructs)
│       ├── node_modules/
│       │   └── ... (package-specific dependencies)
│       └── .gitignore
├── device-management-cdk/
│   └── VideoAnalyticsDeviceManagementCDK/
│       ├── package.json
│       ├── tsconfig.json
│       ├── lib/
│       │   └── ... (TypeScript Device Management CDK)
│       ├── node_modules/
│       │   └── ... (package-specific dependencies)
│       ├── cdk.json
│       └── .gitignore
└── video-logistics-cdk/
    └── VideoAnalyticsVideoLogisticsCDK/
        ├── package.json
        ├── tsconfig.json
        ├── lib/
        │   └── ... (TypeScript Video Logistics CDK)
        ├── node_modules/
        │   └── ... (package-specific dependencies)
        ├── cdk.json
        └── .gitignore
```

1. **Device Management (deployment/device-management-cdk/)**
   - Handles device registration, management, and monitoring
   - Components:
     - Bootstrap Stack: Initial setup and shared resources
     - Workflow Stack: Device management workflows
     - Service Stack: Core Video Analytics Async Device Management services

2. **Video Logistics (deployment/video-logistics-cdk/)**
   - Manages video processing and analytics
   - Components:
     - Bootstrap Stack: Initial setup
     - Timeline Stack: Video timeline management
     - OpenSearch Stack: Search and analytics
     - Bulk Inference Stack: Video processing
     - Workflow Stack: Video processing workflows
     - Service Stack: Core Video Analytics Async Video Logistics Processing services

### Environment Variables

The deployment uses either explicitly set deployment variables OR falls back to default variables:

```bash
# Option 1: Set deployment-specific variables
export CDK_DEPLOY_ACCOUNT=your_aws_account_id
export CDK_DEPLOY_REGION=your_aws_region

# OR

# Option 2: Use default variables
export CDK_DEFAULT_ACCOUNT=your_aws_account_id
export CDK_DEFAULT_REGION=your_aws_region
```

The stack will use variables in this order:
1. `CDK_DEPLOY_ACCOUNT/REGION` if set
2. `CDK_DEFAULT_ACCOUNT/REGION` if set
3. Default values:
   - Region: us-east-1 (Device Management) or us-west-2 (Video Logistics)
   - Account: "YOUR_DEFAULT_ACCOUNT"

### Deployment Process

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd guidance-for-video-analytics-infrastructure-on-aws
   ```

2. **Install Global Dependencies**
   ```bash
   npm install -g aws-cdk typescript
   ```

3. **Install Project Dependencies**
   ```bash
   cd deployment
   npm install
   ```

4. **Build Common Constructs**
   ```bash
   cd common-constructs/VideoAnalyticsCommonConstruct
   npm install
   npm run build
   ```

5. **Deploy Device Management Infrastructure**
   ```bash
   cd ../../device-management-cdk/VideoAnalyticsDeviceManagementCDK
   npm install
   
   # Update snapshot tests if needed
   npm run test -- -u
   
   # Build and synthesize CloudFormation
   npm run build
   cdk synth
   
   # Deploy stacks in order
   cdk deploy DeviceManagementBootstrapStack
   cdk deploy DeviceManagementWorkflowStack
   cdk deploy DeviceManagementServiceStack
   ```

6. **Deploy Video Logistics Infrastructure**
   ```bash
   cd ../../video-logistics-cdk/VideoAnalyticsVideoLogisticsCDK
   npm install
   
   # Update snapshot tests if needed
   npm run test -- -u
   
   # Build and synthesize CloudFormation
   npm run build
   cdk synth
   
   # Deploy stacks in order
   cdk deploy VideoLogisticsBootstrapStack
   cdk deploy VideoLogisticsTimelineStack
   cdk deploy VideoLogisticsOpensearchStack
   cdk deploy VideoLogisticsBulkInferenceStack
   cdk deploy VideoLogisticsWorkflowStack
   cdk deploy VideoLogisticsServiceStack
   ```

## Deployment Validation (required)

After deploying both Device Management and Video Logistics stacks, verify the deployment by checking the following resources:

### 1. Device Management CloudFormation Stack Verification

Verify the following CloudFormation stacks are deployed successfully:

```bash
# Verify all Device Management stacks status
aws cloudformation describe-stacks \
  --query 'Stacks[?contains(StackName, `DeviceManagement`)].{Name:StackName,Status:StackStatus}' \
  --output table
```

Check for the following stacks with status "CREATE_COMPLETE":

1. **Bootstrap Stack**
   - Stack name: `DeviceManagementBootstrapStack`
   ```bash
   aws cloudformation describe-stacks \
     --stack-name DeviceManagementBootstrapStack \
     --query 'Stacks[0].{Name:StackName,Status:StackStatus,Outputs:Outputs}'
   ```

2. **Core Components**
   - Stack name: `DeviceManagementServiceStack`
   ```bash
   aws cloudformation describe-stacks \
     --stack-name DeviceManagementServiceStack \
     --query 'Stacks[0].{Name:StackName,Status:StackStatus,Outputs:Outputs}'
   ```

3. **Workflow Components**
   - Stack name: `DeviceManagementWorkflowStack`
   ```bash
   aws cloudformation describe-stacks \
     --stack-name DeviceManagementWorkflowStack \
     --query 'Stacks[0].{Name:StackName,Status:StackStatus,Outputs:Outputs}'
   ```

You can verify individual stack resources using:
```bash
# Replace STACK_NAME with the specific stack name
aws cloudformation describe-stack-resources \
  --stack-name STACK_NAME \
  --query 'StackResources[].{LogicalID:LogicalResourceId,Type:ResourceType,Status:ResourceStatus}'
```

### 2. Video Logistics CloudFormation Stack Verification

Verify the following CloudFormation stacks are deployed successfully:

```bash
# Verify all Video Logistics stacks status
aws cloudformation describe-stacks \
  --query 'Stacks[?contains(StackName, `VideoLogistics`)].{Name:StackName,Status:StackStatus}' \
  --output table
```

Check for the following stacks with status "CREATE_COMPLETE":

1. **Bootstrap Stack**
   - Stack name: `VideoLogisticsBootstrapStack`
   ```bash
   aws cloudformation describe-stacks \
     --stack-name VideoLogisticsBootstrapStack \
     --query 'Stacks[0].{Name:StackName,Status:StackStatus,Outputs:Outputs}'
   ```

2. **Core Infrastructure**
   - Stack name: `VideoLogisticsOpensearchStack`
   - Stack name: `VideoLogisticsTimelineStack`
   - Stack name: `VideoLogisticsServiceStack`

3. **Processing Components**
   - Stack name: `VideoLogisticsBulkInferenceStack`
   - Stack name: `VideoLogisticsVideoExportStack`
   - Stack name: `VideoLogisticsWorkflowStack`

4. **Management Components**
   - Stack name: `VideoLogisticsSchedulerStack`
   - Stack name: `VideoLogisticsForwardingRulesStack`

You can verify individual stack details using:
```bash
# Replace STACK_NAME with the specific stack name
aws cloudformation describe-stack-resources \
  --stack-name STACK_NAME \
  --query 'StackResources[].{LogicalID:LogicalResourceId,Type:ResourceType,Status:ResourceStatus}'
```

### 3. API Gateway Endpoints Verification

After deployment, you should see two API Gateway endpoints in the AWS Console:

![API Gateway Endpoints](assets/images/api-gateway-endpoints.png)

The endpoints should show:
- VideoAnalyticsDeviceManagementAPIGateway (Edge-optimized REST API)
- VideoAnalyticsVideoLogisticsAPIGateway (Edge-optimized REST API)

These endpoints are used for device management and video logistics operations respectively.

You can verify these endpoints using AWS CLI:

#### Device Management API
```bash
# List API Gateway APIs and find VideoAnalyticsDeviceManagementAPIGateway
aws apigateway get-rest-apis \
  --query 'items[?contains(name, `VideoAnalyticsDeviceManagementAPIGateway`)].{Name:name,ID:id}'

# Get the API ID from above command output and verify stages
export DM_API_ID=<api-id-from-above>
aws apigateway get-stages --rest-api-id $DM_API_ID

# Verify API resources and methods
aws apigateway get-resources --rest-api-id $DM_API_ID \
  --query 'items[?contains(name, `VideoAnalyticsDeviceManagementAPIGateway`)].{Name:name,Type:type}'
```

Expected output should show:
- API named "VideoAnalyticsDeviceManagementAPIGateway"
- Stage "prod" deployed
- Resources for device management operations

#### Video Logistics API
```bash
# List API Gateway APIs and find VideoAnalyticsVideoLogisticsAPIGateway
aws apigateway get-rest-apis \
  --query 'items[?contains(name, `VideoAnalyticsVideoLogisticsAPIGateway`)].{Name:name,ID:id}'

# Get the API ID from above command output and verify stages
export VL_API_ID=<api-id-from-above>
aws apigateway get-stages --rest-api-id $VL_API_ID

# Verify API resources and methods
aws apigateway get-resources --rest-api-id $VL_API_ID
```

Expected output should show:
- API named "VideoAnalyticsVideoLogisticsAPIGateway"
- Stage "prod" deployed
- Resources for video processing operations

### 4. Test API Endpoints

After verifying the API Gateway deployments, you can test the endpoints:

```bash
# Get Device Management API endpoint
export DM_API_ENDPOINT=$(aws apigateway get-rest-apis \
  --query 'items[?contains(name, `VideoAnalyticsDeviceManagementAPIGateway`)].{endpoint:endpoint}' \
  --output text)

# Get Video Logistics API endpoint
export VL_API_ENDPOINT=$(aws apigateway get-rest-apis \
  --query 'items[?contains(name, `VideoAnalyticsVideoLogisticsAPIGateway`)].{endpoint:endpoint}' \
  --output text)

# Test Device Management health check endpoint
curl -X GET "${DM_API_ENDPOINT}/prod/health"

# Test Video Logistics health check endpoint
curl -X GET "${VL_API_ENDPOINT}/prod/health"
```

Both health check endpoints should return a successful response indicating the APIs are properly deployed and functioning.

### 5. Additional Verification Steps

- Check CloudWatch Logs for any deployment errors
- Verify IAM roles and policies are correctly created
- Ensure all Lambda functions are deployed and configured
- Check DynamoDB tables are created with correct schemas
- Verify S3 buckets are created with proper permissions

If any of these verification steps fail, check the CloudFormation stack events and CloudWatch logs for error details.

## Running the Guidance (required)

<Provide instructions to run the Guidance with the sample data or input provided, and interpret the output received.> 

This section should include:

* Guidance inputs
* Commands to run
* Expected output (provide screenshot if possible)
* Output description



## Next Steps (required)

Provide suggestions and recommendations about how customers can modify the parameters and the components of the Guidance to further enhance it according to their requirements.


## Cleanup (required)

- Include detailed instructions, commands, and console actions to delete the deployed Guidance.
- If the Guidance requires manual deletion of resources, such as the content of an S3 bucket, please specify.



## FAQ, known issues, additional considerations, and limitations (optional)


**Known issues (optional)**

<If there are common known issues, or errors that can occur during the Guidance deployment, describe the issue and resolution steps here>


**Additional considerations (if applicable)**

<Include considerations the customer must know while using the Guidance, such as anti-patterns, or billing considerations.>

**Examples:**

- “This Guidance creates a public AWS bucket required for the use-case.”
- “This Guidance created an Amazon SageMaker notebook that is billed per hour irrespective of usage.”
- “This Guidance creates unauthenticated public API endpoints.”


Provide a link to the *GitHub issues page* for users to provide feedback.


**Example:** *“For any feedback, questions, or suggestions, please use the issues tab under this repo.”*

## Revisions (optional)

Document all notable changes to this project.

Consider formatting this section based on Keep a Changelog, and adhering to Semantic Versioning.

## Notices (optional)

Include a legal disclaimer

**Example:**
*Customers are responsible for making their own independent assessment of the information in this Guidance. This Guidance: (a) is for informational purposes only, (b) represents AWS current product offerings and practices, which are subject to change without notice, and (c) does not create any commitments or assurances from AWS and its affiliates, suppliers or licensors. AWS products or services are provided “as is” without warranties, representations, or conditions of any kind, whether express or implied. AWS responsibilities and liabilities to its customers are controlled by AWS agreements, and this Guidance is not part of, nor does it modify, any agreement between AWS and its customers.*


## Authors (optional)

Name of code contributors
