import { App } from 'aws-cdk-lib';
import { ForwardingRulesStack } from './stacks/forwardingRulesStack/forwardingRulesStack';
import { AWSRegion } from 'video_analytics_common_construct';

const app = new App();

// Get the account and region from environment variables or use defaults
const account = process.env.CDK_DEPLOY_ACCOUNT || process.env.CDK_DEFAULT_ACCOUNT || 'YOUR_DEFAULT_ACCOUNT';
const region = (process.env.CDK_DEPLOY_REGION || process.env.CDK_DEFAULT_REGION || 'us-west-2') as AWSRegion;

// Forwarding Rules Stack
// TODO: Add in more stacks as needed
new ForwardingRulesStack(app, 'ForwardingRulesStack', {
    env: {
        account: account,
        region: region
    },
    region: region,
    account: account
});

app.synth();
