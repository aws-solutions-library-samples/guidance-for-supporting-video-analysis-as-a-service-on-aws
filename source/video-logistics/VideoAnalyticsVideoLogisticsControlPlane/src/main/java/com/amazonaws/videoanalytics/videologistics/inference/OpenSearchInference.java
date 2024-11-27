package com.amazonaws.videoanalytics.videologistics.inference;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Base64;

public class OpenSearchInference {
    @JsonProperty
    private String timestamp;
    @JsonProperty
    private OpenSearchMetadata metadata;
    @JsonProperty
    private JsonNode modelOutput;

    private ArrayList<byte[]> thumbnailPayloads;

    public OpenSearchInference() {
    }

    public OpenSearchInference(String timestamp, OpenSearchMetadata metadata, JsonNode modelOutput) {
        this.timestamp = timestamp;
        this.metadata = metadata;
        this.modelOutput = modelOutput;
        final JsonNode thumbnailNode = removeThumbnailNode(modelOutput);
        this.thumbnailPayloads = parseThumbnailsPayload(thumbnailNode);
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public OpenSearchMetadata getMetadata() {
        return this.metadata;
    }

    public  JsonNode getModelOutput() {
        return this.modelOutput;
    }

    public ArrayList<byte[]> getThumbnailPayloads() {
        return this.thumbnailPayloads;
    }
    
    private ArrayList<byte[]> parseThumbnailsPayload(final JsonNode thumbnailsNode) {
        final ArrayList<byte[]> finalList = new ArrayList<>();
        if(thumbnailsNode != null) {
            if (thumbnailsNode.isArray()) {
                final ArrayNode thumbnails = (ArrayNode) thumbnailsNode;
                thumbnails.elements().forEachRemaining(thumbnail -> finalList.add(parseThumbnailPayload(thumbnail)));
            } else {
                finalList.add(parseThumbnailPayload(thumbnailsNode));
            }
        }
        return finalList;
    }

    private byte[] parseThumbnailPayload(final JsonNode thumbnailNode) {
        final String base64EncodedImage = thumbnailNode.get("data").asText();
        return Base64.getDecoder().decode(base64EncodedImage);
    }

    private JsonNode removeThumbnailNode(final JsonNode modelOutput) {
        final JsonNode extensionNode = modelOutput
                .get("MetadataStream")
                .get("VideoAnalytics")
                .get("Frame")
                .get("Extension");

        if (extensionNode == null) {
            return null;
        }

        final JsonNode imageNode = extensionNode.get("Image");
        final ObjectNode extension = (ObjectNode) extensionNode;

        extension.remove("Image");

        return imageNode;
    }

    public void addThumbnailS3Path(final String thumbnailS3Path) {
        this.metadata.addThumbnailS3Path(thumbnailS3Path);
    }

}