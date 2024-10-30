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
import com.amazonaws.videoanalytics.Command;
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

import com.amazonaws.JSON;

/**
 * CommandPayload
 */
@lombok.Builder
@lombok.AllArgsConstructor
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-29T21:30:28.953926Z[UTC]", comments = "Generator version: 7.8.0")
public class CommandPayload {
  public static final String SERIALIZED_NAME_COMMAND = "command";
  @SerializedName(SERIALIZED_NAME_COMMAND)
  private Command command;

  public static final String SERIALIZED_NAME_RETRIES = "retries";
  @SerializedName(SERIALIZED_NAME_RETRIES)
  private BigDecimal retries;

  public static final String SERIALIZED_NAME_TIMEOUT = "timeout";
  @SerializedName(SERIALIZED_NAME_TIMEOUT)
  private BigDecimal timeout;

  public static final String SERIALIZED_NAME_S3_URI = "s3Uri";
  @SerializedName(SERIALIZED_NAME_S3_URI)
  private String s3Uri;

  public CommandPayload() {
  }

  public CommandPayload command(Command command) {
    this.command = command;
    return this;
  }

  /**
   * Get command
   * @return command
   */
  @javax.annotation.Nonnull
  public Command getCommand() {
    return command;
  }

  public void setCommand(Command command) {
    this.command = command;
  }


  public CommandPayload retries(BigDecimal retries) {
    this.retries = retries;
    return this;
  }

  /**
   * Get retries
   * minimum: 0
   * maximum: 10
   * @return retries
   */
  @javax.annotation.Nullable
  public BigDecimal getRetries() {
    return retries;
  }

  public void setRetries(BigDecimal retries) {
    this.retries = retries;
  }


  public CommandPayload timeout(BigDecimal timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * Get timeout
   * minimum: 1
   * maximum: 10080
   * @return timeout
   */
  @javax.annotation.Nullable
  public BigDecimal getTimeout() {
    return timeout;
  }

  public void setTimeout(BigDecimal timeout) {
    this.timeout = timeout;
  }


  public CommandPayload s3Uri(String s3Uri) {
    this.s3Uri = s3Uri;
    return this;
  }

  /**
   * Get s3Uri
   * @return s3Uri
   */
  @javax.annotation.Nullable
  public String getS3Uri() {
    return s3Uri;
  }

  public void setS3Uri(String s3Uri) {
    this.s3Uri = s3Uri;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CommandPayload commandPayload = (CommandPayload) o;
    return Objects.equals(this.command, commandPayload.command) &&
        Objects.equals(this.retries, commandPayload.retries) &&
        Objects.equals(this.timeout, commandPayload.timeout) &&
        Objects.equals(this.s3Uri, commandPayload.s3Uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(command, retries, timeout, s3Uri);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CommandPayload {\n");
    sb.append("    command: ").append(toIndentedString(command)).append("\n");
    sb.append("    retries: ").append(toIndentedString(retries)).append("\n");
    sb.append("    timeout: ").append(toIndentedString(timeout)).append("\n");
    sb.append("    s3Uri: ").append(toIndentedString(s3Uri)).append("\n");
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
    openapiFields.add("command");
    openapiFields.add("retries");
    openapiFields.add("timeout");
    openapiFields.add("s3Uri");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
    openapiRequiredFields.add("command");
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to CommandPayload
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!CommandPayload.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in CommandPayload is not found in the empty JSON string", CommandPayload.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!CommandPayload.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `CommandPayload` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }

      // check to make sure all required properties/fields are present in the JSON string
      for (String requiredField : CommandPayload.openapiRequiredFields) {
        if (jsonElement.getAsJsonObject().get(requiredField) == null) {
          throw new IllegalArgumentException(String.format("The required field `%s` is not found in the JSON string: %s", requiredField, jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      // validate the required field `command`
      Command.validateJsonElement(jsonObj.get("command"));
      if ((jsonObj.get("s3Uri") != null && !jsonObj.get("s3Uri").isJsonNull()) && !jsonObj.get("s3Uri").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `s3Uri` to be a primitive type in the JSON string but got `%s`", jsonObj.get("s3Uri").toString()));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!CommandPayload.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'CommandPayload' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<CommandPayload> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(CommandPayload.class));

       return (TypeAdapter<T>) new TypeAdapter<CommandPayload>() {
           @Override
           public void write(JsonWriter out, CommandPayload value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public CommandPayload read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of CommandPayload given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of CommandPayload
   * @throws IOException if the JSON string is invalid with respect to CommandPayload
   */
  public static CommandPayload fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, CommandPayload.class);
  }

  /**
   * Convert an instance of CommandPayload to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

