package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.AggregateVideoTimeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoTimelineListGenerator {
    /**
     * Calculates the period that the current time will belong to for the final Video Timeline
     * eg, if start time is 4am, current time is 11am and time increment is 3 hours, 4am - 7am is period 0,
     * 7am - 10am is period 1, 10am - 1pm is period 2 and so on, so returned value will be 2
     * @param startTime startTime of the VideoTimeline list
     * @param currentTime time for which we need to find the period
     * @param incrementInMillis time range of periods
     * @return integer value of period that the current time belongs to
     */
    private static int getCurrentPeriod(Long startTime, Long currentTime, Long incrementInMillis) {
        // Calculate the time difference in seconds
        long timeDifferenceInMillis = (currentTime - startTime);  // Convert milliseconds to seconds

        // Calculate the period based on the time difference and the specified increment
        return (int) (timeDifferenceInMillis / incrementInMillis);
    }

    /**
     * Given a list of aggregate timelines for a given time range and time increment, this method generates the final
     * video timeline list that will have density or not based on aggregateVideoTimelines
     * @param aggregateVideoTimelineList list of aggregated video timelines
     * @param startTimeInMillis start time of periods in VideoTimeline
     * @param endTimeInMillis end time of last period of VideoTimeline
     * @param timeIncrementInMillis time range of periods
     * @return list of video densities for given periods
     */
    public static List<VideoTimeline> buildVideoTimelineList(List<AggregateVideoTimeline> aggregateVideoTimelineList,
                                                             Long startTimeInMillis,
                                                             Long endTimeInMillis,
                                                             Long timeIncrementInMillis) {
        // Initialize the map to store densities for each time period
        Map<Integer, VideoTimeline> periodDensities = new HashMap<>();

        // Iterate through the aggregateVideoTimelineList and collate densities for each time period
        for (AggregateVideoTimeline aggregateVideoTimeline : aggregateVideoTimelineList) {
            Long unitTimestamp = aggregateVideoTimeline.getUnitTimestamp();
            long cloudDensityInMillis = (aggregateVideoTimeline.getCloudDensityInMillis() != null &&
                    aggregateVideoTimeline.getCloudDensityInMillis() >= 0) ?
                    aggregateVideoTimeline.getCloudDensityInMillis() : 0L;

            long deviceDensityInMillis = (aggregateVideoTimeline.getDeviceDensityInMillis()!= null &&
                    aggregateVideoTimeline.getDeviceDensityInMillis() >= 0) ?
                    aggregateVideoTimeline.getDeviceDensityInMillis() : 0L;

            int period = getCurrentPeriod(startTimeInMillis, unitTimestamp, timeIncrementInMillis);

            VideoTimeline videoTimeline = periodDensities.getOrDefault(period,
                    VideoTimeline.builder().withCloudDensity(0F).withDeviceDensity(0F).build());

            videoTimeline.setCloudDensity(videoTimeline.getCloudDensity() + (float) cloudDensityInMillis/timeIncrementInMillis);
            videoTimeline.setDeviceDensity(videoTimeline.getDeviceDensity() + (float) deviceDensityInMillis/timeIncrementInMillis);

            periodDensities.put(period, videoTimeline);
        }

        // total size of timeline list
        int totalTimelines = (int) ((endTimeInMillis - startTimeInMillis) / timeIncrementInMillis);
        if(((endTimeInMillis - startTimeInMillis) % timeIncrementInMillis) != 0)  {
            // more times to process than the last whole period in order to reach end time
            totalTimelines += 1;
        }
        // Convert the map to a (potentially sparse) list for output
        List<VideoTimeline> videoTimelineList = new ArrayList<>(totalTimelines);
        for (int i = 0; i < totalTimelines; i++) {
            videoTimelineList.add(periodDensities.getOrDefault(i,
                    VideoTimeline.builder().withCloudDensity(0F).withDeviceDensity(0F).build()));
        }
        return videoTimelineList;
    }
}

