package com.amazonaws.videoanalytics.videologistics.timeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTimeline {
    @JsonProperty
    private String customerId;
    @JsonProperty
    private String deviceId;
    @JsonProperty
    private List<TimestampInfo> timestamps;
    @JsonProperty
    private String location;
    public List<TimestampInfo> getTimestamps() {
        return List.copyOf(this.timestamps);
    }
}