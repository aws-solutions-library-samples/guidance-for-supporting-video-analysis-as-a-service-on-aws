package com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline;

import com.google.common.collect.ImmutableList;

import java.time.temporal.ChronoUnit;
import java.util.List;

public enum TimeIncrementUnits {
    SECONDS(ChronoUnit.SECONDS),
    MINUTES(ChronoUnit.MINUTES),
    HOURS(ChronoUnit.HOURS),
    DAYS(ChronoUnit.DAYS);

    private final ChronoUnit chronoUnit;

    TimeIncrementUnits(ChronoUnit chronoUnit) {
        this.chronoUnit = chronoUnit;
    }

    public ChronoUnit getChronoUnit() {
        return ChronoUnit.valueOf(chronoUnit.name());
    }

    public static final List<TimeIncrementUnits> TIME_INCREMENT_UNITS_LIST = ImmutableList.of(SECONDS, MINUTES, HOURS, DAYS);
}