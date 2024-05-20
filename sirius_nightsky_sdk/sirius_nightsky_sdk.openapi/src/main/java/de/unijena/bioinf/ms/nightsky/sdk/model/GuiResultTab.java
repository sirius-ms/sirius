/*
 * SIRIUS Nightsky API
 * REST API that provides the full functionality of SIRIUS and its web services as background service. It is intended as entry-point for scripting languages and software integration SDKs.This API is exposed by SIRIUS 6.0.0-SNAPSHOT
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
 * Available result tabs in the SIRIUS GUI. Name correspond to the names in the GUI.
 */
public enum GuiResultTab {
  
  FORMULAS("FORMULAS"),
  
  PREDICTED_FINGERPRINT("PREDICTED_FINGERPRINT"),
  
  COMPOUND_CLASSES("COMPOUND_CLASSES"),
  
  STRUCTURES("STRUCTURES"),
  
  STRUCTURE_ANNOTATION("STRUCTURE_ANNOTATION"),
  
  DE_NOVO_STRUCTURES("DE_NOVO_STRUCTURES");

  private String value;

  GuiResultTab(String value) {
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
  public static GuiResultTab fromValue(String value) {
    for (GuiResultTab b : GuiResultTab.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    return null;
  }
}

