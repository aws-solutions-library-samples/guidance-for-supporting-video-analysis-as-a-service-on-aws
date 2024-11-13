package com.amazonaws.videoanalytics.devicemanagement.schema;

public final class SchemaConst {
    //    Common Schema Attributes
    public static final String CREATED_AT = "CreatedAt";
    public static final String JOB_ID = "JobId";
    public static final String WORKFLOW_NAME = "WorkflowName";
    public static final String JOB_STATUS = "JobStatus";
    public static final String DEVICE_ID = "DeviceId";
    public static final String DEVICE_CONFIGURATIONS = "DeviceConfigurations";
    public static final String COMPLETE = "Complete";
    public static final String LAST_UPDATED = "LastUpdated";
    public static final String CUSTOMER_ACCOUNT_ID = "CustomerAccountId";
    public static final String CURRENT_DEVICE_STATE = "CurrentDeviceState";
    public static final String NEW_DEVICE_STATE = "NewDeviceState";
    public static final String MESSAGED_DEVICE = "MessagedDevice";
    public static final String RULE_ID = "RuleId";
    public static final String EVENT_CATEGORY = "EventCategory";
    public static final String RULE_DISABLED = "RuleDisabled";
    public static final String CONDITION = "Condition";
    public static final String NOTIFICATION_DESTINATION = "NotificationDestination";
    public static final String ERROR_DESTINATION = "ErrorDestination";
    public static final String IOT_RULE_CREATED = "IotRuleCreated";
    public static final String ERROR_MESSAGE = "ErrorMessage";
    public static final String DEVICE_GROUP_ID = "DeviceGroupId";
    public static final String DEVICE_TYPE_ID = "DeviceTypeId";
    public static final String REMOVE_DEVICE_TYPE = "RemoveDeviceType";
    public static final String CONFIGURATIONS = "Configurations";
    public static final String CURRENT_VIDEO_SETTINGS = "CurrentVideoSettings";
    public static final String CURRENT_AI_SETTINGS = "CurrentAiSettings";
    public static final String THING_GROUP_ID_LIST = "ThingGroupIdList";
    public static final String REMOVE_THING_GROUP = "RemoveThingGroup";
    public static final String DEVICE_CAPABILITIES_MAP = "DeviceCapabilitiesMap";
    public static final String COMMAND = "Command";
    public static final String RETRIES = "Retries";
    public static final String TIMEOUT = "Timeout";
    public static final String IOT_JOB_ID = "IotJobId";
    public static final String IOT_INSTALL_JOB_ID = "IotInstallJobId";
    public static final String S3_URI = "S3Uri";

    // Device job workflow table names
    public static final String CREATE_DEVICE_TABLE_NAME = "CreateDeviceTable";
    public static final String DEVICE_REGISTRATION_TABLE_NAME =
            "DeviceRegistrationTable";
    public static final String DEVICE_CONFIGURATION_TABLE_NAME =
            "DeviceConfigurationTable";
    public static final String DEVICE_DEREGISTRATION_TABLE_NAME =
            "DeviceDeregistrationTable";
    public static final String CREATE_NOTIFICATION_RULE_TABLE_NAME =
            "CreateNotificationRuleTable";
    public static final String DELETE_NOTIFICATION_RULE_TABLE_NAME =
            "DeleteNotificationRuleTable";
    public static final String UPDATE_NOTIFICATION_RULE_TABLE_NAME =
            "UpdateNotificationRuleTable";
    public static final String CREATE_DEVICE_NOTIFICATION_TABLE_NAME =
            "CreateDeviceNotificationTable";
    public static final String DELETE_DEVICE_NOTIFICATION_TABLE_NAME =
            "DeleteDeviceNotificationTable";
    public static final String UPDATE_DEVICE_TABLE_NAME =
            "UpdateDeviceTable";
    public static final String GET_DEVICE_CAPABILITIES_TABLE =
            "GetDeviceCapabilitiesTable";
    public static final String DELETE_DEVICE_TYPE_TABLE_NAME = 
            "DeleteDeviceTypeTable";
    // Saved information from create keys + certs
    public static final String CERTIFICATE_ARN = "CertificateArn";
    public static final String CERTIFICATE_ID = "CertificateId";
    public static final String CERTIFICATE_PEM = "CertificatePem";
    public static final String PRIVATE_KEY = "PrivateKey";
    public static final String PUBLIC_KEY = "PublicKey";
    // Saved information from create policy
    public static final String POLICY_NAME = "PolicyName";
    public static final String POLICY_ARN = "PolicyArn";
    public static final String VL_JOB_ID = "VlJobId";
    public static final String VL_JOB_STATUS = "VlJobStatus";

}

