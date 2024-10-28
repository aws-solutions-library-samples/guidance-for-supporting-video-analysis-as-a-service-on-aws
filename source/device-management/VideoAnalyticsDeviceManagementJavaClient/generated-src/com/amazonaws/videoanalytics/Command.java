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
 * Gets or Sets Command
 */
@JsonAdapter(Command.Adapter.class)
public enum Command {
  
  REBOOT("REBOOT"),
  
  FACTORY_RESET("FACTORY_RESET"),
  
  SD_CARD_FORMAT("SD_CARD_FORMAT"),
  
  UPDATE("UPDATE");

  private String value;

  Command(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static Command fromValue(String value) {
    for (Command b : Command.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  public static class Adapter extends TypeAdapter<Command> {
    @Override
    public void write(final JsonWriter jsonWriter, final Command enumeration) throws IOException {
      jsonWriter.value(enumeration.getValue());
    }

    @Override
    public Command read(final JsonReader jsonReader) throws IOException {
      String value = jsonReader.nextString();
      return Command.fromValue(value);
    }
  }

  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
    String value = jsonElement.getAsString();
    Command.fromValue(value);
  }
}

