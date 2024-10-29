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
} from "aws-cdk-lib/aws-iam";
import { BlockPublicAccess, Bucket, ObjectOwnership } from "aws-cdk-lib/aws-s3";
import * as iot from "aws-cdk-lib/aws-iot";

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
  }
}
