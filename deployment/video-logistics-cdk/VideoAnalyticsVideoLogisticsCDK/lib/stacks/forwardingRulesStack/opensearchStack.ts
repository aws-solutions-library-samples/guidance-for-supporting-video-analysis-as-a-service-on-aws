import type { App } from 'aws-cdk-lib';
import { Domain } from 'aws-cdk-lib/aws-opensearchservice';
import { Duration, Fn, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import {getVLSearchDomainProps} from './utils';
import {
  AccountRootPrincipal,
  AnyPrincipal,
  ArnPrincipal,
  Effect,
  PolicyDocument,
  PolicyStatement,
  ServicePrincipal
} from 'aws-cdk-lib/aws-iam';
import { Key } from 'aws-cdk-lib/aws-kms';
import * as opensearchserverless from 'aws-cdk-lib/aws-opensearchserverless';
import { AWSRegion } from 'video_analytics_common_construct';
import {OPEN_SEARCH_SERVICE_NAME} from '../const';


export interface OpenSearchStackProps extends StackProps {
    region: AWSRegion;
    account: string;
}

export class OpenSearchStack extends Stack {
  public readonly opensearchEndpoint: string;

  constructor(scope: App, id: string, readonly props: OpenSearchStackProps) {
    super(scope, id, props);

    const vlControlPlaneBulkLambdaRoleArn =`arn:aws:iam::${props.account}:role/BulkInferenceLambdaRole`;
    const vlControlPlanePitCreationLambdaRoleArn =`arn:aws:iam::${props.account}:role/PitCreationLambdaRole`;

    const openSearchDomain = this.createOpenSearchDomain(props);

    this.opensearchEndpoint = openSearchDomain.domainEndpoint;

    openSearchDomain.addAccessPolicies(
        new PolicyStatement({
          actions: ['es:*'],
          effect: Effect.ALLOW,
          principals: [
            new ServicePrincipal(OPEN_SEARCH_SERVICE_NAME),
            new ArnPrincipal(vlControlPlaneBulkLambdaRoleArn),
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