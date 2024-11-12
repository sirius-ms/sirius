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
 * SimplePeak
 */
@JsonPropertyOrder({
  SimplePeak.JSON_PROPERTY_MZ,
  SimplePeak.JSON_PROPERTY_INTENSITY
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class SimplePeak {
  public static final String JSON_PROPERTY_MZ = "mz";
  private Double mz;

  public static final String JSON_PROPERTY_INTENSITY = "intensity";
  private Double intensity;

  public SimplePeak() {
  }

  public SimplePeak mz(Double mz) {
    
    this.mz = mz;
    return this;
  }

   /**
   * Get mz
   * @return mz
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MZ)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getMz() {
    return mz;
  }


  @JsonProperty(JSON_PROPERTY_MZ)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMz(Double mz) {
    this.mz = mz;
  }

  public SimplePeak intensity(Double intensity) {
    
    this.intensity = intensity;
    return this;
  }

   /**
   * Get intensity
   * @return intensity
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_INTENSITY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getIntensity() {
    return intensity;
  }


  @JsonProperty(JSON_PROPERTY_INTENSITY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setIntensity(Double intensity) {
    this.intensity = intensity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SimplePeak simplePeak = (SimplePeak) o;
    return Objects.equals(this.mz, simplePeak.mz) &&
        Objects.equals(this.intensity, simplePeak.intensity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mz, intensity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SimplePeak {\n");
    sb.append("    mz: ").append(toIndentedString(mz)).append("\n");
    sb.append("    intensity: ").append(toIndentedString(intensity)).append("\n");
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
