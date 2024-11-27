package com.amazonaws.videoanalytics.videologistics.inference;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaObject {
    @JsonProperty
    private String modelName;
    @JsonProperty
    private String modelVersion;
    @JsonProperty
    private JsonNode modelOutput;
    @JsonProperty
    private String timestamp;
    public MediaObject(){}

    public MediaObject(
            final String modelName,
            final String modelVersion,
            final JsonNode modelOutput,
            final String timestamp){
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.modelOutput = modelOutput;
        this.timestamp = timestamp;
    }
}