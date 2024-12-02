package com.amazonaws.videoanalytics.videologistics.inference;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class OpenSearchMetadata {

    @JsonProperty
    private String deviceId;
    @JsonProperty
    private List<String> thumbnailS3Paths;

    public OpenSearchMetadata() {
        this.thumbnailS3Paths = new ArrayList<>();
    }

    public OpenSearchMetadata(final String deviceId) {
        this.deviceId = deviceId;
        this.thumbnailS3Paths = new ArrayList<>();
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public void addThumbnailS3Path(final String thumbnailS3Path) {
        this.thumbnailS3Paths.add(thumbnailS3Path);
    }

    public ArrayList<String> getThumbnailS3Paths() {
        return new ArrayList<>(thumbnailS3Paths);
    }
}