package com.amazonaws.videoanalytics.dagger.modules;

import javax.inject.Named;
import javax.inject.Singleton;

import com.amazonaws.videoanalytics.config.LambdaConfiguration;
import static com.amazonaws.videoanalytics.utils.AWSVideoAnalyticsServiceLambdaConstants.REGION_NAME;

import dagger.Module;
import dagger.Provides;

@Module
public class AWSVideoAnalyticsConfigurationModule {
    @Provides
    @Singleton
    @Named(REGION_NAME)
    String providesRegion() {
        return LambdaConfiguration.getInstance().getRegion();
    }
}
