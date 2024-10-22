package com.amazonaws.videoanalytics.workflow.util;

public final class Constants {
    private Constants() {}
    public static final String STATE_MACHINE_ARN = "STATE_MACHINE_ARN";

    // Temporary ARN for step function trigger on update
    public static final String STATE_MACHINE_FOR_DELETE_ARN = "STATE_MACHINE_FOR_DELETE_ARN";
    public static final String STATE_MACHINE_FOR_FINALIZE_ARN = "STATE_MACHINE_FOR_FINALIZE_ARN";
    public static final String PARTITION_KEY = "PARTITION_KEY";
    public static final String PARTITION_KEY_RESULT_PATH = "partitionKey";
    public static final String WORKFLOW_NAME = "WorkflowName";
    public static final String DATASET_WORKFLOW_NAME = "workflowName";
    public static final String NULL_ATTRIBUTE_KEY_METRIC = "attributeKeyNull:";
}
