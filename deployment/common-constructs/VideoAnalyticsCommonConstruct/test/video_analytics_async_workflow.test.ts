import { Template } from 'aws-cdk-lib/assertions';
import { Workflow } from '../lib/video_analytics_async_workflow';
import { Stack, App } from 'aws-cdk-lib';

// Mock the Lambda asset
jest.mock('aws-cdk-lib/aws-lambda', () => {
  const originalModule = jest.requireActual('aws-cdk-lib/aws-lambda');
  return {
    ...originalModule,
    Code: {
      fromAsset: jest.fn().mockReturnValue({
        bindToResource: jest.fn(),
        bind: jest.fn().mockReturnValue({ s3Location: { bucketName: 'test-bucket', objectKey: 'test-key' } }),
      }),
      fromInline: jest.fn().mockReturnValue({
        bindToResource: jest.fn(),
        bind: jest.fn().mockReturnValue({ inlineCode: 'console.log("Mock Lambda");' }),
      }),
    },
  };
});

describe('Workflow Test', () => {
  let app: App;
  let testStack: Stack;

  beforeEach(() => {
    app = new App();
    testStack = new Stack(app, 'TestStack');
  });

  test('Default workflow uses default lambda', () => {
    new Workflow(testStack, 'DefaultWorkflow', {
      tableName: 'TestTable',
      stateMachineArn: 'arn:test',
      partitionKeyName: 'Key',
      gsiPropsList: undefined,
      sortKeyName: undefined
    });

    const template = Template.fromStack(testStack);

    template.hasResourceProperties('AWS::Lambda::Function', {
      Handler: 'com.amazon.aws.videoanalytics.common.workflow.StreamProcessor::handleRequest',
      Environment: {
        Variables: {
          STATE_MACHINE_ARN: 'arn:test',
        }
      }
    });
  });

  test('Workflow with optional step functions adds environment variables', () => {
    new Workflow(testStack, 'OptionalWorkflow', {
      tableName: 'TestTable3',
      stateMachineArn: 'arn:test3',
      stateMachineForDeleteArn: 'arn:delete',
      stateMachineForFinalizeArn: 'arn:finalize',
      partitionKeyName: 'Key3',
      gsiPropsList: undefined,
      sortKeyName: undefined
    });

    const template = Template.fromStack(testStack);

    template.hasResourceProperties('AWS::Lambda::Function', {
      Handler: 'com.amazon.aws.videoanalytics.common.workflow.StreamProcessor::handleRequest',
      Environment: {
        Variables: {
          STATE_MACHINE_ARN: 'arn:test3',
          STATE_MACHINE_FOR_DELETE_ARN: 'arn:delete',
          STATE_MACHINE_FOR_FINALIZE_ARN: 'arn:finalize'
        }
      }
    });
  });
});
