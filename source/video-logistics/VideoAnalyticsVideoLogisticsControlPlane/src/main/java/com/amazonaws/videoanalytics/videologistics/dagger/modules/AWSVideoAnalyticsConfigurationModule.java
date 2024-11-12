package com.amazonaws.videoanalytics.videologistics.dagger.modules;

import com.amazonaws.videoanalytics.videologistics.config.LambdaConfiguration;
import com.amazonaws.videoanalytics.videologistics.schema.LivestreamSession.LivestreamSession;
import com.amazonaws.videoanalytics.videologistics.utils.DateTime;
import com.amazonaws.videoanalytics.videologistics.utils.GuidanceUUIDGenerator;
import com.amazonaws.videoanalytics.videologistics.utils.SchemaConst;

import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

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
    final public DynamoDbTable<LivestreamSession> providesLivestreamSessionTable(DynamoDbEnhancedClient ddbClient) {
        return ddbClient.table(SchemaConst.LIVESTREAM_SESSION_TABLE_NAME, TableSchema.fromBean(LivestreamSession.class));
    }
}
