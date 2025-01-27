# Development Guide - Assets Directory

This directory (`guidance-for-video-analytics-infrastructure-on-aws/assets`) contains compiled artifacts, model specs, and resources that are used by the infrastructure deployment code.

## Directory Structure Overview

```


guidance-for-video-analytics-infrastructure-on-aws/
.
├── assets                   # Compiled artifacts and resources(Model Specs)
│   ├── DEVELOPMENT.md
│   ├── lambda-built         # Compiled Lambda function packages
│   └── model                # Model Specs
├── deployment               # Infrastructure code (CDK)
│   ├── common-constructs
│   ├── device-management-cdk
│   ├── node_modules
│   ├── package.json
│   ├── tsconfig.json
│   └── video-logistics-cdk
└── source                   # Application Source code
    ├── device-management
    ├── edge
    ├── models
    ├── video-logistics
    └── workflow-handler
```

## Purpose

The `assets` directory serves as the destination for compiled artifacts (such as built side loaded JAR files) and other resources that are referenced by the infrastructure code during deployment.

- Compiled Lambda function packages
- Pre-built application artifacts
- Configuration files
- Other deployment resources

## Usage

When deploying infrastructure using AWS CDK, the assets in this directory are referenced using constructs like `code.fromAsset()`. For example:

```typescript
new lambda.Function(this, 'EJFunction', {
  runtime: lambda.Runtime.JAVA_17,
  code: lambda.Code.fromAsset('relative/path/to/jar/file/from/deployment/directory'),
  handler: 'your.handler.path'
});
```

## Workflow

1. Source code is developed in the `/source` directory
2. Build processes compile and package the code
3. Compiled application artifacts are placed in this `/assets` directory
4. Infrastructure code in `/deployment` references these assets during deployment
