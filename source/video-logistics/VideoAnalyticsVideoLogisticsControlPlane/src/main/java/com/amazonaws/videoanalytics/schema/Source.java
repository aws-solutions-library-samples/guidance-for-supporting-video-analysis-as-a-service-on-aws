package com.amazonaws.videoanalytics.schema;

import com.amazonaws.videoanalytics.schema.util.DateAttributeConverter;
import com.amazonaws.videoanalytics.utils.SchemaConst;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@DynamoDbBean
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuppressFBWarnings
public class Source {
    @DynamoDbBean
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class GuidanceIceServer implements Serializable {
        @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.PASSWORD) })
        private String password;

        @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.USERNAME) })
        private String username;

        @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.TTL) })
        private int ttl;

        @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.URIS) })
        private List<String> uris;
    }

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.HLS_URL) })
    private String hlsStreamingURL;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.EXPIRATION_TIME), @DynamoDbConvertedBy(DateAttributeConverter.class) })
    private Date expirationTime;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.SIGNALING_CHANNEL_URL) })
    private String signalingChannelURL;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.ICE_SERVER) })
    private List<GuidanceIceServer> iceServer;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.CLIENT_ID) })
    private String clientId;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.START_TIME), @DynamoDbConvertedBy(DateAttributeConverter.class) })
    private Date startTime;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.PEER_CONNECTION_STATUS) })
    private String peerConnectionState;
}

