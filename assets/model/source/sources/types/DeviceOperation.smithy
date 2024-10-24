$version: "2.0"

namespace com.amazonaws.videoanalytics

enum DeviceState {
    ENABLED
    DISABLED
    CREATED
}

structure DeviceMetaData {
    manufacturer: String,
    model: String,
    mac: String,
    ipAddress: IpAddress,
    aiChipset: String,
    firmwareVersion : String,
    fathomSdkVersion : String,
    aiModelVersion: String
    aiSdkVersion : String,
    deviceStatus: DeviceStatus,
}

structure IpAddress {
    publicIpAddress: String,
    privateIpAddress: String
}

structure DeviceStatus {
    deviceState: DeviceState,
    deviceConnection: DeviceConnection,
    cloudVideoStreaming: CloudVideoStreamingList,
    storage: StorageList,
}

structure DeviceConnection {
    status: Boolean,
    updatedAt: Timestamp
}

list CloudVideoStreamingList {
    member:CloudVideoStreamingElement
}

structure CloudVideoStreamingElement {
    id: String,
    status: VideoStreamingState,
    updatedAt: Timestamp
}

enum VideoStreamingState {
    CONNECTED
    DISCONNECTED
}

list StorageList {
    member:StorageElement
}

structure StorageElement {
    status: StorageState,
    totalCapacity: String,
    usedCapacity: String,
    updatedAt: Timestamp,
    id: String
}

enum StorageState {
    NO_CARD
    ERROR
    NOT_FORMATTED
    UNMOUNTED
    FULL
    NORMAL
}

list Devices {
    member: DeviceInfo
}

structure DeviceInfo {
    deviceName: String,
    deviceId: DeviceId,
    deviceGroupId: DeviceGroupId,
    deviceType: String,
    deviceMetaData: DeviceMetaData,
    deviceCapabilities: KeyValueMap,
    deviceSettings: KeyValueMap,
    createdAt: Timestamp
}

list DeviceOperation {
    member: DeviceOperationInfo
}

structure DeviceOperationInfo {
    deviceOperationId: String,
    deviceOperationName: String,
    deviceOperationStatus: String,
    callerAccount: String,
    sourceIPAddress: String,
    createdAt: Timestamp,
    updatedAt: Timestamp,
    requestParameters: String,
    contents: String,
    errorCode: String,
    errorMessage: String
}

