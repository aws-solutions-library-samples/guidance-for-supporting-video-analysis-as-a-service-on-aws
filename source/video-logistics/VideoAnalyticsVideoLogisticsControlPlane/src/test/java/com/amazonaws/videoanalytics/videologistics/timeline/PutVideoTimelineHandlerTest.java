package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.client.s3.S3Proxy;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.RawVideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.VideoDensityLocation;
import com.amazonaws.videoanalytics.videologistics.schema.SchemaConst;
import com.amazonaws.videoanalytics.videologistics.utils.S3BucketRegionalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PutVideoTimelineHandlerTest {
    private static final String DEVICE_ID = "test-device-id";
    private static final VideoTimelineUtils VIDEO_TIMELINE_UTILS = new VideoTimelineUtils();

    @Mock
    private TimestampListDeserializer timestampListDeserializer;
    @Mock
    private S3Proxy s3Proxy;
    @Mock
    private BatchTimelineMapper batchTimelineMapper;
    @Mock
    private S3BucketRegionalizer s3BucketRegionalizer;
    @Mock
    private RawVideoTimelineDAO rawVideoTimelineDAO;

    private PutVideoTimelineHandler putVideoTimelineHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        putVideoTimelineHandler = new PutVideoTimelineHandler(
                timestampListDeserializer,
                s3Proxy,
                batchTimelineMapper,
                s3BucketRegionalizer,
                VIDEO_TIMELINE_UTILS,
                rawVideoTimelineDAO);
    }

    @Test
    public void addVideoTimelines_singleTimestamp_savesToDAO() {
        String singleTimestamp = "{\"timestamps\":[{\"timestamp\":1696444408123,\"duration\":4000}]}";
        byte[] exampleByteArray = {72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33};
        List<TimestampInfo> timestampList = List.of(
                new TimestampInfo(1696444408123L, 4000L)
        );
        BatchTimeline batchTimeline = BatchTimeline.builder()
                .deviceId(DEVICE_ID)
                .timestamps(timestampList)
                .location(VideoDensityLocation.CLOUD.name())
                .build();

        when(s3BucketRegionalizer.getRegionalizedBucketName(SchemaConst.VIDEO_TIMELINE_TABLE_NAME))
                .thenReturn(SchemaConst.VIDEO_TIMELINE_TABLE_NAME);
        when(timestampListDeserializer.deserialize(singleTimestamp)).thenReturn(timestampList);
        when(batchTimelineMapper.serialize(batchTimeline)).thenReturn(exampleByteArray);

        putVideoTimelineHandler.addVideoTimelines(DEVICE_ID, singleTimestamp, VideoDensityLocation.CLOUD.name());

        verify(rawVideoTimelineDAO, times(1)).save(anyString(), any(), any());
        verify(s3Proxy, times(0)).putObjectBytes(any(), any(), any());
    }

    @Test
    public void addVideoTimelines_multipleTimestamps_savesToS3() {
        String multipleTimestamps = "{\"timestamps\":[{\"timestamp\":1696444408123,\"duration\":4000},{\"timestamp\":1696444412123,\"duration\":4000}]}";
        byte[] exampleByteArray = {72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33};
        List<TimestampInfo> timestampList = List.of(
                new TimestampInfo(1696444408123L, 4000L),
                new TimestampInfo(1696444412123L, 4000L)
        );
        BatchTimeline batchTimeline = BatchTimeline.builder()
                .deviceId(DEVICE_ID)
                .timestamps(timestampList)
                .location(VideoDensityLocation.CLOUD.name())
                .build();

        when(s3BucketRegionalizer.getRegionalizedBucketName(SchemaConst.VIDEO_TIMELINE_TABLE_NAME))
                .thenReturn(SchemaConst.VIDEO_TIMELINE_TABLE_NAME);
        when(timestampListDeserializer.deserialize(multipleTimestamps)).thenReturn(timestampList);
        when(batchTimelineMapper.serialize(batchTimeline)).thenReturn(exampleByteArray);

        putVideoTimelineHandler.addVideoTimelines(DEVICE_ID, multipleTimestamps, VideoDensityLocation.CLOUD.name());

        verify(s3Proxy, times(1)).putObjectBytes(any(), any(), any());
    }

    @Test
    public void addVideoTimelines_emptyTimestamps_noAction() {
        String emptyTimestamps = "{\"timestamps\":[]}";
        List<TimestampInfo> timestampList = List.of();

        when(timestampListDeserializer.deserialize(emptyTimestamps)).thenReturn(timestampList);
        
        putVideoTimelineHandler.addVideoTimelines(DEVICE_ID, emptyTimestamps, VideoDensityLocation.CLOUD.name());

        verify(s3Proxy, times(0)).putObjectBytes(any(), any(), any());
    }
}
