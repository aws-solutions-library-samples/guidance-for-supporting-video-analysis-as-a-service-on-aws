# Video Analytics Infrastructure Deployment

This directory contains the AWS CDK deployment code for both Device Management and Video Logistics components.

## Project Deployment Structure Overview

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

## Prerequisites

Before proceeding with the CDK deployment, ensure all required components are built in the correct order, in order to generate the necessary assets for Lambda Compute resources.

### 1. Build Source Packages and Assets

> For detailed instructions on necessary dependencies, see the [main README.md](../README.md#prerequisites-required).

#### Build Common Assets

1. **Build Common Smithy Models**
   ```bash
   # From repository root
   cd source/models/common
   smithy build
   ```

2. **Build Common Workflow Handler (For Common Async Workflow Assets)**
   ```bash
   # Build workflow handler assets
   cd ../../workflow-handler/VideoAnalyticsWorkflowHandler
   ./gradlew build
   ```

#### Build Device Management Assets

1. **Build Smithy Models**
   ```bash
   # From repository root
   cd source/models/device-management
   smithy build
   ```

2. **Generate OpenAPI Specifications**
   ```bash
   # Generate client SDK
   cd source/device-management/VideoAnalyticsDeviceManagementJavaClient
   ./gradlew openApiGenerate
   ```

3. **Build Control Plane Package**
   ```bash
   # Build JAR files
   cd ../VideoAnalyticsDeviceManagementControlPlane
   ./gradlew build
   ```

#### Build Video Logistics Assets

1. **Build Smithy Models**
   ```bash
   # From repository root
   cd source/models/video-logistics
   smithy build
   ```

2. **Generate OpenAPI Specifications**
   ```bash
   # Generate client SDK
   cd source/video-logistics/VideoAnalyticsVideoLogisticsJavaClient
   ./gradlew openApiGenerate
   ```

3. **Build Control Plane Package**
   ```bash
   # Build JAR files
   cd ../VideoAnalyticsVideoLogisticsControlPlane
   ./gradlew build
   ```

### 2. Build CDK Components

1. **Install Global Dependencies**
   ```bash
   npm install -g aws-cdk typescript
   ```

2. **Install Root Project Dependencies**
   ```bash
   # From the deployment directory
   npm install
   ```

3. **Build Common Constructs**
   ```bash
   cd common-constructs/VideoAnalyticsCommonConstruct
   npm install
   npm run build
   cd ../..
   ```

## Building and Deploying CDK Infrastructure Components

### 1. Device Management

```bash
cd device-management-cdk/VideoAnalyticsDeviceManagementCDK
npm install

# Update snapshot tests if needed
npm run test -- -u

# Build and synthesize CloudFormation
npm run build
cdk synth

# Bootstrap CDK if needed (1st time only)
cdk bootstrap

# Deploy stacks in order
cdk deploy DeviceManagementBootstrapStack
cdk deploy DeviceManagementWorkflowStack
cdk deploy DeviceManagementServiceStack
```

### 2. Video Logistics

```bash
cd video-logistics-cdk/VideoAnalyticsVideoLogisticsCDK
npm install

# Update snapshot tests if needed
npm run test -- -u

# Build and synthesize CloudFormation
npm run build
cdk synth

# Bootstrap CDK if needed (1st time only)
cdk bootstrap

# Deploy stacks in order
cdk deploy VideoLogisticsBootstrapStack
cdk deploy VideoLogisticsTimelineStack
cdk deploy VideoLogisticsOpensearchStack
cdk deploy VideoLogisticsBulkInferenceStack
cdk deploy VideoLogisticsWorkflowStack
cdk deploy VideoLogisticsServiceStack
```

## Environment Variables

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
   - Region: "us-west-2"
   - Account: "YOUR_DEFAULT_ACCOUNT"

## Note

- CDK bootstrap is only required once per account/region combination
- Make sure to build the common constructs before deploying either component
- Follow the stack deployment order as specified above
- For detailed deployment validation steps, see [Deployment Validation](../README.md#deployment-validation-required) in the main README
