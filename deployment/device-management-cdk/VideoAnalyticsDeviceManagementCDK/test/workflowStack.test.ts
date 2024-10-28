import { jest, describe, test, expect } from '@jest/globals';
import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { WorkflowStack } from '../lib/stacks/workflowStacks/workflowStack';
import { AWSRegion } from 'video_analytics_common_construct';

describe('WorkflowStack', () => {
  test('Snapshot test', () => {
    const app = new cdk.App();
    const stack = new WorkflowStack(app, 'MyTestStack', {
      env: {
        account: '123456789012',
        region: 'us-east-1',
      },
      resources: [],
      region: 'us-east-1' as AWSRegion,
      account: '123456789012',
    });

    const template = Template.fromStack(stack);
    expect(template.toJSON()).toMatchSnapshot();
  });
});
