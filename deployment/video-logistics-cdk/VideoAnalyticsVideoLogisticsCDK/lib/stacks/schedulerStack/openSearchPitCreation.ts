import * as cdk from 'aws-cdk-lib';
import { Duration, Fn, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import { Rule, Schedule } from 'aws-cdk-lib/aws-events';
import { LambdaFunction, addLambdaPermission } from 'aws-cdk-lib/aws-events-targets';
import { Effect, ManagedPolicy, PolicyStatement, Role, ServicePrincipal } from 'aws-cdk-lib/aws-iam';
import { Code, EventSourceMapping, Function, IFunction, Runtime } from 'aws-cdk-lib/aws-lambda';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { BillingMode, Table } from 'aws-cdk-lib/aws-dynamodb';
import { VideoAnalyticsAsyncWorkflowResource, AWSRegion, createTable } from 'video_analytics_common_construct';
import { LAMBDA_ASSET_PATH } from '../const';

  
  export interface OpenSearchPitCreationProps extends StackProps {
    region: AWSRegion;
  }
  
  /**
   * This is to create scheduled event to trigger Lambda to create PITs for Open Search indexes periodically.
   */
  export class OpenSearchPitCreation {
    public readonly pitCreationLambda: IFunction;
  
    constructor(stack: cdk.Stack, props: OpenSearchPitCreationProps) {
      const pitCreationPeriodInMinutes = 2;
      const table = this.createOpenSearchPitTable(stack);
      const role = this.getLambdaRole(stack, table);
  
      this.pitCreationLambda = this.createLambda(stack, role, props);
  
      const eventRule = new Rule(stack, 'PitCreation-XMinutesRule', {
        // https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-cron-expressions.html
        schedule: Schedule.cron({ minute: `0/${pitCreationPeriodInMinutes}` })
      });
  
      eventRule.addTarget(
        new LambdaFunction(this.pitCreationLambda, {
          retryAttempts: 1,
          maxEventAge: Duration.minutes(pitCreationPeriodInMinutes)
        })
      );
  
      // Allow the Event Rule to invoke the Lambda function
      addLambdaPermission(eventRule, this.pitCreationLambda);
    }
  
    private createLambda(stack: Stack, role: Role, props: OpenSearchPitCreationProps) {
      return new Function(stack, 'PitCreationLambda', {
        // TODO: Update lambda asset path once code compiled jar is available
        code: Code.fromAsset(LAMBDA_ASSET_PATH),
        description: 'Lambda responsible for PIT creation in Open Search',
        runtime: Runtime.JAVA_17,
        // TODO: Update handler to match new lambda handler path
        handler: 'com.amazon.awsvideoanalyticsvlcontrolplane.inference.PitCreationLambda::handleRequest',
        memorySize: 2048,
        role: role,
        environment: {
          AWSRegion: props.region,
        },
        timeout: Duration.minutes(12),
        logGroup: new LogGroup(stack, 'PitCreationLambdaLogGroup', {
          retention: RetentionDays.TEN_YEARS,
          logGroupName: 'PitCreationLambdaLogGroup'
        })
      });
    }
  
    private getLambdaRole(
      stack: Stack,
      table: Table,
    ) {
      const openSearchPolicy = new PolicyStatement({
        effect: Effect.ALLOW,
        resources: ['*'],
        actions: ['es:ESHttpPost', 'es:ESHttpPut', 'es:ESHttpGet', 'es:ESHttpHead', 'es:ESHttpDelete']
      });
  
      const ddbPolicy = new PolicyStatement({
        effect: Effect.ALLOW,
        resources: [table.tableArn],
        actions: ['dynamodb:GetItem', 'dynamodb:PutItem', 'dynamodb:UpdateItem']
      });
  
      const lambdaRole = new Role(stack, 'PitCreationLambdaRole', {
        roleName: 'PitCreationLambdaRole',
        assumedBy: new ServicePrincipal('lambda.amazonaws.com'),
        description: 'Allows lambda to make a pit creation request to open search'
      });
  
      lambdaRole.addToPolicy(openSearchPolicy);
      if (table.encryptionKey) {
        const kmsPolicy = new PolicyStatement({
          effect: Effect.ALLOW,
          resources: [table.encryptionKey.keyArn],
          actions: ['kms:Encrypt', 'kms:Decrypt', 'kms:GenerateDataKey']
        });
        lambdaRole.addToPolicy(kmsPolicy);
      }
  
      lambdaRole.addToPolicy(ddbPolicy);
  
      lambdaRole.addManagedPolicy(
        ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')
      );
  
      return lambdaRole;
    }
  
    private createOpenSearchPitTable(stack: Stack) {
      const tableName = 'OpenSearchPitTable';
      const partitionKey = 'CustomerAccountIdModelName';
      const sortKey = 'Endpoint';
      const timeToLiveAttribute = undefined;
      const stream = undefined;
  
      return createTable(
        stack,
        tableName,
        partitionKey,
        sortKey,
        timeToLiveAttribute,
        stream,
        BillingMode.PAY_PER_REQUEST
      );
    }
  }
  