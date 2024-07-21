/*
 * SIRIUS Nightsky API
 * REST API that provides the full functionality of SIRIUS and its web services as background service. It is intended as entry-point for scripting languages and software integration SDKs.This API is exposed by SIRIUS 6
 *
 * The version of the OpenAPI document: 2.1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package de.unijena.bioinf.ms.nightsky.sdk.model;

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Gets or Sets DataQuality
 */
public enum DataQuality {
  
  NOT_APPLICABLE("NOT_APPLICABLE"),
  
  LOWEST("LOWEST"),
  
  BAD("BAD"),
  
  DECENT("DECENT"),
  
  GOOD("GOOD");

  private String value;

  DataQuality(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static DataQuality fromValue(String value) {
    for (DataQuality b : DataQuality.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    return null;
  }
}

