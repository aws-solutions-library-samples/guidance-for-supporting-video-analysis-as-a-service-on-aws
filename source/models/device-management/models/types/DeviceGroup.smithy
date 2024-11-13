$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

use com.amazonaws.videoanalytics#DeviceId
use com.amazonaws.videoanalytics#KeyValueMap

structure DeviceGroups {
    deviceGroupId: DeviceGroupId
}

list DeviceGroupsList {
    member: DeviceGroups
}

list DeviceGroupIdList {
    member: DeviceGroupId
}

structure DeviceGroupAttributePayload {
    attributes: KeyValueMap,
    merge: Boolean
}

structure DeviceGroupMetaData {
    @timestampFormat("date-time")
    creationDate: Timestamp,
    parentGroupId: DeviceGroupId,
    rootToParentDeviceGroups: DeviceGroupsList
}

structure DeviceGroupPayload {
    deviceGroupIdList: DeviceGroupIdList,
    remove: Boolean
}

// Device group ID has the same pattern as IoT thing group name: https://docs.aws.amazon.com/iot/latest/apireference/API_DescribeThingGroup.html#API_DescribeThingGroup_RequestParameters
@pattern("^[a-zA-Z0-9:_\\-]+$")
@length(min: 1, max: 128)
string DeviceGroupId

// Device type ID has the same pattern as IoT thing type name: https://docs.aws.amazon.com/iot/latest/apireference/API_CreateThingType.html#API_CreateThingType_RequestParameters
@pattern("^[a-zA-Z0-9:_\\-]+$")
@length(min: 1, max: 128)
string DeviceTypeId
