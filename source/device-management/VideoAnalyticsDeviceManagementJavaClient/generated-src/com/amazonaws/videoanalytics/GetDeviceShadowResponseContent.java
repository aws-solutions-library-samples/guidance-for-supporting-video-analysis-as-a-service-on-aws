/*
 * Video Analytic Guidance Solution
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 2024-10-18
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.amazonaws.videoanalytics;

import java.util.Objects;
import com.amazonaws.videoanalytics.ShadowMap;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.JSON;

/**
 * GetDeviceShadowResponseContent
 */
@lombok.Builder
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-24T17:18:34.468319Z[UTC]", comments = "Generator version: 7.8.0")
public class GetDeviceShadowResponseContent {
  public static final String SERIALIZED_NAME_SHADOW_PAYLOAD = "shadowPayload";
  @SerializedName(SERIALIZED_NAME_SHADOW_PAYLOAD)
  private ShadowMap shadowPayload;

  public GetDeviceShadowResponseContent() {
  }

  public GetDeviceShadowResponseContent shadowPayload(ShadowMap shadowPayload) {
    this.shadowPayload = shadowPayload;
    return this;
  }

  /**
   * Get shadowPayload
   * @return shadowPayload
   */
  @javax.annotation.Nullable
  public ShadowMap getShadowPayload() {
    return shadowPayload;
  }

  public void setShadowPayload(ShadowMap shadowPayload) {
    this.shadowPayload = shadowPayload;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetDeviceShadowResponseContent getDeviceShadowResponseContent = (GetDeviceShadowResponseContent) o;
    return Objects.equals(this.shadowPayload, getDeviceShadowResponseContent.shadowPayload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(shadowPayload);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetDeviceShadowResponseContent {\n");
    sb.append("    shadowPayload: ").append(toIndentedString(shadowPayload)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }


  public static HashSet<String> openapiFields;
  public static HashSet<String> openapiRequiredFields;

  static {
    // a set of all properties/fields (JSON key names)
    openapiFields = new HashSet<String>();
    openapiFields.add("shadowPayload");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to GetDeviceShadowResponseContent
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!GetDeviceShadowResponseContent.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in GetDeviceShadowResponseContent is not found in the empty JSON string", GetDeviceShadowResponseContent.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!GetDeviceShadowResponseContent.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `GetDeviceShadowResponseContent` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      // validate the optional field `shadowPayload`
      if (jsonObj.get("shadowPayload") != null && !jsonObj.get("shadowPayload").isJsonNull()) {
        ShadowMap.validateJsonElement(jsonObj.get("shadowPayload"));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!GetDeviceShadowResponseContent.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'GetDeviceShadowResponseContent' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<GetDeviceShadowResponseContent> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(GetDeviceShadowResponseContent.class));

       return (TypeAdapter<T>) new TypeAdapter<GetDeviceShadowResponseContent>() {
           @Override
           public void write(JsonWriter out, GetDeviceShadowResponseContent value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public GetDeviceShadowResponseContent read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of GetDeviceShadowResponseContent given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of GetDeviceShadowResponseContent
   * @throws IOException if the JSON string is invalid with respect to GetDeviceShadowResponseContent
   */
  public static GetDeviceShadowResponseContent fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, GetDeviceShadowResponseContent.class);
  }

  /**
   * Convert an instance of GetDeviceShadowResponseContent to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

