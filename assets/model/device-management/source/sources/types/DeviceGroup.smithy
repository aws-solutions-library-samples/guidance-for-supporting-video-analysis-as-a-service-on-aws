$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

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
creationDate: Timestamp,
parentGroupId: DeviceGroupId,
rootToParentDeviceGroups: DeviceGroupsList
}

structure DeviceGroupPayload {
deviceGroupIdList: DeviceGroupIdList,
remove: Boolean
}

list DeviceIdList {
member: String
}

@length(min: 1, max: 128)
string DeviceGroupId
