package com.amazonaws.videoanalytics.workflow.dagger;

import com.amazonaws.videoanalytics.workflow.dagger.modules.AWSModule;
import com.amazonaws.videoanalytics.workflow.dagger.modules.WorkflowModule;
import com.amazonaws.videoanalytics.workflow.functions.DynamoRecordProcessor;
import com.amazonaws.videoanalytics.workflow.util.DynamoDbUtil;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Component;

import javax.inject.Singleton;

@Component(
        modules = {
                AWSModule.class,
                WorkflowModule.class
        }
)
@Singleton
public interface AWSVideoAnalyticsWorkflowHandlerLambdaComponent {
    AWSStepFunctions getAWSStepFunctions();
    DynamoDbUtil getDynamoDbUtil();
    ObjectMapper getObjectMapper();
    DynamoRecordProcessor getRecordProcessor();
}
