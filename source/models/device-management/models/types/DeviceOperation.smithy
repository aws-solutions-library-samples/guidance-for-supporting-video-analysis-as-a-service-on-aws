$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

use com.amazonaws.videoanalytics#DeviceId
use com.amazonaws.videoanalytics#KeyValueMap

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
    sdkVersion : String,
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
    cloudVideoStreaming: CloudVideoStreamingList
}

structure DeviceConnection {
    status: Boolean,
    @timestampFormat("date-time")
    updatedAt: Timestamp
}

list CloudVideoStreamingList {
    member:CloudVideoStreamingElement
}

structure CloudVideoStreamingElement {
    id: String,
    status: VideoStreamingState,
    @timestampFormat("date-time")
    updatedAt: Timestamp
}

enum VideoStreamingState {
    CONNECTED
    DISCONNECTED
}

list Devices {
    member: DeviceInfo
}

structure DeviceInfo {
    deviceName: String,
    deviceId: DeviceId,
    deviceMetaData: DeviceMetaData,
    deviceSettings: KeyValueMap,
    @timestampFormat("date-time")
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
    @timestampFormat("date-time")
    createdAt: Timestamp,
    @timestampFormat("date-time")
    updatedAt: Timestamp,
    requestParameters: String,
    contents: String,
    errorCode: String,
    errorMessage: String
}
