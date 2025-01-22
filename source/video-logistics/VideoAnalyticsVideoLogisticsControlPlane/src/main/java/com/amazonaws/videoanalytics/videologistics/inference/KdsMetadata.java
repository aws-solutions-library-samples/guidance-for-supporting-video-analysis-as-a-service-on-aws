package com.amazonaws.videoanalytics.videologistics.inference;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KdsMetadata {
    @JsonProperty
    private String deviceId;
    @JsonProperty
    private String modelName;
    @JsonProperty
    private String modelVersion;
    @JsonProperty
    private String timestamp;
    @JsonProperty
    private List<ThumbnailMetadata> thumbnailMetadata;

    public KdsMetadata(){}

    public KdsMetadata(final String deviceId, final String modelName,
                       final String modelVersion, final String timestamp, final List<ThumbnailMetadata> thumbnailMetadata) {
        this.deviceId = deviceId;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.timestamp = timestamp;
        this.thumbnailMetadata = thumbnailMetadata;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public String getModelName() {
        return this.modelName;
    }

    public String getModelVersion() {
        return this.modelVersion;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public List<ThumbnailMetadata> getThumbnailMetadata() {
        return List.copyOf(this.thumbnailMetadata);
    }

    public Long getContentLength(int index) {
        return thumbnailMetadata.get(index).getContentLength();
    }

    public String getChecksum(int index) {
        return thumbnailMetadata.get(index).getChecksum();
    }

    public String getKDSPartitionKey() {
        // Always have the same shard for a device
        return String.format("%s", deviceId);
    }

    @Override
    public String toString() {
        return String.format("[deviceId=%s, modelName=%s, modelVersion=%s, timestamp=%s, thumbnailMetadata=%s]",
                deviceId, modelName, modelVersion, timestamp, thumbnailMetadata);
    }
}
