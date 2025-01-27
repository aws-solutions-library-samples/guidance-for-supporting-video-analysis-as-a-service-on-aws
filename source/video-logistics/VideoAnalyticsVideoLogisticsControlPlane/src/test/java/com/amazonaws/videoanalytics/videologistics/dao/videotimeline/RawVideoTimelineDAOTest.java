package com.amazonaws.videoanalytics.videologistics.dao.videotimeline;

import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.RawVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.PaginatedListResponse;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.VideoDensityLocation;
import com.amazonaws.videoanalytics.videologistics.timeline.TimestampInfo;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineUtils;
import com.amazonaws.videoanalytics.videologistics.utils.GsonDDBNextTokenMarshaller;
import com.amazonaws.videoanalytics.videologistics.schema.SchemaConst;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RawVideoTimelineDAOTest {
    private static final String DEVICE_ID = "d234";
    private static final VideoTimelineUtils VIDEO_TIMELINE_UTILS = new VideoTimelineUtils();
    private static final String PARTITION_KEY = VIDEO_TIMELINE_UTILS.generateRawPartitionKey(DEVICE_ID);
    private static final Long TIMESTAMP = 1696444404123L;
    private static final Long DURATION = 4000L;

    @Mock
    private DynamoDbTable<RawVideoTimeline> videoTimelineTable;
    private RawVideoTimelineDAO rawVideoTimelineDAO;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        rawVideoTimelineDAO = new RawVideoTimelineDAO(videoTimelineTable, VIDEO_TIMELINE_UTILS);
    }

    @Test
    void save_locationCloud_persistsTimeline() {
        RawVideoTimeline expectedTimeline = RawVideoTimeline.builder()
                .deviceId(DEVICE_ID)
                .timestamp(TIMESTAMP)
                .durationInMillis(DURATION)
                .location(VideoDensityLocation.CLOUD)
                .build();
        when(videoTimelineTable.getItem(any(Key.class))).thenReturn(expectedTimeline);

        rawVideoTimelineDAO.save(DEVICE_ID, new TimestampInfo(TIMESTAMP, DURATION), VideoDensityLocation.CLOUD.name());
        
        verify(videoTimelineTable).putItem(any(RawVideoTimeline.class));
        RawVideoTimeline persistedTimeline = rawVideoTimelineDAO.load(DEVICE_ID, TIMESTAMP);
        assertEquals(TIMESTAMP, persistedTimeline.getTimestamp());
        assertEquals(VideoDensityLocation.CLOUD, persistedTimeline.getLocation());
    }

    @Test
    void listRawVideoTimelines_emptyRange_returnsEmptyList() {
        Page<RawVideoTimeline> emptyPage = Page.builder(RawVideoTimeline.class)
                .items(Collections.emptyList())
                .build();
        PageIterable<RawVideoTimeline> results = PageIterable.create(
                () -> Collections.singletonList(emptyPage).iterator());
        when(videoTimelineTable.query(any(QueryEnhancedRequest.class))).thenReturn(results);

        PaginatedListResponse<RawVideoTimeline> response = rawVideoTimelineDAO.listRawVideoTimelines(
                DEVICE_ID, 0L, Long.MAX_VALUE, null);
        
        assertTrue(response.getResults().isEmpty());
        assertNull(response.getNextToken());
    }

    @Test
    void listRawVideoTimelines_singlePage_returnsAllItems() {
        List<RawVideoTimeline> expectedTimelines = createTestTimelines();
        List<Long> timestamps = List.of(1696444404123L, 1696444408235L, 1696444412452L);
        
        Page<RawVideoTimeline> page = Page.builder(RawVideoTimeline.class)
                .items(expectedTimelines)
                .build();
        PageIterable<RawVideoTimeline> results = PageIterable.create(
                () -> Collections.singletonList(page).iterator());
        when(videoTimelineTable.query(any(QueryEnhancedRequest.class))).thenReturn(results);

        PaginatedListResponse<RawVideoTimeline> response = rawVideoTimelineDAO.listRawVideoTimelines(
                DEVICE_ID, timestamps.get(0), timestamps.get(timestamps.size() - 1), null);
        
        assertEquals(expectedTimelines.size(), response.getResults().size());
        assertIterableEquals(expectedTimelines, response.getResults());
        assertNull(response.getNextToken());
    }

    private List<RawVideoTimeline> createTestTimelines() {
        return List.of(
            RawVideoTimeline.builder()
                .deviceId(DEVICE_ID)
                .timestamp(1696444404123L)
                .durationInMillis(DURATION)
                .location(VideoDensityLocation.CLOUD)
                .build(),
            RawVideoTimeline.builder()
                .deviceId(DEVICE_ID)
                .timestamp(1696444408235L)
                .durationInMillis(DURATION)
                .location(VideoDensityLocation.CLOUD)
                .build(),
            RawVideoTimeline.builder()
                .deviceId(DEVICE_ID)
                .timestamp(1696444412452L)
                .durationInMillis(DURATION)
                .location(VideoDensityLocation.CLOUD)
                .build()
        );
    }
}