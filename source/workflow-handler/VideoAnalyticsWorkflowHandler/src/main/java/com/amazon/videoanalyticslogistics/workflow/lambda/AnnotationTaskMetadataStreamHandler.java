package com.amazonaws.videoanalytics.workflow.lambda;

import static com.amazonaws.videoanalytics.workflow.util.Constants.STATE_MACHINE_ARN;
import static com.amazonaws.videoanalytics.workflow.util.Constants.STATE_MACHINE_FOR_DELETE_ARN;

import com.amazonaws.videoanalytics.workflow.dagger.AWSVideoAnalyticsWorkflowHandlerLambdaComponent;
import com.amazonaws.videoanalytics.workflow.dagger.DaggerAWSVideoAnalyticsWorkflowHandlerLambdaComponent;
import com.amazonaws.videoanalytics.workflow.functions.DynamoRecordProcessor;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import dagger.internal.Preconditions;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Handler implementation that listens to DynamoDb stream events. This will execute a step function
 * if task metadata status transitions from COLLECTION to IN_PROGRESS or from any status to
 * DELETING.
 */
public class AnnotationTaskMetadataStreamHandler implements
    RequestHandler<DynamodbEvent, List<StartExecutionResult>> {

    private static final Logger LOG = LogManager.getLogger(AnnotationTaskMetadataStreamHandler.class);

    private static final String STATUS_KEY = "status";

    private static final String DELETING_STATUS = "DELETING";

    private static final String IN_PROGRESS_STATUS = "IN_PROGRESS";

    private final DynamoRecordProcessor recordProcessor;

    public AnnotationTaskMetadataStreamHandler() {
        AWSVideoAnalyticsWorkflowHandlerLambdaComponent awsVideoAnalyticsWorkflowHandlerLambdaComponent =
            DaggerAWSVideoAnalyticsWorkflowHandlerLambdaComponent.create();
        recordProcessor = awsVideoAnalyticsWorkflowHandlerLambdaComponent.getRecordProcessor();
    }

    @Inject
    public AnnotationTaskMetadataStreamHandler(final DynamoRecordProcessor recordProcessor) {
        this.recordProcessor = Preconditions.checkNotNull(recordProcessor, "Dynamo record processor cannot be null.");
    }

    @Override
    public List<StartExecutionResult> handleRequest(final DynamodbEvent dynamodbEvent, final Context context) {
        LOG.info(
            "Processing {} Annotation Task Metadata stream records", dynamodbEvent.getRecords().size());
        final List<StartExecutionResult> startExecutionRequestList = new ArrayList<>();
        final List<DynamodbStreamRecord> ddbRecords = dynamodbEvent.getRecords();
        for (DynamodbStreamRecord streamRecord : ddbRecords) {
            final OperationType recordEventType = OperationType.fromValue(streamRecord.getEventName());
            if (OperationType.MODIFY == recordEventType) {
                handleModifyEvent(streamRecord, startExecutionRequestList);
            }
        }
        return startExecutionRequestList;
    }

    private void handleModifyEvent(final DynamodbStreamRecord streamRecord,
        final List<StartExecutionResult> startExecutionRequestList) {
        final AttributeValue newStatusValue = streamRecord.getDynamodb().getNewImage().get(STATUS_KEY);
        final AttributeValue oldStatusValue = streamRecord.getDynamodb().getOldImage().get(STATUS_KEY);

        if (newStatusValue != null && oldStatusValue != null) {
            final String newStatus = newStatusValue.getS();
            final String oldStatus = oldStatusValue.getS();
            final boolean statusChanged = !newStatus.equals(oldStatus);
            if (statusChanged && newStatus.equals(IN_PROGRESS_STATUS)) {
                final String stepFunctionArn = System.getenv(STATE_MACHINE_ARN);
                startExecutionRequestList.add(recordProcessor.processRecord(streamRecord, stepFunctionArn));
            } else if (statusChanged && newStatus.equals(DELETING_STATUS)) {
                final String stepFunctionArn = System.getenv(STATE_MACHINE_FOR_DELETE_ARN);
                startExecutionRequestList.add(recordProcessor.processRecord(streamRecord, stepFunctionArn));
            }
        }
    }
}
