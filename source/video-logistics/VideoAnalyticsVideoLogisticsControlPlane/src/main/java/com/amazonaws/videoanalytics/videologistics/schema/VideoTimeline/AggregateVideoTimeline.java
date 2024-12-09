package com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline;

import java.util.Date;
import com.amazonaws.videoanalytics.videologistics.schema.util.DateAttributeConverter;
import com.amazonaws.videoanalytics.videologistics.schema.SchemaConst;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuppressFBWarnings
public class AggregateVideoTimeline {
    @Getter(onMethod_ = { @DynamoDbPartitionKey, @DynamoDbAttribute(SchemaConst.VIDEO_TIMELINE_PARTITION_KEY) })
    private String deviceIdTimeUnit;

    @Getter(onMethod_ = { @DynamoDbSortKey, @DynamoDbAttribute(SchemaConst.UNIT_TIMESTAMP) })
    private Long unitTimestamp;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.TIME_INCREMENT_UNIT) })
    private TimeIncrementUnits timeIncrementUnits;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.CLOUD_DENSITY_IN_MILLIS) })
    @Builder.Default
    private Long cloudDensityInMillis = 0L;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.DEVICE_DENSITY_IN_MILLIS) })
    @Builder.Default
    private Long deviceDensityInMillis = 0L;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.DENSITY_IN_MILLIS) })
    private Long densityInMillis;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.EXPIRATION_TIMESTAMP) })
    private Long expirationTimestamp;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.CREATED_AT), @DynamoDbConvertedBy(DateAttributeConverter.class) })
    private Date createdAt;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.LAST_UPDATED), @DynamoDbConvertedBy(DateAttributeConverter.class) })
    private Date lastUpdated;
}
