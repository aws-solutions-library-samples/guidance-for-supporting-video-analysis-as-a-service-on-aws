package com.amazonaws.videoanalytics.videologistics.inference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.MODEL_NAME;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.MODEL_VERSION;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.INFERENCE_TIMESTAMP_STR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class MediaObjectDeserializerTest {
    @Mock
    private ObjectMapper mockObjectMapper;

    @InjectMocks
    private MediaObjectDeserializer mockDeserializer;

    private ObjectMapper objectMapper = new ObjectMapper();

    private MediaObjectDeserializer deserializer = new MediaObjectDeserializer(new ObjectMapper());

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void deserialize() throws IOException {
        String expectedModelOutputStr = readMediaFromResourcesFolder("test-model-output.json");
        JsonNode expectedModelOutput = objectMapper.readTree(expectedModelOutputStr);
        MediaObject expectedMedia = new MediaObject(MODEL_NAME, MODEL_VERSION, expectedModelOutput, INFERENCE_TIMESTAMP_STR);

        String mediaObjectJson = readMediaFromResourcesFolder("test-media-object.json");
        MediaObject mediaObject = deserializer.deserialize(mediaObjectJson);

        assertEquals(expectedMedia.getModelVersion(), mediaObject.getModelVersion());
        assertEquals(expectedMedia.getModelName(), mediaObject.getModelName());
        assertEquals(expectedMedia.getModelVersion(), mediaObject.getModelVersion());

        assertEquals(expectedModelOutput, mediaObject.getModelOutput());
    }

    @Test
    public void deserialize_Exception() throws IOException {
        String inferenceJson = readMediaFromResourcesFolder("test-incorrect-media.json");
        when(mockObjectMapper.readValue(inferenceJson, MediaObject.class)).thenThrow(new RuntimeException());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            mockDeserializer.deserialize(inferenceJson);
        });
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Failed to deserialize"));
    }

    private String readMediaFromResourcesFolder(final String fileName) {
        try {
            InputStream inputStream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("mediaObject/" + fileName);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read media object file from " + fileName, e);
        }
    }
}
