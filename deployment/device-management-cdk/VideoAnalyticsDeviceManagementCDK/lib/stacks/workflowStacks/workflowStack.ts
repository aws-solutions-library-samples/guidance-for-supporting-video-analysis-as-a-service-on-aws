import * as cdk from "aws-cdk-lib";
import { StackProps } from "aws-cdk-lib";
import { Construct } from "constructs";
import { StateMachine } from "aws-cdk-lib/aws-stepfunctions";
import { IFunction } from "aws-cdk-lib/aws-lambda";
import {
  VideoAnalyticsAsyncWorkflowResource,
  AWSRegion,
} from "video_analytics_common_construct";
import { StartCreateDevice } from "./startCreateDevice";

export interface WorkflowStackProps extends StackProps {
  resources: VideoAnalyticsAsyncWorkflowResource[];
  region: AWSRegion;
  account: string;
}

/**
 * Workflow stack for generating DDB stream, DDB stream handler lambda, DLQ, lambda role
 */
export class WorkflowStack extends cdk.Stack {
  public readonly stateMachines: StateMachine[];
  public readonly workflowLambdaFunctions: IFunction[];

  constructor(scope: Construct, id: string, props: WorkflowStackProps) {
    super(scope, id, props);

    console.log("WorkflowStack constructor called");
    console.log("Props:", JSON.stringify(props));

    // Initialize the arrays
    this.stateMachines = [];
    this.workflowLambdaFunctions = [];

    const resources =
      props.resources != undefined && props.resources.length > 0
        ? props.resources
        : getWorkflowResources(this, props);
    console.log("Resources:", resources);

    resources.forEach((resource) => {
      console.log("Finalizing setup for resource:", resource);
      resource.finalizeSetup();
      if (resource.stateMachine !== undefined) {
        this.stateMachines.push(resource.stateMachine);
      }
      if (resource.workflow && resource.workflow.lambda) {
        this.workflowLambdaFunctions.push(resource.workflow.lambda);
      }
    });
  }
}

export function getWorkflowResources(
  scope: Construct,
  props: WorkflowStackProps
): VideoAnalyticsAsyncWorkflowResource[] {
  console.log("getWorkflowResources called");
  return [
    new StartCreateDevice(scope, "StartCreateDevice", props),
  ];
}
