# Video Analytics Application Source Code Architecture and Organization

## Overview

This directory contains the application source code that implements the Video Analytics Infrastructure on AWS. The implementation follows the architecture defined in the [deployment README](../deployment/README.md), which provides a detailed view of the infrastructure components and their interactions through a sequence diagram of the async workflow pattern.

This source code implements a scalable video analytics infrastructure with a microservices architecture. The system consists of two main services (Device Management and Video Logistics) that work together to provide comprehensive video analytics capabilities through AWS managed services:

1. **Device Management Service**
   - Handles device registration and lifecycle management
   - Manages device configuration and state through device shadows
   - Provides secure device authentication and authorization

2. **Video Logistics Service**
   - Manages video ingestion through Kinesis Video Streams (KVS)
   - Processes video streams for analytics and ML inference
   - Handles video timeline management and playback
   - Supports both live streaming and historical video access
   - Integrates with S3 for media storage and retrieval

The architecture follows these key design principles:
- API-first development using Smithy for contract definition
- Asynchronous operations with DynamoDB stream orchestration (see workflow diagram in deployment README)
- Separation of API handlers (activity) from business logic (workflow)
- Microservices communication through well-defined interfaces

## Directory Structure

```
source/
├── models/                    # Smithy API models
│   ├── commons/              # Common model definitions
│   ├── device-management/    # Device Management API models
│   └── video-logistics/      # Video Logistics API models
├── device-management/        # Device Management service components
│   ├── VideoAnalyticsDeviceManagementControlPlane/
│   │   ├── src/
│   │   │   └── main/java/com/amazonaws/videoanalytics/devicemanagement/
│   │   │       ├── activity/     # API path proxy Lambda handlers
│   │   │       ├── workflow/     # Async workflow Lambda functions
│   │   │       ├── schema/       # DynamoDB stream workflow table schemas
│   │   │       ├── config/       # Application configuration
│   │   │       ├── dao/         # Data access objects
│   │   │       └── utils/       # Common utilities
│   │   ├── build.gradle     # Build configuration
│   │   └── DEVELOPMENT.md   # Development guidelines
│   └── VideoAnalyticsDeviceManagementJavaClient/
│       ├── src/             # Generated client code
│       └── build.gradle     # Build configuration
├── video-logistics/          # Video Logistics service components
│   ├── VideoAnalyticsVideoLogisticsControlPlane/
│   │   ├── src/
│   │   │   └── main/java/com/amazonaws/videoanalytics/videologistics/
│   │   │       ├── activity/     # API path proxy Lambda handlers
│   │   │       ├── workflow/     # Async workflow Lambda functions
│   │   │       ├── schema/       # DynamoDB stream workflow table schemas
│   │   │       ├── timeline/     # Video timeline operations and processing
│   │   │       ├── inference/    # ML inference processing components
│   │   │       ├── client/       # Internal service clients and utilities abstractions
│   │   │       ├── config/       # Application configuration
│   │   │       ├── dao/         # Data access objects
│   │   │       ├── validator/    # Input validation
│   │   │       └── utils/       # Common utilities
│   │   ├── build.gradle     # Build configuration
│   │   └── DEVELOPMENT.md   # Development guidelines
│   └── VideoAnalyticsVideoLogisticsJavaClient/
│       ├── src/             # Generated client code
│       └── build.gradle     # Build configuration
├── workflow-handler/         # Common Async workflow handling components
│   └── VideoAnalyticsWorkflowHandler/
│       ├── src/             # Workflow implementation
│       ├── build.gradle     # Build configuration
│       └── DEVELOPMENT.md   # Development guidelines
└── edge/                     # Edge device components and utilities
    ├── crates/              # Rust crate modules
    ├── Cargo.toml           # Rust project configuration
    └── DEVELOPMENT.md       # Development guidelines
```

## Component Overview

### Models
The `models` directory contains Smithy API model definitions that serve as the source of truth for Video Analytics service interfaces. These models are used to generate client SDKs and service implementations for Video Analytics Operations.

### Device Management
The Device Management component handles device registration, configuration, and lifecycle management:
- **Control Plane**: Implementation of device management service APIs and business logic
  - `activity/`: Lambda handlers for API Gateway paths defined in the API contract
  - `workflow/`: Lambda functions for DynamoDB stream orchestrated async workflows
  - `schema/`: DynamoDB table schemas for workflow state management
  - Other supporting components for configuration, data access, and utilities

### Video Logistics
The Video Logistics component manages video ingestion, processing, and analytics:
- **Control Plane**: Implementation of video processing service APIs and business logic
  - `activity/`: Lambda handlers for API Gateway paths defined in the API contract
  - `workflow/`: Lambda functions for DynamoDB stream orchestrated async workflows
  - `schema/`: DynamoDB table schemas for workflow state management
  - `timeline/`: Components for video timeline operations and processing
  - `inference/`: ML inference processing and management
  - `client/`: Internal service clients for AWS services
  - Supporting components for validation, configuration, and data access

### Workflow Handler
Common workflow handling components used across services for asynchronous task processing and orchestration.

### Edge
Edge components for video capture, processing, and streaming from edge devices to AWS services.

## API Actions

### Prerequisites

Before using the APIs, ensure you have:

1. **Edge Device Setup**
   - An ONVIF-compatible camera connected to an edge device (or via SSM host)
   - Camera accessible via ONVIF (port 80) and RTSP (port 554)
   - Edge process running with proper configuration
   - See [edge/DEVELOPMENT.md](edge/DEVELOPMENT.md) for detailed edge setup instructions

2. **Device Registration**
   - Device must be registered using `POST /start-create-device/{deviceId}` before using other operations
   - Wait for successful registration status via `POST /get-create-device-status/{jobId}`
   - Device registration is required for both Device Management and Video Logistics operations

### Device Management API

The Device Management API provides endpoints for device registration and management:

#### Device Registration
- `POST /start-create-device/{deviceId}` - Start device registration process
- `POST /get-create-device-status/{jobId}` - Get status of device registration

**Note:** Device registration through `start-create-device` is a prerequisite for all other operations. You must successfully register a device before you can use any other endpoints with that device ID.

#### Device Operations
- `POST /get-device/{deviceId}` - Get device information
- `POST /get-device-shadow/{deviceId}` - Get device shadow (current state)
- `POST /update-device-shadow/{deviceId}` - Update device shadow (desired state)

### Video Logistics API

The Video Logistics API handles video streaming, playback, and analytics:

#### Device Registration
- `POST /start-register-device/{deviceId}` - Start device registration for video logistics
- `POST /get-register-device-status/{jobId}` - Get status of video logistics registration

#### Livestreaming
- `POST /create-livestream-session` - Create a new livestream session

#### Playback
- `POST /create-playback-session` - Create a new playback session

#### Video Timeline
- `POST /list-video-timelines` - List available video timelines
- `POST /list-detailed-video-timeline` - Get detailed video timeline information
- `POST /put-video-timeline` - Update video timeline information

#### Snapshots
- `POST /create-snapshot-upload-path` - Generate pre-signed URL for snapshot upload

#### Media Processing
- `POST /import-media-object` - Import media for processing and inference

All endpoints use POST method as they are integrated with AWS Lambda through API Gateway proxy integration. The endpoints follow an async workflow pattern where applicable, with separate endpoints for starting operations and checking their status. The API also provides endpoints for getting the status of completed or failed operations.

## Building the Project

The build process must be executed in the following order:

1. **Build Smithy Models**
   ```bash
   # Build common models
   cd models/commons
   smithy build

   # Build device management models
   cd ../device-management
   smithy build

   # Build video logistics models
   cd ../video-logistics
   smithy build
   ```

2. **Generate API Clients**
   ```bash
   # Generate Device Management Client
   cd ../../device-management/VideoAnalyticsDeviceManagementJavaClient
   ./gradlew openApiGenerate

   # Generate Video Logistics Client
   cd ../../video-logistics/VideoAnalyticsVideoLogisticsJavaClient
   ./gradlew openApiGenerate
   ```

3. **Build Control Plane Services**
   The control plane packages (Device Management and Video Logistics) follow a similar build process using Gradle:

   1. **Project Setup**
      - Uses Java 17 for compilation
      - Includes Lombok for reducing boilerplate code
      - Configures source sets for main code and generated client code

   2. **Dependencies**
      - AWS SDK BOM (Bill of Materials) for version management
      - AWS services: DynamoDB, IoT, Step Functions, Kinesis Video Streams (KVS), S3, etc.
      - Generated client code from Smithy models
      - Testing frameworks and utilities

   3. **Build Steps**
      - Compiles Java source code
      - Processes Lombok annotations
      - Packages dependencies using Shadow plugin for fat JAR creation
      - Runs tests and generates coverage reports
      - Copies built JAR to Lambda deployment assets

   4. **Build Output**
      - Creates a fat JAR containing all dependencies
      - JAR is used for Lambda function deployment
      - Generated artifacts are placed in centralized assets location for CDK deployment

   To build the control plane packages:
   ```bash
   # Build Device Management Control Plane
   cd ../../device-management/VideoAnalyticsDeviceManagementControlPlane
   ./gradlew build

   # Build Video Logistics Control Plane
   cd ../../video-logistics/VideoAnalyticsVideoLogisticsControlPlane
   ./gradlew build
   ```

4. **Build Workflow Handler**
   ```bash
   cd ../../workflow-handler/VideoAnalyticsWorkflowHandler
   ./gradlew build
   ```

5. **Build Edge Components**
   ```bash
   cd ../../edge
   cargo build
   ```
   For detailed edge component setup, configuration, and running instructions, refer to [edge/DEVELOPMENT.md](edge/DEVELOPMENT.md).

Note: Each component requires specific environment setup. Please refer to the individual `DEVELOPMENT.md` files in each component's directory for detailed requirements and configuration instructions.

## Development Guidelines

Each component contains its own `DEVELOPMENT.md` with specific guidelines:

- [Edge Development Guide](edge/DEVELOPMENT.md)
- [Device Management Development Guide](device-management/VideoAnalyticsDeviceManagementControlPlane/DEVELOPMENT.md)
- [Video Logistics Development Guide](video-logistics/VideoAnalyticsVideoLogisticsControlPlane/DEVELOPMENT.md)
- [Workflow Handler Development Guide](workflow-handler/VideoAnalyticsWorkflowHandler/DEVELOPMENT.md)

Please refer to these guides for component-specific development instructions, testing procedures, and best practices.
