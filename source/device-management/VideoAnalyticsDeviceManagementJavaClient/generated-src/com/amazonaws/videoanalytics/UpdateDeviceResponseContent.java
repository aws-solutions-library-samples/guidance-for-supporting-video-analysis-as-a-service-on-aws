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
import com.amazonaws.videoanalytics.DeviceMetaData;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
 * UpdateDeviceResponseContent
 */
@lombok.Builder
@lombok.AllArgsConstructor
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-28T21:16:17.605797Z[UTC]", comments = "Generator version: 7.8.0")
public class UpdateDeviceResponseContent {
  public static final String SERIALIZED_NAME_DEVICE_NAME = "deviceName";
  @SerializedName(SERIALIZED_NAME_DEVICE_NAME)
  private String deviceName;

  public static final String SERIALIZED_NAME_DEVICE_ID = "deviceId";
  @SerializedName(SERIALIZED_NAME_DEVICE_ID)
  private String deviceId;

  public static final String SERIALIZED_NAME_DEVICE_GROUP_ID = "deviceGroupId";
  @SerializedName(SERIALIZED_NAME_DEVICE_GROUP_ID)
  private String deviceGroupId;

  public static final String SERIALIZED_NAME_DEVICE_TYPE = "deviceType";
  @SerializedName(SERIALIZED_NAME_DEVICE_TYPE)
  private String deviceType;

  public static final String SERIALIZED_NAME_DEVICE_META_DATA = "deviceMetaData";
  @SerializedName(SERIALIZED_NAME_DEVICE_META_DATA)
  private DeviceMetaData deviceMetaData;

  public static final String SERIALIZED_NAME_DEVICE_CAPABILITIES = "deviceCapabilities";
  @SerializedName(SERIALIZED_NAME_DEVICE_CAPABILITIES)
  private Map<String, String> deviceCapabilities = new HashMap<>();

  public static final String SERIALIZED_NAME_DEVICE_SETTINGS = "deviceSettings";
  @SerializedName(SERIALIZED_NAME_DEVICE_SETTINGS)
  private Map<String, String> deviceSettings = new HashMap<>();

  public static final String SERIALIZED_NAME_CREATED_AT = "createdAt";
  @SerializedName(SERIALIZED_NAME_CREATED_AT)
  private Double createdAt;

  public UpdateDeviceResponseContent() {
  }

  public UpdateDeviceResponseContent deviceName(String deviceName) {
    this.deviceName = deviceName;
    return this;
  }

  /**
   * Get deviceName
   * @return deviceName
   */
  @javax.annotation.Nullable
  public String getDeviceName() {
    return deviceName;
  }

  public void setDeviceName(String deviceName) {
    this.deviceName = deviceName;
  }


  public UpdateDeviceResponseContent deviceId(String deviceId) {
    this.deviceId = deviceId;
    return this;
  }

  /**
   * Get deviceId
   * @return deviceId
   */
  @javax.annotation.Nullable
  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }


  public UpdateDeviceResponseContent deviceGroupId(String deviceGroupId) {
    this.deviceGroupId = deviceGroupId;
    return this;
  }

  /**
   * Get deviceGroupId
   * @return deviceGroupId
   */
  @javax.annotation.Nullable
  public String getDeviceGroupId() {
    return deviceGroupId;
  }

  public void setDeviceGroupId(String deviceGroupId) {
    this.deviceGroupId = deviceGroupId;
  }


  public UpdateDeviceResponseContent deviceType(String deviceType) {
    this.deviceType = deviceType;
    return this;
  }

  /**
   * Get deviceType
   * @return deviceType
   */
  @javax.annotation.Nullable
  public String getDeviceType() {
    return deviceType;
  }

  public void setDeviceType(String deviceType) {
    this.deviceType = deviceType;
  }


  public UpdateDeviceResponseContent deviceMetaData(DeviceMetaData deviceMetaData) {
    this.deviceMetaData = deviceMetaData;
    return this;
  }

  /**
   * Get deviceMetaData
   * @return deviceMetaData
   */
  @javax.annotation.Nullable
  public DeviceMetaData getDeviceMetaData() {
    return deviceMetaData;
  }

  public void setDeviceMetaData(DeviceMetaData deviceMetaData) {
    this.deviceMetaData = deviceMetaData;
  }


  public UpdateDeviceResponseContent deviceCapabilities(Map<String, String> deviceCapabilities) {
    this.deviceCapabilities = deviceCapabilities;
    return this;
  }

  public UpdateDeviceResponseContent putDeviceCapabilitiesItem(String key, String deviceCapabilitiesItem) {
    if (this.deviceCapabilities == null) {
      this.deviceCapabilities = new HashMap<>();
    }
    this.deviceCapabilities.put(key, deviceCapabilitiesItem);
    return this;
  }

  /**
   * Get deviceCapabilities
   * @return deviceCapabilities
   */
  @javax.annotation.Nullable
  public Map<String, String> getDeviceCapabilities() {
    return deviceCapabilities;
  }

  public void setDeviceCapabilities(Map<String, String> deviceCapabilities) {
    this.deviceCapabilities = deviceCapabilities;
  }


  public UpdateDeviceResponseContent deviceSettings(Map<String, String> deviceSettings) {
    this.deviceSettings = deviceSettings;
    return this;
  }

  public UpdateDeviceResponseContent putDeviceSettingsItem(String key, String deviceSettingsItem) {
    if (this.deviceSettings == null) {
      this.deviceSettings = new HashMap<>();
    }
    this.deviceSettings.put(key, deviceSettingsItem);
    return this;
  }

  /**
   * Get deviceSettings
   * @return deviceSettings
   */
  @javax.annotation.Nullable
  public Map<String, String> getDeviceSettings() {
    return deviceSettings;
  }

  public void setDeviceSettings(Map<String, String> deviceSettings) {
    this.deviceSettings = deviceSettings;
  }


  public UpdateDeviceResponseContent createdAt(Double createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @javax.annotation.Nullable
  public Double getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Double createdAt) {
    this.createdAt = createdAt;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateDeviceResponseContent updateDeviceResponseContent = (UpdateDeviceResponseContent) o;
    return Objects.equals(this.deviceName, updateDeviceResponseContent.deviceName) &&
        Objects.equals(this.deviceId, updateDeviceResponseContent.deviceId) &&
        Objects.equals(this.deviceGroupId, updateDeviceResponseContent.deviceGroupId) &&
        Objects.equals(this.deviceType, updateDeviceResponseContent.deviceType) &&
        Objects.equals(this.deviceMetaData, updateDeviceResponseContent.deviceMetaData) &&
        Objects.equals(this.deviceCapabilities, updateDeviceResponseContent.deviceCapabilities) &&
        Objects.equals(this.deviceSettings, updateDeviceResponseContent.deviceSettings) &&
        Objects.equals(this.createdAt, updateDeviceResponseContent.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(deviceName, deviceId, deviceGroupId, deviceType, deviceMetaData, deviceCapabilities, deviceSettings, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateDeviceResponseContent {\n");
    sb.append("    deviceName: ").append(toIndentedString(deviceName)).append("\n");
    sb.append("    deviceId: ").append(toIndentedString(deviceId)).append("\n");
    sb.append("    deviceGroupId: ").append(toIndentedString(deviceGroupId)).append("\n");
    sb.append("    deviceType: ").append(toIndentedString(deviceType)).append("\n");
    sb.append("    deviceMetaData: ").append(toIndentedString(deviceMetaData)).append("\n");
    sb.append("    deviceCapabilities: ").append(toIndentedString(deviceCapabilities)).append("\n");
    sb.append("    deviceSettings: ").append(toIndentedString(deviceSettings)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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
    openapiFields.add("deviceName");
    openapiFields.add("deviceId");
    openapiFields.add("deviceGroupId");
    openapiFields.add("deviceType");
    openapiFields.add("deviceMetaData");
    openapiFields.add("deviceCapabilities");
    openapiFields.add("deviceSettings");
    openapiFields.add("createdAt");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to UpdateDeviceResponseContent
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!UpdateDeviceResponseContent.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in UpdateDeviceResponseContent is not found in the empty JSON string", UpdateDeviceResponseContent.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!UpdateDeviceResponseContent.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `UpdateDeviceResponseContent` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if ((jsonObj.get("deviceName") != null && !jsonObj.get("deviceName").isJsonNull()) && !jsonObj.get("deviceName").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `deviceName` to be a primitive type in the JSON string but got `%s`", jsonObj.get("deviceName").toString()));
      }
      if ((jsonObj.get("deviceId") != null && !jsonObj.get("deviceId").isJsonNull()) && !jsonObj.get("deviceId").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `deviceId` to be a primitive type in the JSON string but got `%s`", jsonObj.get("deviceId").toString()));
      }
      if ((jsonObj.get("deviceGroupId") != null && !jsonObj.get("deviceGroupId").isJsonNull()) && !jsonObj.get("deviceGroupId").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `deviceGroupId` to be a primitive type in the JSON string but got `%s`", jsonObj.get("deviceGroupId").toString()));
      }
      if ((jsonObj.get("deviceType") != null && !jsonObj.get("deviceType").isJsonNull()) && !jsonObj.get("deviceType").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `deviceType` to be a primitive type in the JSON string but got `%s`", jsonObj.get("deviceType").toString()));
      }
      // validate the optional field `deviceMetaData`
      if (jsonObj.get("deviceMetaData") != null && !jsonObj.get("deviceMetaData").isJsonNull()) {
        DeviceMetaData.validateJsonElement(jsonObj.get("deviceMetaData"));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!UpdateDeviceResponseContent.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'UpdateDeviceResponseContent' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<UpdateDeviceResponseContent> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(UpdateDeviceResponseContent.class));

       return (TypeAdapter<T>) new TypeAdapter<UpdateDeviceResponseContent>() {
           @Override
           public void write(JsonWriter out, UpdateDeviceResponseContent value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public UpdateDeviceResponseContent read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of UpdateDeviceResponseContent given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of UpdateDeviceResponseContent
   * @throws IOException if the JSON string is invalid with respect to UpdateDeviceResponseContent
   */
  public static UpdateDeviceResponseContent fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, UpdateDeviceResponseContent.class);
  }

  /**
   * Convert an instance of UpdateDeviceResponseContent to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

