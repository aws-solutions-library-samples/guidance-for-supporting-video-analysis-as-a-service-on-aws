/*
 * Video Analytic Guidance Solution - Device Management
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 2024-10-18
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.amazonaws.videoanalytics.devicemanagement;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;

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

import com.amazonaws.videoanalytics.JSON;

/**
 * ShadowMap
 */
@lombok.Builder
@lombok.AllArgsConstructor
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-11-01T14:43:28.076578-05:00[America/Chicago]", comments = "Generator version: 7.8.0")
public class ShadowMap {
  public static final String SERIALIZED_NAME_SHADOW_NAME = "shadowName";
  @SerializedName(SERIALIZED_NAME_SHADOW_NAME)
  private String shadowName;

  public static final String SERIALIZED_NAME_STATE_DOCUMENT = "stateDocument";
  @SerializedName(SERIALIZED_NAME_STATE_DOCUMENT)
  private Object stateDocument = null;

  public ShadowMap() {
  }

  public ShadowMap shadowName(String shadowName) {
    this.shadowName = shadowName;
    return this;
  }

  /**
   * Get shadowName
   * @return shadowName
   */
  @javax.annotation.Nullable
  public String getShadowName() {
    return shadowName;
  }

  public void setShadowName(String shadowName) {
    this.shadowName = shadowName;
  }


  public ShadowMap stateDocument(Object stateDocument) {
    this.stateDocument = stateDocument;
    return this;
  }

  /**
   * Get stateDocument
   * @return stateDocument
   */
  @javax.annotation.Nullable
  public Object getStateDocument() {
    return stateDocument;
  }

  public void setStateDocument(Object stateDocument) {
    this.stateDocument = stateDocument;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShadowMap shadowMap = (ShadowMap) o;
    return Objects.equals(this.shadowName, shadowMap.shadowName) &&
        Objects.equals(this.stateDocument, shadowMap.stateDocument);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(shadowName, stateDocument);
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ShadowMap {\n");
    sb.append("    shadowName: ").append(toIndentedString(shadowName)).append("\n");
    sb.append("    stateDocument: ").append(toIndentedString(stateDocument)).append("\n");
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
    openapiFields.add("shadowName");
    openapiFields.add("stateDocument");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to ShadowMap
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!ShadowMap.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in ShadowMap is not found in the empty JSON string", ShadowMap.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!ShadowMap.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `ShadowMap` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if ((jsonObj.get("shadowName") != null && !jsonObj.get("shadowName").isJsonNull()) && !jsonObj.get("shadowName").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `shadowName` to be a primitive type in the JSON string but got `%s`", jsonObj.get("shadowName").toString()));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!ShadowMap.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'ShadowMap' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<ShadowMap> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(ShadowMap.class));

       return (TypeAdapter<T>) new TypeAdapter<ShadowMap>() {
           @Override
           public void write(JsonWriter out, ShadowMap value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public ShadowMap read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of ShadowMap given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of ShadowMap
   * @throws IOException if the JSON string is invalid with respect to ShadowMap
   */
  public static ShadowMap fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, ShadowMap.class);
  }

  /**
   * Convert an instance of ShadowMap to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

