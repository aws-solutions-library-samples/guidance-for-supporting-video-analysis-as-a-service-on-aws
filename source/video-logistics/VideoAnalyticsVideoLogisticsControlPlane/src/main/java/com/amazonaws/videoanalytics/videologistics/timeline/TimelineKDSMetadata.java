package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.TimeIncrementUnits;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.VideoDensityLocation;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TimelineKDSMetadata {
    @JsonProperty
    private String deviceId;
    @JsonProperty
    private TimeIncrementUnits timeIncrementUnits;
    @JsonProperty
    private Long bucketStartTime;
    @JsonProperty
    private Long timestampToBeStored;
    @JsonProperty
    private Long durationInMillis;
    @JsonProperty
    private VideoDensityLocation videoDensityLocation;
    @JsonProperty
    private boolean isCatchup;

    public TimelineKDSMetadata(){}

    public TimelineKDSMetadata(final String deviceId, final TimeIncrementUnits timeIncrementUnits,
                               final Long bucketStartTime, final Long timestampToBeStored, final Long durationInMillis,
                               final VideoDensityLocation videoDensityLocation, final boolean isCatchup) {
        this.deviceId = deviceId;
        this.timeIncrementUnits = timeIncrementUnits;
        this.bucketStartTime = bucketStartTime;
        this.timestampToBeStored = timestampToBeStored;
        this.durationInMillis = durationInMillis;
        this.videoDensityLocation = videoDensityLocation;
        this.isCatchup = isCatchup;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public TimeIncrementUnits getTimeIncrementUnits() {
        return timeIncrementUnits;
    }

    public void setTimeIncrementUnits(TimeIncrementUnits timeIncrementUnits) {
        this.timeIncrementUnits = timeIncrementUnits;
    }

    public Long getBucketStartTime() {
        return bucketStartTime;
    }

    public void setBucketStartTime(Long bucketStartTime) {
        this.bucketStartTime = bucketStartTime;
    }

    public Long getTimestampToBeStored() {
        return timestampToBeStored;
    }

    public void setTimestampToBeStored(Long timestampToBeStored) {
        this.timestampToBeStored = timestampToBeStored;
    }

    public Long getDurationInMillis() {
        return durationInMillis;
    }

    public void setDurationInMillis(Long durationInMillis) {
        this.durationInMillis = durationInMillis;
    }

    public VideoDensityLocation getVideoDensityLocation() {
        return videoDensityLocation;
    }

    public void setVideoDensityLocation(VideoDensityLocation videoDensityLocation) {
        this.videoDensityLocation = videoDensityLocation;
    }

    public boolean isCatchup() {
        return isCatchup;
    }

    public void setCatchup(boolean isCatchup) {
        this.isCatchup = isCatchup;
    }

    public String getKDSPartitionKey() {
        // Always have the same shard for a device
        return String.format("%s#%s#%d", deviceId, timeIncrementUnits, bucketStartTime);
    }

    @Override
    public String toString() {
        return String.format("[deviceId=%s, timeIncrementUnit=%s, bucketStartTime=%s, timestampToBeStored=%s, " +
                        " duration={}, location={}, isCatchup=%s]", deviceId, timeIncrementUnits, bucketStartTime,
                timestampToBeStored, durationInMillis, videoDensityLocation, isCatchup);
    }

}


