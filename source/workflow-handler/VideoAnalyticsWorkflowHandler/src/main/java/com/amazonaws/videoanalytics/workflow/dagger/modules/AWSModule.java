package com.amazonaws.videoanalytics.workflow.dagger.modules;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import dagger.Module;
import dagger.Provides;

import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

import javax.inject.Singleton;

@Module
public class AWSModule {
    @Provides
    @Singleton
    public MetricsLogger provideMetricsLogger() {
        return new MetricsLogger();
    }

    @Provides
    @Singleton
    public AWSStepFunctions provideStepFunctionClient() {
        return AWSStepFunctionsClientBuilder.defaultClient();
    }
}
