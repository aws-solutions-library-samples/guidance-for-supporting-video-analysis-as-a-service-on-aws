package com.amazonaws.videoanalytics.videologistics.dagger.modules;

import com.amazonaws.videoanalytics.videologistics.config.LambdaConfiguration;
import com.amazonaws.videoanalytics.videologistics.utils.GuidanceUUIDGenerator;

import dagger.Module;
import dagger.Provides;

import javax.inject.Named;
import javax.inject.Singleton;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.REGION_NAME;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.ACCOUNT_ID;



@Module
public class AWSVideoAnalyticsConfigurationModule {
    @Provides
    @Singleton
    @Named(REGION_NAME)
    public String providesRegion() {
        return LambdaConfiguration.getInstance().getRegion();
    }

    @Provides
    @Singleton
    @Named(ACCOUNT_ID)
    String providesAccountId() {
        return System.getenv(ACCOUNT_ID);
    }

    @Provides
    @Singleton
    final public GuidanceUUIDGenerator providesGuidanceUUIDGenerator() {
        return new GuidanceUUIDGenerator();
    }
}
