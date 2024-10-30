package com.amazonaws.videoanalytics.dagger;

import com.amazonaws.videoanalytics.dagger.modules.AWSVideoAnalyticsConfigurationModule;
import com.amazonaws.videoanalytics.dagger.modules.AWSModule;

import dagger.Component;

import javax.inject.Singleton;

@Component(
    modules = {
        AWSModule.class,
        AWSVideoAnalyticsConfigurationModule.class
    }
)

@Singleton
public interface AWSVideoAnalyticsDMControlPlaneComponent {
}
