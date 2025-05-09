# Development Setup

## Workspace Configuration

To ensure consistent dependency management across the device management CDK and common constructs, we use a workspace configuration. This setup is crucial because these projects have dependencies on each other and share common libraries like `aws-cdk-lib`.

### Directory Structure

The workspace is structured as follows:

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
└── xxxxxx-cdk/
    └── XXXXXCDK/
        ├── package.json
        ├── tsconfig.json
        ├── lib/
        │   └── ... (TypeScript CDK)
        ├── node_modules/
        │   └── ... (package-specific dependencies)
        ├── cdk.json
        └── .gitignore
```

### Steps to Set Up Workspaces

1. In the `deployment` directory, create or update the `package.json` file with the following content:

   ```json
   {
     "private": true,
     "workspaces": [
       "common-constructs/VideoAnalyticsCommonConstruct",
       "device-management-cdk/VideoAnalyticsDeviceManagementCDK"
     ],
     "devDependencies": {
       "@types/node": "^22.7.9"
     }
   }
   ```

2. Create a `tsconfig.json` file in the `deployment` directory with the following content:

   ```json
   {
     "compilerOptions": {
       "composite": true
     },
     "files": [],
     "references": [
       { "path": "./common-constructs/VideoAnalyticsCommonConstruct" },
       { "path": "./device-management-cdk/VideoAnalyticsDeviceManagementCDK" }
     ]
   }
   ```

3. Update the `tsconfig.json` file in `common-constructs/VideoAnalyticsCommonConstruct`:

   ```json
   {
     "compilerOptions": {
       // ... other options ...
       "outDir": "./dist",
       "rootDir": "./lib",
       "composite": true,
       "declarationMap": true
     },
     "include": [
       "lib/**/*.ts"
     ],
     "exclude": [
       "node_modules",
       "dist"
     ]
   }
   ```

4. Update the `tsconfig.json` file in `device-management-cdk/VideoAnalyticsDeviceManagementCDK`:

   ```json
   {
     "compilerOptions": {
       // ... other options ...
       "outDir": "./dist",
       "rootDir": "./lib",
       "typeRoots": [
         "../../../node_modules/@types",
         "./node_modules/@types"
       ],
       "types": ["node"],
       "baseUrl": "./",
       "paths": {
         "*": [
           "../../../node_modules/*",
           "node_modules/*"
         ],
         "video_analytics_common_construct": ["../../common-constructs/VideoAnalyticsCommonConstruct/lib"]
       },
       "composite": true
     },
     "exclude": [
       "node_modules",
       "cdk.out",
       "dist"
     ],
     "include": [
       "lib/**/*.ts"
     ]
   }
   ```

5. Update the `package.json` file in `common-constructs/VideoAnalyticsCommonConstruct`:

   ```json
   {
     // ... other fields ...
     "main": "dist/index.js",
     "types": "dist/index.d.ts",
     "files": [
       "dist/**/*"
     ],
     "scripts": {
       "clean": "rm -rf dist bin node_modules package-lock.json",
       "build": "npm run clean && npm install && tsc && npm run test",
       // ... other scripts ...
     },
     // ... dependencies and devDependencies ...
   }
   ```

6. Update the `package.json` file in `device-management-cdk/VideoAnalyticsDeviceManagementCDK`:

   ```json
   {
     // ... other fields ...
     "scripts": {
       "clean": "rm -rf dist node_modules package-lock.json",
       "build": "npm install && tsc",
       // ... other scripts ...
     },
     "dependencies": {
       // ... other dependencies ...
       "video_analytics_common_construct": "file:../../common-constructs/VideoAnalyticsCommonConstruct"
     },
     // ... devDependencies ...
   }
   ```

7. From the `deployment` directory, run:

   ```bash
   npm install
   ```

### Adding New CDK Packages

When adding a new CDK package:

1. Create a new directory for your CDK package within the `deployment` directory.
2. Initialize your new CDK project in this directory.
3. Add the new package to the `workspaces` array in the root `package.json` file.
4. Add a reference to the new package in the root `tsconfig.json` file.
5. Update the new package's `tsconfig.json` and `package.json` files as shown in steps 4 and 6 above.
6. Run `npm install` from the `deployment` directory to update the workspace.

## Building the Project

### Build Order Requirements

Before building the CDK stacks, you must build the components in this specific order:

1. **Smithy Model**
   ```bash
   cd source/device-management/VideoAnalyticsDeviceManagementModel
   smithy build
   ```
   This generates the OpenAPI specification from the Smithy model.

2. **Java Client**
   ```bash
   cd source/device-management/VideoAnalyticsDeviceManagementJavaClient
   ./gradlew openApiGenerate
   ```
   This generates the Java client code from the OpenAPI spec.

3. **Control Plane Application**
   ```bash
   cd source/device-management/VideoAnalyticsDeviceManagementControlPlane
   ./gradlew clean build
   ```
   This builds the main application JAR and copies it to `assets/lambda-built/device-management-assets/` for CDK deployment.

> **Important**: The Control Plane build task includes a `transformJarToDeploymentAsset` step that copies the built JAR to the assets directory used by the CDK deployment. This directory path is referenced in `deployment/device-management-cdk/VideoAnalyticsDeviceManagementCDK/lib/stacks/const.ts`.

### Building the Entire Project

To build all components:

1. Navigate to the `deployment` directory
2. Run:
   ```bash
   npm run build --workspaces
   ```

To build individual packages, navigate to their directories and run `npm run build`.

### Build Order

The recommended build order is:

1. Common Constructs
2. Device Management CDK
3. Any other CDK packages

## Rationale

This setup ensures:

1. Shared dependencies across projects
2. Efficient management of inter-project dependencies
3. Consistency in dependency versions
4. Efficient building process
5. Reduced disk space usage through shared dependencies

By following this setup, we maintain a consistent and efficient development environment across all Video Analytics infrastructure projects.

## Snapshot Testing

We use Jest snapshot testing for our CDK stacks to ensure that any changes to our infrastructure are intentional and reviewed.

### Running Snapshot Tests

To run the snapshot tests:

1. Navigate to the `deployment/device-management-cdk/VideoAnalyticsDeviceManagementCDK` directory.
2. Run the following command:

   ```bash
   npm test
   ```

### Updating Snapshots

When you make intentional changes to your CDK stacks, you'll need to update the snapshots. To do this:

1. Navigate to the `deployment/device-management-cdk/VideoAnalyticsDeviceManagementCDK` directory.
2. Run the following command:

   ```bash
   npm run test:update
   ```

   This command runs the tests and updates any changed snapshots.

3. Review the changes in the updated snapshot files (located in the `__snapshots__` directory) to ensure they match your expectations.
4. Commit the updated snapshot files along with your code changes.

### Best Practices

- Always review snapshot changes carefully before committing them.
- If a snapshot test fails unexpectedly, investigate the cause before updating the snapshot.
- Use snapshot testing in combination with other testing methods for comprehensive coverage.
- Keep snapshots focused and manageable. If they become too large, consider breaking them into smaller, more specific tests.

### Troubleshooting

If you encounter issues with snapshot tests:

1. Ensure your local environment is up-to-date (`npm install`).
2. Check that all dependencies are correctly installed and configured.
3. Verify that your CDK stack is correctly implemented and creating resources as expected.
4. If snapshots are consistently empty, review your stack implementation and consider adding specific resource assertions to your tests.
5. Always review the changes in the updated snapshot files (located in the `__snapshots__` directory) to ensure they match your expectations, and update the snapshot with command: `npm run test:update` if you have made intentional changes to the snapshot.

## Helpful Build Commands

### Common Commands

1. **Update Tests, Build, and Synthesize CDK**
   ```bash
   npm run test -- -u && npm run build && cdk synth
   ```
   This command chain will:
   - Update Jest snapshots (`-u` flag)
   - Build all workspace packages
   - Synthesize the CDK stack to CloudFormation

2. **Clean and Build Java Components**

   # Clean and build Control Plane
   cd ../VideoAnalyticsDeviceManagementControlPlane
   ./gradlew clean build
   ```

3. **Run Tests Only**
   ```bash
   # Run all workspace tests
   npm test --workspaces

   # Run tests for a specific package
   cd deployment/device-management-cdk/VideoAnalyticsDeviceManagementCDK
   npm test
   ```

4. **Update Snapshots Only**
   ```bash
   npm run test -- -u
   ```

5. **Build and Deploy**
   ```bash
   # Build and deploy to default environment
   npm run build && cdk deploy

   # Build and deploy specific stack
   npm run build && cdk deploy StackName
   ```

## Development Notes

### API GW IAM Auth
Smithy does not support the API GW `x-amazon-apigateway-auth` trait, so we have to manually add it to the OpenAPI json spec file in DM and VL CDKs to configure this field.

