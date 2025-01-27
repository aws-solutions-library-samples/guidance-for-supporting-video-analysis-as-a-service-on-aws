package com.amazonaws.videoanalytics.videologistics.timeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class TimestampInfo {
    @JsonProperty
    private Long timestamp;
    @JsonProperty
    private Long duration;

    public TimestampInfo(){}

    public TimestampInfo(final Long timestamp, final Long duration) {
        this.timestamp = timestamp;
        this.duration = duration;
    }
}
