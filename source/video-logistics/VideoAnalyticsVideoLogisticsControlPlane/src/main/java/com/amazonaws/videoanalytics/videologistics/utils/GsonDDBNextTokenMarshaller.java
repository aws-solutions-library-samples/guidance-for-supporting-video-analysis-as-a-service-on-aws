package com.amazonaws.videoanalytics.videologistics.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;

/**
 * Class defining marshalling for DynamoDB nextToken conversion using Gson.
 * The nextToken that we send back to the customer is expected to be a string, while the lastEvaluatedKey
 * we get back from DDB is a Map. We marshall said map into a json and encode it into a base64 string
 * The interceptor would then encrypt this string as it is sent along to the user.
 * https://refresh.sage.amazon.dev/posts/1501650
 */
public final class GsonDDBNextTokenMarshaller {
    private static final Type NEXT_TOKEN_TYPE = new TypeReference<Map<String, AttributeValue>>() {
    }.getType();
    private static final Gson GSON = getGsonBuilderForAttributeValue();

    private GsonDDBNextTokenMarshaller() {
    }

    /**
     * Marshall a DynamoDB NextToken into a text blob. This blob must be sent back unmodified in a subsequent request in
     * order for pagination to work properly.
     *
     * @param nextToken DynamoDB NextToken object from a Scan or Query result page.
     * @return Text blob to be sent in a client response.
     */
    public static String marshall(final Map<String, AttributeValue> nextToken) {
        if (nextToken == null) {
            return null;
        }
        String marshalledToken = GSON.toJson(nextToken);
        return Base64.encodeBase64String(marshalledToken.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Unmarshall a text blob into a DynamoDB NextToken.
     *
     * @param marshalledToken marshalled text blob representing a DynamoDB NextToken
     * @return DynamoDB NextToken object for use in Scan and Query operations.
     */
    public static Map<String, AttributeValue> unmarshall(final String marshalledToken) {
        if (marshalledToken == null) {
            return null;
        }
        String decodedMarshalledToken = new String(Base64.decodeBase64(marshalledToken), StandardCharsets.UTF_8);
        return GSON.fromJson(decodedMarshalledToken, NEXT_TOKEN_TYPE);
    }

    /**
     * Support deserialization of Attribute type. Currently supported types are string and number.
     */
    private static Gson getGsonBuilderForAttributeValue() {
        final GsonBuilder gsonBuilder = new GsonBuilder().registerTypeAdapter(new TypeReference<AttributeValue>() {
        }.getType(), (JsonDeserializer<AttributeValue>) (jsonElement, type, jsonDeserializationContext) -> {
            final JsonObject jsonObject = jsonElement.getAsJsonObject();

            if (jsonObject.has("s")) {
                return AttributeValue.fromS(jsonObject.get("s").getAsString());
            } else if (jsonObject.has("n")) {
                return AttributeValue.fromN(jsonObject.get("n").getAsString());
            }
            throw new RuntimeException("Unsupported key type for lastEvaluateKey");
        });
        return gsonBuilder.create();
    }

}