package com.amazonaws.videoanalytics.workflow.functions;

import com.amazonaws.videoanalytics.workflow.util.Constants;
import com.amazonaws.videoanalytics.workflow.util.DynamoDbUtil;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default implementation for processing Dynamo records and executing a step function.
 */
public class DefaultDynamoRecordProcessor implements DynamoRecordProcessor {

    private static final Logger LOG = LogManager.getLogger(DefaultDynamoRecordProcessor.class);

    private static final String PARTITION_KEY = System.getenv(Constants.PARTITION_KEY);

    private final DynamoDbUtil dynamoDbUtil;

    private final ObjectMapper objectMapper;

    private final AWSStepFunctions awsStepFunctions;


    @Inject
    public DefaultDynamoRecordProcessor(final DynamoDbUtil dynamoDbUtil, final ObjectMapper objectMapper,
        final AWSStepFunctions awsStepFunctions) {

        this.dynamoDbUtil = Preconditions.checkNotNull(dynamoDbUtil, "DynamoDbUtil cannot be null");
        this.objectMapper = Preconditions.checkNotNull(objectMapper, "Object mapper cannot be null");
        this.awsStepFunctions = Preconditions.checkNotNull(awsStepFunctions, "Step function client cannot be null");
    }

    @Override
    public StartExecutionResult processRecord(final DynamodbStreamRecord record, final String workflowStepFunctionArn) {
        Preconditions.checkNotNull(record, "Dynamo record cannot be null");
        Preconditions.checkNotNull(workflowStepFunctionArn, "Step function arn cannot be null");

        final Optional<String> partitionKey = dynamoDbUtil.getValueFromRecord(record, PARTITION_KEY);
        final Optional<String> dbWorkflowName = dynamoDbUtil.getValueFromRecord(record, Constants.WORKFLOW_NAME);
        if (partitionKey.isEmpty()) {
            throw new RuntimeException("Partition key is empty");
        }
        LOG.info("WorkflowStepFunctionArn: " + workflowStepFunctionArn);
        Map<String, String> stateMachineInput = new HashMap<>();
        stateMachineInput.put(Constants.PARTITION_KEY_RESULT_PATH, partitionKey.get());

        // Throw the java NoSuchElementException if the workflow name is null in ddb table
        final String workflowName = dbWorkflowName.orElseThrow();
        LOG.info("Workflow name is " + workflowName);

        final StartExecutionRequest request;
        try {
            request = new StartExecutionRequest()
                .withStateMachineArn(workflowStepFunctionArn)
                .withName(workflowName)
                .withInput(objectMapper.writeValueAsString(stateMachineInput));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to serialize input", e);
        }
        LOG.info("Execution request " + request);
        final StartExecutionResult result = awsStepFunctions.startExecution(request);
        LOG.info("Started workflow, execution arn was " + result.getExecutionArn());
        return result;
    }
}
