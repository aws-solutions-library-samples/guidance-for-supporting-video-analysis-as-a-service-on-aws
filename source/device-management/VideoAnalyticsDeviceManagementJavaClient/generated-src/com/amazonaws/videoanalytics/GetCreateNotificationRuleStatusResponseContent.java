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
import com.amazonaws.videoanalytics.Status;
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
 * GetCreateNotificationRuleStatusResponseContent
 */
@lombok.Builder
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-24T17:18:34.468319Z[UTC]", comments = "Generator version: 7.8.0")
public class GetCreateNotificationRuleStatusResponseContent {
  public static final String SERIALIZED_NAME_JOB_ID = "jobId";
  @SerializedName(SERIALIZED_NAME_JOB_ID)
  private String jobId;

  public static final String SERIALIZED_NAME_RULE_ID = "ruleId";
  @SerializedName(SERIALIZED_NAME_RULE_ID)
  private String ruleId;

  public static final String SERIALIZED_NAME_EVENT_CATEGORY = "eventCategory";
  @SerializedName(SERIALIZED_NAME_EVENT_CATEGORY)
  private EventCategory eventCategory;

  public static final String SERIALIZED_NAME_CONDITION = "condition";
  @SerializedName(SERIALIZED_NAME_CONDITION)
  private Condition condition;

  public static final String SERIALIZED_NAME_DESTINATION = "destination";
  @SerializedName(SERIALIZED_NAME_DESTINATION)
  private Destination destination;

  public static final String SERIALIZED_NAME_ERROR_DESTINATION = "errorDestination";
  @SerializedName(SERIALIZED_NAME_ERROR_DESTINATION)
  private Destination errorDestination;

  public static final String SERIALIZED_NAME_RULE_DISABLED = "ruleDisabled";
  @SerializedName(SERIALIZED_NAME_RULE_DISABLED)
  private Boolean ruleDisabled;

  public static final String SERIALIZED_NAME_ERROR_MESSAGE = "errorMessage";
  @SerializedName(SERIALIZED_NAME_ERROR_MESSAGE)
  private String errorMessage;

  public static final String SERIALIZED_NAME_STATUS = "status";
  @SerializedName(SERIALIZED_NAME_STATUS)
  private Status status;

  public static final String SERIALIZED_NAME_CREATE_TIME = "createTime";
  @SerializedName(SERIALIZED_NAME_CREATE_TIME)
  private Double createTime;

  public static final String SERIALIZED_NAME_MODIFIED_TIME = "modifiedTime";
  @SerializedName(SERIALIZED_NAME_MODIFIED_TIME)
  private Double modifiedTime;

  public GetCreateNotificationRuleStatusResponseContent() {
  }

  public GetCreateNotificationRuleStatusResponseContent jobId(String jobId) {
    this.jobId = jobId;
    return this;
  }

  /**
   * Get jobId
   * @return jobId
   */
  @javax.annotation.Nullable
  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }


  public GetCreateNotificationRuleStatusResponseContent ruleId(String ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  /**
   * Get ruleId
   * @return ruleId
   */
  @javax.annotation.Nullable
  public String getRuleId() {
    return ruleId;
  }

  public void setRuleId(String ruleId) {
    this.ruleId = ruleId;
  }


  public GetCreateNotificationRuleStatusResponseContent eventCategory(EventCategory eventCategory) {
    this.eventCategory = eventCategory;
    return this;
  }

  /**
   * Get eventCategory
   * @return eventCategory
   */
  @javax.annotation.Nullable
  public EventCategory getEventCategory() {
    return eventCategory;
  }

  public void setEventCategory(EventCategory eventCategory) {
    this.eventCategory = eventCategory;
  }


  public GetCreateNotificationRuleStatusResponseContent condition(Condition condition) {
    this.condition = condition;
    return this;
  }

  /**
   * Get condition
   * @return condition
   */
  @javax.annotation.Nullable
  public Condition getCondition() {
    return condition;
  }

  public void setCondition(Condition condition) {
    this.condition = condition;
  }


  public GetCreateNotificationRuleStatusResponseContent destination(Destination destination) {
    this.destination = destination;
    return this;
  }

  /**
   * Get destination
   * @return destination
   */
  @javax.annotation.Nullable
  public Destination getDestination() {
    return destination;
  }

  public void setDestination(Destination destination) {
    this.destination = destination;
  }


  public GetCreateNotificationRuleStatusResponseContent errorDestination(Destination errorDestination) {
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


  public GetCreateNotificationRuleStatusResponseContent ruleDisabled(Boolean ruleDisabled) {
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


  public GetCreateNotificationRuleStatusResponseContent errorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * Get errorMessage
   * @return errorMessage
   */
  @javax.annotation.Nullable
  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }


  public GetCreateNotificationRuleStatusResponseContent status(Status status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @javax.annotation.Nullable
  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }


  public GetCreateNotificationRuleStatusResponseContent createTime(Double createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Get createTime
   * @return createTime
   */
  @javax.annotation.Nullable
  public Double getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Double createTime) {
    this.createTime = createTime;
  }


  public GetCreateNotificationRuleStatusResponseContent modifiedTime(Double modifiedTime) {
    this.modifiedTime = modifiedTime;
    return this;
  }

  /**
   * Get modifiedTime
   * @return modifiedTime
   */
  @javax.annotation.Nullable
  public Double getModifiedTime() {
    return modifiedTime;
  }

  public void setModifiedTime(Double modifiedTime) {
    this.modifiedTime = modifiedTime;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetCreateNotificationRuleStatusResponseContent getCreateNotificationRuleStatusResponseContent = (GetCreateNotificationRuleStatusResponseContent) o;
    return Objects.equals(this.jobId, getCreateNotificationRuleStatusResponseContent.jobId) &&
        Objects.equals(this.ruleId, getCreateNotificationRuleStatusResponseContent.ruleId) &&
        Objects.equals(this.eventCategory, getCreateNotificationRuleStatusResponseContent.eventCategory) &&
        Objects.equals(this.condition, getCreateNotificationRuleStatusResponseContent.condition) &&
        Objects.equals(this.destination, getCreateNotificationRuleStatusResponseContent.destination) &&
        Objects.equals(this.errorDestination, getCreateNotificationRuleStatusResponseContent.errorDestination) &&
        Objects.equals(this.ruleDisabled, getCreateNotificationRuleStatusResponseContent.ruleDisabled) &&
        Objects.equals(this.errorMessage, getCreateNotificationRuleStatusResponseContent.errorMessage) &&
        Objects.equals(this.status, getCreateNotificationRuleStatusResponseContent.status) &&
        Objects.equals(this.createTime, getCreateNotificationRuleStatusResponseContent.createTime) &&
        Objects.equals(this.modifiedTime, getCreateNotificationRuleStatusResponseContent.modifiedTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobId, ruleId, eventCategory, condition, destination, errorDestination, ruleDisabled, errorMessage, status, createTime, modifiedTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetCreateNotificationRuleStatusResponseContent {\n");
    sb.append("    jobId: ").append(toIndentedString(jobId)).append("\n");
    sb.append("    ruleId: ").append(toIndentedString(ruleId)).append("\n");
    sb.append("    eventCategory: ").append(toIndentedString(eventCategory)).append("\n");
    sb.append("    condition: ").append(toIndentedString(condition)).append("\n");
    sb.append("    destination: ").append(toIndentedString(destination)).append("\n");
    sb.append("    errorDestination: ").append(toIndentedString(errorDestination)).append("\n");
    sb.append("    ruleDisabled: ").append(toIndentedString(ruleDisabled)).append("\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    modifiedTime: ").append(toIndentedString(modifiedTime)).append("\n");
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
    openapiFields.add("jobId");
    openapiFields.add("ruleId");
    openapiFields.add("eventCategory");
    openapiFields.add("condition");
    openapiFields.add("destination");
    openapiFields.add("errorDestination");
    openapiFields.add("ruleDisabled");
    openapiFields.add("errorMessage");
    openapiFields.add("status");
    openapiFields.add("createTime");
    openapiFields.add("modifiedTime");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to GetCreateNotificationRuleStatusResponseContent
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!GetCreateNotificationRuleStatusResponseContent.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in GetCreateNotificationRuleStatusResponseContent is not found in the empty JSON string", GetCreateNotificationRuleStatusResponseContent.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!GetCreateNotificationRuleStatusResponseContent.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `GetCreateNotificationRuleStatusResponseContent` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if ((jsonObj.get("jobId") != null && !jsonObj.get("jobId").isJsonNull()) && !jsonObj.get("jobId").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `jobId` to be a primitive type in the JSON string but got `%s`", jsonObj.get("jobId").toString()));
      }
      if ((jsonObj.get("ruleId") != null && !jsonObj.get("ruleId").isJsonNull()) && !jsonObj.get("ruleId").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `ruleId` to be a primitive type in the JSON string but got `%s`", jsonObj.get("ruleId").toString()));
      }
      // validate the optional field `eventCategory`
      if (jsonObj.get("eventCategory") != null && !jsonObj.get("eventCategory").isJsonNull()) {
        EventCategory.validateJsonElement(jsonObj.get("eventCategory"));
      }
      // validate the optional field `condition`
      if (jsonObj.get("condition") != null && !jsonObj.get("condition").isJsonNull()) {
        Condition.validateJsonElement(jsonObj.get("condition"));
      }
      // validate the optional field `destination`
      if (jsonObj.get("destination") != null && !jsonObj.get("destination").isJsonNull()) {
        Destination.validateJsonElement(jsonObj.get("destination"));
      }
      // validate the optional field `errorDestination`
      if (jsonObj.get("errorDestination") != null && !jsonObj.get("errorDestination").isJsonNull()) {
        Destination.validateJsonElement(jsonObj.get("errorDestination"));
      }
      if ((jsonObj.get("errorMessage") != null && !jsonObj.get("errorMessage").isJsonNull()) && !jsonObj.get("errorMessage").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `errorMessage` to be a primitive type in the JSON string but got `%s`", jsonObj.get("errorMessage").toString()));
      }
      // validate the optional field `status`
      if (jsonObj.get("status") != null && !jsonObj.get("status").isJsonNull()) {
        Status.validateJsonElement(jsonObj.get("status"));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!GetCreateNotificationRuleStatusResponseContent.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'GetCreateNotificationRuleStatusResponseContent' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<GetCreateNotificationRuleStatusResponseContent> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(GetCreateNotificationRuleStatusResponseContent.class));

       return (TypeAdapter<T>) new TypeAdapter<GetCreateNotificationRuleStatusResponseContent>() {
           @Override
           public void write(JsonWriter out, GetCreateNotificationRuleStatusResponseContent value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public GetCreateNotificationRuleStatusResponseContent read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of GetCreateNotificationRuleStatusResponseContent given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of GetCreateNotificationRuleStatusResponseContent
   * @throws IOException if the JSON string is invalid with respect to GetCreateNotificationRuleStatusResponseContent
   */
  public static GetCreateNotificationRuleStatusResponseContent fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, GetCreateNotificationRuleStatusResponseContent.class);
  }

  /**
   * Convert an instance of GetCreateNotificationRuleStatusResponseContent to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

