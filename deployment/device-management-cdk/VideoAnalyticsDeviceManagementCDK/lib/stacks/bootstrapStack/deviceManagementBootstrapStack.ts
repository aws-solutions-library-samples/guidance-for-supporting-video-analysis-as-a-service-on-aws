import { Stack, StackProps, RemovalPolicy, ArnFormat } from "aws-cdk-lib";
import { Construct } from "constructs";
import { AWSRegion } from "video_analytics_common_construct";
import { IOT_CONNECTED_THING_NAME, IOT_CREDENTIAL_THING_NAME } from "../const";
import {
  ManagedPolicy,
  PolicyStatement,
  Role,
  ServicePrincipal,
  CfnRole,
  PolicyDocument,
  Effect,
} from "aws-cdk-lib/aws-iam";
import { BlockPublicAccess, Bucket, ObjectOwnership } from "aws-cdk-lib/aws-s3";
import * as iot from "aws-cdk-lib/aws-iot";
import {
  AwsCustomResource,
  PhysicalResourceId,
} from "aws-cdk-lib/custom-resources";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";

export interface DeviceManagementBootstrapStackProps extends StackProps {
  readonly region: AWSRegion;
  readonly account: string;
}

/**
 * Provisions the Device management resources
 */
export class DeviceManagementBootstrapStack extends Stack {
  constructor(
    scope: Construct,
    id: string,
    props: DeviceManagementBootstrapStackProps
  ) {
    super(scope, id, props);

    console.log("DeviceManagementBootstrapStack constructor called");
    console.log("Props:", JSON.stringify(props));

    const role = new Role(this, "iot-detach-policy-custom-resource-role", {
      roleName: "iot-detach-policy-custom-resource-role",
      assumedBy: new ServicePrincipal("lambda.amazonaws.com"),
    });

    new ManagedPolicy(this, "detach-state-policy", {
      roles: [role],
      managedPolicyName: "detach-state-policy",
      statements: [
        new PolicyStatement({
          actions: ["iot:DetachPolicy"],
          resources: ["*"],
        }),
      ],
    });

    const deviceKvsRole = new CfnRole(this, "DeviceKVSRole", {
      assumeRolePolicyDocument: {
        Version: "2012-10-17",
        Statement: [
          {
            Effect: "Allow",
            Principal: {
              Service: ["credentials.iot.amazonaws.com"],
            },
            Action: ["sts:AssumeRole"],
          },
        ],
      },
      policies: [
        {
          policyName: "DeviceKVSAccess",
          policyDocument: {
            Version: "2012-10-17",
            Statement: [
              {
                Effect: "Allow",
                Action: [
                  "kinesisvideo:DescribeStream",
                  "kinesisvideo:PutMedia",
                  "kinesisvideo:TagStream",
                  "kinesisvideo:GetDataEndpoint",
                  "kinesisvideo:DescribeSignalingChannel",
                  "kinesisvideo:ConnectAsMaster",
                  "kinesisvideo:GetSignalingChannelEndpoint",
                  "kinesisvideo:GetIceServerConfig",
                  "s3:PutObject",
                ],
                Resource: [
                  `arn:aws:kinesisvideo:${this.region}:${this.account}:stream/${IOT_CREDENTIAL_THING_NAME}/*`,
                  `arn:aws:s3:::video-analytics-${this.account}-${this.region}/event-thumbnails/${IOT_CREDENTIAL_THING_NAME}/*`,
                  `arn:aws:kinesisvideo:${this.region}:${this.account}:channel/${IOT_CREDENTIAL_THING_NAME}-LiveStreamSignalingChannel/*`,
                ],
              },
              {
                Effect: "Allow",
                Action: ["s3:PutObject", "s3:GetObject", "s3:ListBucket"],
                Resource: [`arn:aws:s3:::${this.account}*`],
              },
            ],
          },
        },
      ],
      maxSessionDuration: 43200,
    });

    if (deviceKvsRole == null) {
      throw new Error(
        `A combination of conditions caused 'deviceKvsRole' to be undefined. Fixit.`
      );
    }
    const deviceKvsRoleAlias = new iot.CfnRoleAlias(
      this,
      "DeviceKVSRoleAlias",
      {
        roleAlias: "kvsDeviceIoTRoleAlias",
        roleArn: deviceKvsRole.attrArn,
        credentialDurationSeconds: 43200,
      }
    );

    const deviceS3Role = new CfnRole(this, "DeviceS3Role", {
      assumeRolePolicyDocument: {
        Version: "2012-10-17",
        Statement: [
          {
            Effect: "Allow",
            Principal: {
              Service: ["credentials.iot.amazonaws.com"],
            },
            Action: ["sts:AssumeRole"],
          },
        ],
      },
      policies: [
        {
          policyName: "DeviceS3Access",
          policyDocument: {
            Version: "2012-10-17",
            Statement: [
              {
                Effect: "Allow",
                Action: ["s3:GetObject"],
                Resource: [
                  `arn:aws:s3:::video-analytics-firmware-bucket-${this.account}-${this.region}/*`,
                ],
              },
            ],
          },
        },
      ],
      maxSessionDuration: 43200,
    });

    if (deviceS3Role == null) {
      throw new Error(
        `A combination of conditions caused 'deviceS3Role' to be undefined. Fixit.`
      );
    }

    const deviceS3RoleAlias = new iot.CfnRoleAlias(this, "DeviceS3RoleAlias", {
      roleAlias: "s3DeviceIotRoleAlias",
      roleArn: deviceS3Role.attrArn,
      credentialDurationSeconds: 43200,
    });

    new Bucket(this, "videoAnalyticsFirmwareBucket", {
      bucketName: `video-analytics-firmware-bucket-${this.account}-${this.region}`,
      publicReadAccess: false,
      blockPublicAccess: BlockPublicAccess.BLOCK_ALL,
      removalPolicy: RemovalPolicy.RETAIN,
      objectOwnership: ObjectOwnership.OBJECT_WRITER,
      enforceSSL: true,
      serverAccessLogsPrefix: "access-logs/",
    });

    new CfnRole(this, "PermissionToPublishToSNSRole", {
      roleName: "PermissionToPublishToSNSRole"!,
      assumeRolePolicyDocument: {
        Version: "2012-10-17",
        Statement: [
          {
            Effect: "Allow",
            Principal: {
              Service: ["iot.amazonaws.com"],
            },
            Action: ["sts:AssumeRole"],
          },
        ],
      },
      policies: [
        {
          policyName: "SNSWriteAccess",
          policyDocument: {
            Version: "2012-10-17",
            Statement: [
              {
                Effect: "Allow",
                Action: ["sns:Publish"],
                Resource: [`arn:aws:sns:${this.region}:${this.account}:*`],
              },
              {
                Effect: "Allow",
                Action: [
                  "kms:Decrypt",
                  "kms:Generate*",
                  "kms:DescribeKey",
                  "kms:Encrypt",
                  "kms:ReEncrypt*",
                ],
                Resource: [
                  Stack.of(this).formatArn({
                    service: "kms",
                    region: this.region,
                    account: this.account,
                    resource: "key/*",
                    arnFormat: ArnFormat.COLON_RESOURCE_NAME,
                  }),
                ],
              },
            ],
          },
        },
      ],
    });

    if (deviceKvsRoleAlias == null) {
      throw new Error(
        `A combination of conditions caused 'deviceKvsRoleAlias' to be undefined. Fixit.`
      );
    }
    if (deviceS3RoleAlias == null) {
      throw new Error(
        `A combination of conditions caused 'deviceS3RoleAlias' to be undefined. Fixit.`
      );
    }

    new iot.CfnPolicy(this, "DeviceX509CertPolicy", {
      policyDocument: {
        Version: "2012-10-17",
        Statement: [
          {
            Effect: "Allow",
            Action: ["iot:Connect", "iot:AssumeRoleWithCertificate"],
            Resource: [
              deviceKvsRoleAlias.attrRoleAliasArn,
              deviceS3RoleAlias.attrRoleAliasArn,
            ],
          },
          {
            Effect: "Allow",
            Action: ["iot:Publish"],
            Resource: [
              `arn:aws:iot:${this.region}:${this.account}:topic/video-analytics/${IOT_CONNECTED_THING_NAME}/timeline`,
              `arn:aws:iot:${this.region}:${this.account}:topic/video-analytics/${IOT_CONNECTED_THING_NAME}/snapshot`,
            ],
          },
        ],
      },
      // while this policy is titled kvsDeviceIoTPolicy,
      // it is really just the policy used to access aws from edge device
      policyName: "kvsDeviceIoTPolicy",
    });

    /**
     * Device in CREATED, ENABLED, and DISABLED state are all permitted to execute commands.
     * Therefore, iotJobsPolicy that grants IoT job permission is attached to all special state thing groups.
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

    const createdStateIotPolicy = new iot.CfnPolicy(
      this,
      "CreatedStatePolicy",
      {
        policyDocument: {
          Version: "2012-10-17",
          Statement: [
            {
              Effect: "Allow",
              Action: ["iot:Connect"],
              Resource: [
                `arn:aws:iot:${this.region}:${this.account}:client/${IOT_CONNECTED_THING_NAME}`,
              ],
            },
            {
              Effect: "Allow",
              Action: ["iot:Publish", "iot:Receive"],
              Resource: [
                `arn:aws:iot:${this.region}:${this.account}:topic/videoanalytics/initial-pub-sub/${IOT_CONNECTED_THING_NAME}`,
                `arn:aws:iot:${this.region}:${this.account}:topic/management/${IOT_CONNECTED_THING_NAME}/connection`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/name/*/get*`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/name/*/update*`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/get*`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/update*`,
              ],
            },
            {
              Effect: "Allow",
              Action: ["iot:Subscribe"],
              Resource: [
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/videoanalytics/initial-pub-sub/${IOT_CONNECTED_THING_NAME}`,
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/management/${IOT_CONNECTED_THING_NAME}/connection`,
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/name/*/get/*`,
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/name/*/update/*`,
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/get/*`,
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/update/*`,
              ],
            },
          ],
        },
        policyName: `CreatedPolicy_${this.region}`,
      }
    );

    const createdStateThingGroup = new iot.CfnThingGroup(
      this,
      "CreatedStateThingGroup",
      {
        thingGroupName: "SpecialGroup_CreatedState",
      }
    );

    // There is an issue with CDK where role for AwsCustomResource is created during stack instantiation.
    // Any other role created for AwsCustomResource after the first one will be ignored
    // and CDK re-uses the already existing role.
    // See issue: https://github.com/aws/aws-cdk/issues/13601?ref=blog.purple-technology.com

    // If you need to add permissions for a AwsCustomResource, add to iotCustomResourceIamPolicy.
    // Do NOT create a separate role. Creating a separate role will not deploy successfully.
    const iotCustomResourceRole = new Role(this, "iot-custom-resource-role", {
      roleName: "iot-custom-resource-role",
      assumedBy: new ServicePrincipal("lambda.amazonaws.com"),
    });

    const iotCustomResourceIamPolicy = new ManagedPolicy(
      this,
      "iot-custom-resource",
      {
        roles: [iotCustomResourceRole],
        managedPolicyName: "iot-custom-resource",
        statements: [
          new PolicyStatement({
            actions: [
              // For attaching IoT policy to special state thing group during resource creation
              "iot:AttachPolicy",
              // For detaching IoT policy to special state thing group during resource deletion
              "iot:DetachPolicy",
              // turn on IoT fleet indexing during resource creation
              "iot:UpdateIndexingConfiguration",
            ],
            resources: ["*"],
          }),
        ],
      }
    );

    const attachCreatedPolicy = new AwsCustomResource(
      this,
      "AttachCreatedPolicy",
      {
        onCreate: {
          service: "@aws-sdk/client-iot",
          action: "attachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_CreatedState`,
            policyName: `CreatedPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of("attachCreated"),
        },
        onUpdate: {
          service: "@aws-sdk/client-iot",
          action: "attachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_CreatedState`,
            policyName: `CreatedPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of("attachCreated"),
        },
        onDelete: {
          service: "@aws-sdk/client-iot",
          action: "detachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_CreatedState`,
            policyName: `CreatedPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of("detachCreated"),
        },
        role: iotCustomResourceRole,
        removalPolicy: RemovalPolicy.DESTROY,
        installLatestAwsSdk: true,
      }
    );
    attachCreatedPolicy.node.addDependency(createdStateIotPolicy);
    attachCreatedPolicy.node.addDependency(createdStateThingGroup);
    attachCreatedPolicy.node.addDependency(iotCustomResourceIamPolicy);

    const attachIotJobPolicyToCreatedState = new AwsCustomResource(
      this,
      "AttachIotJobPolicyToCreatedState",
      {
        onCreate: {
          service: "@aws-sdk/client-iot",
          action: "attachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_CreatedState`,
            policyName: `IotJobPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of(
            "attachIotJobPolicyCreated"
          ),
        },
        onUpdate: {
          service: "@aws-sdk/client-iot",
          action: "attachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_CreatedState`,
            policyName: `IotJobPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of(
            "attachIotJobPolicyCreated"
          ),
        },
        onDelete: {
          service: "@aws-sdk/client-iot",
          action: "detachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_CreatedState`,
            policyName: `IotJobPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of(
            "detachIotJobPolicyCreated"
          ),
        },
        role: iotCustomResourceRole,
        removalPolicy: RemovalPolicy.DESTROY,
        installLatestAwsSdk: true,
      }
    );
    attachIotJobPolicyToCreatedState.node.addDependency(iotJobsPolicy);
    attachIotJobPolicyToCreatedState.node.addDependency(createdStateThingGroup);
    attachIotJobPolicyToCreatedState.node.addDependency(
      iotCustomResourceIamPolicy
    );

    const disabledStateIotPolicy = new iot.CfnPolicy(
      this,
      "DisabledStatePolicy",
      {
        policyDocument: {
          Version: "2012-10-17",
          Statement: [
            {
              Effect: "Allow",
              Action: ["iot:Connect"],
              Resource: [
                `arn:aws:iot:${this.region}:${this.account}:client/${IOT_CONNECTED_THING_NAME}`,
              ],
            },
            {
              Effect: "Allow",
              Action: ["iot:Publish", "iot:Receive"],
              Resource: [
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/connection`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/name/*/get/*`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/name/*/update/*`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/get`,
              ],
            },
            {
              Effect: "Allow",
              Action: ["iot:Subscribe"],
              Resource: [
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/connection`,
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/name/*/get/*`,
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/name/*/update/*`,
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/get`,
              ],
            },
            {
              Effect: "Allow",
              Action: ["iot:Publish"],
              Resource: [
                `arn:aws:iot:${this.region}:${this.account}:topic/videoanalytics/${IOT_CONNECTED_THING_NAME}/timeline`,
                `arn:aws:iot:${this.region}:${this.account}:topic/videoanalytics/${IOT_CONNECTED_THING_NAME}/ai-metadata`,
                `arn:aws:iot:${this.region}:${this.account}:topic/videoanalytics/${IOT_CONNECTED_THING_NAME}/snapshot`,
              ],
            },
          ],
        },
        policyName: `DisabledPolicy_${this.region}`,
      }
    );

    const disabledStateThingGroup = new iot.CfnThingGroup(
      this,
      "DisabledStateThingGroup",
      {
        thingGroupName: "SpecialGroup_DisabledState",
      }
    );

    const attachDisabledPolicy = new AwsCustomResource(
      this,
      "AttachDisabledPolicy",
      {
        onCreate: {
          service: "@aws-sdk/client-iot",
          action: "attachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_DisabledState`,
            policyName: `DisabledPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of("attachDisabled"),
        },
        onUpdate: {
          service: "@aws-sdk/client-iot",
          action: "attachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_DisabledState`,
            policyName: `DisabledPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of("attachDisabled"),
        },
        onDelete: {
          service: "@aws-sdk/client-iot",
          action: "detachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_DisabledState`,
            policyName: `DisabledPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of("detachDisabled"),
        },
        role: iotCustomResourceRole,
        removalPolicy: RemovalPolicy.DESTROY,
        installLatestAwsSdk: true,
      }
    );
    attachDisabledPolicy.node.addDependency(disabledStateIotPolicy);
    attachDisabledPolicy.node.addDependency(disabledStateThingGroup);
    attachDisabledPolicy.node.addDependency(iotCustomResourceIamPolicy);

    const attachIotJobPolicyToDisabledState = new AwsCustomResource(
      this,
      "AttachIotJobPolicyToDisabledState",
      {
        onCreate: {
          service: "@aws-sdk/client-iot",
          action: "attachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_DisabledState`,
            policyName: `IotJobPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of(
            "attachIotJobPolicyDisabled"
          ),
        },
        onUpdate: {
          service: "@aws-sdk/client-iot",
          action: "attachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_DisabledState`,
            policyName: `IotJobPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of(
            "attachIotJobPolicyDisabled"
          ),
        },
        onDelete: {
          service: "@aws-sdk/client-iot",
          action: "detachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_DisabledState`,
            policyName: `IotJobPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of(
            "detachIotJobPolicyDisabled"
          ),
        },
        role: iotCustomResourceRole,
        removalPolicy: RemovalPolicy.DESTROY,
        installLatestAwsSdk: true,
      }
    );
    attachIotJobPolicyToDisabledState.node.addDependency(iotJobsPolicy);
    attachIotJobPolicyToDisabledState.node.addDependency(
      disabledStateThingGroup
    );
    attachIotJobPolicyToDisabledState.node.addDependency(
      iotCustomResourceIamPolicy
    );

    const enabledStateIotPolicy = new iot.CfnPolicy(
      this,
      "EnabledStatePolicy",
      {
        policyDocument: {
          Version: "2012-10-17",
          Statement: [
            {
              Effect: "Allow",
              Action: ["iot:Connect"],
              Resource: [
                `arn:aws:iot:${this.region}:${this.account}:client/${IOT_CONNECTED_THING_NAME}`,
              ],
            },
            {
              Effect: "Allow",
              Action: ["iot:Publish", "iot:Receive"],
              Resource: [
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/connection`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/name/*/get*`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/name/*/update*`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/get*`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/update*`,
              ],
            },
            {
              Effect: "Allow",
              Action: ["iot:Subscribe"],
              Resource: [
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/connection`,
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/name/*/get/*`,
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/name/*/update/*`,
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/get/*`,
                `arn:aws:iot:${this.region}:${this.account}:topicfilter/$aws/things/${IOT_CONNECTED_THING_NAME}/shadow/update/*`,
              ],
            },
            {
              Effect: "Allow",
              Action: ["iot:Publish"],
              Resource: [
                `arn:aws:iot:${this.region}:${this.account}:topic/videoanalytics/${IOT_CONNECTED_THING_NAME}/timeline`,
                `arn:aws:iot:${this.region}:${this.account}:topic/videoanalytics/${IOT_CONNECTED_THING_NAME}/ai-metadata`,
                `arn:aws:iot:${this.region}:${this.account}:topic/videoanalytics/${IOT_CONNECTED_THING_NAME}/snapshot`,
                `arn:aws:iot:${this.region}:${this.account}:topic/$aws/rules/DeviceTelemetryCloudWatchLogsRule/things/${IOT_CONNECTED_THING_NAME}/logs`,
              ],
            },
          ],
        },
        policyName: `EnabledPolicy_${this.region}`,
      }
    );

    const enabledStateThingGroup = new iot.CfnThingGroup(
      this,
      "EnabledStateThingGroup",
      {
        thingGroupName: "SpecialGroup_EnabledState",
      }
    );

    const attachEnabledPolicy = new AwsCustomResource(
      this,
      "AttachEnabledPolicy",
      {
        onCreate: {
          service: "@aws-sdk/client-iot",
          action: "attachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_EnabledState`,
            policyName: `EnabledPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of("attachEnabled"),
        },
        onUpdate: {
          service: "@aws-sdk/client-iot",
          action: "attachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_EnabledState`,
            policyName: `EnabledPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of("attachEnabled"),
        },
        onDelete: {
          service: "@aws-sdk/client-iot",
          action: "detachPolicy",
          parameters: {
            target: `arn:aws:iot:${this.region}:${this.account}:thinggroup/SpecialGroup_EnabledState`,
            policyName: `EnabledPolicy_${this.region}`,
          },
          physicalResourceId: PhysicalResourceId.of("detachEnabled"),
        },
        role: iotCustomResourceRole,
        removalPolicy: RemovalPolicy.DESTROY,
        installLatestAwsSdk: true,
      }
    );
    attachEnabledPolicy.node.addDependency(enabledStateIotPolicy);
    attachEnabledPolicy.node.addDependency(enabledStateThingGroup);
    attachEnabledPolicy.node.addDependency(iotCustomResourceIamPolicy);

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
    attachIotJobPolicyToEnabledState.node.addDependency(enabledStateThingGroup);
    attachIotJobPolicyToEnabledState.node.addDependency(
      iotCustomResourceIamPolicy
    );

    // Log Group for deviceTelemetryCloudWatchLogsPolicy and Rule
    const deviceTelemetryCloudWatchLogGroup = new LogGroup(
      this,
      "DeviceTelemetryCloudWatchLogGroup",
      {
        logGroupName: "DeviceTelemetryCloudWatchLogGroup",
        removalPolicy: RemovalPolicy.RETAIN_ON_UPDATE_OR_DELETE,
        retention: RetentionDays.TWO_WEEKS,
      }
    );

    // Managed policies for deviceTelemetryCloudWatchLogsRuleRole
    const deviceTelemetryCloudWatchLogsPolicy = new ManagedPolicy(
      this,
      "DeviceTelemetryCloudWatchLogsPolicy",
      {
        managedPolicyName: "DeviceTelemetryCloudWatchLogsPolicy",
        description:
          "Policy for log stream creation for telemetry information from edge device to customer account CloudWatch Logs",
        document: new PolicyDocument({
          statements: [
            new PolicyStatement({
              effect: Effect.ALLOW,
              actions: [
                "logs:CreateLogStream",
                "logs:DescribeLogStreams",
                "logs:PutLogEvents",
              ],
              resources: [deviceTelemetryCloudWatchLogGroup.logGroupArn],
            }),
          ],
        }),
      }
    );

    // deviceTelemetryCloudWatchLogsRuleRole to access CloudWatch Logs with correct permissions
    const deviceTelemetryCloudWatchLogsRuleRole = new Role(
      this,
      "DeviceTelemetryCloudWatchLogsRuleRole",
      {
        roleName: "DeviceTelemetryCloudWatchLogsRuleRole",
        assumedBy: new ServicePrincipal("iot.amazonaws.com"),
        managedPolicies: [deviceTelemetryCloudWatchLogsPolicy],
      }
    );

    // Create the topic rule deviceTelemetryCloudWatchLogsRule
    new iot.CfnTopicRule(this, "DeviceTelemetryCloudWatchLogsRule", {
      ruleName: "DeviceTelemetryCloudWatchLogsRule",
      topicRulePayload: {
        sql: "SELECT (SELECT * AS log FROM * WHERE timestamp <> '' AND message.level <> '') as logsList, clientid() AS deviceId FROM 'rules/deviceTelemetryCloudWatchLogsRule/things/+/logs'",
        awsIotSqlVersion: "2016-03-23",
        ruleDisabled: false,
        actions: [
          {
            cloudwatchLogs: {
              logGroupName: deviceTelemetryCloudWatchLogGroup.logGroupName,
              roleArn: deviceTelemetryCloudWatchLogsRuleRole.roleArn,
              batchMode: true,
            },
          },
        ],
      },
    });
  }
}
