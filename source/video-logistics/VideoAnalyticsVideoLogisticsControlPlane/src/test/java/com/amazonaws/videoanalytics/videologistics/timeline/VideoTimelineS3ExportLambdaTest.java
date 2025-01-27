package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.client.s3.S3Proxy;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.RawVideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.VideoDensityLocation;
import com.amazonaws.videoanalytics.videologistics.utils.S3BucketRegionalizer;
import com.amazonaws.videoanalytics.videologistics.schema.SchemaConst;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import org.joda.time.DateTime;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VideoTimelineS3ExportLambdaTest {
    private static final String CUSTOMER_ACCOUNT_ID = "12345678910";
    private static final String DEVICE_ID = "dev123";
    private static final String VIDEO_TIMELINE_TABLE = "video-timeline-table";
    private static final Date CURRENT_DATE = new Date();
    private static final Date FUTURE_DATE = new Date(CURRENT_DATE.getTime() + 180000);
    private static final ScheduledEvent EVENT = new ScheduledEvent()
            .withAccount(CUSTOMER_ACCOUNT_ID)
            .withTime(new DateTime(FUTURE_DATE.getTime()))
            .withId("EventId123");

    @Mock
    private RawVideoTimelineDAO rawVideoTimelineDAO;
    @Mock
    private BatchTimelineMapper batchTimelineMapper;
    @Mock
    private S3Proxy s3Proxy;
    @Mock
    private S3BucketRegionalizer s3BucketRegionalizer;
    @Mock
    private ResponseBytes responseBytes;
    @Mock
    private Context context;

    @InjectMocks
    private VideoTimelineS3ExportLambda videoTimelineS3ExportLambda;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(s3BucketRegionalizer.getRegionalizedBucketName("videoanalytics-timeline-bucket"))
                .thenReturn(VIDEO_TIMELINE_TABLE);
    }

    @Test
    void handleRequest_singleObject_successfulProcessing() throws Exception {
        List<TimestampInfo> timestampList = List.of(
                new TimestampInfo(1696444408123L, 4000L),
                new TimestampInfo(1696444412123L, 4000L)
        );

        S3Object s3Object = S3Object.builder()
                .key("file1")
                .build();

        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder()
                .contents(Collections.singletonList(s3Object))
                .isTruncated(false)
                .build();

        BatchTimeline batchTimeline = BatchTimeline.builder()
                .deviceId(DEVICE_ID)
                .timestamps(timestampList)
                .location(VideoDensityLocation.CLOUD.name())
                .build();

        when(s3Proxy.listS3Objects(VIDEO_TIMELINE_TABLE, null)).thenReturn(mockResponse);
        when(s3Proxy.getS3ObjectBytes(anyString(), anyString())).thenReturn(responseBytes);
        when(batchTimelineMapper.deserialize(any())).thenReturn(batchTimeline);

        videoTimelineS3ExportLambda.handleRequest(EVENT, context);

        verify(s3Proxy, times(1)).deleteObject(any(), any());
        verify(rawVideoTimelineDAO, times(2)).save(any(), any(), any());
    }

    @Test
    void handleRequest_multiplePages_successfulProcessing() throws Exception {
        List<TimestampInfo> timestampList = List.of(
                new TimestampInfo(1696444408123L, 4000L),
                new TimestampInfo(1696444412123L, 4000L)
        );

        ListObjectsV2Response firstResponse = ListObjectsV2Response.builder()
                .contents(List.of(
                    S3Object.builder().key("file1").build(),
                    S3Object.builder().key("file2").build()
                ))
                .nextContinuationToken("token")
                .isTruncated(true)
                .build();

        ListObjectsV2Response secondResponse = ListObjectsV2Response.builder()
                .contents(List.of(S3Object.builder().key("file3").build()))
                .isTruncated(false)
                .build();

        BatchTimeline batchTimeline = BatchTimeline.builder()
                .deviceId(DEVICE_ID)
                .timestamps(timestampList)
                .location(VideoDensityLocation.CLOUD.name())
                .build();

        when(s3Proxy.listS3Objects(VIDEO_TIMELINE_TABLE, null)).thenReturn(firstResponse);
        when(s3Proxy.listS3Objects(VIDEO_TIMELINE_TABLE, "token")).thenReturn(secondResponse);
        when(s3Proxy.getS3ObjectBytes(anyString(), anyString())).thenReturn(responseBytes);
        when(batchTimelineMapper.deserialize(any())).thenReturn(batchTimeline);

        videoTimelineS3ExportLambda.handleRequest(EVENT, context);

        verify(s3Proxy, times(3)).deleteObject(any(), any());
        verify(rawVideoTimelineDAO, times(6)).save(any(), any(), any());
    }

    @Test
    void handleRequest_noObjects_noProcessing() throws Exception {
        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder()
                .contents(Collections.emptyList())
                .isTruncated(false)
                .build();

        when(s3Proxy.listS3Objects(VIDEO_TIMELINE_TABLE, null)).thenReturn(mockResponse);

        videoTimelineS3ExportLambda.handleRequest(EVENT, context);

        verify(s3Proxy, times(0)).getS3ObjectBytes(any(), any());
        verify(s3Proxy, times(0)).deleteObject(any(), any());
        verify(rawVideoTimelineDAO, times(0)).save(any(), any(), any());
    }

    @Test
    void handleRequest_deserializationFails_objectDeleted() throws Exception {
        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder()
                .contents(List.of(S3Object.builder().key("file1").build()))
                .isTruncated(false)
                .build();

        when(s3Proxy.listS3Objects(VIDEO_TIMELINE_TABLE, null)).thenReturn(mockResponse);
        when(s3Proxy.getS3ObjectBytes(anyString(), anyString())).thenReturn(responseBytes);
        doThrow(new IOException("Failed to deserialize")).when(batchTimelineMapper).deserialize(any());

        videoTimelineS3ExportLambda.handleRequest(EVENT, context);

        verify(s3Proxy, times(1)).deleteObject(VIDEO_TIMELINE_TABLE, "file1");
        verify(rawVideoTimelineDAO, times(0)).save(any(), any(), any());
    }
}
