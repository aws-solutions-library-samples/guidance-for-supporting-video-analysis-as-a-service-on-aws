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
import com.amazonaws.videoanalytics.videologistics.TimeIncrementUnits;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.math.BigDecimal;
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

import com.amazonaws.videoanalytics.JSON;

/**
 * ListVideoTimelinesRequestContent
 */
@lombok.Builder
@lombok.AllArgsConstructor
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.8.0")
public class ListVideoTimelinesRequestContent {
  public static final String SERIALIZED_NAME_DEVICE_ID = "deviceId";
  @SerializedName(SERIALIZED_NAME_DEVICE_ID)
  private String deviceId;

  public static final String SERIALIZED_NAME_START_TIME = "startTime";
  @SerializedName(SERIALIZED_NAME_START_TIME)
  private Double startTime;

  public static final String SERIALIZED_NAME_END_TIME = "endTime";
  @SerializedName(SERIALIZED_NAME_END_TIME)
  private Double endTime;

  public static final String SERIALIZED_NAME_TIME_INCREMENT = "timeIncrement";
  @SerializedName(SERIALIZED_NAME_TIME_INCREMENT)
  private BigDecimal timeIncrement;

  public static final String SERIALIZED_NAME_TIME_INCREMENT_UNITS = "timeIncrementUnits";
  @SerializedName(SERIALIZED_NAME_TIME_INCREMENT_UNITS)
  private TimeIncrementUnits timeIncrementUnits;

  public static final String SERIALIZED_NAME_NEXT_TOKEN = "nextToken";
  @SerializedName(SERIALIZED_NAME_NEXT_TOKEN)
  private String nextToken;

  public ListVideoTimelinesRequestContent() {
  }

  public ListVideoTimelinesRequestContent deviceId(String deviceId) {
    this.deviceId = deviceId;
    return this;
  }

  /**
   * Get deviceId
   * @return deviceId
   */
  @javax.annotation.Nonnull
  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }


  public ListVideoTimelinesRequestContent startTime(Double startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Get startTime
   * @return startTime
   */
  @javax.annotation.Nonnull
  public Double getStartTime() {
    return startTime;
  }

  public void setStartTime(Double startTime) {
    this.startTime = startTime;
  }


  public ListVideoTimelinesRequestContent endTime(Double endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * Get endTime
   * @return endTime
   */
  @javax.annotation.Nonnull
  public Double getEndTime() {
    return endTime;
  }

  public void setEndTime(Double endTime) {
    this.endTime = endTime;
  }


  public ListVideoTimelinesRequestContent timeIncrement(BigDecimal timeIncrement) {
    this.timeIncrement = timeIncrement;
    return this;
  }

  /**
   * Get timeIncrement
   * @return timeIncrement
   */
  @javax.annotation.Nonnull
  public BigDecimal getTimeIncrement() {
    return timeIncrement;
  }

  public void setTimeIncrement(BigDecimal timeIncrement) {
    this.timeIncrement = timeIncrement;
  }


  public ListVideoTimelinesRequestContent timeIncrementUnits(TimeIncrementUnits timeIncrementUnits) {
    this.timeIncrementUnits = timeIncrementUnits;
    return this;
  }

  /**
   * Get timeIncrementUnits
   * @return timeIncrementUnits
   */
  @javax.annotation.Nonnull
  public TimeIncrementUnits getTimeIncrementUnits() {
    return timeIncrementUnits;
  }

  public void setTimeIncrementUnits(TimeIncrementUnits timeIncrementUnits) {
    this.timeIncrementUnits = timeIncrementUnits;
  }


  public ListVideoTimelinesRequestContent nextToken(String nextToken) {
    this.nextToken = nextToken;
    return this;
  }

  /**
   * Get nextToken
   * @return nextToken
   */
  @javax.annotation.Nullable
  public String getNextToken() {
    return nextToken;
  }

  public void setNextToken(String nextToken) {
    this.nextToken = nextToken;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListVideoTimelinesRequestContent listVideoTimelinesRequestContent = (ListVideoTimelinesRequestContent) o;
    return Objects.equals(this.deviceId, listVideoTimelinesRequestContent.deviceId) &&
        Objects.equals(this.startTime, listVideoTimelinesRequestContent.startTime) &&
        Objects.equals(this.endTime, listVideoTimelinesRequestContent.endTime) &&
        Objects.equals(this.timeIncrement, listVideoTimelinesRequestContent.timeIncrement) &&
        Objects.equals(this.timeIncrementUnits, listVideoTimelinesRequestContent.timeIncrementUnits) &&
        Objects.equals(this.nextToken, listVideoTimelinesRequestContent.nextToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(deviceId, startTime, endTime, timeIncrement, timeIncrementUnits, nextToken);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ListVideoTimelinesRequestContent {\n");
    sb.append("    deviceId: ").append(toIndentedString(deviceId)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    timeIncrement: ").append(toIndentedString(timeIncrement)).append("\n");
    sb.append("    timeIncrementUnits: ").append(toIndentedString(timeIncrementUnits)).append("\n");
    sb.append("    nextToken: ").append(toIndentedString(nextToken)).append("\n");
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
    openapiFields.add("deviceId");
    openapiFields.add("startTime");
    openapiFields.add("endTime");
    openapiFields.add("timeIncrement");
    openapiFields.add("timeIncrementUnits");
    openapiFields.add("nextToken");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
    openapiRequiredFields.add("deviceId");
    openapiRequiredFields.add("startTime");
    openapiRequiredFields.add("endTime");
    openapiRequiredFields.add("timeIncrement");
    openapiRequiredFields.add("timeIncrementUnits");
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to ListVideoTimelinesRequestContent
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!ListVideoTimelinesRequestContent.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in ListVideoTimelinesRequestContent is not found in the empty JSON string", ListVideoTimelinesRequestContent.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!ListVideoTimelinesRequestContent.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `ListVideoTimelinesRequestContent` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }

      // check to make sure all required properties/fields are present in the JSON string
      for (String requiredField : ListVideoTimelinesRequestContent.openapiRequiredFields) {
        if (jsonElement.getAsJsonObject().get(requiredField) == null) {
          throw new IllegalArgumentException(String.format("The required field `%s` is not found in the JSON string: %s", requiredField, jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if (!jsonObj.get("deviceId").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `deviceId` to be a primitive type in the JSON string but got `%s`", jsonObj.get("deviceId").toString()));
      }
      // validate the required field `timeIncrementUnits`
      TimeIncrementUnits.validateJsonElement(jsonObj.get("timeIncrementUnits"));
      if ((jsonObj.get("nextToken") != null && !jsonObj.get("nextToken").isJsonNull()) && !jsonObj.get("nextToken").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `nextToken` to be a primitive type in the JSON string but got `%s`", jsonObj.get("nextToken").toString()));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!ListVideoTimelinesRequestContent.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'ListVideoTimelinesRequestContent' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<ListVideoTimelinesRequestContent> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(ListVideoTimelinesRequestContent.class));

       return (TypeAdapter<T>) new TypeAdapter<ListVideoTimelinesRequestContent>() {
           @Override
           public void write(JsonWriter out, ListVideoTimelinesRequestContent value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public ListVideoTimelinesRequestContent read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of ListVideoTimelinesRequestContent given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of ListVideoTimelinesRequestContent
   * @throws IOException if the JSON string is invalid with respect to ListVideoTimelinesRequestContent
   */
  public static ListVideoTimelinesRequestContent fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, ListVideoTimelinesRequestContent.class);
  }

  /**
   * Convert an instance of ListVideoTimelinesRequestContent to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

