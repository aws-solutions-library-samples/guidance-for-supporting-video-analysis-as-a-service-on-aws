package com.amazonaws.videoanalytics.devicemanagement.utils;

public class WorkflowConstants {
    public static final String DEVICE_ID = "deviceId";
    public static final String DEVICE_TYPE = "deviceType";
    public static final String JOB_ID = "jobId";
    public static final String MESSAGE_FIELD = "messageField";
    public static final String PROVISIONING_SHADOW_NAME = "provisioning";
    public static final String VIDEO_ENCODER_SHADOW_NAME = "videoEncoder";
    public static final String AI_SHADOW_NAME = "ai";
    public static final String DEVICE_CREATE_CREDENTIALS_MESSAGE = "createCredentials";
    public static final String DEVICE_DEREGISTER_MESSAGE = "deregister";
    public static final String CREDENTIALS_ACTIVATED_MESSAGE = "credentialsActivated";

    public static final String DEVICE_DISABLED_MESSAGE = "disabled";
    public static final String DEVICE_ENABLED_MESSAGE = "enabled";
    public static final String ENABLED_STATE_NOTIFICATIONS = "enabledStateNotifications";
    public static final String DISABLED_STATE_NOTIFICATIONS = "disabledStateNotifications";
    public static final String STATE_EVENT_MQTT_TOPIC_PREFIX = "notifications/state/";
    public static final String CONNECTIVITY_ON_NOTIFICATIONS = "connectivityOnNotifications";
    public static final String CONNECTIVITY_OFF_NOTIFICATIONS = "connectivityOffNotifications";

    public static final String REQUEST_CAPABILITIES = "requestCapabilities";

    private WorkflowConstants() {
        // Private default constructor so that JaCoCo marks utility class as covered
    }
}
