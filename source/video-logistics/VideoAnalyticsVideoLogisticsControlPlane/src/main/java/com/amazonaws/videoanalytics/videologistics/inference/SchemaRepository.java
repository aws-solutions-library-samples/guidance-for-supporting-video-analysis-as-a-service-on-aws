package com.amazonaws.videoanalytics.videologistics.inference;

import com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.SpecVersionDetector;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Post PP3, when we have createModelSchema API to create model schema, we need to change this schema repository to
 * get the model schema through getModelSchema API.
 */
public class SchemaRepository {
    private static final Logger LOG = LogManager.getLogger(SchemaRepository.class);

    private final static  String INFERENCE_PROPERTY_PATH_SPLITTER = ".";
    private final static  String INFERENCE_PROPERTY_PATH_SPLITTER_ESCAPE = "\\" + INFERENCE_PROPERTY_PATH_SPLITTER;
    private static final String MODEL_SCHEMA_FILES_FOLDER = "modelschema";
    private static final String INFERENCE_MODEL_OUTPUT = "modelOutput";
    private static final String TIMESTAMP_PROPERTY = "timestamp";
    private static final String METADATA_PROPERTY = "metadata";
    private static final String DEVICE_ID_PROPERTY = "deviceId";
    private static final String THUMBNAILS_PROPERTY = "thumbnailS3Paths";
    private static final Set<String> INFERENCE_NON_NESTED_TOP_LEVEL_PROPERTIES = Set.of(METADATA_PROPERTY, TIMESTAMP_PROPERTY);
    private static final Set<String> METADATA_PROPERTIES = Set.of(DEVICE_ID_PROPERTY, THUMBNAILS_PROPERTY);
    private static final String SCHEMA_PROPERTIES = "properties";
    private static final String SCHEMA_ARRAY_ITEM_PROPERTY = "items";
    private static final String SCHEMA_PROPERTY_TYPE = "type";
    private static final String SCHEMA_ONE_OF_PROPERTY = "oneOf";
    private static final String SCHEMA_PROPERTY_ARRAY_TYPE = "array";
    private static final List<String> DEFAULT_MODEL_SCHEMA_FILES = List.of(
        "Test-1.0.json",
        "Event-1.0.json",
        "UpdateAttributes-1.0.json",
        "Trajectory-1.0.json");

    private static final ImmutableList<String> DEFAULT_MODEL_NAMES = ImmutableList.copyOf(DEFAULT_MODEL_SCHEMA_FILES.stream()
                        .map( fileName -> fileName.split("-")[0]).collect(Collectors.toList()));

    private final Map<String, JsonSchema> modelSchemaMap = new HashMap<>();

    public SchemaRepository(final ObjectMapper objectMapper) {
        this(objectMapper, DEFAULT_MODEL_SCHEMA_FILES);
    }

    public SchemaRepository(final ObjectMapper objectMapper, final List<String> modelSchemaFiles) {
        init(objectMapper, modelSchemaFiles);
    }

    public JsonSchema getModelSchema(final String modelName, final String modelVersion) {
        String key = modelName + "-" + modelVersion;
        return modelSchemaMap.get(key);
    }

    public boolean exists(final String modelName, final String modelVersion) {
        if (Strings.isNullOrEmpty(modelVersion)) {
            return getAllModelNames().contains(modelName);
        }

        return getModelSchema(modelName, modelVersion) != null;

    }

    public List<String> getAllModelNames() {
        return DEFAULT_MODEL_NAMES;
    }

    /**
     *
     * @param path The path of the property, e.g. modelOutput.MetadataStream.VideoAnalytics.Frame.Object.ObjectId
     * @param modelName the model name is used to fetch schema file
     * @param modelVersion the model version is used to fetch schema file
     * @return path list for nested ancestor, e.g. for path A.B.C.D.E, if B and D is array, then return [B, D]
     */
    public List<String> getNestedAncestorProperties(final String path, final String modelName, final String modelVersion) {
        List<String> nestedAncestorProperties = Lists.newArrayList();
        JsonSchema modelSchema = getModelSchema(modelName, modelVersion);
        JsonNode currentNode = modelSchema.getSchemaNode().get(SCHEMA_PROPERTIES);
        String[] parts = path.split(INFERENCE_PROPERTY_PATH_SPLITTER_ESCAPE);
        // metadata/timestamp properties were added by inference ingestion, it's not nested
        if (INFERENCE_NON_NESTED_TOP_LEVEL_PROPERTIES.contains(parts[0])) {
            return nestedAncestorProperties;
        }

        // Skip modelOutput since inference schema doesn't have modelOutput (added by inference ingestion)
        for (int i = 1; i < parts.length; i++) {
            JsonNode propertyNode = currentNode.get(parts[i]);
            JsonNode propertyType = propertyNode.get(SCHEMA_PROPERTY_TYPE);
            String nestedProperty = getNestedAncestor(propertyType, parts, i);
            if (!nestedProperty.isEmpty()) {
                nestedAncestorProperties.add(nestedProperty);
            }
            modelSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(propertyNode);
            propertyNode = modelSchema.getSchemaNode();
            // Object type has properties defined
            if (propertyNode.has(SCHEMA_PROPERTIES)) {
                currentNode = propertyNode.get(SCHEMA_PROPERTIES);
            } else if (propertyNode.has(SCHEMA_ARRAY_ITEM_PROPERTY)) { // propertyNode is an array
                currentNode = propertyNode.get(SCHEMA_ARRAY_ITEM_PROPERTY).get(SCHEMA_PROPERTIES);
            } else if (propertyNode.has(SCHEMA_ONE_OF_PROPERTY)) { // propertyNode is oneOf: an array or object
                Iterator<JsonNode> oneOfNodesIter = propertyNode.get(SCHEMA_ONE_OF_PROPERTY).elements();
                while (oneOfNodesIter.hasNext()) {
                    JsonNode oneOfNode = oneOfNodesIter.next();
                    propertyType = oneOfNode.get(SCHEMA_PROPERTY_TYPE);
                    nestedProperty = getNestedAncestor(propertyType, parts, i);
                    if (!nestedProperty.isEmpty()) {
                        nestedAncestorProperties.add(nestedProperty);
                        // Move to array's properties
                        currentNode = oneOfNode.get(SCHEMA_ARRAY_ITEM_PROPERTY).get(SCHEMA_PROPERTIES);
                    }
                }
            }
        }
        return nestedAncestorProperties;
    }

    public void validateProperties(final Set<String> propertyPaths, final String modelName, final String modelVersion) {
        JsonSchema modelSchema = getModelSchema(modelName, modelVersion);
        propertyPaths.forEach( path -> validateProperty(modelSchema, path));
    }

    private void validateProperty(final JsonSchema modelSchema,final String propertyPath) {
        String errorMessage = String.format(VideoAnalyticsExceptionMessage.INVALID_PROPERTY_IN_AGGREGATION, propertyPath);
        String[] parts = propertyPath.split(INFERENCE_PROPERTY_PATH_SPLITTER_ESCAPE);
        String firstPart = parts[0];
        // timestamp.xxx is invalid
        if(TIMESTAMP_PROPERTY.equals(firstPart)) {
            if (parts.length > 1) {
                throw new RuntimeException(errorMessage);
            }
            return;
        }

        // only metadata.deviceId and metadata.thumbnailS3Paths are valid
        if (METADATA_PROPERTY.equals(firstPart)) {
            if (parts.length > 2 || parts.length == 2 && !METADATA_PROPERTIES.contains(parts[1])) {
                throw new RuntimeException(errorMessage);
            }
            return;
        }

        // Inference document should have modelOutput property
        if (!INFERENCE_MODEL_OUTPUT.equals(firstPart)) {
            throw new RuntimeException(errorMessage);
        }

        // Validate modelOutput schema
       if (!isPropertyValid(parts, 1, modelSchema.getSchemaNode())) {
           throw new RuntimeException(errorMessage);
       }
    }

    /**
     * Validate property path is defined in schema
     *
     * @param parts all parts from property path
     * @param index the start index to validate
     * @param currentNode the Json node in the schema file
     *
     * @return true if valid
     */
    private boolean isPropertyValid(final String[] parts, final int index, final JsonNode currentNode) {
        if (index == parts.length) {
            return true;
        }

        String property = parts[index];
        // Object type
        if (currentNode.has(SCHEMA_PROPERTIES)) {
            JsonNode node = currentNode.get(SCHEMA_PROPERTIES).get(property);
            return node != null && isPropertyValid(parts, index + 1, node);
        }

        // propertyNode is an array
        if (currentNode.has(SCHEMA_ARRAY_ITEM_PROPERTY)) {
            JsonNode node = currentNode.get(SCHEMA_ARRAY_ITEM_PROPERTY).get(SCHEMA_PROPERTIES);
            return node != null && isPropertyValid(parts, index, node);
        }

        // propertyNode is oneOf: an array or object
        if (currentNode.has(SCHEMA_ONE_OF_PROPERTY)) {
            Iterator<JsonNode> oneOfNodesIter = currentNode.get(SCHEMA_ONE_OF_PROPERTY).elements();
            while (oneOfNodesIter.hasNext()) {
                if (isPropertyValid(parts, index, oneOfNodesIter.next())) {
                    return true;
                }
            }
        }

        return false;
    }

    private String getNestedAncestor(JsonNode propertyTypeNode, String[] parts, int index) {
        if (propertyTypeNode != null && SCHEMA_PROPERTY_ARRAY_TYPE.equals(propertyTypeNode.asText())) {
            return Arrays.stream(parts, 0, index + 1)
                    .collect(Collectors.joining(INFERENCE_PROPERTY_PATH_SPLITTER));
        }
        return StringUtils.EMPTY;
    }

    private void init(final ObjectMapper objectMapper, final List<String> modelSchemaFiles) {
        for (String fileName : modelSchemaFiles) {
            int index = fileName.lastIndexOf(".json");
            if (index <= 0) {
                throw new RuntimeException("Invalid model schema file name: " + fileName);
            }

            String schemaKey = fileName.substring(0, index);
            try {
                InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(MODEL_SCHEMA_FILES_FOLDER + File.separator + fileName);

                JsonNode schemaNode = objectMapper.readTree(is);
                JsonSchema jsonSchema = JsonSchemaFactory
                    .getInstance(SpecVersionDetector.detect(schemaNode))
                    .getSchema(schemaNode);
                modelSchemaMap.put(schemaKey, jsonSchema);
            } catch (IOException e) {
                throw new RuntimeException("Fail to load model schema file from: " + fileName);
            }
        }
    }
}