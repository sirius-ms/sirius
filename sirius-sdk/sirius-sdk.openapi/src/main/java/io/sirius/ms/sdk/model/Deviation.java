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


package io.sirius.ms.sdk.model;

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Deviation
 */
@JsonPropertyOrder({
  Deviation.JSON_PROPERTY_PPM,
  Deviation.JSON_PROPERTY_ABSOLUTE
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class Deviation {
  public static final String JSON_PROPERTY_PPM = "ppm";
  private Double ppm;

  public static final String JSON_PROPERTY_ABSOLUTE = "absolute";
  private Double absolute;

  public Deviation() {
  }

  public Deviation ppm(Double ppm) {
    
    this.ppm = ppm;
    return this;
  }

   /**
   * Get ppm
   * @return ppm
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PPM)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getPpm() {
    return ppm;
  }


  @JsonProperty(JSON_PROPERTY_PPM)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPpm(Double ppm) {
    this.ppm = ppm;
  }

  public Deviation absolute(Double absolute) {
    
    this.absolute = absolute;
    return this;
  }

   /**
   * Get absolute
   * @return absolute
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ABSOLUTE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getAbsolute() {
    return absolute;
  }


  @JsonProperty(JSON_PROPERTY_ABSOLUTE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAbsolute(Double absolute) {
    this.absolute = absolute;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Deviation deviation = (Deviation) o;
    return Objects.equals(this.ppm, deviation.ppm) &&
        Objects.equals(this.absolute, deviation.absolute);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ppm, absolute);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Deviation {\n");
    sb.append("    ppm: ").append(toIndentedString(ppm)).append("\n");
    sb.append("    absolute: ").append(toIndentedString(absolute)).append("\n");
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

}

