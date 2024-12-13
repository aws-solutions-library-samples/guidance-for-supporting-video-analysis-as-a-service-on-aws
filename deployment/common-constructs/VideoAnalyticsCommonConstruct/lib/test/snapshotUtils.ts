/* eslint-disable @typescript-eslint/no-explicit-any */
// Disabling no-explicit-any rule because this file inherently has to work with a lot of poorly-defined JSON objects

import { merge } from 'lodash';
import * as cdk from "aws-cdk-lib";
import { Template } from "aws-cdk-lib/assertions";
import { AWSRegion } from "../serviceConstructs/util";

const testProps = {
  env: {
    account: "123456789012",
    region: "us-east-1",
  },
  region: "us-east-1" as AWSRegion,
  account: "123456789012",
};

export const createStackSnapshotTest = (name: string, StackClass: any) => {
  describe(name, () => {
    test("Snapshot test", () => {
      const app = new cdk.App();
      const stack = new StackClass(app, name, testProps);
      const template = Template.fromStack(stack);
      const stackJson = sanitizeLambdas(template.toJSON());
      const matcher = createCdkPropertyMatcher(stackJson, { ignoreLambdaAssets: true });

      expect(stackJson).toMatchSnapshot(matcher);
    });
  });
};


// This is the type used by the aws-cdk-lib when calling `.toJSON()` on a template.
type SnapshotJSON = {
  [key: string]: any;
};

/**
 * Performs the full set of sanitizing options for lambdas. In the future, we may want to add an optional
 * configuration param to only perform select sanitizations.
 *
 * @param stackJson
 * @returns the new JSON template.
 */
export const sanitizeLambdas = (stackJson: SnapshotJSON): SnapshotJSON => {
  return stripLambdaVersion(stripLambdaMetadata(stackJson));
};

/**
 * Identifies all Lambda versions and replaces them with a placeholder string. In certain lambda configurations,
 * these version hashes can change with every build.
 *
 * @param stackJson
 *
 * @returns the new JSON template.
 */
export const stripLambdaVersion = (stackJson: SnapshotJSON): SnapshotJSON => {
  const versionList = Object.keys(stackJson.Resources).filter((resource: string) => {
    return (stackJson.Resources as any)[resource].Type === 'AWS::Lambda::Version';
  });

  return versionList.length
    ? JSON.parse(
        JSON.stringify(stackJson).replace(
          new RegExp(versionList.join('|'), 'g'),
          'LAMBDA_VERSION_HASH'
        )
      )
    : stackJson;
};

/**
 * Deletes the metadata param from a lambda function. This param is conditionally inserted based on the build
 * environment and we don't want it to cause snapshots to fail.
 *
 * @param stackJson
 *
 * @returns the new JSON template.
 */
export const stripLambdaMetadata = (stackJson: SnapshotJSON) => {
  const newJson = { ...stackJson };
  const lambdaList = Object.keys(newJson.Resources).filter((resource: string) => {
    return (newJson.Resources as any)[resource].Type === 'AWS::Lambda::Function';
  });

  lambdaList.forEach((lambdaName) => {
    if ((newJson.Resources as any)[lambdaName].Metadata) {
      delete (newJson.Resources as any)[lambdaName].Metadata;
    }
  });

  return newJson;
};

/**
 * Identifies all API Gateway Deployment hashes and replaces them with a placeholder string. This hash changes
 * whenever the model changes, so should be ignored from the snapshot.
 *
 * @param stackJson
 *
 * @returns the new JSON template.
 */
export const stripApiGatewayDeploymentHash = (stackJson: SnapshotJSON): SnapshotJSON => {
  const apiGatewayDeploymentList = Object.keys(stackJson.Resources).filter((resource: string) => {
    return (stackJson.Resources as any)[resource].Type === 'AWS::ApiGateway::Deployment';
  });

  return apiGatewayDeploymentList.length
    ? JSON.parse(
        JSON.stringify(stackJson).replace(
          new RegExp(apiGatewayDeploymentList.join('|'), 'g'),
          'API_GATEWAY_DEPLOYMENT_HASH'
        )
      )
    : stackJson;
};

/**
 * Helper function for `createCdkPropertyMatcher`. Creates matchers to ignore certain paths in lambda assets that
 * will change between builds.
 */
const generateIgnoreLambdaAssetsMatcher = (stackJson: SnapshotJSON) => {
  const lambdaList = Object.keys(stackJson.Resources).filter((resource: string) => {
    return (stackJson.Resources as any)[resource].Type === 'AWS::Lambda::Function';
  });

  const resourceMatcher = lambdaList.reduce((accumulator, lambdaName) => {
    const lambdaMatcher = {
      Properties: {
        /* ignore all lambda tags because the object key changes with every build.
         also, don't expect every Lambda to be tagged. */
        ...((stackJson.Resources as any)[lambdaName].Properties.Tags && {
          Tags: expect.any(Array)
        }),
        Code: {
          S3Key: expect.anything()
        },
        // some constructs insert the build timestamp into the lambda description. Match any string
        ...((stackJson.Resources as any)[lambdaName].Properties.Description && {
          Description: expect.any(String)
        })
      }
    };

    return { ...accumulator, [lambdaName]: lambdaMatcher };
  }, {});

  return { Resources: resourceMatcher };
};

/**
 * Helper function for `createCdkPropertyMatcher`. Creates matchers to ignore the "body" field in
 * a Rest API resource.
 */
const generateIgnoreRestApiBodyMatcher = (stackJson: SnapshotJSON) => {
  const restApiList = Object.keys(stackJson.Resources).filter((resource: string) => {
    return (stackJson.Resources as any)[resource].Type === 'AWS::ApiGateway::RestApi';
  });

  const resourceMatcher = restApiList.reduce((accumulator, restApiName) => {
    const restApiBodyMatcher = {
      Properties: {
        Body: expect.anything()
      }
    };

    return { ...accumulator, [restApiName]: restApiBodyMatcher };
  }, {});

  return { Resources: resourceMatcher };
};

type CreateCdkPropertyMatcherProps = {
  customMatcher?: object;
  ignoreLambdaAssets?: boolean;
  ignoreRestApiBody?: boolean;
};

/**
 * Creates a Jest property matcher object to be used when performing a snapshot test
 *
 * @param stackJson JSON representation of the CDK Stack Template
 * @param configuration
 *  @param customMatcher User-specified jest matcher.
 *  @param ignoreLambdaAssets Boolean indicating whether to apply the lambda asset matcher
 *  @param ignoreRestApiBody Boolean indicating whether to ignore the "body" field of a Rest API
 * @returns A jest matcher object to be used in a snapshot test
 */
export const createCdkPropertyMatcher = (
  stackJson: object,
  { customMatcher = {}, ignoreLambdaAssets, ignoreRestApiBody }: CreateCdkPropertyMatcherProps
): object => {
  const matcherList: any[] = [];
  if (
    !(
      Object.prototype.hasOwnProperty.call(stackJson, 'Resources') &&
      typeof (stackJson as any).Resources === 'object'
    )
  ) {
    throw new Error('Stack JSON does not have a properly formatted "Resources" attribute.');
  }

  if (ignoreLambdaAssets) {
    matcherList.push(generateIgnoreLambdaAssetsMatcher(stackJson as SnapshotJSON));
  }

  if (ignoreRestApiBody) {
    matcherList.push(generateIgnoreRestApiBodyMatcher(stackJson as SnapshotJSON));
  }

  return matcherList.reduce((accumulator, current) => {
    return merge(accumulator, current);
  }, customMatcher);
};
