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

This deployment has been tested on macOS and Linux operating systems. The following prerequisites are required:

1. **Development Tools**
   - Java 17 or higher
     ```bash
     # For Ubuntu/Debian
     sudo apt update
     sudo apt install openjdk-17-jdk
     
     # For Amazon Linux/RHEL/CentOS
     sudo yum install java-17-amazon-corretto
     
     # Verify installation
     java -version
     ```
   
   - Node.js and npm
     ```bash
     # For Ubuntu/Debian
     curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
     sudo apt install nodejs
     
     # For Amazon Linux/RHEL/CentOS
     curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -
     sudo yum install nodejs
     
     # Verify installation
     node --version
     npm --version
     ```
   
   - Rust toolchain
     ```bash
     # Install Rust using rustup
     curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
     source $HOME/.cargo/env
     
     # Verify installation
     rustc --version
     cargo --version
     ```
   
   - Gradle 8.10 (included via Gradle Wrapper)
     ```bash
     # No manual installation needed as we use Gradle Wrapper
     # Verify wrapper works
     ./gradlew --version
     ```
   
   - Git
     ```bash
     # For Ubuntu/Debian
     sudo apt install git
     
     # For Amazon Linux/RHEL/CentOS
     sudo yum install git
     
     # Verify installation
     git --version
     ```

   - Smithy CLI
     ```bash
     # Install Smithy CLI using npm
     npm install -g @smithy/cli
     
     # Verify installation
     smithy --version
     
     # Alternative: If using the JAR directly
     # Download the latest smithy-cli jar from Maven Central
     wget https://repo1.maven.org/maven2/software/amazon/smithy/smithy-cli/1.41.0/smithy-cli-1.41.0.jar
     
     # Create an alias for easy use
     echo 'alias smithy="java -jar /path/to/smithy-cli-1.41.0.jar"' >> ~/.bashrc
     source ~/.bashrc
     ```

2. **AWS Tools**
   - AWS CLI
     ```bash
     # For Ubuntu/Debian
     curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
     sudo apt install unzip
     unzip awscliv2.zip
     sudo ./aws/install
     
     # For Amazon Linux/RHEL/CentOS
     curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
     sudo yum install unzip
     unzip awscliv2.zip
     sudo ./aws/install
     
     # Verify installation
     aws --version
     
     # Configure AWS CLI
     aws configure
     ```
   
   - AWS CDK CLI (install using: `npm install -g aws-cdk`)
     ```bash
     # Install AWS CDK CLI
     npm install -g aws-cdk
     
     # Verify installation
     cdk --version
     ```
   
   - AWS Systems Manager (SSM) access if testing with cameras connected via SSM

### AWS CDK Bootstrap

If you haven't used AWS CDK before in your account, run the following command:
```bash
cdk bootstrap aws://ACCOUNT-NUMBER/REGION
```

## Deployment Steps (required)

Follow these steps in sequence to deploy the solution:

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd guidance-for-video-analytics-infrastructure-on-aws
   ```

2. **Build Edge Process (Rust)**:
   ```bash
   cd source/edge
   # For local architecture
   cargo build --release
   
   # For ARM devices:
   cargo install cross --git https://github.com/cross-rs/cross
   cross build --target armv7-unknown-linux-gnueabihf --release
   cd ../..
   ```

3. **Build Model and Generate API Specifications**:
   ```bash
   # Build Smithy Model
   cd source/video-logistics/VideoAnalyticsVideoLogisticsModel
   smithy build
   cd ../../..
   
   # Generate Java Client Code
   cd source/video-logistics/VideoAnalyticsVideoLogisticsJavaClient
   ./gradlew openApiGenerate
   cd ../../..
   ```

4. **Build Control Plane (Java)**:
   ```bash
   cd source/video-logistics/VideoAnalyticsVideoLogisticsControlPlane
   ./gradlew clean build
   # This will automatically copy the Lambda resource JAR to assets/lambda-built/video-logistics-assets/
   cd ../../..
   ```

5. **Set up CDK Development Environment**:
   ```bash
   cd deployment
   
   # Install dependencies for all workspaces
   npm install
   
   # Build all CDK packages
   npm run build --workspaces
   ```

6. **Configure Deployment Environment**:
   ```bash
   # Set your AWS account and region
   export CDK_DEPLOY_ACCOUNT=your_aws_account_id
   export CDK_DEPLOY_REGION=your_aws_region
   
   # Ensure you have valid AWS credentials
   aws sts get-caller-identity
   ```

7. **Deploy CDK Stacks**:
   ```bash
   cd video-logistics-cdk/VideoAnalyticsVideoLogisticsCDK
   
   # First, synthesize the CloudFormation template to verify
   cdk synth
   
   # Run tests and update snapshots if needed
   npm test
   # If you made intentional changes to the infrastructure:
   npm run test:update
   
   # Deploy all stacks
   cdk deploy --all
   
   # Or deploy specific stacks
   cdk deploy VideoLogisticsWorkflowStack
   ```

The deployment will create the following resources:
- API Gateway endpoints for video logistics
- Lambda functions for control plane operations
- Required IAM roles and policies
- Necessary S3 buckets and DynamoDB tables
- Video Analytics infrastructure components

### Deployment Validation

After deployment, verify the following:

1. Check the AWS CloudFormation console to ensure all stacks are deployed successfully
2. Verify the API Gateway endpoints are created and accessible
3. Confirm Lambda functions are properly deployed with the correct runtime
4. Check that all IAM roles and policies are created with appropriate permissions

### Cleanup

To remove all deployed resources, take video logistics for example:
```bash
cd deployment/video-logistics-cdk/VideoAnalyticsVideoLogisticsCDK
cdk destroy --all
```

## Deployment Validation  (required)

<Provide steps to validate a successful deployment, such as terminal output, verifying that the resource is created, status of the CloudFormation template, etc.>


**Examples:**

* Open CloudFormation console and verify the status of the template with the name starting with xxxxxx.
* If deployment is successful, you should see an active database instance with the name starting with <xxxxx> in        the RDS console.
*  Run the following CLI command to validate the deployment: ```aws cloudformation describe xxxxxxxxxxxxx```



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
