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
 * IpAddress
 */
@lombok.Builder
@lombok.AllArgsConstructor
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.8.0")
public class IpAddress {
  public static final String SERIALIZED_NAME_PUBLIC_IP_ADDRESS = "publicIpAddress";
  @SerializedName(SERIALIZED_NAME_PUBLIC_IP_ADDRESS)
  private String publicIpAddress;

  public static final String SERIALIZED_NAME_PRIVATE_IP_ADDRESS = "privateIpAddress";
  @SerializedName(SERIALIZED_NAME_PRIVATE_IP_ADDRESS)
  private String privateIpAddress;

  public IpAddress() {
  }

  public IpAddress publicIpAddress(String publicIpAddress) {
    this.publicIpAddress = publicIpAddress;
    return this;
  }

  /**
   * Get publicIpAddress
   * @return publicIpAddress
   */
  @javax.annotation.Nullable
  public String getPublicIpAddress() {
    return publicIpAddress;
  }

  public void setPublicIpAddress(String publicIpAddress) {
    this.publicIpAddress = publicIpAddress;
  }


  public IpAddress privateIpAddress(String privateIpAddress) {
    this.privateIpAddress = privateIpAddress;
    return this;
  }

  /**
   * Get privateIpAddress
   * @return privateIpAddress
   */
  @javax.annotation.Nullable
  public String getPrivateIpAddress() {
    return privateIpAddress;
  }

  public void setPrivateIpAddress(String privateIpAddress) {
    this.privateIpAddress = privateIpAddress;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IpAddress ipAddress = (IpAddress) o;
    return Objects.equals(this.publicIpAddress, ipAddress.publicIpAddress) &&
        Objects.equals(this.privateIpAddress, ipAddress.privateIpAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(publicIpAddress, privateIpAddress);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IpAddress {\n");
    sb.append("    publicIpAddress: ").append(toIndentedString(publicIpAddress)).append("\n");
    sb.append("    privateIpAddress: ").append(toIndentedString(privateIpAddress)).append("\n");
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
    openapiFields.add("publicIpAddress");
    openapiFields.add("privateIpAddress");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to IpAddress
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!IpAddress.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in IpAddress is not found in the empty JSON string", IpAddress.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!IpAddress.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `IpAddress` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if ((jsonObj.get("publicIpAddress") != null && !jsonObj.get("publicIpAddress").isJsonNull()) && !jsonObj.get("publicIpAddress").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `publicIpAddress` to be a primitive type in the JSON string but got `%s`", jsonObj.get("publicIpAddress").toString()));
      }
      if ((jsonObj.get("privateIpAddress") != null && !jsonObj.get("privateIpAddress").isJsonNull()) && !jsonObj.get("privateIpAddress").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `privateIpAddress` to be a primitive type in the JSON string but got `%s`", jsonObj.get("privateIpAddress").toString()));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!IpAddress.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'IpAddress' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<IpAddress> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(IpAddress.class));

       return (TypeAdapter<T>) new TypeAdapter<IpAddress>() {
           @Override
           public void write(JsonWriter out, IpAddress value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public IpAddress read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of IpAddress given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of IpAddress
   * @throws IOException if the JSON string is invalid with respect to IpAddress
   */
  public static IpAddress fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, IpAddress.class);
  }

  /**
   * Convert an instance of IpAddress to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

