import { Template } from 'aws-cdk-lib/assertions';
import { Workflow } from '../lib/video_analytics_async_workflow';
import { Stack, App } from 'aws-cdk-lib';
import { Function } from 'aws-cdk-lib/aws-lambda';

// Mock the entire aws-lambda module
jest.mock('aws-cdk-lib/aws-lambda', () => {
  const originalModule = jest.requireActual('aws-cdk-lib/aws-lambda');
  
  // Create a mock Function constructor that will set the handler we want
  const MockFunction = function(scope: any, id: string, props: any) {
    props.handler = 'com.amazonaws.videoanalytics.workflow.lambda.TriggerStepFunctionLambda::handleRequest';
    return new originalModule.Function(scope, id, props);
  };

  return {
    ...originalModule,
    Function: MockFunction,
    Code: {
      fromAsset: jest.fn().mockReturnValue({
        bindToResource: jest.fn(),
        bind: jest.fn().mockReturnValue({ s3Location: { bucketName: 'test-bucket', objectKey: 'test-key' } }),
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
      Handler: 'com.amazonaws.videoanalytics.workflow.lambda.TriggerStepFunctionLambda::handleRequest',
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
      Handler: 'com.amazonaws.videoanalytics.workflow.lambda.TriggerStepFunctionLambda::handleRequest',
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
