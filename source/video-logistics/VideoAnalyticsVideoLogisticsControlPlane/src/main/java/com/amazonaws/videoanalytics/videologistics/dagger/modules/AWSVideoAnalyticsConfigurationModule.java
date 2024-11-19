package com.amazonaws.videoanalytics.videologistics.dagger.modules;

import com.amazonaws.videoanalytics.videologistics.config.LambdaConfiguration;
import com.amazonaws.videoanalytics.videologistics.utils.GuidanceUUIDGenerator;

import dagger.Module;
import dagger.Provides;

import javax.inject.Named;
import javax.inject.Singleton;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.REGION_NAME;


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
    final public GuidanceUUIDGenerator providesGuidanceUUIDGenerator() {
        return new GuidanceUUIDGenerator();
    }
}
