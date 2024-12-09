package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage;
import lombok.Builder;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VideoTimelineAggregator {
    private static final Logger LOG = LogManager.getLogger(VideoTimelineAggregator.class);
    private static final long KVS_TTL_DURATION = 7776000L; // 90 days in seconds
    private static final long SECONDS_BUCKET_DURATION = 5L;
    private static final long UNIT_BUCKET_DURATION = 1L;

    private final VideoTimelineUtils videoTimelineUtils;
    
    @Inject
    public VideoTimelineAggregator(VideoTimelineUtils videoTimelineUtils) {
        this.videoTimelineUtils = videoTimelineUtils;
    }

    @Data
    @Builder
    public static class TimelineStorage {
        private Long bucketStartDate;
        private Long durationToBeStored;
    }

    @Data
    @Builder
    public static class AggregateVideoTimeline {
        private String deviceIdTimeUnit;
        private String timeIncrementUnits;
        private Long cloudDensityInMillis;
        private Long deviceDensityInMillis;
        private Long unitTimestamp;
        private Long expirationTimestamp;
        private Date createdAt;
        private Date lastUpdated;
    }

    public enum VideoDensityLocation {
        CLOUD,
        DEVICE
    }

    public List<TimelineStorage> getTimeBuckets(String timeIncrementUnits, Long timestampToBeStored, Long durationInMillis) {
        Long rangeStart = videoTimelineUtils.getUnitTime(timeIncrementUnits, timestampToBeStored);
        Instant rangeStartInstant = Instant.ofEpochMilli(rangeStart);
        Long rangeEnd = videoTimelineUtils.getUnitTime(timeIncrementUnits, timestampToBeStored + durationInMillis);
        List<TimelineStorage> timestampList = new ArrayList<>();

        long durationStep = "SECONDS".equals(timeIncrementUnits) ? SECONDS_BUCKET_DURATION : UNIT_BUCKET_DURATION;
        ChronoUnit timeUnit = ChronoUnit.valueOf(timeIncrementUnits);
        long bucketDurationMillis = videoTimelineUtils.getBucketDuration(timeUnit);

        int numberOfBuckets = (int) (rangeStartInstant.until(Instant.ofEpochMilli(rangeEnd), timeUnit) / durationStep) + 1;

        for (int i = 0; i < numberOfBuckets; i++) {
            long bucketStartDate = rangeStartInstant.plus(i * durationStep, timeUnit).toEpochMilli();
            long bucketEndDate = bucketStartDate + bucketDurationMillis;

            long timeInBucket = Math.min(bucketEndDate - timestampToBeStored, durationInMillis);
            timestampList.add(TimelineStorage.builder()
                    .bucketStartDate(bucketStartDate)
                    .durationToBeStored(timeInBucket)
                    .build());

            durationInMillis -= timeInBucket;
            timestampToBeStored = bucketEndDate;
            if(durationInMillis <= 0 || timestampToBeStored > rangeEnd) break;
        }
        return timestampList;
    }

    public AggregateVideoTimeline getUpdatedVideoTimeline(String timeIncrementUnits,
                                                         Long bucketStartDate,
                                                         Long timeInBucket,
                                                         String partitionKey,
                                                         AggregateVideoTimeline aggregateVideoTimeline,
                                                         VideoDensityLocation location,
                                                         boolean isCatchUp) {
        if(aggregateVideoTimeline == null) {
            return buildNewAggregateVideoTimeline(timeIncrementUnits, partitionKey, location, bucketStartDate, timeInBucket);
        }

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

    private AggregateVideoTimeline buildNewAggregateVideoTimeline(String timeIncrementUnits,
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

        Long expirationTimestamp = ((bucketStartDate + 
                videoTimelineUtils.getBucketDuration(ChronoUnit.valueOf(timeIncrementUnits)))/1000) + KVS_TTL_DURATION;

        Date currentDate = new Date();
        return AggregateVideoTimeline.builder()
                .deviceIdTimeUnit(partitionKey)
                .timeIncrementUnits(timeIncrementUnits)
                .cloudDensityInMillis(cloudDensityInMillis)
                .deviceDensityInMillis(deviceDensityInMillis)
                .unitTimestamp(bucketStartDate)
                .expirationTimestamp(expirationTimestamp)
                .createdAt(currentDate)
                .lastUpdated(currentDate)
                .build();
    }

    private static Long subtractTimeFromBucket(Long deviceDensity, long timeInBucket, long bucketStartDate) {
        long newTimeInBucket = deviceDensity - timeInBucket;
        if(newTimeInBucket < 0) {
            LOG.warn("Density time mismatch for bucket start date: {}, device density: {}, time in bucket: {}",
                    bucketStartDate, deviceDensity, timeInBucket);
        }
        return newTimeInBucket;
    }
}