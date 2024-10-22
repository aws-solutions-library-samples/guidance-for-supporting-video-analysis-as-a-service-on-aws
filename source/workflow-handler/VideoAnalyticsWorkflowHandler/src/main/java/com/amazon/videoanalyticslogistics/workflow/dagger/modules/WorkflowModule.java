package com.amazonaws.videoanalytics.workflow.dagger.modules;

import com.amazonaws.videoanalytics.workflow.functions.DefaultDynamoRecordProcessor;
import com.amazonaws.videoanalytics.workflow.functions.DynamoRecordProcessor;
import com.amazonaws.videoanalytics.workflow.util.DynamoDbUtil;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import dagger.Provides;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

import javax.inject.Singleton;

@Module
public class WorkflowModule {
    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        return new ObjectMapper();
    }

    @Provides
    @Singleton
    public DynamoDbUtil provideDynamoDbUtil(MetricsLogger metricsLogger) {
        return new DynamoDbUtil(metricsLogger);
    }

    @Provides
    @Singleton
    public DynamoRecordProcessor providesRecordProcessor(DynamoDbUtil dynamoDbUtil,
        ObjectMapper objectMapper, AWSStepFunctions stepFunctionsClient) {
        return new DefaultDynamoRecordProcessor(dynamoDbUtil, objectMapper, stepFunctionsClient);
    }
}
