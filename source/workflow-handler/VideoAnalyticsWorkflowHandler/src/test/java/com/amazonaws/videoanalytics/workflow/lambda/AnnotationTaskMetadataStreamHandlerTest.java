package com.amazonaws.videoanalytics.workflow.lambda;

import static com.amazonaws.videoanalytics.workflow.util.Constants.PARTITION_KEY;
import static com.amazonaws.videoanalytics.workflow.util.Constants.STATE_MACHINE_ARN;
import static com.amazonaws.videoanalytics.workflow.util.Constants.STATE_MACHINE_FOR_DELETE_ARN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.videoanalytics.workflow.functions.DynamoRecordProcessor;
import com.amazonaws.videoanalytics.workflow.util.DynamoDbUtil;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AnnotationTaskMetadataStreamHandlerTest {
    private DynamodbEvent ddbEvent;
    private AnnotationTaskMetadataStreamHandler classUnderTest;

    private static final String TEST = "TEST";
    private static final String STATUS_KEY = "status";
    private static final String COLLECTION = "COLLECTION";
    private static final String DELETING = "DELETING";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String RUNNING = "RUNNING";
    private static final String CANCELLED = "CANCELLED";

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
        classUnderTest = new AnnotationTaskMetadataStreamHandler(recordProcessor);
        ddbEvent = new DynamodbEvent();
        when(dynamoDbUtil.getValueFromRecord(any(DynamodbEvent.DynamodbStreamRecord.class), anyString()))
            .thenReturn(Optional.of(TEST));
    }

    @Test
    public void testHandleRequestWithEmptyRecords() {
        ddbEvent.setRecords(List.of());

        List<StartExecutionResult> executionResultList = classUnderTest.handleRequest(ddbEvent, context);
        assertEquals(executionResultList.size(), 0);
        verifyNoInteractions(recordProcessor);
    }

    @Test
    public void testHandleRequestWithInsertStreamRecord() {
        ddbEvent.setRecords(Collections.singletonList(mockDBStreamRecord));
        when(mockDBStreamRecord.getEventName()).thenReturn(String.valueOf(OperationType.INSERT));
        final List<StartExecutionResult> result = classUnderTest.handleRequest(ddbEvent, context);
        assertEquals(result.size(), 0);
        verifyNoInteractions(recordProcessor);
    }

    @Test
    public void testHandleRequestWithModifyStreamRecordNewStatusDeleting() {
        final DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord =
            prepareMockStreamRecordForModify(COLLECTION, DELETING);
        ddbEvent.setRecords(Collections.singletonList(dynamodbStreamRecord));
        when(dynamodbStreamRecord.getEventName()).thenReturn(String.valueOf(OperationType.MODIFY));
        when(recordProcessor.processRecord(dynamodbStreamRecord, "deleteArn")).thenReturn(new StartExecutionResult());

        List<StartExecutionResult> executionResultList = classUnderTest.handleRequest(ddbEvent, context);
        assertEquals(executionResultList.size(), 1);
    }

    @Test
    public void testHandleRequestWithModifyStreamRecordNewStatusInProgress() {
        final DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord =
            prepareMockStreamRecordForModify(COLLECTION, IN_PROGRESS);
        ddbEvent.setRecords(Collections.singletonList(dynamodbStreamRecord));
        when(dynamodbStreamRecord.getEventName()).thenReturn(String.valueOf(OperationType.MODIFY));
        when(recordProcessor.processRecord(dynamodbStreamRecord, "arn")).thenReturn(new StartExecutionResult());

        List<StartExecutionResult> executionResultList = classUnderTest.handleRequest(ddbEvent, context);
        assertEquals(executionResultList.size(), 1);
    }

    @Test
    public void testHandleRequestWithModifyStreamRecordSameStatus() {
        final DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord =
            prepareMockStreamRecordForModify(DELETING, DELETING);
        ddbEvent.setRecords(Collections.singletonList(dynamodbStreamRecord));
        when(dynamodbStreamRecord.getEventName()).thenReturn(String.valueOf(OperationType.MODIFY));

        List<StartExecutionResult> executionResultList = classUnderTest.handleRequest(ddbEvent, context);
        assertEquals(executionResultList.size(), 0);
        verifyNoInteractions(recordProcessor);
    }

    @Test
    public void testHandleRequestWithModifyStreamRecordNewStatusCancelled() {
        final DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord =
            prepareMockStreamRecordForModify(IN_PROGRESS, CANCELLED);
        ddbEvent.setRecords(Collections.singletonList(dynamodbStreamRecord));
        when(dynamodbStreamRecord.getEventName()).thenReturn(String.valueOf(OperationType.MODIFY));

        List<StartExecutionResult> executionResultList = classUnderTest.handleRequest(ddbEvent, context);
        assertEquals(executionResultList.size(), 0);
        verifyNoInteractions(recordProcessor);
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
