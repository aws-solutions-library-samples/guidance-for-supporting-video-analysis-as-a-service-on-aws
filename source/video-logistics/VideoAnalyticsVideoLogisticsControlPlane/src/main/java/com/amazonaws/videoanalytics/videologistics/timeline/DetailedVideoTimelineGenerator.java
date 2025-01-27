package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.Timeline;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.RawVideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.PaginatedListResponse;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.RawVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.DetailedTimelinePaginatedResponse;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.VideoDensityLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DetailedVideoTimelineGenerator {
    private static final Logger LOG = LogManager.getLogger(DetailedVideoTimelineGenerator.class);

    private final RawVideoTimelineDAO rawVideoTimelineDAO;
    // Max fragment duration allowed by KVS is 20s. This helps us determine how far to look back in time to get accurate
    // detailed timeline information.
    public static final Long MAX_KVS_FRAGMENT_DURATION_BUFFER = 20*1000L;

    @Inject
    public DetailedVideoTimelineGenerator(final RawVideoTimelineDAO rawVideoTimelineDAO) {
        this.rawVideoTimelineDAO = rawVideoTimelineDAO;
    }

    public DetailedTimelinePaginatedResponse getDetailedVideoTimeLine(String deviceId,
                                                                    Long startTimeInMillis,
                                                                    Long endTimeInMillis,
                                                                    String nextToken) {
        // Remove MAX_KVS_FRAGMENT_DURATION from start time in order to look back to fragments that might have overlap
        // with query range
        Long startTimeWithBuffer = startTimeInMillis - MAX_KVS_FRAGMENT_DURATION_BUFFER;
        PaginatedListResponse<RawVideoTimeline> paginatedListTimelineResponse =
                rawVideoTimelineDAO.listRawVideoTimelines(deviceId, startTimeWithBuffer, endTimeInMillis, nextToken);

        List<RawVideoTimeline> rawVideoTimelineList = paginatedListTimelineResponse.getResults();
        List<Timeline> cloudTimelineList = new ArrayList<>();
        List<Timeline> deviceTimelineList = new ArrayList<>();

        for (RawVideoTimeline rawVideoTimeline : rawVideoTimelineList) {
            long startTime = rawVideoTimeline.getTimestamp();
            long endTime = startTime + rawVideoTimeline.getDurationInMillis() > endTimeInMillis ?
                    endTimeInMillis : startTime + rawVideoTimeline.getDurationInMillis();
            
            // only include timestamps in the list for which end time is greater than start time
            if (endTime > startTimeInMillis) {
                // if start time of a timeline object is smaller than query start time, adjust it
                if (startTime < startTimeInMillis) {
                    startTime = startTimeInMillis;
                }
                
                Timeline timeline = Timeline.builder()
                        .startTime(Double.valueOf(startTime))
                        .endTime(Double.valueOf(endTime))
                        .build();

                List<Timeline> targetList = VideoDensityLocation.CLOUD.equals(rawVideoTimeline.getLocation()) ?
                        cloudTimelineList : deviceTimelineList;

                // check to see if the timestamps are adjacent
                boolean doStitchTimeline = !targetList.isEmpty() &&
                        timestampsAreAdjacent(targetList.get(targetList.size() - 1).getEndTime().longValue(), startTime);

                if (doStitchTimeline) {
                    targetList.get(targetList.size() - 1).setEndTime(Double.valueOf(endTime));
                } else {
                    targetList.add(timeline);
                }
            }
        }

        LOG.info(String.format(
                "Returning detailed timeline list for CLOUD with size=%d, and DEVICE with size=%d",
                cloudTimelineList.size(),
                deviceTimelineList.size()));

        return new DetailedTimelinePaginatedResponse(
                cloudTimelineList,
                deviceTimelineList,
                paginatedListTimelineResponse.getNextToken()
        );
    }

    /**
     * Checks to make sure the current timeline immediately follows the last checked timeline
     * @param previousEndTime end time of the last checked timeline
     * @param currentStartTime start time of the current timeline
     * @return true if the timestamps are adjacent (within 1 second), false otherwise
     */
    private boolean timestampsAreAdjacent(long previousEndTime, long currentStartTime) {
        // truncate to second precisions from millisecond precision
        return (currentStartTime/1000) - (previousEndTime/1000) <= 1;
    }
}