package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.RawVideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.VideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.RawVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.TimeIncrementUnits;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.VideoDensityLocation;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

import java.util.List;
import java.util.Optional;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.NO_PARTITION_KEY_ERROR;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.NO_SORT_KEY_ERROR;
import static com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.TimeIncrementUnits.TIME_INCREMENT_UNITS_LIST;
import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.LOCATION;
import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.RAW_PARTITION_KEY;
import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.RAW_SORT_KEY;

public class TimelineForwarderLambda {
    
}