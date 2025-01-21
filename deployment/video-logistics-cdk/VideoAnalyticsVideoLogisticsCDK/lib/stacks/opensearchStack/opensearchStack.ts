import type { App } from 'aws-cdk-lib';
import { CfnOutput, Stack, StackProps } from 'aws-cdk-lib';
import {
  AccountRootPrincipal,
  AnyPrincipal,
  ArnPrincipal,
  Effect,
  ManagedPolicy,
  PolicyDocument,
  PolicyStatement,
  Role,
  ServicePrincipal
} from 'aws-cdk-lib/aws-iam';
import { Key } from 'aws-cdk-lib/aws-kms';
import { Domain } from 'aws-cdk-lib/aws-opensearchservice';
import { AWSRegion } from 'video_analytics_common_construct';
import { OPEN_SEARCH_SERVICE_NAME } from '../const';
import { getVLSearchDomainProps } from './utils';


export interface OpenSearchStackProps extends StackProps {
    region: AWSRegion;
    account: string;
}

export class OpenSearchStack extends Stack {
  public readonly opensearchEndpoint: string;
  public readonly bulkInferenceLambdaRoleArn: string;

  constructor(scope: App, id: string, readonly props: OpenSearchStackProps) {
    super(scope, id, props);

    const bulkInferenceLambdaRole = new Role(this, 'BulkInferenceLambdaRole', {
      roleName: 'BulkInferenceLambdaRole',
      assumedBy: new ServicePrincipal('lambda.amazonaws.com'),
      description: 'Allows lambda to make a bulk request to open search'
    });

    bulkInferenceLambdaRole.addManagedPolicy(
      ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')
    );

    const openSearchPolicy = new PolicyStatement({
      effect: Effect.ALLOW,
      resources: [
        `arn:aws:es:${this.region}:${this.account}:domain/valopensearchdomain`,
        `arn:aws:es:${this.region}:${this.account}:domain/valopensearchdomain/*`
      ],
      actions: ['es:ESHttpPost', 'es:ESHttpPut', 'es:ESHttpGet', 'es:ESHttpHead']
    });

    const kmsPolicy = new PolicyStatement({
      effect: Effect.ALLOW,
      resources: [`arn:aws:kms:${this.region}:${this.account}:key/*`],
      actions: ['kms:Decrypt', 'kms:Encrypt', 'kms:ReEncrypt*', 'kms:GenerateDataKey*']
    });

    bulkInferenceLambdaRole.addToPolicy(openSearchPolicy);
    bulkInferenceLambdaRole.addToPolicy(kmsPolicy);

    this.bulkInferenceLambdaRoleArn = bulkInferenceLambdaRole.roleArn;
    new CfnOutput(this, 'BulkInferenceLambdaRoleArn', {
      value: this.bulkInferenceLambdaRoleArn,
      exportName: 'BulkInferenceLambdaRoleArn',
    });

    const openSearchDomain = this.createOpenSearchDomain(props);

    this.opensearchEndpoint = openSearchDomain.domainEndpoint;

    openSearchDomain.addAccessPolicies(
        new PolicyStatement({
          actions: ['es:*'],
          effect: Effect.ALLOW,
          principals: [
            new ServicePrincipal(OPEN_SEARCH_SERVICE_NAME),
            new ArnPrincipal(bulkInferenceLambdaRole.roleArn),
        ],
          resources: [openSearchDomain.domainArn, `${openSearchDomain.domainArn}/*`]
        })
      );
    }

  private createOpenSearchDomain(props: OpenSearchStackProps): Domain {
    const domainId = `VALOpenSearchDomain`;
    const keyPolicy = new PolicyDocument({
      statements: [
        new PolicyStatement({
          actions: [
            'kms:List*',
            'kms:Describe*'
          ],
          principals: [new AnyPrincipal()],
          resources: ['*'],
          conditions: {
            ArnLike: {
              'aws:PrincipalArn': `arn:aws:iam::${this.account}:role/*`
            }
          }
        }),
        new PolicyStatement({
          actions: [
            'kms:CreateGrant'
          ],
          principals: [new AnyPrincipal()],
          resources: ['*'],
          conditions: {
            ArnLike: {
              'aws:PrincipalArn': `arn:aws:iam::${this.account}:role/*`
            },
            'StringEquals': {
              'kms:ViaService': `es.${props.region}.amazonaws.com`
            },
            'Bool': {
              'kms:GrantIsForAWSResource': 'true'
            }
          }
        })
      ]
    });
    const openSearchClusterKey = new Key(this, `VALDomainKey`, {
      enabled: true,
      alias: `DomainKey`,
      enableKeyRotation: true,
      admins: [new AccountRootPrincipal()],
      policy: keyPolicy
    });

    return new Domain(
        this,
        domainId,
        getVLSearchDomainProps(domainId.toLowerCase(), openSearchClusterKey)
    );
  }
}
