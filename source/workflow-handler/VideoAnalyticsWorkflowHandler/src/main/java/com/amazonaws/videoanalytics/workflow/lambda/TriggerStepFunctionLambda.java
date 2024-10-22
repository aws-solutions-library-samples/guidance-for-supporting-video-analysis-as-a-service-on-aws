package com.amazonaws.videoanalytics.workflow.lambda;

import com.amazonaws.videoanalytics.workflow.dagger.AWSVideoAnalyticsWorkflowHandlerLambdaComponent;
import com.amazonaws.videoanalytics.workflow.functions.DynamoRecordProcessor;
import com.amazonaws.videoanalytics.workflow.util.Constants;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import com.amazonaws.videoanalytics.workflow.dagger.DaggerAWSVideoAnalyticsWorkflowHandlerLambdaComponent;

import dagger.internal.Preconditions;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Lambda handler for Dynamo streams that triggers step functions for new records and
 * record modifications when the status changes to `DELETING`
 */
public class TriggerStepFunctionLambda implements RequestHandler<DynamodbEvent, List<StartExecutionResult>> {
    private static final String WORKFLOW_STEP_FUNCTION_ARN = System.getenv(Constants.STATE_MACHINE_ARN);
    // Temporary workaround to trigger step function on DDBStream Modify for deleting bucket
    private static final String WORKFLOW_STEP_FUNCTION_FOR_DELETE_ARN =
            System.getenv(Constants.STATE_MACHINE_FOR_DELETE_ARN);

    private static final String WORKFLOW_STEP_FUNCTION_FOR_FINALIZE_ARN =
        System.getenv(Constants.STATE_MACHINE_FOR_FINALIZE_ARN);

    private final DynamoRecordProcessor recordProcessor;

    public TriggerStepFunctionLambda() {
        AWSVideoAnalyticsWorkflowHandlerLambdaComponent awsVideoAnalyticsWorkflowHandlerLambdaComponent =
                DaggerAWSVideoAnalyticsWorkflowHandlerLambdaComponent.create();
        recordProcessor = awsVideoAnalyticsWorkflowHandlerLambdaComponent.getRecordProcessor();
    }

    @Inject
    public TriggerStepFunctionLambda(final DynamoRecordProcessor recordProcessor) {
        this.recordProcessor = Preconditions.checkNotNull(recordProcessor, "Dynamo record processor cannot be null.");
    }

    @Override
    public List<StartExecutionResult> handleRequest(final DynamodbEvent ddbEvent, final Context context) {
        final List<StartExecutionResult> startExecutionRequestList = new ArrayList<>();

        final List<DynamodbStreamRecord> ddbRecords = ddbEvent.getRecords();
        for (DynamodbStreamRecord record : ddbRecords) {
            final OperationType recordEventType = OperationType.fromValue(record.getEventName());
            if (OperationType.INSERT == recordEventType) {
                startExecutionRequestList.add(processRecord(record));
            } else if (OperationType.MODIFY == recordEventType) {
                // checks added to trigger delete workflow only on status change to DELETING
                AttributeValue newStatusValue = record.getDynamodb().getNewImage().get(Constants.STATUS_KEY);
                AttributeValue oldStatusValue = record.getDynamodb().getOldImage().get(Constants.STATUS_KEY);
                if (newStatusValue != null && oldStatusValue != null) {
                    String newStatus = newStatusValue.getS();
                    String oldStatus = oldStatusValue.getS();
                    if (newStatus.equals(Constants.DELETING_STATUS) && !newStatus.equals(oldStatus)) {
                        startExecutionRequestList.add(processRecordModifyForDelete(record));
                    }
                    if (newStatus.equals(Constants.FINALIZING_STATUS) && !newStatus.equals(oldStatus)) {
                        startExecutionRequestList.add(processRecordModifyForFinalize(record));
                    }
                }
            }
        }
        return startExecutionRequestList;
    }

    private StartExecutionResult processRecord(final DynamodbStreamRecord record) {
        return recordProcessor.processRecord(record, WORKFLOW_STEP_FUNCTION_ARN);
    }

    private StartExecutionResult processRecordModifyForDelete(final DynamodbStreamRecord record) {
        return recordProcessor.processRecord(record, WORKFLOW_STEP_FUNCTION_FOR_DELETE_ARN);
    }

    private StartExecutionResult processRecordModifyForFinalize(final DynamodbStreamRecord record) {
        return recordProcessor.processRecord(record, WORKFLOW_STEP_FUNCTION_FOR_FINALIZE_ARN);
    }
}
