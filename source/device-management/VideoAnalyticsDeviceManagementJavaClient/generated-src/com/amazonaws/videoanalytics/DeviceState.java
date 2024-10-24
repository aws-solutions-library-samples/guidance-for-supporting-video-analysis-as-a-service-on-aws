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
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import com.google.gson.TypeAdapter;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Gets or Sets DeviceState
 */
@JsonAdapter(DeviceState.Adapter.class)
public enum DeviceState {
  
  ENABLED("ENABLED"),
  
  DISABLED("DISABLED"),
  
  CREATED("CREATED");

  private String value;

  DeviceState(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static DeviceState fromValue(String value) {
    for (DeviceState b : DeviceState.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  public static class Adapter extends TypeAdapter<DeviceState> {
    @Override
    public void write(final JsonWriter jsonWriter, final DeviceState enumeration) throws IOException {
      jsonWriter.value(enumeration.getValue());
    }

    @Override
    public DeviceState read(final JsonReader jsonReader) throws IOException {
      String value = jsonReader.nextString();
      return DeviceState.fromValue(value);
    }
  }

  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
    String value = jsonElement.getAsString();
    DeviceState.fromValue(value);
  }
}

