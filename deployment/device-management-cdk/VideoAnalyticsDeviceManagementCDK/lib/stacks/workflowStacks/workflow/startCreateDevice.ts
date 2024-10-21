import type { Role } from 'aws-cdk-lib/aws-iam';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Fail, JsonPath, StateMachine, Succeed, TaskInput } from 'aws-cdk-lib/aws-stepfunctions';
import { Workflow, WorkflowProps, VideoAnalyticsAsyncWorkflowResource, createLambdaRole, AWSRegionUtils } from 'video_analytics_common_construct/';
import { AWSRegion } from 'video_analytics_common_construct/lib/serviceConstructs/util';
import { Function } from 'aws-cdk-lib/aws-lambda';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { LambdaInvoke } from 'aws-cdk-lib/aws-stepfunctions-tasks';
import { Duration } from 'aws-cdk-lib';
import { Construct } from 'constructs';


export class StartCreateDevice extends VideoAnalyticsAsyncWorkflowResource {
  constructor(scope: Construct, id: string) {
    super(scope, id);
  }

  createStepFunction(): void {
    //TODO PLACEHOLDER
  }

  postWorkflowCreationCallback(): void {
    //TODO PLACEHOLDER
  }
}   