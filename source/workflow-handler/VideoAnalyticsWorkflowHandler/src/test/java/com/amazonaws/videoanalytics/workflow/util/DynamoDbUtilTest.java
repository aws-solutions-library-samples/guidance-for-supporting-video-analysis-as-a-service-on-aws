package com.amazonaws.videoanalytics.workflow.util;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;

import java.util.Map;

import static com.amazonaws.videoanalytics.workflow.util.Constants.PARTITION_KEY;
import static com.amazonaws.videoanalytics.workflow.util.Constants.WORKFLOW_NAME;
import static com.amazonaws.videoanalytics.workflow.util.Constants.DATASET_WORKFLOW_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class DynamoDbUtilTest {
    @Mock
    private MetricsLogger metricsLogger;
    private DynamoDbUtil dynamoDbUtil;
    private static final String TEST_STRING = "TEST_STRING";
    private static final String TEST_NUM = "0L";
    private static final String STATUS_KEY = "status";
    private static final String TEST_METRIC = "testMetric";
    private DynamodbStreamRecord dynamodbStreamRecord;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        dynamoDbUtil = new DynamoDbUtil(metricsLogger);
    }

    @Test
    public void testGetStringValueFromRecordIsSuccessful() {
        dynamodbStreamRecord = prepareMockStreamRecord(true, WORKFLOW_NAME);
        assertEquals(TEST_STRING, dynamoDbUtil.getValueFromRecord(dynamodbStreamRecord, STATUS_KEY).get());
    }

    @Test
    public void testGetNumberValueFromRecordIsSuccessful() {
        dynamodbStreamRecord = prepareMockStreamRecord(false, WORKFLOW_NAME);
        assertEquals(TEST_NUM, dynamoDbUtil.getValueFromRecord(dynamodbStreamRecord, STATUS_KEY).get());
    }

    @Test
    public void testGetValueFromRecordMismatch() {
        dynamodbStreamRecord = prepareMockStreamRecord(true, WORKFLOW_NAME);
        assertNotEquals("Something", dynamoDbUtil.getValueFromRecord(dynamodbStreamRecord, STATUS_KEY).get());
    }

    @Test
    public void testGetValueFromRecordWithDatasetWorkflowName() throws InvalidMetricException {
        dynamodbStreamRecord = prepareMockStreamRecord(true, DATASET_WORKFLOW_NAME);
        assertEquals(TEST_STRING, dynamoDbUtil.getValueFromRecord(dynamodbStreamRecord, STATUS_KEY).get());
    }

    @Test
    public void testGetValueFromRecordNullAttribute() throws InvalidMetricException {
        dynamodbStreamRecord = prepareMockStreamRecord(true, null);
        assertEquals(TEST_STRING, dynamoDbUtil.getValueFromRecord(dynamodbStreamRecord, STATUS_KEY).get());
        assertEquals(TEST_STRING, dynamoDbUtil.getValueFromRecord(dynamodbStreamRecord, PARTITION_KEY).get());
        assertTrue(dynamoDbUtil.getValueFromRecord(dynamodbStreamRecord, WORKFLOW_NAME).isEmpty());
        verify(metricsLogger, times(1)).putMetric(anyString(), anyDouble(), any(Unit.class));
    }

    @Test
    public void testGetValueFromRecordInvalidMetricException() throws InvalidMetricException {
        doThrow(new InvalidMetricException(TEST_STRING)).when(metricsLogger)
                .putMetric(eq(null), anyDouble(), any(Unit.class));
        dynamodbStreamRecord = prepareMockStreamRecord(true, null);
        assertEquals(TEST_STRING, dynamoDbUtil.getValueFromRecord(dynamodbStreamRecord, STATUS_KEY).get());
        assertEquals(TEST_STRING, dynamoDbUtil.getValueFromRecord(dynamodbStreamRecord, PARTITION_KEY).get());
        assertTrue(dynamoDbUtil.getValueFromRecord(dynamodbStreamRecord, WORKFLOW_NAME).isEmpty());
        assertDoesNotThrow(() -> metricsLogger.putMetric(anyString(), anyDouble(), any(Unit.class)));
    }

    private DynamodbStreamRecord prepareMockStreamRecord(boolean stringAttribute, String workflowNameKey) {
        DynamodbStreamRecord mockDBStreamRecord = mock(DynamodbEvent.DynamodbStreamRecord.class);
        StreamRecord mockStreamRecord = mock(StreamRecord.class);
        doReturn(prepareMockImage(stringAttribute, workflowNameKey)).when(mockStreamRecord).getNewImage();
        doReturn(mockStreamRecord).when(mockDBStreamRecord).getDynamodb();
        return mockDBStreamRecord;
    }

    private Map<String, AttributeValue> prepareMockImage(boolean stringAttribute, String workflowNameKey) {
        final AttributeValue attributeValue;
        if (stringAttribute) {
            attributeValue = new AttributeValue().withS(TEST_STRING);
        } else {
            attributeValue = new AttributeValue().withN(TEST_NUM);
        }

        Map<String, AttributeValue> dbImage = Map.of(
                STATUS_KEY, attributeValue,
                PARTITION_KEY, attributeValue
        );
        if (workflowNameKey != null) {
            dbImage = Map.of(
                    STATUS_KEY, attributeValue,
                    PARTITION_KEY, attributeValue,
                    workflowNameKey, attributeValue
            );
        }

        return dbImage;
    }
}
