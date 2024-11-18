package com.amazonaws.videoanalytics.videologistics.dagger.modules;

import com.amazonaws.videoanalytics.videologistics.config.LambdaConfiguration;
import com.amazonaws.videoanalytics.videologistics.utils.DateTime;
import com.amazonaws.videoanalytics.videologistics.utils.GuidanceUUIDGenerator;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    @Provides
    @Singleton
    public DateTime providesDateTime() {
        return new DateTime();
    }

    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        // Disable auto-detection. Explicitly allowlist fields for ser/de using @JsonProperty to avoid
        // inadvertently serializing fields not meant for storage.
        objectMapper.setVisibility(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.NONE)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        return objectMapper;
    }
}
