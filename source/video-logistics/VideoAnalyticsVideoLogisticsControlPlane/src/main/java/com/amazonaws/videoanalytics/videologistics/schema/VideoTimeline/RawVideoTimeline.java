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
public class RawVideoTimeline {

    @Getter(onMethod_ = { @DynamoDbPartitionKey, @DynamoDbAttribute(SchemaConst.DEVICE_ID) })
    private String deviceId;

    @Getter(onMethod_ = { @DynamoDbSortKey, @DynamoDbAttribute(SchemaConst.TIMESTAMP) })
    private Long timestamp;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.DURATION_IN_MILLIS) })
    private Long durationInMillis;

    /** TTL enabled on this attribute, time stored in epoch seconds **/
    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.EXPIRATION_TIMESTAMP) })
    private Long expirationTimestamp;

    // We don't encrypt the location attribute because it needs to be in a condition
    // (see the DAO for this class).
    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.LOCATION) })
    private VideoDensityLocation location;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.CREATED_AT), @DynamoDbConvertedBy(DateAttributeConverter.class) })
    private Date createdAt;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.LAST_UPDATED), @DynamoDbConvertedBy(DateAttributeConverter.class) })
    private Date lastUpdated;
}

