package com.amazonaws.videoanalytics.videologistics.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;

import com.amazonaws.videoanalytics.videologistics.inference.SchemaRepository;
import java.util.Set;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.NO_SCHEMA_FOR_MODEL;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INFERENCE_NOT_IN_JSON;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INFERENCE_VALIDATION_FAILURE;

public class InferenceValidator {
    private final ObjectMapper objectMapper;
    private final SchemaRepository schemaRepository;

    public InferenceValidator(final ObjectMapper objectMapper, final SchemaRepository schemaRepository) {
        this.objectMapper = objectMapper;
        this.schemaRepository = schemaRepository;
    }

    /**
     * The validation is based on https://json-schema.org/draft/2020-12/json-schema-validation.html
     * Currently we define schema at resources folder
     */
    public void validate(final String modelName, final String modelVersion, final String inference) {
        String modelInfo = modelName + "-" + modelVersion;

        JsonSchema jsonSchema = schemaRepository.getModelSchema(modelName, modelVersion);
        if (jsonSchema == null) {
            throw new RuntimeException(String.format(NO_SCHEMA_FOR_MODEL, modelInfo));
        }

        JsonNode inferenceNode;
        try{
            inferenceNode = objectMapper.readTree(inference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format(INFERENCE_NOT_IN_JSON, modelInfo, inference));
        }

        Set<ValidationMessage> result = jsonSchema.validate(inferenceNode);
        if (!result.isEmpty()) {
            throw new RuntimeException(String.format(String.format(INFERENCE_VALIDATION_FAILURE, modelInfo, result, inference), modelInfo, inference));
        }
    }

}
