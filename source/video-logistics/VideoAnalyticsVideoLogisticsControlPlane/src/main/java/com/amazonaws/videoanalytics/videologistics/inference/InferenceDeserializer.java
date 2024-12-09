package com.amazonaws.videoanalytics.videologistics.inference;

import com.fasterxml.jackson.databind.ObjectMapper;

public class InferenceDeserializer {
    private final ObjectMapper objectMapper;

    public InferenceDeserializer(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public KdsInference deserialize(final String inference) {
        try {
            return objectMapper.readValue(inference, KdsInference.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize inference as KdsInference from KDS");
        }
    }

    public static OpenSearchInference toOpenSearchInference(final KdsInference kdsInference) {
        KdsMetadata kdsMetadata = kdsInference.getMetadata();

        OpenSearchMetadata metadata = new OpenSearchMetadata(kdsMetadata.getDeviceId());

        return new OpenSearchInference(kdsMetadata.getTimestamp(), metadata, kdsInference.getModelOutput());
    }
}
