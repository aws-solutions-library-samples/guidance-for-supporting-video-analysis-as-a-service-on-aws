import { App } from "aws-cdk-lib";
import { ServiceStack } from "./stacks/serviceStack/serviceStack";
import { WorkflowStack } from "./stacks/workflowStacks/workflowStack";
import { AWSRegion } from "video_analytics_common_construct";
import { DeviceManagementBootstrapStack } from "./stacks/bootstrapStack/deviceManagementBootstrapStack";

const app = new App();

// Get the account and region from environment variables or use defaults
const account =
  process.env.CDK_DEPLOY_ACCOUNT ||
  process.env.CDK_DEFAULT_ACCOUNT ||
  "YOUR_DEFAULT_ACCOUNT";
const region = (process.env.CDK_DEPLOY_REGION ||
  process.env.CDK_DEFAULT_REGION ||
  "us-east-1") as AWSRegion;

// new WorkflowStack(app, "DeviceManagementWorkflowStack", {
//   env: {
//     account: account,
//     region: region,
//   },
//   resources: [], // this will be populated by the workflowStack.ts since resources are created in the workflowStack.ts
//   region: region,
//   account: account,
// });

new ServiceStack(app, "DeviceManagementServiceStack", {
  env: {
    account: account,
    region: region,
  },
  region: region,
  account: account,
});

new DeviceManagementBootstrapStack(app, "DeviceManagementBootstrapStack", {
  env: {
    account: account,
    region: region,
  },
  region: region,
  account: account,
});

app.synth();
