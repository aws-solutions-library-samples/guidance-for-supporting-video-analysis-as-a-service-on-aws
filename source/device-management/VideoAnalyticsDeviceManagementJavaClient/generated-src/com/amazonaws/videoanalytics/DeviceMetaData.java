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
import com.amazonaws.videoanalytics.DeviceStatus;
import com.amazonaws.videoanalytics.IpAddress;
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
 * DeviceMetaData
 */
@lombok.Builder
@lombok.AllArgsConstructor
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-29T21:30:28.953926Z[UTC]", comments = "Generator version: 7.8.0")
public class DeviceMetaData {
  public static final String SERIALIZED_NAME_MANUFACTURER = "manufacturer";
  @SerializedName(SERIALIZED_NAME_MANUFACTURER)
  private String manufacturer;

  public static final String SERIALIZED_NAME_MODEL = "model";
  @SerializedName(SERIALIZED_NAME_MODEL)
  private String model;

  public static final String SERIALIZED_NAME_MAC = "mac";
  @SerializedName(SERIALIZED_NAME_MAC)
  private String mac;

  public static final String SERIALIZED_NAME_IP_ADDRESS = "ipAddress";
  @SerializedName(SERIALIZED_NAME_IP_ADDRESS)
  private IpAddress ipAddress;

  public static final String SERIALIZED_NAME_AI_CHIPSET = "aiChipset";
  @SerializedName(SERIALIZED_NAME_AI_CHIPSET)
  private String aiChipset;

  public static final String SERIALIZED_NAME_FIRMWARE_VERSION = "firmwareVersion";
  @SerializedName(SERIALIZED_NAME_FIRMWARE_VERSION)
  private String firmwareVersion;

  public static final String SERIALIZED_NAME_SDK_VERSION = "sdkVersion";
  @SerializedName(SERIALIZED_NAME_SDK_VERSION)
  private String sdkVersion;

  public static final String SERIALIZED_NAME_AI_MODEL_VERSION = "aiModelVersion";
  @SerializedName(SERIALIZED_NAME_AI_MODEL_VERSION)
  private String aiModelVersion;

  public static final String SERIALIZED_NAME_AI_SDK_VERSION = "aiSdkVersion";
  @SerializedName(SERIALIZED_NAME_AI_SDK_VERSION)
  private String aiSdkVersion;

  public static final String SERIALIZED_NAME_DEVICE_STATUS = "deviceStatus";
  @SerializedName(SERIALIZED_NAME_DEVICE_STATUS)
  private DeviceStatus deviceStatus;

  public DeviceMetaData() {
  }

  public DeviceMetaData manufacturer(String manufacturer) {
    this.manufacturer = manufacturer;
    return this;
  }

  /**
   * Get manufacturer
   * @return manufacturer
   */
  @javax.annotation.Nullable
  public String getManufacturer() {
    return manufacturer;
  }

  public void setManufacturer(String manufacturer) {
    this.manufacturer = manufacturer;
  }


  public DeviceMetaData model(String model) {
    this.model = model;
    return this;
  }

  /**
   * Get model
   * @return model
   */
  @javax.annotation.Nullable
  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }


  public DeviceMetaData mac(String mac) {
    this.mac = mac;
    return this;
  }

  /**
   * Get mac
   * @return mac
   */
  @javax.annotation.Nullable
  public String getMac() {
    return mac;
  }

  public void setMac(String mac) {
    this.mac = mac;
  }


  public DeviceMetaData ipAddress(IpAddress ipAddress) {
    this.ipAddress = ipAddress;
    return this;
  }

  /**
   * Get ipAddress
   * @return ipAddress
   */
  @javax.annotation.Nullable
  public IpAddress getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(IpAddress ipAddress) {
    this.ipAddress = ipAddress;
  }


  public DeviceMetaData aiChipset(String aiChipset) {
    this.aiChipset = aiChipset;
    return this;
  }

  /**
   * Get aiChipset
   * @return aiChipset
   */
  @javax.annotation.Nullable
  public String getAiChipset() {
    return aiChipset;
  }

  public void setAiChipset(String aiChipset) {
    this.aiChipset = aiChipset;
  }


  public DeviceMetaData firmwareVersion(String firmwareVersion) {
    this.firmwareVersion = firmwareVersion;
    return this;
  }

  /**
   * Get firmwareVersion
   * @return firmwareVersion
   */
  @javax.annotation.Nullable
  public String getFirmwareVersion() {
    return firmwareVersion;
  }

  public void setFirmwareVersion(String firmwareVersion) {
    this.firmwareVersion = firmwareVersion;
  }


  public DeviceMetaData sdkVersion(String sdkVersion) {
    this.sdkVersion = sdkVersion;
    return this;
  }

  /**
   * Get sdkVersion
   * @return sdkVersion
   */
  @javax.annotation.Nullable
  public String getSdkVersion() {
    return sdkVersion;
  }

  public void setSdkVersion(String sdkVersion) {
    this.sdkVersion = sdkVersion;
  }


  public DeviceMetaData aiModelVersion(String aiModelVersion) {
    this.aiModelVersion = aiModelVersion;
    return this;
  }

  /**
   * Get aiModelVersion
   * @return aiModelVersion
   */
  @javax.annotation.Nullable
  public String getAiModelVersion() {
    return aiModelVersion;
  }

  public void setAiModelVersion(String aiModelVersion) {
    this.aiModelVersion = aiModelVersion;
  }


  public DeviceMetaData aiSdkVersion(String aiSdkVersion) {
    this.aiSdkVersion = aiSdkVersion;
    return this;
  }

  /**
   * Get aiSdkVersion
   * @return aiSdkVersion
   */
  @javax.annotation.Nullable
  public String getAiSdkVersion() {
    return aiSdkVersion;
  }

  public void setAiSdkVersion(String aiSdkVersion) {
    this.aiSdkVersion = aiSdkVersion;
  }


  public DeviceMetaData deviceStatus(DeviceStatus deviceStatus) {
    this.deviceStatus = deviceStatus;
    return this;
  }

  /**
   * Get deviceStatus
   * @return deviceStatus
   */
  @javax.annotation.Nullable
  public DeviceStatus getDeviceStatus() {
    return deviceStatus;
  }

  public void setDeviceStatus(DeviceStatus deviceStatus) {
    this.deviceStatus = deviceStatus;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeviceMetaData deviceMetaData = (DeviceMetaData) o;
    return Objects.equals(this.manufacturer, deviceMetaData.manufacturer) &&
        Objects.equals(this.model, deviceMetaData.model) &&
        Objects.equals(this.mac, deviceMetaData.mac) &&
        Objects.equals(this.ipAddress, deviceMetaData.ipAddress) &&
        Objects.equals(this.aiChipset, deviceMetaData.aiChipset) &&
        Objects.equals(this.firmwareVersion, deviceMetaData.firmwareVersion) &&
        Objects.equals(this.sdkVersion, deviceMetaData.sdkVersion) &&
        Objects.equals(this.aiModelVersion, deviceMetaData.aiModelVersion) &&
        Objects.equals(this.aiSdkVersion, deviceMetaData.aiSdkVersion) &&
        Objects.equals(this.deviceStatus, deviceMetaData.deviceStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(manufacturer, model, mac, ipAddress, aiChipset, firmwareVersion, sdkVersion, aiModelVersion, aiSdkVersion, deviceStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeviceMetaData {\n");
    sb.append("    manufacturer: ").append(toIndentedString(manufacturer)).append("\n");
    sb.append("    model: ").append(toIndentedString(model)).append("\n");
    sb.append("    mac: ").append(toIndentedString(mac)).append("\n");
    sb.append("    ipAddress: ").append(toIndentedString(ipAddress)).append("\n");
    sb.append("    aiChipset: ").append(toIndentedString(aiChipset)).append("\n");
    sb.append("    firmwareVersion: ").append(toIndentedString(firmwareVersion)).append("\n");
    sb.append("    sdkVersion: ").append(toIndentedString(sdkVersion)).append("\n");
    sb.append("    aiModelVersion: ").append(toIndentedString(aiModelVersion)).append("\n");
    sb.append("    aiSdkVersion: ").append(toIndentedString(aiSdkVersion)).append("\n");
    sb.append("    deviceStatus: ").append(toIndentedString(deviceStatus)).append("\n");
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
    openapiFields.add("manufacturer");
    openapiFields.add("model");
    openapiFields.add("mac");
    openapiFields.add("ipAddress");
    openapiFields.add("aiChipset");
    openapiFields.add("firmwareVersion");
    openapiFields.add("sdkVersion");
    openapiFields.add("aiModelVersion");
    openapiFields.add("aiSdkVersion");
    openapiFields.add("deviceStatus");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to DeviceMetaData
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!DeviceMetaData.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in DeviceMetaData is not found in the empty JSON string", DeviceMetaData.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!DeviceMetaData.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `DeviceMetaData` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if ((jsonObj.get("manufacturer") != null && !jsonObj.get("manufacturer").isJsonNull()) && !jsonObj.get("manufacturer").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `manufacturer` to be a primitive type in the JSON string but got `%s`", jsonObj.get("manufacturer").toString()));
      }
      if ((jsonObj.get("model") != null && !jsonObj.get("model").isJsonNull()) && !jsonObj.get("model").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `model` to be a primitive type in the JSON string but got `%s`", jsonObj.get("model").toString()));
      }
      if ((jsonObj.get("mac") != null && !jsonObj.get("mac").isJsonNull()) && !jsonObj.get("mac").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `mac` to be a primitive type in the JSON string but got `%s`", jsonObj.get("mac").toString()));
      }
      // validate the optional field `ipAddress`
      if (jsonObj.get("ipAddress") != null && !jsonObj.get("ipAddress").isJsonNull()) {
        IpAddress.validateJsonElement(jsonObj.get("ipAddress"));
      }
      if ((jsonObj.get("aiChipset") != null && !jsonObj.get("aiChipset").isJsonNull()) && !jsonObj.get("aiChipset").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `aiChipset` to be a primitive type in the JSON string but got `%s`", jsonObj.get("aiChipset").toString()));
      }
      if ((jsonObj.get("firmwareVersion") != null && !jsonObj.get("firmwareVersion").isJsonNull()) && !jsonObj.get("firmwareVersion").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `firmwareVersion` to be a primitive type in the JSON string but got `%s`", jsonObj.get("firmwareVersion").toString()));
      }
      if ((jsonObj.get("sdkVersion") != null && !jsonObj.get("sdkVersion").isJsonNull()) && !jsonObj.get("sdkVersion").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `sdkVersion` to be a primitive type in the JSON string but got `%s`", jsonObj.get("sdkVersion").toString()));
      }
      if ((jsonObj.get("aiModelVersion") != null && !jsonObj.get("aiModelVersion").isJsonNull()) && !jsonObj.get("aiModelVersion").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `aiModelVersion` to be a primitive type in the JSON string but got `%s`", jsonObj.get("aiModelVersion").toString()));
      }
      if ((jsonObj.get("aiSdkVersion") != null && !jsonObj.get("aiSdkVersion").isJsonNull()) && !jsonObj.get("aiSdkVersion").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `aiSdkVersion` to be a primitive type in the JSON string but got `%s`", jsonObj.get("aiSdkVersion").toString()));
      }
      // validate the optional field `deviceStatus`
      if (jsonObj.get("deviceStatus") != null && !jsonObj.get("deviceStatus").isJsonNull()) {
        DeviceStatus.validateJsonElement(jsonObj.get("deviceStatus"));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!DeviceMetaData.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'DeviceMetaData' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<DeviceMetaData> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(DeviceMetaData.class));

       return (TypeAdapter<T>) new TypeAdapter<DeviceMetaData>() {
           @Override
           public void write(JsonWriter out, DeviceMetaData value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public DeviceMetaData read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of DeviceMetaData given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of DeviceMetaData
   * @throws IOException if the JSON string is invalid with respect to DeviceMetaData
   */
  public static DeviceMetaData fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, DeviceMetaData.class);
  }

  /**
   * Convert an instance of DeviceMetaData to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

