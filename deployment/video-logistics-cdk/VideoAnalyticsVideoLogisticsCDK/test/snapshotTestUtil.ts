import { describe, test, expect } from "@jest/globals";
import * as cdk from "aws-cdk-lib";
import { Template } from "aws-cdk-lib/assertions";
import { AWSRegion } from "video_analytics_common_construct";

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
      expect(template.toJSON()).toMatchSnapshot();
    });
  });
};
