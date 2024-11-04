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
import com.amazonaws.videoanalytics.devicemanagement.StorageState;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

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
 * StorageElement
 */
@lombok.Builder
@lombok.AllArgsConstructor
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-11-01T14:43:28.076578-05:00[America/Chicago]", comments = "Generator version: 7.8.0")
public class StorageElement {
  public static final String SERIALIZED_NAME_STATUS = "status";
  @SerializedName(SERIALIZED_NAME_STATUS)
  private StorageState status;

  public static final String SERIALIZED_NAME_TOTAL_CAPACITY = "totalCapacity";
  @SerializedName(SERIALIZED_NAME_TOTAL_CAPACITY)
  private String totalCapacity;

  public static final String SERIALIZED_NAME_USED_CAPACITY = "usedCapacity";
  @SerializedName(SERIALIZED_NAME_USED_CAPACITY)
  private String usedCapacity;

  public static final String SERIALIZED_NAME_UPDATED_AT = "updatedAt";
  @SerializedName(SERIALIZED_NAME_UPDATED_AT)
  private Date updatedAt;

  public static final String SERIALIZED_NAME_ID = "id";
  @SerializedName(SERIALIZED_NAME_ID)
  private String id;

  public StorageElement() {
  }

  public StorageElement status(StorageState status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @javax.annotation.Nullable
  public StorageState getStatus() {
    return status;
  }

  public void setStatus(StorageState status) {
    this.status = status;
  }


  public StorageElement totalCapacity(String totalCapacity) {
    this.totalCapacity = totalCapacity;
    return this;
  }

  /**
   * Get totalCapacity
   * @return totalCapacity
   */
  @javax.annotation.Nullable
  public String getTotalCapacity() {
    return totalCapacity;
  }

  public void setTotalCapacity(String totalCapacity) {
    this.totalCapacity = totalCapacity;
  }


  public StorageElement usedCapacity(String usedCapacity) {
    this.usedCapacity = usedCapacity;
    return this;
  }

  /**
   * Get usedCapacity
   * @return usedCapacity
   */
  @javax.annotation.Nullable
  public String getUsedCapacity() {
    return usedCapacity;
  }

  public void setUsedCapacity(String usedCapacity) {
    this.usedCapacity = usedCapacity;
  }


  public StorageElement updatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @javax.annotation.Nullable
  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }


  public StorageElement id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @javax.annotation.Nullable
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StorageElement storageElement = (StorageElement) o;
    return Objects.equals(this.status, storageElement.status) &&
        Objects.equals(this.totalCapacity, storageElement.totalCapacity) &&
        Objects.equals(this.usedCapacity, storageElement.usedCapacity) &&
        Objects.equals(this.updatedAt, storageElement.updatedAt) &&
        Objects.equals(this.id, storageElement.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, totalCapacity, usedCapacity, updatedAt, id);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StorageElement {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    totalCapacity: ").append(toIndentedString(totalCapacity)).append("\n");
    sb.append("    usedCapacity: ").append(toIndentedString(usedCapacity)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
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
    openapiFields.add("status");
    openapiFields.add("totalCapacity");
    openapiFields.add("usedCapacity");
    openapiFields.add("updatedAt");
    openapiFields.add("id");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to StorageElement
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!StorageElement.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in StorageElement is not found in the empty JSON string", StorageElement.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!StorageElement.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `StorageElement` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      // validate the optional field `status`
      if (jsonObj.get("status") != null && !jsonObj.get("status").isJsonNull()) {
        StorageState.validateJsonElement(jsonObj.get("status"));
      }
      if ((jsonObj.get("totalCapacity") != null && !jsonObj.get("totalCapacity").isJsonNull()) && !jsonObj.get("totalCapacity").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `totalCapacity` to be a primitive type in the JSON string but got `%s`", jsonObj.get("totalCapacity").toString()));
      }
      if ((jsonObj.get("usedCapacity") != null && !jsonObj.get("usedCapacity").isJsonNull()) && !jsonObj.get("usedCapacity").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `usedCapacity` to be a primitive type in the JSON string but got `%s`", jsonObj.get("usedCapacity").toString()));
      }
      if ((jsonObj.get("id") != null && !jsonObj.get("id").isJsonNull()) && !jsonObj.get("id").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `id` to be a primitive type in the JSON string but got `%s`", jsonObj.get("id").toString()));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!StorageElement.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'StorageElement' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<StorageElement> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(StorageElement.class));

       return (TypeAdapter<T>) new TypeAdapter<StorageElement>() {
           @Override
           public void write(JsonWriter out, StorageElement value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public StorageElement read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of StorageElement given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of StorageElement
   * @throws IOException if the JSON string is invalid with respect to StorageElement
   */
  public static StorageElement fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, StorageElement.class);
  }

  /**
   * Convert an instance of StorageElement to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

