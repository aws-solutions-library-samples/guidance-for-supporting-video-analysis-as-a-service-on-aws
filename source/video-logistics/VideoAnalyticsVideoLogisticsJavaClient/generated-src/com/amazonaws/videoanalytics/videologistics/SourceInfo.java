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
import com.amazonaws.videoanalytics.videologistics.PeerConnectionState;
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
 * SourceInfo
 */
@lombok.Builder
@lombok.AllArgsConstructor
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.8.0")
public class SourceInfo {
  public static final String SERIALIZED_NAME_H_L_S_STREAMING_U_R_L = "hLSStreamingURL";
  @SerializedName(SERIALIZED_NAME_H_L_S_STREAMING_U_R_L)
  private String hLSStreamingURL;

  public static final String SERIALIZED_NAME_EXPIRATION_TIME = "expirationTime";
  @SerializedName(SERIALIZED_NAME_EXPIRATION_TIME)
  private Double expirationTime;

  public static final String SERIALIZED_NAME_PEER_CONNECTION_STATE = "peerConnectionState";
  @SerializedName(SERIALIZED_NAME_PEER_CONNECTION_STATE)
  private PeerConnectionState peerConnectionState;

  public static final String SERIALIZED_NAME_SIGNALING_CHANNEL_U_R_L = "SignalingChannelURL";
  @SerializedName(SERIALIZED_NAME_SIGNALING_CHANNEL_U_R_L)
  private String signalingChannelURL;

  public static final String SERIALIZED_NAME_CLIENT_ID = "clientId";
  @SerializedName(SERIALIZED_NAME_CLIENT_ID)
  private String clientId;

  public SourceInfo() {
  }

  public SourceInfo hLSStreamingURL(String hLSStreamingURL) {
    this.hLSStreamingURL = hLSStreamingURL;
    return this;
  }

  /**
   * Get hLSStreamingURL
   * @return hLSStreamingURL
   */
  @javax.annotation.Nullable
  public String gethLSStreamingURL() {
    return hLSStreamingURL;
  }

  public void sethLSStreamingURL(String hLSStreamingURL) {
    this.hLSStreamingURL = hLSStreamingURL;
  }


  public SourceInfo expirationTime(Double expirationTime) {
    this.expirationTime = expirationTime;
    return this;
  }

  /**
   * Get expirationTime
   * @return expirationTime
   */
  @javax.annotation.Nullable
  public Double getExpirationTime() {
    return expirationTime;
  }

  public void setExpirationTime(Double expirationTime) {
    this.expirationTime = expirationTime;
  }


  public SourceInfo peerConnectionState(PeerConnectionState peerConnectionState) {
    this.peerConnectionState = peerConnectionState;
    return this;
  }

  /**
   * Get peerConnectionState
   * @return peerConnectionState
   */
  @javax.annotation.Nullable
  public PeerConnectionState getPeerConnectionState() {
    return peerConnectionState;
  }

  public void setPeerConnectionState(PeerConnectionState peerConnectionState) {
    this.peerConnectionState = peerConnectionState;
  }


  public SourceInfo signalingChannelURL(String signalingChannelURL) {
    this.signalingChannelURL = signalingChannelURL;
    return this;
  }

  /**
   * Get signalingChannelURL
   * @return signalingChannelURL
   */
  @javax.annotation.Nullable
  public String getSignalingChannelURL() {
    return signalingChannelURL;
  }

  public void setSignalingChannelURL(String signalingChannelURL) {
    this.signalingChannelURL = signalingChannelURL;
  }


  public SourceInfo clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  /**
   * Get clientId
   * @return clientId
   */
  @javax.annotation.Nullable
  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SourceInfo sourceInfo = (SourceInfo) o;
    return Objects.equals(this.hLSStreamingURL, sourceInfo.hLSStreamingURL) &&
        Objects.equals(this.expirationTime, sourceInfo.expirationTime) &&
        Objects.equals(this.peerConnectionState, sourceInfo.peerConnectionState) &&
        Objects.equals(this.signalingChannelURL, sourceInfo.signalingChannelURL) &&
        Objects.equals(this.clientId, sourceInfo.clientId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hLSStreamingURL, expirationTime, peerConnectionState, signalingChannelURL, clientId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SourceInfo {\n");
    sb.append("    hLSStreamingURL: ").append(toIndentedString(hLSStreamingURL)).append("\n");
    sb.append("    expirationTime: ").append(toIndentedString(expirationTime)).append("\n");
    sb.append("    peerConnectionState: ").append(toIndentedString(peerConnectionState)).append("\n");
    sb.append("    signalingChannelURL: ").append(toIndentedString(signalingChannelURL)).append("\n");
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
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
    openapiFields.add("hLSStreamingURL");
    openapiFields.add("expirationTime");
    openapiFields.add("peerConnectionState");
    openapiFields.add("SignalingChannelURL");
    openapiFields.add("clientId");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to SourceInfo
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!SourceInfo.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in SourceInfo is not found in the empty JSON string", SourceInfo.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!SourceInfo.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `SourceInfo` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if ((jsonObj.get("hLSStreamingURL") != null && !jsonObj.get("hLSStreamingURL").isJsonNull()) && !jsonObj.get("hLSStreamingURL").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `hLSStreamingURL` to be a primitive type in the JSON string but got `%s`", jsonObj.get("hLSStreamingURL").toString()));
      }
      // validate the optional field `peerConnectionState`
      if (jsonObj.get("peerConnectionState") != null && !jsonObj.get("peerConnectionState").isJsonNull()) {
        PeerConnectionState.validateJsonElement(jsonObj.get("peerConnectionState"));
      }
      if ((jsonObj.get("SignalingChannelURL") != null && !jsonObj.get("SignalingChannelURL").isJsonNull()) && !jsonObj.get("SignalingChannelURL").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `SignalingChannelURL` to be a primitive type in the JSON string but got `%s`", jsonObj.get("SignalingChannelURL").toString()));
      }
      if ((jsonObj.get("clientId") != null && !jsonObj.get("clientId").isJsonNull()) && !jsonObj.get("clientId").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `clientId` to be a primitive type in the JSON string but got `%s`", jsonObj.get("clientId").toString()));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!SourceInfo.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'SourceInfo' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<SourceInfo> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(SourceInfo.class));

       return (TypeAdapter<T>) new TypeAdapter<SourceInfo>() {
           @Override
           public void write(JsonWriter out, SourceInfo value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public SourceInfo read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of SourceInfo given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of SourceInfo
   * @throws IOException if the JSON string is invalid with respect to SourceInfo
   */
  public static SourceInfo fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, SourceInfo.class);
  }

  /**
   * Convert an instance of SourceInfo to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

