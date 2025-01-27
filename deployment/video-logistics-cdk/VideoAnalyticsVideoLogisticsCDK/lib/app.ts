import { App } from "aws-cdk-lib";
import { AWSRegion } from "video_analytics_common_construct";
import {
  ServiceStack,
  TimelineStack,
  WorkflowStack,
} from "./stacks";
import { VideoLogisticsBootstrapStack } from "./stacks/bootstrapStack/videoLogisticsBootstrapStack";
import { OpenSearchStack } from "./stacks/opensearchStack";
import { BulkInferenceStack } from "./stacks/workflowStack/bulkInferenceStack";

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

const bootstrapStack = new VideoLogisticsBootstrapStack(app, 'VideoLogisticsBootstrapStack', envConfig);

// Create all other stacks
new TimelineStack(app, 'VideoLogisticsTimelineStack', envConfig);
const opensearchStack = new OpenSearchStack(app, 'VideoLogisticsOpensearchStack', envConfig);
const bulkInferenceStack = new BulkInferenceStack(app, 'VideoLogisticsBulkInferenceStack', {...envConfig, 
  opensearchEndpoint: opensearchStack.opensearchEndpoint});
new WorkflowStack(app, 'VideoLogisticsWorkflowStack', envConfig);
new ServiceStack(app, 'VideoLogisticsServiceStack', envConfig);

app.synth();
