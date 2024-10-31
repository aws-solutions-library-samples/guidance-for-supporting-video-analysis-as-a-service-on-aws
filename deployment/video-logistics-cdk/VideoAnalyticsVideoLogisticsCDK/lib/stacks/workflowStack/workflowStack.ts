import { Stack, type StackProps } from 'aws-cdk-lib';
import type { Construct } from 'constructs';
import type { PolicyStatement } from 'aws-cdk-lib/aws-iam';
import type { StateMachine } from 'aws-cdk-lib/aws-stepfunctions';
import type { IFunction } from 'aws-cdk-lib/aws-lambda';
import { AWSRegion } from 'video_analytics_common_construct';
import { getWorkflowResources, WorkflowStackProps } from './workflowResources';

/**
 * Lambda  function and step Functions are generated. And DLQ was created. And DDB triggering was added.
 */
export class WorkflowStack extends Stack {
  public readonly stateMachines: StateMachine[];
  public readonly workflowLambdaFunctions: IFunction[];

  constructor(scope: Construct, id: string, props: WorkflowStackProps) {
    super(scope, id, props);

    this.stateMachines = [];
    this.workflowLambdaFunctions = [];

    getWorkflowResources(this, props).forEach((resource) => {
      resource.finalizeSetup();
      if (resource.stateMachine !== undefined) {
        this.stateMachines.push(resource.stateMachine);
      }
      this.workflowLambdaFunctions.push(resource.workflow.lambda);
    });
  }
}