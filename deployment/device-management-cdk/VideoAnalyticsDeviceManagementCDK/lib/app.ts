import { App } from "aws-cdk-lib";
import { ServiceStack } from "./stacks/serviceStack/serviceStack";
import { WorkflowStack } from "./stacks/workflowStacks/workflowStack";
import { AWSRegion } from "video_analytics_common_construct";
import { BootstrapStack } from "./stacks/bootstrapStack/bootstrapStack";

const app = new App();

// Get the account and region from environment variables or use defaults
const account =
  process.env.CDK_DEPLOY_ACCOUNT ||
  process.env.CDK_DEFAULT_ACCOUNT ||
  "YOUR_DEFAULT_ACCOUNT";
const region = (process.env.CDK_DEPLOY_REGION ||
  process.env.CDK_DEFAULT_REGION ||
  "us-west-2") as AWSRegion;


const bootstrapStack = new BootstrapStack(app, "DeviceManagementBootstrapStack", {
  env: {
    account: account,
    region: region,
  },
  region: region,
  account: account,
});

// Create workflow stack with dependency on bootstrap stack
const workflowStack = new WorkflowStack(app, "DeviceManagementWorkflowStack", {
  env: {
    account: account,
    region: region,
  },
  resources: [], // Resources will be populated by the workflowStack.ts
  region: region,
  account: account,
});
workflowStack.addDependency(bootstrapStack);

// Create service stack with dependency on bootstrap stack
const serviceStack = new ServiceStack(app, "DeviceManagementServiceStack", {
  env: {
    account: account,
    region: region,
  },
  region: region,
  account: account,
});
serviceStack.addDependency(bootstrapStack);

app.synth();
