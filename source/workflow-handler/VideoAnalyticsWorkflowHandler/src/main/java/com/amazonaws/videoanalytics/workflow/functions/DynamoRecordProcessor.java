package com.amazonaws.videoanalytics.workflow.functions;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;

public interface DynamoRecordProcessor {
    StartExecutionResult processRecord(DynamodbStreamRecord record, String stepFunctionArn);
}
