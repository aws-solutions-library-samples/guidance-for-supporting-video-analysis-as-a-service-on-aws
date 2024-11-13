package com.amazonaws.videoanalytics.videologistics.schema.PlaybackSession;

import com.amazonaws.videoanalytics.videologistics.schema.util.DateAttributeConverter;
import com.amazonaws.videoanalytics.videologistics.utils.SchemaConst;
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
import software.amazon.cryptography.dbencryptionsdk.dynamodb.enhancedclient.DynamoDbEncryptionDoNothing;

import java.util.Date;
import java.util.List;

@DynamoDbBean
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuppressFBWarnings
public class PlaybackSession {
    @Getter(onMethod_ = { @DynamoDbPartitionKey, @DynamoDbAttribute(SchemaConst.SESSION_ID) })
    private String sessionId;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.SESSION_STATUS), @DynamoDbEncryptionDoNothing })
    private String sessionStatus;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.START_TIME), @DynamoDbConvertedBy(DateAttributeConverter.class),
            @DynamoDbEncryptionDoNothing })
    private Date startTime;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.END_TIME), @DynamoDbConvertedBy(DateAttributeConverter.class),
            @DynamoDbEncryptionDoNothing })
    private Date endTime;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.CREATED_AT), @DynamoDbConvertedBy(DateAttributeConverter.class),
            @DynamoDbEncryptionDoNothing })
    private Date createdAt;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.LAST_UPDATED), @DynamoDbConvertedBy(DateAttributeConverter.class),
            @DynamoDbEncryptionDoNothing })
    private Date lastUpdated;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.CONNECTED_AT), @DynamoDbConvertedBy(DateAttributeConverter.class),
            @DynamoDbEncryptionDoNothing })
    private Date connectedAt;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.STREAM_LOG_ARN), @DynamoDbEncryptionDoNothing })
    private String streamLogArn;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.ERROR_CODE), @DynamoDbEncryptionDoNothing })
    private String errorCode;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.ERROR_MESSAGE), @DynamoDbEncryptionDoNothing })
    private String errorMessage;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.DEVICE_ID), @DynamoDbEncryptionDoNothing })
    private String deviceId;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.STREAM_SOURCE), @DynamoDbEncryptionDoNothing })
    private List<StreamSource> streamSource;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.WORKFLOW_NAME), @DynamoDbEncryptionDoNothing })
    private String workflowName;
}
