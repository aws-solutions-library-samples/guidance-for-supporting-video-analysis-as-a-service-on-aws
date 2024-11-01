import type { Construct } from 'constructs';
import { Duration, Fn, RemovalPolicy, StackProps, Stack } from 'aws-cdk-lib';
import { AWSRegion, createTable } from 'video_analytics_common_construct';
import { OpenSearchPitCreation } from './openSearchPitCreation';
import type { PolicyStatement } from 'aws-cdk-lib/aws-iam';
import type { IFunction } from 'aws-cdk-lib/aws-lambda';

export interface SchedulerStackProps extends StackProps {
  region: AWSRegion;
}

/**
 *This stack will create scheduled event to trigger Lambda at a regular basis, e.g. every 1 minute
 */
export class SchedulerStack extends Stack {
  public readonly scheduledLambdas: IFunction[];

  constructor(scope: Construct, id: string, props: SchedulerStackProps) {
    super(scope, id, props);

    this.scheduledLambdas = [];

    const pitCreation = new OpenSearchPitCreation(this, {
      region: props.region,
    });
    this.scheduledLambdas.push(pitCreation.pitCreationLambda);
  }
}