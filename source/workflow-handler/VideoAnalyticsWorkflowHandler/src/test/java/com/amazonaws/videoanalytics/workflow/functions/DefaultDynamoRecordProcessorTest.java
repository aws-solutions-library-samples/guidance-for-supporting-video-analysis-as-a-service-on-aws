package com.amazonaws.videoanalytics.workflow.functions;

import static com.amazonaws.videoanalytics.workflow.util.Constants.PARTITION_KEY;
import static com.amazonaws.videoanalytics.workflow.util.Constants.PARTITION_KEY_RESULT_PATH;
import static com.amazonaws.videoanalytics.workflow.util.Constants.WORKFLOW_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.amazonaws.videoanalytics.workflow.util.Constants;
import com.amazonaws.videoanalytics.workflow.util.DynamoDbUtil;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DefaultDynamoRecordProcessorTest {

    private static final String TEST = "TEST";

    private static final String WORKFLOW_STEP_FUNCTION_ARN = "ARN";

    private static final String STATUS_KEY = "status";

    private DefaultDynamoRecordProcessor classUnderTest;

    @Mock
    private AWSStepFunctions stepFunctionClient;
    
    @Mock
    private DynamoDbUtil dynamoDbUtil;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DynamodbEvent.DynamodbStreamRecord mockDBStreamRecord;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        environmentVariables.set(PARTITION_KEY, PARTITION_KEY);

        classUnderTest = new DefaultDynamoRecordProcessor(dynamoDbUtil, objectMapper, stepFunctionClient);
    }

    @Test
    public void processRecordWithoutPartitionKeyThrowsException() {
        final DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord = prepareNoWorkflowIdMockStreamRecord();
        final Throwable exception = assertThrows(RuntimeException.class,
            () -> classUnderTest.processRecord(dynamodbStreamRecord, WORKFLOW_STEP_FUNCTION_ARN));
        Assertions.assertEquals("Partition key is empty", exception.getMessage());
    }

    @Test
    public void processRecordWithMalformedInputThrowsException() throws JsonProcessingException {
        final Map<String, String> expectedStateMachineInput = Map.of(
            PARTITION_KEY_RESULT_PATH, PARTITION_KEY
        );
        when(objectMapper.writeValueAsString(expectedStateMachineInput)).thenThrow(JsonProcessingException.class);
        final DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord = prepareMockStreamRecord();
        final Throwable exception = assertThrows(RuntimeException.class,
            () -> classUnderTest.processRecord(dynamodbStreamRecord, WORKFLOW_STEP_FUNCTION_ARN));
        Assertions.assertEquals("Unable to serialize input", exception.getMessage());
    }

    @Test
    public void processRecordWithValidInputStartsStepFunctionExecution()
        throws JsonProcessingException {

        final DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord = prepareMockStreamRecord();
        final Map<String, String> expectedStateMachineInput = Map.of(
            PARTITION_KEY_RESULT_PATH, PARTITION_KEY
        );
        when(objectMapper.writeValueAsString(expectedStateMachineInput)).thenReturn(TEST);
        final StartExecutionRequest expectedStartExecutionRequest = new StartExecutionRequest()
            .withInput(TEST)
            .withName(WORKFLOW_NAME)
            .withStateMachineArn(WORKFLOW_STEP_FUNCTION_ARN);
        final StartExecutionResult expectedResult = new StartExecutionResult().withExecutionArn(TEST);
        when(stepFunctionClient.startExecution(expectedStartExecutionRequest)).thenReturn(expectedResult);
        final StartExecutionResult result = classUnderTest.processRecord(
            dynamodbStreamRecord, WORKFLOW_STEP_FUNCTION_ARN);
        assertEquals(expectedResult, result);
    }

    @Test
    public void processRecordWithoutWorkflowNameSetsDefaultName() {
        final DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord = prepareNoWorkflowNameMockStreamRecord();
        final StartExecutionResult expectedResult = new StartExecutionResult().withExecutionArn(TEST);
        when(stepFunctionClient.startExecution(any())).thenReturn(expectedResult);
        assertThrows(NoSuchElementException.class, () ->
            classUnderTest.processRecord(dynamodbStreamRecord, WORKFLOW_STEP_FUNCTION_ARN));
    }

    private DynamodbEvent.DynamodbStreamRecord prepareNoWorkflowIdMockStreamRecord() {
        when(dynamoDbUtil.getValueFromRecord(mockDBStreamRecord, PARTITION_KEY)).thenReturn(Optional.empty());
        when(dynamoDbUtil.getValueFromRecord(mockDBStreamRecord, STATUS_KEY)).thenReturn(Optional.of(STATUS_KEY));
        when(dynamoDbUtil.getValueFromRecord(mockDBStreamRecord, WORKFLOW_NAME)).thenReturn(Optional.of(WORKFLOW_NAME));

        return mockDBStreamRecord;
    }

    private DynamodbEvent.DynamodbStreamRecord prepareMockStreamRecord() {
        when(dynamoDbUtil.getValueFromRecord(mockDBStreamRecord, PARTITION_KEY)).thenReturn(Optional.of(PARTITION_KEY));
        when(dynamoDbUtil.getValueFromRecord(mockDBStreamRecord, STATUS_KEY)).thenReturn(Optional.of(STATUS_KEY));
        when(dynamoDbUtil.getValueFromRecord(mockDBStreamRecord, WORKFLOW_NAME)).thenReturn(Optional.of(WORKFLOW_NAME));
        return mockDBStreamRecord;
    }

    private DynamodbEvent.DynamodbStreamRecord prepareNoWorkflowNameMockStreamRecord() {
        when(dynamoDbUtil.getValueFromRecord(mockDBStreamRecord, PARTITION_KEY)).thenReturn(Optional.of(PARTITION_KEY));
        when(dynamoDbUtil.getValueFromRecord(mockDBStreamRecord, STATUS_KEY)).thenReturn(Optional.of(STATUS_KEY));
        when(dynamoDbUtil.getValueFromRecord(mockDBStreamRecord, WORKFLOW_NAME)).thenReturn(Optional.empty());
        return mockDBStreamRecord;
    }
}
