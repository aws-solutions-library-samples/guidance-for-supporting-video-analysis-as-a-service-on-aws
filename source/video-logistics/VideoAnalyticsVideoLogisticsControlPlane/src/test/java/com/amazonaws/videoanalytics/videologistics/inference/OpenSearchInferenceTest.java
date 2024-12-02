package com.amazonaws.videoanalytics.videologistics.inference;

import com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OpenSearchInferenceTest {

    @Test
    public void testConstructor_noThumbnail() throws IOException {
        String modelOutputStr = InferenceTestUtils.readInferenceFromResourcesFolder("test-inference-example-no-thumbnail-extension.json");
        JsonNode modelOutput = new ObjectMapper().readTree(modelOutputStr);
        String timestamp = "2024/06/25";
        OpenSearchMetadata metadata = new OpenSearchMetadata();

        OpenSearchInference inference = new OpenSearchInference(timestamp, metadata, modelOutput);

        assertEquals(new ArrayList<>(), inference.getThumbnailPayloads());
        assertEquals(metadata, inference.getMetadata());
        assertEquals(timestamp, inference.getTimestamp());
    }

}

