$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

resource DeviceNotification {
    operations: [
        GetCreateDeviceNotificationStatus,
        GetCreateNotificationRuleStatus,
        GetDeleteDeviceNotificationStatus,
        GetNotificationRule,
        GetUpdateNotificationRuleStatus,
        StartCreateDeviceNotification,
        StartCreateNotificationRule,
        StartDeleteDeviceNotification,
        StartUpdateNotificationRule
    ]
}
