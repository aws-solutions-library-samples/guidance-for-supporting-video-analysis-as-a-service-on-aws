import { Construct } from 'constructs';
import { PolicyStatement, PolicyDocument, Role, ServicePrincipal, ManagedPolicy } from 'aws-cdk-lib/aws-iam';
import { LAMBDA_SERVICE_PRINCIPAL, LAMBDA_MANAGED_POLICY_NAME } from './const';

/**
 * Create new lambda role with policy statements
 */
export function createLambdaRole(
  scope: Construct,
  roleName: string,
  statements: PolicyStatement[]
): Role {
  // Check if all statements have at least one resource
  statements.forEach((statement, index) => {
    if (statement.resources.length === 0) {
      throw new Error(`PolicyStatement at index ${index} in role ${roleName} does not specify any resources.`);
    }
  });

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

export function createBasicLambdaRole(scope: Construct, roleName: string): Role {
    return new Role(scope, roleName, {
        roleName: roleName,
        assumedBy: new ServicePrincipal(LAMBDA_SERVICE_PRINCIPAL),
        managedPolicies: [ManagedPolicy.fromAwsManagedPolicyName(LAMBDA_MANAGED_POLICY_NAME)]
    });
}
