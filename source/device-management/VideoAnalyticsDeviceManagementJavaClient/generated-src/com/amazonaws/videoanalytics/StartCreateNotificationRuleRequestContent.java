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
import com.amazonaws.videoanalytics.Condition;
import com.amazonaws.videoanalytics.Destination;
import com.amazonaws.videoanalytics.EventCategory;
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
 * StartCreateNotificationRuleRequestContent
 */
@lombok.Builder
@lombok.AllArgsConstructor
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-28T21:16:17.605797Z[UTC]", comments = "Generator version: 7.8.0")
public class StartCreateNotificationRuleRequestContent {
  public static final String SERIALIZED_NAME_RULE_ID = "ruleId";
  @SerializedName(SERIALIZED_NAME_RULE_ID)
  private String ruleId;

  public static final String SERIALIZED_NAME_EVENT_CATEGORY = "eventCategory";
  @SerializedName(SERIALIZED_NAME_EVENT_CATEGORY)
  private EventCategory eventCategory;

  public static final String SERIALIZED_NAME_RULE_DISABLED = "ruleDisabled";
  @SerializedName(SERIALIZED_NAME_RULE_DISABLED)
  private Boolean ruleDisabled;

  public static final String SERIALIZED_NAME_CONDITION = "condition";
  @SerializedName(SERIALIZED_NAME_CONDITION)
  private Condition condition;

  public static final String SERIALIZED_NAME_DESTINATION = "destination";
  @SerializedName(SERIALIZED_NAME_DESTINATION)
  private Destination destination;

  public static final String SERIALIZED_NAME_ERROR_DESTINATION = "errorDestination";
  @SerializedName(SERIALIZED_NAME_ERROR_DESTINATION)
  private Destination errorDestination;

  public StartCreateNotificationRuleRequestContent() {
  }

  public StartCreateNotificationRuleRequestContent ruleId(String ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  /**
   * Get ruleId
   * @return ruleId
   */
  @javax.annotation.Nonnull
  public String getRuleId() {
    return ruleId;
  }

  public void setRuleId(String ruleId) {
    this.ruleId = ruleId;
  }


  public StartCreateNotificationRuleRequestContent eventCategory(EventCategory eventCategory) {
    this.eventCategory = eventCategory;
    return this;
  }

  /**
   * Get eventCategory
   * @return eventCategory
   */
  @javax.annotation.Nonnull
  public EventCategory getEventCategory() {
    return eventCategory;
  }

  public void setEventCategory(EventCategory eventCategory) {
    this.eventCategory = eventCategory;
  }


  public StartCreateNotificationRuleRequestContent ruleDisabled(Boolean ruleDisabled) {
    this.ruleDisabled = ruleDisabled;
    return this;
  }

  /**
   * Get ruleDisabled
   * @return ruleDisabled
   */
  @javax.annotation.Nullable
  public Boolean getRuleDisabled() {
    return ruleDisabled;
  }

  public void setRuleDisabled(Boolean ruleDisabled) {
    this.ruleDisabled = ruleDisabled;
  }


  public StartCreateNotificationRuleRequestContent condition(Condition condition) {
    this.condition = condition;
    return this;
  }

  /**
   * Get condition
   * @return condition
   */
  @javax.annotation.Nonnull
  public Condition getCondition() {
    return condition;
  }

  public void setCondition(Condition condition) {
    this.condition = condition;
  }


  public StartCreateNotificationRuleRequestContent destination(Destination destination) {
    this.destination = destination;
    return this;
  }

  /**
   * Get destination
   * @return destination
   */
  @javax.annotation.Nonnull
  public Destination getDestination() {
    return destination;
  }

  public void setDestination(Destination destination) {
    this.destination = destination;
  }


  public StartCreateNotificationRuleRequestContent errorDestination(Destination errorDestination) {
    this.errorDestination = errorDestination;
    return this;
  }

  /**
   * Get errorDestination
   * @return errorDestination
   */
  @javax.annotation.Nullable
  public Destination getErrorDestination() {
    return errorDestination;
  }

  public void setErrorDestination(Destination errorDestination) {
    this.errorDestination = errorDestination;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StartCreateNotificationRuleRequestContent startCreateNotificationRuleRequestContent = (StartCreateNotificationRuleRequestContent) o;
    return Objects.equals(this.ruleId, startCreateNotificationRuleRequestContent.ruleId) &&
        Objects.equals(this.eventCategory, startCreateNotificationRuleRequestContent.eventCategory) &&
        Objects.equals(this.ruleDisabled, startCreateNotificationRuleRequestContent.ruleDisabled) &&
        Objects.equals(this.condition, startCreateNotificationRuleRequestContent.condition) &&
        Objects.equals(this.destination, startCreateNotificationRuleRequestContent.destination) &&
        Objects.equals(this.errorDestination, startCreateNotificationRuleRequestContent.errorDestination);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ruleId, eventCategory, ruleDisabled, condition, destination, errorDestination);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StartCreateNotificationRuleRequestContent {\n");
    sb.append("    ruleId: ").append(toIndentedString(ruleId)).append("\n");
    sb.append("    eventCategory: ").append(toIndentedString(eventCategory)).append("\n");
    sb.append("    ruleDisabled: ").append(toIndentedString(ruleDisabled)).append("\n");
    sb.append("    condition: ").append(toIndentedString(condition)).append("\n");
    sb.append("    destination: ").append(toIndentedString(destination)).append("\n");
    sb.append("    errorDestination: ").append(toIndentedString(errorDestination)).append("\n");
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
    openapiFields.add("ruleId");
    openapiFields.add("eventCategory");
    openapiFields.add("ruleDisabled");
    openapiFields.add("condition");
    openapiFields.add("destination");
    openapiFields.add("errorDestination");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
    openapiRequiredFields.add("ruleId");
    openapiRequiredFields.add("eventCategory");
    openapiRequiredFields.add("condition");
    openapiRequiredFields.add("destination");
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to StartCreateNotificationRuleRequestContent
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!StartCreateNotificationRuleRequestContent.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in StartCreateNotificationRuleRequestContent is not found in the empty JSON string", StartCreateNotificationRuleRequestContent.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!StartCreateNotificationRuleRequestContent.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `StartCreateNotificationRuleRequestContent` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }

      // check to make sure all required properties/fields are present in the JSON string
      for (String requiredField : StartCreateNotificationRuleRequestContent.openapiRequiredFields) {
        if (jsonElement.getAsJsonObject().get(requiredField) == null) {
          throw new IllegalArgumentException(String.format("The required field `%s` is not found in the JSON string: %s", requiredField, jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if (!jsonObj.get("ruleId").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `ruleId` to be a primitive type in the JSON string but got `%s`", jsonObj.get("ruleId").toString()));
      }
      // validate the required field `eventCategory`
      EventCategory.validateJsonElement(jsonObj.get("eventCategory"));
      // validate the required field `condition`
      Condition.validateJsonElement(jsonObj.get("condition"));
      // validate the required field `destination`
      Destination.validateJsonElement(jsonObj.get("destination"));
      // validate the optional field `errorDestination`
      if (jsonObj.get("errorDestination") != null && !jsonObj.get("errorDestination").isJsonNull()) {
        Destination.validateJsonElement(jsonObj.get("errorDestination"));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!StartCreateNotificationRuleRequestContent.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'StartCreateNotificationRuleRequestContent' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<StartCreateNotificationRuleRequestContent> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(StartCreateNotificationRuleRequestContent.class));

       return (TypeAdapter<T>) new TypeAdapter<StartCreateNotificationRuleRequestContent>() {
           @Override
           public void write(JsonWriter out, StartCreateNotificationRuleRequestContent value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public StartCreateNotificationRuleRequestContent read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of StartCreateNotificationRuleRequestContent given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of StartCreateNotificationRuleRequestContent
   * @throws IOException if the JSON string is invalid with respect to StartCreateNotificationRuleRequestContent
   */
  public static StartCreateNotificationRuleRequestContent fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, StartCreateNotificationRuleRequestContent.class);
  }

  /**
   * Convert an instance of StartCreateNotificationRuleRequestContent to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

