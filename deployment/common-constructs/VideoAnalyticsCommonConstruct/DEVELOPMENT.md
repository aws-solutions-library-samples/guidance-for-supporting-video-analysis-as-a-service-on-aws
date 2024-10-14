# Video Analytics Infrastructure Common Construct

This package provides common CDK constructs, types, and utilities for use with the Guidance on Video Analytics Infrastructure. It is designed to be consumed by other CDK packages implementing video analytics related workflows.

## Overview

This common construct defines reusable components and utilities that facilitate the creation of video analytics infrastructure using AWS CDK. It includes implementations for asynchronous workflows, DynamoDB table creation, and other shared functionalities.

## Key Features

1. **Asynchronous Workflow**: The package implements an abstract `VideoAnalyticsAsyncWorkflowResource` class and a concrete `Workflow` class, which together provide a framework for creating asynchronous video processing workflows.

2. **DynamoDB Utilities**: Includes functions for creating DynamoDB tables and Global Secondary Indexes (GSIs) with customizable configurations.

3. **Common Constants**: Defines shared constants used across video analytics constructs.

## Asynchronous Workflow

The asynchronous workflow implementation is centered around the `VideoAnalyticsAsyncWorkflowResource` and `Workflow` classes. Here's a detailed overview of how the workflow processes each API call:

1. **API Call**: The customer initiates the process by making an API call to the service.

2. **API Gateway**: Amazon API Gateway receives the incoming request.

3. **API Lambda**: API Gateway triggers the corresponding API Lambda function.

4. **DynamoDB Write**: The API Lambda function stores the job details in a DynamoDB table.

5. **DynamoDB Stream**: Upon writing the job entry, DynamoDB generates a change log in its stream.

6. **Stream Processor Lambda**: The DynamoDB stream invokes a dedicated Stream Processor Lambda function.

7. **Step Functions Invocation**: The Stream Processor Lambda function initiates the appropriate Step Functions state machine.

This workflow architecture ensures:

- Immediate response to the client (asynchronous processing)
- Reliable job tracking and state management
- Scalable and event-driven processing
- Separation of concerns between job ingestion and processing

The `VideoAnalyticsAsyncWorkflowResource` class provides an abstract base for implementing this workflow:

- `createStepFunction()`: Defines the Step Functions state machine for your specific workflow.
- `createWorkflow()`: Sets up the DynamoDB table, stream, and associated Lambda function.
- `postWorkflowCreationCallback()`: Allows for additional setup after the main workflow components are created.

The `Workflow` class handles the creation of:

- DynamoDB table with stream enabled
- Stream Processor Lambda function
- Necessary IAM roles and permissions
- Dead Letter Queue for error handling

## Usage

To use this construct in your CDK project:

1. Install the package as a dependency.
2. Import the necessary classes and utilities.
3. Extend the `VideoAnalyticsAsyncWorkflowResource` class to create your custom asyncronous Workflow resource.
4. Use the provided utilities to create DynamoDB tables and other resources as needed.

## Consuming as a Dependency for Video Logistics Deployment CDK

To consume this package as a dependency in another CDK package within the same project structure, follow these steps:

1. Ensure that the `VideoAnalyticsCommonConstruct` package is located in the `common-constructs` directory relative to your consuming package.

2. Your consuming package should be located in a subdirectory under the `deployment` directory, at the same level as the `common-constructs` directory.

3. In the `package.json` file of your consuming package, add the following dependency using a relative path:

   ```json
   {
     "dependencies": {
       "video_analytics_common_construct": "file:../../common-constructs/VideoAnalyticsCommonConstruct"
     }
   }
   ```

   This tells npm to use the relative path to resolve the package, rather than looking for it in the npm registry.

4. Run `npm install` in your consuming package to install the dependency (The consuming package's build process should include npm install so you don't have to run it manually).

5. You can now import and use the constructs and utilities from the `VideoAnalyticsCommonConstruct` package in your TypeScript code:

   ```typescript
   import { VideoAnalyticsAsyncWorkflowResource, Workflow } from 'video_analytics_common_construct';
   ```

Remember to rebuild the `VideoAnalyticsCommonConstruct` package whenever you make changes to it, and then run `npm install` again in your consuming package to pick up the changes.

Note: Ensure that your project's build and deployment scripts are set up to handle local dependencies correctly, especially if you're using a monorepo structure or deploying in different environments.

## Building the Package

To build the package, follow these steps:

1. Ensure you have Node.js and npm installed on your system.
2. Clone the repository and navigate to the package directory.
3. Run the following command to build the package:

   ```
   npm run build
   ```

   This command will clean the project, install dependencies, run linting, compile TypeScript, and run tests.

## Testing

To run the tests for this package, use the following command:

```
npm test
```

## Linting

To run the linter for this package, use the following command:

```
npm run lint
```

If you haven't set up ESLint yet, you may need to initialize the ESLint configuration:
 `npm init @eslint/config`

Example .eslintrc.js file:
```
module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  plugins: ['@typescript-eslint'],
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'prettier',
  ],
  env: {
    node: true,
    jest: true,
  },
  rules: {
    // Add any custom rules here
  },
};
```
