package com.amazonaws.videoanalytics.videologistics.inference;

import com.amazonaws.videoanalytics.videologistics.inference.KdsMetadata;
import com.amazonaws.videoanalytics.videologistics.validator.InferenceValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import javax.inject.Inject;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.core.SdkBytes;

public class ImportMediaObjectHandler {

    private final MediaObjectDeserializer mediaObjectDeserializer;
    private final InferenceValidator inferenceValidator;
    private final KinesisClient kinesisClient;
    private final InferenceSerializer serializer;

    private static final String KINESIS_DATA_STREAM_NAME = "BulkInferenceKDS";

    @Inject
    ImportMediaObjectHandler(final InferenceValidator inferenceValidator,
                             final KinesisClient kinesisClient,
                             final MediaObjectDeserializer mediaObjectDeserializer,
                             final InferenceSerializer serializer){
        this.mediaObjectDeserializer = mediaObjectDeserializer;
        this.inferenceValidator = inferenceValidator;
        this.kinesisClient = kinesisClient;
        this.serializer = serializer;
    }

    /**
     * Handler to handle storing media objects in kds
     * @param deviceId id of the streaming device
     * @param mediaObject blob 
     * @return
     */
    public void importMediaObject(String deviceId, ByteBuffer mediaObject) {
        // Adding validation here
        String str = new String(mediaObject.array(), StandardCharsets.UTF_8);
        MediaObject media = mediaObjectDeserializer.deserialize(str);

        // KDS metadata expects thumbnails to be passed in separately, we simply need to pass an empty list here
        // as the thumbnail information is extracted by the BulkInferenceLambda from the model output
        KdsMetadata metadata = new KdsMetadata(deviceId, 
            media.getModelName(), media.getModelVersion(), media.getTimestamp(), List.of());

        inferenceValidator.validate(media.getModelName(), media.getModelVersion(), media.getModelOutput().toString());

        String inferenceAsJson = serializer.serialize(metadata, media.getModelOutput().toString());

        PutRecordRequest request = PutRecordRequest.builder()
            .partitionKey(metadata.getKDSPartitionKey())
            .streamName(KINESIS_DATA_STREAM_NAME)
            .data(SdkBytes.fromUtf8String(inferenceAsJson))
            .build();
        PutRecordResponse response = kinesisClient.putRecord(request);
    }
}