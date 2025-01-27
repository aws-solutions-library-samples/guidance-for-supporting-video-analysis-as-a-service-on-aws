package com.amazonaws.videoanalytics.videologistics.inference;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ThumbnailMetadata {
    @JsonProperty
    private String checksum;
    @JsonProperty
    private Long contentLength;

    public ThumbnailMetadata(){
    }

    public ThumbnailMetadata(String checksum, Long contentLength) {
        this.checksum = checksum;
        this.contentLength = contentLength;
    }

    public Long getContentLength() {
        return contentLength;
    }

    public String getChecksum() {
        return checksum;
    }
}