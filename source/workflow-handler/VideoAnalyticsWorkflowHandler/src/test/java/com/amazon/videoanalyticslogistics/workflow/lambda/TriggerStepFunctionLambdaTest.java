package com.amazonaws.videoanalytics.workflow.lambda;

import com.amazonaws.videoanalytics.workflow.functions.DynamoRecordProcessor;
import com.amazonaws.videoanalytics.workflow.util.DynamoDbUtil;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.amazonaws.videoanalytics.workflow.util.Constants.DELETING_STATUS;
import static com.amazonaws.videoanalytics.workflow.util.Constants.FINALIZING_STATUS;
import static com.amazonaws.videoanalytics.workflow.util.Constants.PARTITION_KEY;
import static com.amazonaws.videoanalytics.workflow.util.Constants.STATE_MACHINE_ARN;
import static com.amazonaws.videoanalytics.workflow.util.Constants.STATE_MACHINE_FOR_DELETE_ARN;
import static com.amazonaws.videoanalytics.workflow.util.Constants.STATE_MACHINE_FOR_FINALIZE_ARN;
import static com.amazonaws.videoanalytics.workflow.util.Constants.STATUS_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.anyString;

public class TriggerStepFunctionLambdaTest {

    private DynamodbEvent ddbEvent;
    private TriggerStepFunctionLambda triggerStepFunctionLambda;

    private static final String TEST = "TEST";
    private static final String COMPLETED = "COMPLETED";
    private static final String RUNNING = "RUNNING";
    @Mock
    private DynamoDbUtil dynamoDbUtil;
    @Mock
    private Context context;
    @Mock
    private StreamRecord mockStreamRecord;
    @Mock
    private DynamodbEvent.DynamodbStreamRecord mockDBStreamRecord;

    @Mock
    private DynamoRecordProcessor recordProcessor;


    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        environmentVariables.set("PARTITION_KEY", PARTITION_KEY);
        environmentVariables.set(STATE_MACHINE_ARN, "arn");
        environmentVariables.set(STATE_MACHINE_FOR_DELETE_ARN, "deleteArn");
        environmentVariables.set(STATE_MACHINE_FOR_FINALIZE_ARN, "finalizeArn");
        triggerStepFunctionLambda = new TriggerStepFunctionLambda(recordProcessor);
        ddbEvent = new DynamodbEvent();
        when(dynamoDbUtil.getValueFromRecord(any(DynamodbEvent.DynamodbStreamRecord.class), anyString()))
                .thenReturn(Optional.of(TEST));
    }

    @Test
    public void testHandleRequestWithEmptyRecords() {
        ddbEvent.setRecords(Collections.EMPTY_LIST);

        List<StartExecutionResult> executionResultList = triggerStepFunctionLambda.handleRequest(ddbEvent, context);
        assertEquals(executionResultList.size(), 0);
        verifyNoInteractions(recordProcessor);
    }

    @Test
    public void testHandleRequestWithInsertStreamRecord() {
        ddbEvent.setRecords(Collections.singletonList(mockDBStreamRecord));
        when(mockDBStreamRecord.getEventName()).thenReturn(String.valueOf(OperationType.INSERT));
        when(recordProcessor.processRecord(mockDBStreamRecord, "arn")).thenReturn(new StartExecutionResult());

        final List<StartExecutionResult> result = triggerStepFunctionLambda.handleRequest(ddbEvent, context);
        assertEquals(result.size(), 1);
    }

    @Test
    public void testHandleRequestWithNonInsertStreamRecord() {
        ddbEvent.setRecords(Collections.singletonList(mockDBStreamRecord));
        when(mockDBStreamRecord.getEventName()).thenReturn(String.valueOf(OperationType.REMOVE));
        List<StartExecutionResult> executionResultList = triggerStepFunctionLambda.handleRequest(ddbEvent, context);
        assertEquals(executionResultList.size(), 0);
        verifyNoInteractions(recordProcessor);
    }

    @Test
    public void testHandleRequestWithModifyStreamRecordNewStatusDeleting() {
        final DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord =
            prepareMockStreamRecordForModify(COMPLETED, DELETING_STATUS);
        ddbEvent.setRecords(Collections.singletonList(dynamodbStreamRecord));
        when(dynamodbStreamRecord.getEventName()).thenReturn(String.valueOf(OperationType.MODIFY));
        when(recordProcessor.processRecord(dynamodbStreamRecord, "deleteArn")).thenReturn(new StartExecutionResult());

        List<StartExecutionResult> executionResultList = triggerStepFunctionLambda.handleRequest(ddbEvent, context);
        assertEquals(executionResultList.size(), 1);
    }

    @Test
    public void testHandleRequestWithModifyStreamRecordNewStatusFinalizing() {
        final DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord =
            prepareMockStreamRecordForModify(COMPLETED, FINALIZING_STATUS);
        ddbEvent.setRecords(Collections.singletonList(dynamodbStreamRecord));
        when(dynamodbStreamRecord.getEventName()).thenReturn(String.valueOf(OperationType.MODIFY));
        when(recordProcessor.processRecord(dynamodbStreamRecord, "finalizingArn"))
            .thenReturn(new StartExecutionResult());

        List<StartExecutionResult> executionResultList = triggerStepFunctionLambda.handleRequest(ddbEvent, context);
        assertEquals(executionResultList.size(), 1);
    }

    @Test
    public void testHandleRequestWithModifyStreamRecordSameStatus() {
        final DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord =
            prepareMockStreamRecordForModify(DELETING_STATUS, DELETING_STATUS);
        ddbEvent.setRecords(Collections.singletonList(dynamodbStreamRecord));
        when(dynamodbStreamRecord.getEventName()).thenReturn(String.valueOf(OperationType.MODIFY));

        List<StartExecutionResult> executionResultList = triggerStepFunctionLambda.handleRequest(ddbEvent, context);
        assertEquals(executionResultList.size(), 0);
        verifyNoInteractions(recordProcessor);
    }

    @Test
    public void testHandleRequestWithModifyStreamRecordNewStatusNotDeleting() {
        final DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord =
            prepareMockStreamRecordForModify(RUNNING, COMPLETED);
        ddbEvent.setRecords(Collections.singletonList(dynamodbStreamRecord));
        when(dynamodbStreamRecord.getEventName()).thenReturn(String.valueOf(OperationType.MODIFY));

        List<StartExecutionResult> executionResultList = triggerStepFunctionLambda.handleRequest(ddbEvent, context);
        assertEquals(executionResultList.size(), 0);
    }

    private AttributeValue createImageAttribute(String value) {
        return new AttributeValue().withS(value);
    }

    private DynamodbEvent.DynamodbStreamRecord prepareMockStreamRecordForModify(String oldValue, String newValue) {

        Map<String, AttributeValue> newDbImage = Map.of(
                STATUS_KEY, createImageAttribute(newValue)
        );
        Map<String, AttributeValue> oldDbImage = Map.of(
                STATUS_KEY, createImageAttribute(oldValue)
        );

        doReturn(newDbImage).when(mockStreamRecord).getNewImage();
        doReturn(oldDbImage).when(mockStreamRecord).getOldImage();
        doReturn(mockStreamRecord).when(mockDBStreamRecord).getDynamodb();
        return mockDBStreamRecord;
    }
}
