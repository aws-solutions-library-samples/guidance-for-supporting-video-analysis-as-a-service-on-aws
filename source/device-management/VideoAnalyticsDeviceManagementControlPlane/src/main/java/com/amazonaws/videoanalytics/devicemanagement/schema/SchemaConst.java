package com.amazonaws.videoanalytics.devicemanagement.schema;

public final class SchemaConst {
    //    Common Schema Attributes
    public static final String CREATED_AT = "CreatedAt";
    public static final String JOB_ID = "JobId";
    public static final String WORKFLOW_NAME = "WorkflowName";
    public static final String JOB_STATUS = "JobStatus";
    public static final String DEVICE_ID = "DeviceId";
    public static final String COMPLETE = "Complete";
    public static final String LAST_UPDATED = "LastUpdated";
    public static final String ERROR_MESSAGE = "ErrorMessage";

    // Device job workflow table names
    public static final String CREATE_DEVICE_TABLE_NAME = "CreateDeviceTable";
    // Saved information from create keys + certs
    public static final String CERTIFICATE_ARN = "CertificateArn";
    public static final String CERTIFICATE_ID = "CertificateId";
    // Saved information from VL device registration workflow
    public static final String VL_JOB_ID = "VlJobId";
    public static final String VL_JOB_STATUS = "VlJobStatus";

}

