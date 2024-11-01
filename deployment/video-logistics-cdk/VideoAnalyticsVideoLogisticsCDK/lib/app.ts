import { App } from "aws-cdk-lib";
import { 
    ForwardingRulesStack,
    SchedulerStack,
    TimelineStack,
    VideoExportStack,
    WorkflowStack
 } from "./stacks";
import { AWSRegion } from "video_analytics_common_construct";
import { VideoLogisticsBootstrapStack } from "./stacks/bootstrapStack/videoLogisticsBootstrapStack";

const app = new App();

// Get the account and region from environment variables or use defaults
const account =
  process.env.CDK_DEPLOY_ACCOUNT ||
  process.env.CDK_DEFAULT_ACCOUNT ||
  "YOUR_DEFAULT_ACCOUNT";
const region = (process.env.CDK_DEPLOY_REGION ||
  process.env.CDK_DEFAULT_REGION ||
  "us-west-2") as AWSRegion;

// Common environment config
const envConfig = {
    env: {
        account: account,
        region: region
    },
    region: region,
    account: account
};

// Create all stacks
new ForwardingRulesStack(app, 'ForwardingRulesStack', envConfig);
new SchedulerStack(app, 'SchedulerStack', envConfig);
new TimelineStack(app, 'TimelineStack', envConfig);
new VideoExportStack(app, 'VideoExportStack', envConfig);
new WorkflowStack(app, 'WorkflowStack', envConfig);

app.synth();
