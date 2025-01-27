package com.amazonaws.videoanalytics.videologistics.inference;

import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class KdsInference {

    @JsonProperty
    private KdsMetadata metadata;
    @JsonProperty
    private JsonNode modelOutput;
    @JsonProperty
    private List<String> thumbnailS3Paths;

    public KdsInference() {
    }

    public KdsInference(KdsMetadata metadata, JsonNode modelOutput) {
        this.metadata = metadata;
        this.modelOutput = modelOutput;
        this.thumbnailS3Paths = List.of();
    }

    public KdsInference(KdsMetadata metadata, JsonNode modelOutput, List<String> thumbnailS3Paths) {
        this.metadata = metadata;
        this.modelOutput = modelOutput;
        this.thumbnailS3Paths = List.copyOf(thumbnailS3Paths);
    }

    public KdsMetadata getMetadata() {
        return this.metadata;
    }
    public List<String> getThumbnailS3Paths() {
        return List.copyOf(thumbnailS3Paths);
    }

    public JsonNode getModelOutput() {
        return this.modelOutput;
    }

    public String getOpenSearchDocumentId() {
        // There could be multiple inferences within one second. Without digest, only the first inference within second
        // will be stored in Open Search, the rest will be dropped.
        return String.format("%s-%s-%s-%s-%s", metadata.getDeviceId(), metadata.getTimestamp(), metadata.getModelName(),
            metadata.getModelVersion(), getEventDigest());
    }

    public String getEventDigest() {
        return DigestUtils.sha384Hex(modelOutput.toString());
    }
}
