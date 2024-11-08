package com.amazonaws.videoanalytics.dagger.modules;

import com.amazonaws.videoanalytics.config.LambdaConfiguration;
import com.amazonaws.videoanalytics.schema.LivestreamSession.LivestreamSession;
import com.amazonaws.videoanalytics.utils.DateTime;
import com.amazonaws.videoanalytics.utils.GuidanceUUIDGenerator;
import com.amazonaws.videoanalytics.utils.KVSWebRTCUtils;
import com.amazonaws.videoanalytics.utils.SchemaConst;
import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import javax.inject.Named;
import javax.inject.Singleton;

import static com.amazonaws.videoanalytics.utils.AWSVideoAnalyticsServiceLambdaConstants.REGION_NAME;

@Module
public class AWSVideoAnalyticsConfigurationModule {
    @Provides
    @Singleton
    @Named(REGION_NAME)
    String providesRegion() {
        return LambdaConfiguration.getInstance().getRegion();
    }

    @Provides
    @Singleton
    final public GuidanceUUIDGenerator provideGuidanceUUIDGenerator() {
        return new GuidanceUUIDGenerator();
    }

    @Provides
    @Singleton
    public DateTime providesDateTime() {
        return new DateTime();
    }

    @Provides
    @Singleton
    public KVSWebRTCUtils provideKvsWebRtcUtils() {
        return new KVSWebRTCUtils();
    }

    @Provides
    @Singleton
    final public DynamoDbTable<LivestreamSession> provideLivestreamSessionTable(DynamoDbEnhancedClient ddbClient) {
        return ddbClient.table(SchemaConst.LIVESTREAM_SESSION_TABLE_NAME, TableSchema.fromBean(LivestreamSession.class));
    }
}
