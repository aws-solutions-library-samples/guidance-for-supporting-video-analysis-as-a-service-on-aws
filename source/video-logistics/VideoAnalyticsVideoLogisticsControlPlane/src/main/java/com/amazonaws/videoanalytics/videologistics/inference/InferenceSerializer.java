package com.amazonaws.videoanalytics.videologistics.inference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class InferenceSerializer {
    private final ObjectMapper objectMapper;

    public InferenceSerializer(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(final KdsMetadata metadata, final String modelOutput) {
        try {
            JsonNode modelOutputNode = objectMapper.readTree(modelOutput);
            KdsInference inference = new KdsInference(metadata, modelOutputNode);
            return objectMapper.writeValueAsString(inference);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize inference as json for KDS: " + metadata);
        }
    }

    public String serialize(final OpenSearchInference inference) {
        try {
            return objectMapper.writeValueAsString(inference);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize inference as json for Open Search: " + inference.getMetadata());
        }
    }

    public String serialize(KdsMetadata metadata, String modifiedModelOutput, List<String> thumbnailS3Paths) {
        try {
            JsonNode modelOutputNode = objectMapper.readTree(modifiedModelOutput);
            KdsInference inference = new KdsInference(metadata, modelOutputNode, thumbnailS3Paths);
            return objectMapper.writeValueAsString(inference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize inference as json for KDS. ", e);
        }
    }
}