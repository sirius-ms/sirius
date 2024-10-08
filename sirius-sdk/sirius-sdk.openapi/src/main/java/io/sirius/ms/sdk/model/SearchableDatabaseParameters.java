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
 * SearchableDatabaseParameters
 */
@JsonPropertyOrder({
  SearchableDatabaseParameters.JSON_PROPERTY_DISPLAY_NAME,
  SearchableDatabaseParameters.JSON_PROPERTY_LOCATION,
  SearchableDatabaseParameters.JSON_PROPERTY_MATCH_RT_OF_REFERENCE_SPECTRA
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class SearchableDatabaseParameters {
  public static final String JSON_PROPERTY_DISPLAY_NAME = "displayName";
  private String displayName;

  public static final String JSON_PROPERTY_LOCATION = "location";
  private String location;

  public static final String JSON_PROPERTY_MATCH_RT_OF_REFERENCE_SPECTRA = "matchRtOfReferenceSpectra";
  private Boolean matchRtOfReferenceSpectra = false;

  public SearchableDatabaseParameters() {
  }

  public SearchableDatabaseParameters displayName(String displayName) {
    
    this.displayName = displayName;
    return this;
  }

   /**
   * display name of the database  Should be short
   * @return displayName
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DISPLAY_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getDisplayName() {
    return displayName;
  }


  @JsonProperty(JSON_PROPERTY_DISPLAY_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public SearchableDatabaseParameters location(String location) {
    
    this.location = location;
    return this;
  }

   /**
   * Storage location of user database  Might be NULL for non-user databases or if default location is used.
   * @return location
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_LOCATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getLocation() {
    return location;
  }


  @JsonProperty(JSON_PROPERTY_LOCATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setLocation(String location) {
    this.location = location;
  }

  public SearchableDatabaseParameters matchRtOfReferenceSpectra(Boolean matchRtOfReferenceSpectra) {
    
    this.matchRtOfReferenceSpectra = matchRtOfReferenceSpectra;
    return this;
  }

   /**
   * Indicates whether this database shall be used to use retention time information for library matching.  Typically used for in-house spectral libraries that have been measured on
   * @return matchRtOfReferenceSpectra
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MATCH_RT_OF_REFERENCE_SPECTRA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isMatchRtOfReferenceSpectra() {
    return matchRtOfReferenceSpectra;
  }


  @JsonProperty(JSON_PROPERTY_MATCH_RT_OF_REFERENCE_SPECTRA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMatchRtOfReferenceSpectra(Boolean matchRtOfReferenceSpectra) {
    this.matchRtOfReferenceSpectra = matchRtOfReferenceSpectra;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SearchableDatabaseParameters searchableDatabaseParameters = (SearchableDatabaseParameters) o;
    return Objects.equals(this.displayName, searchableDatabaseParameters.displayName) &&
        Objects.equals(this.location, searchableDatabaseParameters.location) &&
        Objects.equals(this.matchRtOfReferenceSpectra, searchableDatabaseParameters.matchRtOfReferenceSpectra);
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayName, location, matchRtOfReferenceSpectra);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SearchableDatabaseParameters {\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    matchRtOfReferenceSpectra: ").append(toIndentedString(matchRtOfReferenceSpectra)).append("\n");
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

