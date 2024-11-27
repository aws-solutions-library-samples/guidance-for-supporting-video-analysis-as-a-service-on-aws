package com.amazonaws.videoanalytics.videologistics.utils;

import com.amazonaws.videoanalytics.videologistics.inference.KdsInference;
import com.amazonaws.videoanalytics.videologistics.inference.ThumbnailMetadata;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class InferenceTestUtils {
    public static final Date INFERENCE_TIMESTAMP = Date.from(Instant.ofEpochMilli(1696639307418L));
    public static final String INFERENCE_TIMESTAMP_STR = Instant.ofEpochMilli(1696639307418L).toString();
    public static final String ACCOUNT_ID = "Account#123";
    public static final String INVALID_ACCOUNT_ID = "Account#456";
    public static final String DEVICE_ID = "Device#123";
    public static final String MODEL_NAME = "Test";
    public static final String MODEL_VERSION = "1.0";
    public static final String DUMMY_PARTITION_KEY = "Dummy Key";
    public static final String ENCRYPTED_FAS = "encryptedFASToken";
    public static final String KDS_INFERENCE_1 = InferenceTestUtils.readInferenceFromResourcesFolder("test-inference-kds-example-1.json");
    public static final String KDS_INFERENCE_2 = InferenceTestUtils.readInferenceFromResourcesFolder("test-inference-kds-example-2.json");
    public static final String KDS_INFERENCE_3 = InferenceTestUtils.readInferenceFromResourcesFolder("test-inference-kds-example-3.json");
    public static final String KDS_INFERENCE_4 = InferenceTestUtils.readInferenceFromResourcesFolder("test-inference-kds-example-4.json");
    public static final String KDS_INFERENCE_w_THUMBNAILS =
            InferenceTestUtils.readInferenceFromResourcesFolder("test-inference-kds-extracted-thumbnails.json");
    public static final String OPEN_SEARCH_INFERENCE_JSON_1 = "{\"MetadataStream\":{\"VideoAnalytics\":{\"Frame\":" +
            "{\"Extension\":{\"Image\":{\"ImageId\":0,\"data_size\":36896,\"data\":\"dGhpcyBpcyBhbiBpbWFnZQ==\"}}}}}," +
            "\"confidence\": 0.1}";
    public static final String OPEN_SEARCH_INFERENCE_JSON_2 = "{\"confidence\": 0.2}";
    public static final String OPEN_SEARCH_INFERENCE_JSON_3 = "{\"confidence\": 0.3}";
    public static final String OPEN_SEARCH_INFERENCE_JSON_4 = "{\"confidence\": 0.4}";
    public static final String THUMBNAIL_UPLOAD_PATH = "bucketname/prefix/image.jpeg";
    public static final String IMAGE = "this is an image";

    private static final String RESOURCE_FOLDER = "inferences/";
    public static final ThumbnailMetadata THUMBNAIL_METADATA = new ThumbnailMetadata("CHECKSUM", 10L);
    public static final List<ThumbnailMetadata> THUMBNAIL_METADATA_LIST = Collections.unmodifiableList(List.of(THUMBNAIL_METADATA));
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    private InferenceTestUtils() {
    }

    public static String readInferenceFromResourcesFolder(final String fileName) {
        try {
            InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(RESOURCE_FOLDER + fileName);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Fail to read inference file from " + fileName, e);
        }
    }
}