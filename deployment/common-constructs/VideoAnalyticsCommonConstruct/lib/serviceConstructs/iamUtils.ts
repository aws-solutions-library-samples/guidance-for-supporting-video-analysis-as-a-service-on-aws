import type { Construct } from 'constructs';
import type { PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { PolicyDocument, Role, ServicePrincipal, ManagedPolicy } from 'aws-cdk-lib/aws-iam';
import { LAMBDA_SERVICE_PRINCIPAL, LAMBDA_MANAGED_POLICY_NAME } from './const';

/**
 * Create new lambda role with policy statements
 */
export function createLambdaRole(
  scope: Construct,
  roleName: string,
  statements: PolicyStatement[]
) {
  return new Role(scope, roleName, {
    roleName: roleName,
    assumedBy: new ServicePrincipal(LAMBDA_SERVICE_PRINCIPAL),
    inlinePolicies: {
      AccessPolicy: new PolicyDocument({
        statements: statements
      })
    },
    managedPolicies: [ManagedPolicy.fromAwsManagedPolicyName(LAMBDA_MANAGED_POLICY_NAME)]
  });
}

export function createBasicLambdaRole(scope: Construct, roleName: string) {
    return new Role(scope, roleName, {
        roleName: roleName,
        assumedBy: new ServicePrincipal(LAMBDA_SERVICE_PRINCIPAL),
        managedPolicies: [ManagedPolicy.fromAwsManagedPolicyName(LAMBDA_MANAGED_POLICY_NAME)]
    });
}
