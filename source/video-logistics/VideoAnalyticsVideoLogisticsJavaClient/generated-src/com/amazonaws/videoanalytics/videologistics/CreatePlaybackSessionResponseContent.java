/*
 * Video Analytic Guidance Solution - Video Logistics
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 2024-10-18
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.amazonaws.videoanalytics.videologistics;

import java.util.Objects;
import com.amazonaws.videoanalytics.videologistics.StreamSource;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * CreatePlaybackSessionResponseContent
 */
@lombok.Builder
@lombok.AllArgsConstructor
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.8.0")
public class CreatePlaybackSessionResponseContent {
  public static final String SERIALIZED_NAME_STREAM_SOURCES = "streamSources";
  @SerializedName(SERIALIZED_NAME_STREAM_SOURCES)
  private List<StreamSource> streamSources = new ArrayList<>();

  public CreatePlaybackSessionResponseContent() {
  }

  public CreatePlaybackSessionResponseContent streamSources(List<StreamSource> streamSources) {
    this.streamSources = streamSources;
    return this;
  }

  public CreatePlaybackSessionResponseContent addStreamSourcesItem(StreamSource streamSourcesItem) {
    if (this.streamSources == null) {
      this.streamSources = new ArrayList<>();
    }
    this.streamSources.add(streamSourcesItem);
    return this;
  }

  /**
   * Get streamSources
   * @return streamSources
   */
  @javax.annotation.Nullable
  public List<StreamSource> getStreamSources() {
    return streamSources;
  }

  public void setStreamSources(List<StreamSource> streamSources) {
    this.streamSources = streamSources;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreatePlaybackSessionResponseContent createPlaybackSessionResponseContent = (CreatePlaybackSessionResponseContent) o;
    return Objects.equals(this.streamSources, createPlaybackSessionResponseContent.streamSources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(streamSources);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreatePlaybackSessionResponseContent {\n");
    sb.append("    streamSources: ").append(toIndentedString(streamSources)).append("\n");
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
    openapiFields.add("streamSources");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to CreatePlaybackSessionResponseContent
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!CreatePlaybackSessionResponseContent.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in CreatePlaybackSessionResponseContent is not found in the empty JSON string", CreatePlaybackSessionResponseContent.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!CreatePlaybackSessionResponseContent.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `CreatePlaybackSessionResponseContent` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if (jsonObj.get("streamSources") != null && !jsonObj.get("streamSources").isJsonNull()) {
        JsonArray jsonArraystreamSources = jsonObj.getAsJsonArray("streamSources");
        if (jsonArraystreamSources != null) {
          // ensure the json data is an array
          if (!jsonObj.get("streamSources").isJsonArray()) {
            throw new IllegalArgumentException(String.format("Expected the field `streamSources` to be an array in the JSON string but got `%s`", jsonObj.get("streamSources").toString()));
          }

          // validate the optional field `streamSources` (array)
          for (int i = 0; i < jsonArraystreamSources.size(); i++) {
            StreamSource.validateJsonElement(jsonArraystreamSources.get(i));
          };
        }
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!CreatePlaybackSessionResponseContent.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'CreatePlaybackSessionResponseContent' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<CreatePlaybackSessionResponseContent> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(CreatePlaybackSessionResponseContent.class));

       return (TypeAdapter<T>) new TypeAdapter<CreatePlaybackSessionResponseContent>() {
           @Override
           public void write(JsonWriter out, CreatePlaybackSessionResponseContent value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public CreatePlaybackSessionResponseContent read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of CreatePlaybackSessionResponseContent given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of CreatePlaybackSessionResponseContent
   * @throws IOException if the JSON string is invalid with respect to CreatePlaybackSessionResponseContent
   */
  public static CreatePlaybackSessionResponseContent fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, CreatePlaybackSessionResponseContent.class);
  }

  /**
   * Convert an instance of CreatePlaybackSessionResponseContent to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

