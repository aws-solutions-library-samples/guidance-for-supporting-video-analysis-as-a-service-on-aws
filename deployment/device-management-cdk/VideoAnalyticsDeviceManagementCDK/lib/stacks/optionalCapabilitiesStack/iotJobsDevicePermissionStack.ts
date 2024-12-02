import { Stack, StackProps, RemovalPolicy } from "aws-cdk-lib";
import { Construct } from "constructs";
import { AWSRegion } from "video_analytics_common_construct";
import { IOT_CONNECTED_THING_NAME, IOT_CREDENTIAL_THING_NAME } from "../const";
import {
  ManagedPolicy,
  PolicyStatement,
  Role,
  ServicePrincipal,
  Effect,
} from "aws-cdk-lib/aws-iam";
import * as iot from "aws-cdk-lib/aws-iot";
import {
  AwsCustomResource,
  PhysicalResourceId,
} from "aws-cdk-lib/custom-resources";

export interface IotJobsDevicePermissionStackProps extends StackProps {
    readonly region: AWSRegion;
    readonly account: string;
  }

export class IotJobsDevicePermissionStack extends Stack {
    constructor(
      scope: Construct,
      id: string,
      props: IotJobsDevicePermissionStackProps
    ) {
      super(scope, id, props);
  
      console.log("DeviceManagementBootstrapStack constructor called");
      console.log("Props:", JSON.stringify(props));

      const iotCustomResourceRole = new Role(this, "IotCustomResourceRole", {
        roleName: "iot-custom-resource-role",
        assumedBy: new ServicePrincipal("lambda.amazonaws.com"),
      });
    
    const iotCustomResourceIamPolicy = new ManagedPolicy(
        this,
        "IotCustomResourceIamPolicy",
        {
          managedPolicyName: "iot-custom-resource-policy",
          roles: [iotCustomResourceRole],
          statements: [
            new PolicyStatement({
              effect: Effect.ALLOW,
              actions: [
                "iot:AttachPolicy",
                "iot:DetachPolicy",
                "iot:AttachThingPrincipal",
                "iot:DetachThingPrincipal",
                "iot:AttachPrincipalPolicy",
                "iot:DetachPrincipalPolicy",
              ],
              resources: ["*"],
            }),
            new PolicyStatement({
              effect: Effect.ALLOW,
              actions: ["lambda:InvokeFunction"],
              resources: ["*"],
            }),
          ],
        }
      );
    
    /**
     * Device in ENABLED state is permitted to execute commands.
     * Therefore, iotJobsPolicy that grants IoT job permission is attached to SpecialGroup_EnabledState
     *
     * IoT documentation on reserved topics for IoT Job:
     * https://docs.aws.amazon.com/iot/latest/developerguide/reserved-topics.html#reserved-topics-job
     * IoT documentation on wildcard usage for IoT policies vs MQTT topic filters (wildcard syntax is different)
     * https://docs.aws.amazon.com/iot/latest/developerguide/pub-sub-policy.html#pub-sub-policy-cert
     * MQTT subscription needs to be on topicfilter not topic name
     * https://docs.aws.amazon.com/iot/latest/developerguide/topics.html
     * */
    const iotJobsPolicy = new iot.CfnPolicy(this, "IotJobPolicy", {
        policyDocument: {
            Version: "2012-10-17",
            Statement: [
            {
                Effect: "Allow",
                Action: ["iot:Publish"],
                Resource: [
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/jobs/*/update`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/jobs/start-next`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/jobs/get`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/jobs/$next/get`,
                ],
            },
            {
                Effect: "Allow",
                Action: ["iot:Subscribe"],
                Resource: [
                // for MQTT topics:
                // $aws/things/thingName/jobs/jobId/update/accepted
                // $aws/things/thingName/jobs/jobId/update/rejected
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/jobs/*/update/*`,
                // for MQTT topics:
                // $aws/things/thingName/jobs/start-next/accepted
                // $aws/things/thingName/jobs/start-next/rejected
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/jobs/start-next/*`,
                // for MQTT topics:
                // $aws/things/thingName/jobs/get/accepted
                // $aws/things/thingName/jobs/get/rejected
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/jobs/get/*`,
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/jobs/notify`,
                // for MQTT topics:
                // $aws/things/thingName/jobs/$next/get/accepted
                // $aws/things/thingName/jobs/$next/get/rejected
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/jobs/$next/get/*`,
                ],
            },
            {
                Effect: "Allow",
                Action: ["iot:Receive"],
                Resource: [
                // for MQTT topic:
                // $aws/things/thingName/jobs/jobId/update/accepted
                // $aws/things/thingName/jobs/jobId/update/rejected
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/jobs/*/update/*`,
                // for MQTT topic:
                // $aws/things/thingName/jobs/start-next/accepted
                // $aws/things/thingName/jobs/start-next/rejected
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/jobs/start-next/*`,
                // for MQTT topic:
                // $aws/things/thingName/jobs/get/accepted
                // $aws/things/thingName/jobs/get/rejected
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/jobs/get/*`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/jobs/notify`,
                // for MQTT topics:
                // $aws/things/thingName/jobs/$next/get/accepted
                // $aws/things/thingName/jobs/$next/get/rejected
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/jobs/$next/get/*`,
                ],
            },
            ],
        },
        policyName: `IotJobPolicy_${this.region}`,
        });
    
    const attachIotJobPolicyToEnabledState = new AwsCustomResource(
        this,
        "AttachIotJobPolicyToEnabledState",
        {
          onCreate: {
            service: "@aws-sdk/client-iot",
            action: "attachPolicy",
            parameters: {
              target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_EnabledState`,
              policyName: `IotJobPolicy_${this.region}`,
            },
            physicalResourceId: PhysicalResourceId.of(
              "attachIotJobPolicyEnabled"
            ),
          },
          onUpdate: {
            service: "@aws-sdk/client-iot",
            action: "attachPolicy",
            parameters: {
              target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_EnabledState`,
              policyName: `IotJobPolicy_${this.region}`,
            },
            physicalResourceId: PhysicalResourceId.of(
              "attachIotJobPolicyEnabled"
            ),
          },
          onDelete: {
            service: "@aws-sdk/client-iot",
            action: "detachPolicy",
            parameters: {
              target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_EnabledState`,
              policyName: `IotJobPolicy_${this.region}`,
            },
            physicalResourceId: PhysicalResourceId.of(
              "detachIotJobPolicyEnabled"
            ),
          },
          role: iotCustomResourceRole,
          removalPolicy: RemovalPolicy.DESTROY,
          installLatestAwsSdk: true,
        }
      );
      attachIotJobPolicyToEnabledState.node.addDependency(iotJobsPolicy);
      attachIotJobPolicyToEnabledState.node.addDependency(iotCustomResourceIamPolicy);
      attachIotJobPolicyToEnabledState.node.addDependency(iotCustomResourceRole);
    }

}