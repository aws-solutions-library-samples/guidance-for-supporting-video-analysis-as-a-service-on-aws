package com.amazonaws.videoanalytics.videologistics.timeline;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimelineStorage {
    private final Long bucketStartDate;
    private final Long durationToBeStored;

    public TimelineStorage(final Long bucketStartDate, final Long durationToBeStored) {
        this.bucketStartDate = bucketStartDate;
        this.durationToBeStored = durationToBeStored;
    }
}

