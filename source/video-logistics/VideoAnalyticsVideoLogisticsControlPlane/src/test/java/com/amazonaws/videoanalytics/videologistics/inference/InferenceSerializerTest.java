package com.amazonaws.videoanalytics.videologistics.inference;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.DEVICE_ID;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.INFERENCE_TIMESTAMP_STR;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.MODEL_NAME;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.MODEL_VERSION;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.OPEN_SEARCH_INFERENCE_JSON_1;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.THUMBNAIL_METADATA_LIST;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.readInferenceFromResourcesFolder;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;

public class InferenceSerializerTest {

    @Mock
    private ObjectMapper mockObjectMapper;

    @InjectMocks
    private InferenceSerializer mockSerializer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final InferenceSerializer serializer = new InferenceSerializer(objectMapper);

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        // Disable auto-detection. Explicitly allowlist fields for ser/de using @JsonProperty to avoid
        // inadvertently serializing fields not meant for storage.
        objectMapper.setVisibility(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.NONE)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    }

    @Test
    public void serializeForKds() {
        KdsMetadata kdsMetadata = new KdsMetadata(DEVICE_ID,
            MODEL_NAME, MODEL_VERSION, INFERENCE_TIMESTAMP_STR, THUMBNAIL_METADATA_LIST);
        String modelOutput = readInferenceFromResourcesFolder("test-inference-example.json");
        String inference = serializer.serialize(kdsMetadata, modelOutput);

        String expectedInference = readInferenceFromResourcesFolder("test-inference-kds-example-1.json");
        assertEquals(expectedInference, inference);
    }

    @Test
    public void serializeForKds_Exception() throws IOException {
        KdsMetadata kdsMetadata = new KdsMetadata(DEVICE_ID, MODEL_NAME, MODEL_VERSION, INFERENCE_TIMESTAMP_STR, THUMBNAIL_METADATA_LIST);
        String modelOutput = readInferenceFromResourcesFolder("test-inference-example.json");

        when(mockObjectMapper.readTree(modelOutput)).thenThrow(new RuntimeException());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            mockSerializer.serialize(kdsMetadata, modelOutput);
        });
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Failed to serialize inference as json for KDS"));
    }

    @Test
    public void serializeForOpenSearch() throws IOException {
        OpenSearchMetadata openSearchMetadata = new OpenSearchMetadata(DEVICE_ID);
        OpenSearchInference openSearchInference = new OpenSearchInference(INFERENCE_TIMESTAMP_STR,
            openSearchMetadata, objectMapper.readTree(OPEN_SEARCH_INFERENCE_JSON_1));
        openSearchMetadata.addThumbnailS3Path("s3://upload/path");
        String inference = serializer.serialize(openSearchInference);

        String expectedInference = "{\"timestamp\":\"1696639307\",\"metadata\":{\"deviceId\":\"Device#123\"," +
                "\"thumbnailS3Paths\":[\"s3://upload/path\"]},\"modelOutput\":{\"MetadataStream\":{\"VideoAnalytics\":{\"Frame\":{\"Extension\":{}}}},\"confidence\":0.1}}";
        assertEquals(expectedInference, inference);
    }

    @Test
    public void serializeForOpenSearch_Exception() throws IOException {
        OpenSearchMetadata openSearchMetadata = new OpenSearchMetadata(DEVICE_ID);
        OpenSearchInference openSearchInference = new OpenSearchInference(INFERENCE_TIMESTAMP_STR,
            openSearchMetadata, objectMapper.readTree(OPEN_SEARCH_INFERENCE_JSON_1));

        when(mockObjectMapper.writeValueAsString(openSearchInference)).thenThrow(new RuntimeException());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            mockSerializer.serialize(openSearchInference);
        });
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Failed to serialize inference as json for Open Search"));
    }
}
