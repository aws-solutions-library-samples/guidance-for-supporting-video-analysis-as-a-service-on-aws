
package com.amazonaws.videoanalytics.videologistics.schema.PlaybackSession;

import com.amazonaws.videoanalytics.videologistics.SourceType;
import com.amazonaws.videoanalytics.videologistics.utils.SchemaConst;
import com.amazonaws.videoanalytics.videologistics.schema.Source;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.io.Serializable;

@DynamoDbBean
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuppressFBWarnings
public class StreamSource implements Serializable {
    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.STREAM_SESSION_TYPE) })
    private SourceType streamSessionType;

    @Getter(onMethod_ = { @DynamoDbAttribute(SchemaConst.SOURCE) })
    private Source source;
}
