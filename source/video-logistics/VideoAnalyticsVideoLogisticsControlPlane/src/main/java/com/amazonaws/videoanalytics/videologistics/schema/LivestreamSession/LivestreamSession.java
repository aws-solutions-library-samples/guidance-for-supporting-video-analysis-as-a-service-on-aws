package com.amazonaws.videoanalytics.videologistics.schema.LivestreamSession;

import com.amazonaws.videoanalytics.videologistics.schema.Source;
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

@DynamoDbBean
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuppressFBWarnings
public class LivestreamSession {@Getter(onMethod_ = { @DynamoDbPartitionKey, @DynamoDbAttribute(SchemaConst.SESSION_ID) })
private String sessionId;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.SESSION_STATUS), @DynamoDbEncryptionDoNothing })
    private String sessionStatus;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.CREATED_AT), @DynamoDbConvertedBy(DateAttributeConverter.class),
            @DynamoDbEncryptionDoNothing })
    private Date createdAt;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.LAST_UPDATED), @DynamoDbConvertedBy(DateAttributeConverter.class),
            @DynamoDbEncryptionDoNothing })
    private Date lastUpdated;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.ERROR_CODE), @DynamoDbEncryptionDoNothing })
    private String errorCode;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.ERROR_MESSAGE), @DynamoDbEncryptionDoNothing })
    private String errorMessage;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.DEVICE_ID), @DynamoDbEncryptionDoNothing })
    private String deviceId;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.SOURCE), @DynamoDbEncryptionDoNothing })
    private Source source;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.WORKFLOW_NAME), @DynamoDbEncryptionDoNothing })
    private String workflowName;
}
