package com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline;

import com.amazonaws.videoanalytics.videologistics.Timeline;
import com.google.common.collect.ImmutableList;
import lombok.Getter;

import java.util.List;
    
@Getter
public class DetailedTimelinePaginatedResponse {
    private final ImmutableList<Timeline> cloudTimeline;
    private final ImmutableList<Timeline> deviceTimeline;
    private final String nextToken;

    public DetailedTimelinePaginatedResponse(final List<Timeline> cloudTimeline,
                                             final List<Timeline> deviceTimeline,
                                             final String nextToken){
        this.cloudTimeline = ImmutableList.<Timeline>builder().addAll(cloudTimeline).build();
        this.deviceTimeline = ImmutableList.<Timeline>builder().addAll(deviceTimeline).build();
        this.nextToken = nextToken;
    }
}