import { EngineVersion } from 'aws-cdk-lib/aws-opensearchservice';
import type { DomainProps } from 'aws-cdk-lib/aws-opensearchservice/lib/domain';
import {Key} from "aws-cdk-lib/aws-kms";
import { EbsDeviceVolumeType } from 'aws-cdk-lib/aws-ec2';

export function getVLSearchDomainProps(
  domainName: string,
  key: Key
): DomainProps {
  return {
    version: EngineVersion.OPENSEARCH_2_9,
    domainName: domainName,
    enableVersionUpgrade: true,
    capacity: {
      dataNodes: 3,
      dataNodeInstanceType: 'c6g.large.search',
      masterNodes: 3,
      masterNodeInstanceType: 'm6g.large.search',
      warmNodes: 3,
      warmInstanceType: 'ultrawarm1.medium.search'
    },
    ebs: {
      enabled: true,
      volumeSize: 100,
      volumeType: EbsDeviceVolumeType.GP3
    },
    nodeToNodeEncryption: true,
    encryptionAtRest: {
      enabled: true,
      kmsKey: key
    },
    zoneAwareness: {
      enabled: true,
      availabilityZoneCount: 3
    },
    enforceHttps: true
  };
}