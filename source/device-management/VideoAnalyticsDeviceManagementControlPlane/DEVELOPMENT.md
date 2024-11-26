# Development Guide

## Build Requirements
- Java 17 or higher
- Gradle (optional, as we use Gradle Wrapper)

## Gradle Wrapper(fixed version with 8.10)
This project uses the Gradle Wrapper (gradlew) to ensure consistent builds across different environments and developers. The wrapper files are committed to the repository and include:

- `gradle/wrapper/gradle-wrapper.jar`: The wrapper JAR file
- `gradle/wrapper/gradle-wrapper.properties`: Wrapper configuration file
- `gradlew`: Unix shell script
- `gradlew.bat`: Windows batch file

### gradle-wrapper.properties
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- `distributionUrl`: Specifies Gradle version (8.10) used for the build
- `distributionBase` and `zipStoreBase`: Define base location for Gradle distribution
- `distributionPath` and `zipStorePath`: Define relative paths for downloaded Gradle distribution

### Why We Commit the Wrapper
- Ensures all developers use the same Gradle version
- No need to install Gradle locally
- Guarantees build reproducibility
- Simplifies CI/CD setup
- Follows Gradle best practices

When cloning the repository, you can immediately build the project using:
```bash
./gradlew build
```

No additional Gradle installation is required.

## Building the Project

1. Generate the Gradle Wrapper (if not already present):
```bash
gradle wrapper
```

2. Build the project using the Gradle Wrapper:
```bash
./gradlew build
```

This will:
- Compile the code
- Run tests
- Generate a JaCoCo test coverage report
- Create a fat JAR with all dependencies using Shadow plugin

The output artifacts will be located in:
- Fat JAR (with all dependencies) compiled to location: `build/libs/VideoAnalyticsDeviceManagementControlPlane-1.0-<VERSION>.jar`
- Test reports: `build/reports/tests`
- JaCoCo coverage report: `build/jacocoHtml`

## Additional Gradle Tasks

Clean the build and Gradle cache:
```bash
./gradlew cleanBuildCache
```

Run tests only:
```bash
./gradlew test
```

Generate test coverage report:
```bash
./gradlew jacocoTestReport
```

## Build Output
When you run `./gradlew build`, the following will happen automatically:
1. Package application code compilation
2. Unite Test execution
3. JaCoCo test coverage report generation
4. Shadow JAR creation for application code + dependencies
5. JAR deployment to `assets/lambda-built/device-management-assets` (transformJarToDeploymentAsset)

## Video Logistics Integration
Device Management workflows interact with Video Logistics endpoints through API Gateway. This integration requires:

### Environment Variables(Defined in CDK deployment stack)
- `VIDEO_LOGISTICS_API_NAME`: Name of the Video Logistics API Gateway
- `AWS_REGION`: AWS region where the API Gateway is deployed

### Endpoint Resolution
The `ApigService` class dynamically resolves the Video Logistics API endpoint by:
1. Looking up the API Gateway ID using the `VIDEO_LOGISTICS_API_NAME`
2. Constructing the endpoint URL using the format:
   ```
   https://{api-id}.execute-api.{region}.amazonaws.com/prod
   ```

### Workflow Integration
Device Management handlers (e.g., `CreateKVSStreamHandler`) use `ApigService` to:
- Dynamically resolve the Video Logistics endpoint
- Make HTTP requests to Video Logistics APIs
- Handle responses and update workflow state

Example workflow integration:
```java
// Resolve endpoint and invoke Video Logistics API
HttpExecuteResponse response = apigService.invokeStartVlRegisterDevice(
    deviceId,    // Device to register
    headers,     // Optional HTTP headers
    requestBody  // Optional request payload
);
```

## Notes
- The project uses Shadow plugin to create a fat JAR that includes all dependencies for the lambda function Asset consumption
- The build automatically includes source files from `../VideoAnalyticsDeviceManagementJavaClient/generated-src`
