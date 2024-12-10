package com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Converts the incoming timeline list to
 * also include optional nextToken
 */
public class PaginatedListResponse<T> {
    private final ImmutableList<T> results;
    private final String nextToken;

    public PaginatedListResponse(List<T> results, String nextToken) {
        this.results = ImmutableList.<T>builder().addAll(results).build();
        this.nextToken = nextToken;
    }
    public List<T> getResults() {
        return this.results;
    }
    public String getNextToken() {
        return nextToken;
    }
}
