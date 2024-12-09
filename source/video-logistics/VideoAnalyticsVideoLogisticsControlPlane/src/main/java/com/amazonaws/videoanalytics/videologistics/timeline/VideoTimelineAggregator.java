package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.AggregateVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.VideoDensityLocation;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.TimeIncrementUnits;
import lombok.Builder;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.validation.constraints.NotNull;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VideoTimelineAggregator {
    private static final Logger LOG = LogManager.getLogger(VideoTimelineAggregator.class);

    private static final long SECONDS_BUCKET_DURATION = 5L;
    private static final long UNIT_BUCKET_DURATION = 1L;
    private static final long KVS_TTL_DURATION = 7 * 24 * 60 * 60L; // 7 days in seconds
    private static final String DENSITY_TIME_MISMATCH_ERROR = 
        "Time mismatch found for bucket %d. Device density: %d, Time in bucket: %d";

    private final VideoTimelineUtils videoTimelineUtils;
    
    @Inject
    public VideoTimelineAggregator(VideoTimelineUtils videoTimelineUtils) {
        this.videoTimelineUtils = videoTimelineUtils;
    }

    /**
     * Returns list of time buckets that would need to be updated for a given time increment unit
     * @param timeIncrementUnits
     * @param timestampToBeStored
     * @param durationInMillis
     * @return List of time buckets and duration to be stored in each bucket
     */
    public List<TimelineStorage> getTimeBuckets(TimeIncrementUnits timeIncrementUnits, Long timestampToBeStored, Long durationInMillis) {
        Long rangeStart = videoTimelineUtils.getUnitTime(timeIncrementUnits, timestampToBeStored);
        Instant rangeStartInstant = Instant.ofEpochMilli(rangeStart);
        Long rangeEnd = videoTimelineUtils.getUnitTime(timeIncrementUnits, timestampToBeStored + durationInMillis);
        List<TimelineStorage> timestampList = new ArrayList<>();

        // Calculates duration of each bucket in that unit, eg. 5s, 1hr, 1 min, 1 day
        long durationStep = timeIncrementUnits.getChronoUnit() == ChronoUnit.SECONDS ?
                SECONDS_BUCKET_DURATION : UNIT_BUCKET_DURATION;

        ChronoUnit timeUnit = timeIncrementUnits.getChronoUnit();
        long bucketDurationMillis = videoTimelineUtils.getBucketDuration(timeUnit);

        // calculates number of buckets that would be required for the given time range to be stored
        int numberOfBuckets = (int) (rangeStartInstant.until(Instant.ofEpochMilli(rangeEnd), timeUnit) / durationStep) + 1;

        for (int i = 0; i < numberOfBuckets; i++) {
            // Calculates bucket start date given a timestamp. eg. Bucket start date for storing 10:58:59pm in seconds
            // would be 10:58:55pm for SECONDS, 10:58:00pm for MINUTES, 10:00:00pm for HOURS
            long bucketStartDate = rangeStartInstant.
                    plus(i * durationStep, timeIncrementUnits.getChronoUnit()).toEpochMilli();
            long bucketEndDate = bucketStartDate + bucketDurationMillis;

            // Calculates amount of time in bucket, it can = the duration of the range
            long timeInBucket = Math.min(bucketEndDate - timestampToBeStored, durationInMillis);
            timestampList.add(
                    TimelineStorage.builder().bucketStartDate(bucketStartDate).durationToBeStored(timeInBucket).build()
            );
            // Overflow logic: when time to be stored spills over to the next bucket
            durationInMillis -= timeInBucket; // indicates time left to be accounted for
            // new start time of time to be stored will be the end time of the previous bucket
            timestampToBeStored = bucketEndDate;
            if(durationInMillis <= 0 || timestampToBeStored > rangeEnd) break;
        }
        return timestampList;
    }

    /**
     * Returns an updated/new video timeline for given parameters depending on whether or not the timeline exists
     * @param timeIncrementUnits
     * @param bucketStartDate = start date of bucket for which timeline is to be stored
     * @param timeInBucket = time to be stored in the bucket = duration to be stored
     * @param partitionKey
     * @param aggregateVideoTimeline
     * @param location
     * @param isCatchUp
     * @return
     */
    public AggregateVideoTimeline getUpdatedVideoTimeline(TimeIncrementUnits timeIncrementUnits,
                                                          Long bucketStartDate,
                                                          Long timeInBucket,
                                                          String partitionKey,
                                                          AggregateVideoTimeline aggregateVideoTimeline,
                                                          VideoDensityLocation location, boolean isCatchUp) {

        if(aggregateVideoTimeline == null) {
            return buildNewAggregateVideoTimeline(timeIncrementUnits, partitionKey, location, bucketStartDate, timeInBucket);
        } else {
            // bucket start date exists, we need to update it
            Long cloudDensity = aggregateVideoTimeline.getCloudDensityInMillis();
            Long deviceDensity = aggregateVideoTimeline.getDeviceDensityInMillis();

            if(isCatchUp) {
                Long subtractedTimeFromBucket = subtractTimeFromBucket(deviceDensity, timeInBucket, bucketStartDate);
                aggregateVideoTimeline.setDeviceDensityInMillis(subtractedTimeFromBucket);
                aggregateVideoTimeline.setCloudDensityInMillis(cloudDensity + timeInBucket);
            } else {
                if(location == VideoDensityLocation.DEVICE) {
                    aggregateVideoTimeline.setDeviceDensityInMillis(deviceDensity + timeInBucket);
                } else if(location == VideoDensityLocation.CLOUD) {
                    aggregateVideoTimeline.setCloudDensityInMillis(cloudDensity + timeInBucket);
                }
            }
            aggregateVideoTimeline.setLastUpdated(new Date());
            return aggregateVideoTimeline;
        }
    }

    private AggregateVideoTimeline buildNewAggregateVideoTimeline(TimeIncrementUnits timeIncrementUnits,
                                                                  String partitionKey,
                                                                  VideoDensityLocation location,
                                                                  long bucketStartDate,
                                                                  Long timeInBucket) {
        Long cloudDensityInMillis = 0L;
        Long deviceDensityInMillis = 0L;

        if(location == VideoDensityLocation.CLOUD) {
            cloudDensityInMillis += timeInBucket;
        } else if(location == VideoDensityLocation.DEVICE) {
            deviceDensityInMillis += timeInBucket;
        }

        // This bucket should expire when all the "timestamps" in this bucket expire, ie, for an HOURS bucket with start
        // time of 01-12-2024T11:00:00Z and KVS timeout of 3 months, the bucket expiration is 04-12-2024T11:00:00Z+1hr
        // ie. 04-12-2024T12:00:00Z, etc. This timestamp should also be in order of epoch seconds (vs milliseconds for everything else)
        Long expirationTimestamp = ((bucketStartDate +
                videoTimelineUtils.getBucketDuration(timeIncrementUnits.getChronoUnit()))/1000) + KVS_TTL_DURATION;

        Date currentDate = new Date();
        return AggregateVideoTimeline.builder()
                .deviceIdTimeUnit(partitionKey)
                .timeIncrementUnits(timeIncrementUnits)
                .cloudDensityInMillis(cloudDensityInMillis)
                .deviceDensityInMillis(deviceDensityInMillis)
                .unitTimestamp(bucketStartDate)
                .expirationTimestamp(expirationTimestamp) // in epoch seconds
                .createdAt(currentDate)
                .lastUpdated(currentDate)
                .build();
    }

    @NotNull
    private static Long subtractTimeFromBucket(Long deviceDensity, long timeInBucket, long bucketStartDate) {
        long newTimeInBucket = deviceDensity - timeInBucket;
        if(newTimeInBucket < 0) {
            LOG.warn(String.format(
                    DENSITY_TIME_MISMATCH_ERROR,
                    bucketStartDate,
                    deviceDensity,
                    timeInBucket
            ));
        }
        return newTimeInBucket;
    }

}