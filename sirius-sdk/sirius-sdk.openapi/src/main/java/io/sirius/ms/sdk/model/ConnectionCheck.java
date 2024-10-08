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
import io.sirius.ms.sdk.model.ConnectionError;
import io.sirius.ms.sdk.model.LicenseInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * ConnectionCheck
 */
@JsonPropertyOrder({
  ConnectionCheck.JSON_PROPERTY_LICENSE_INFO,
  ConnectionCheck.JSON_PROPERTY_ERRORS
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class ConnectionCheck {
  public static final String JSON_PROPERTY_LICENSE_INFO = "licenseInfo";
  private LicenseInfo licenseInfo;

  public static final String JSON_PROPERTY_ERRORS = "errors";
  private List<ConnectionError> errors = new ArrayList<>();

  public ConnectionCheck() {
  }

  public ConnectionCheck licenseInfo(LicenseInfo licenseInfo) {
    
    this.licenseInfo = licenseInfo;
    return this;
  }

   /**
   * Get licenseInfo
   * @return licenseInfo
  **/
  @jakarta.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_LICENSE_INFO)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public LicenseInfo getLicenseInfo() {
    return licenseInfo;
  }


  @JsonProperty(JSON_PROPERTY_LICENSE_INFO)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setLicenseInfo(LicenseInfo licenseInfo) {
    this.licenseInfo = licenseInfo;
  }

  public ConnectionCheck errors(List<ConnectionError> errors) {
    
    this.errors = errors;
    return this;
  }

  public ConnectionCheck addErrorsItem(ConnectionError errorsItem) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
    }
    this.errors.add(errorsItem);
    return this;
  }

   /**
   * List of errors ordered by significance. first error should be reported and addressed first.  Following errors might just be follow-up errors
   * @return errors
  **/
  @jakarta.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_ERRORS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public List<ConnectionError> getErrors() {
    return errors;
  }


  @JsonProperty(JSON_PROPERTY_ERRORS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setErrors(List<ConnectionError> errors) {
    this.errors = errors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectionCheck connectionCheck = (ConnectionCheck) o;
    return Objects.equals(this.licenseInfo, connectionCheck.licenseInfo) &&
        Objects.equals(this.errors, connectionCheck.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(licenseInfo, errors);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionCheck {\n");
    sb.append("    licenseInfo: ").append(toIndentedString(licenseInfo)).append("\n");
    sb.append("    errors: ").append(toIndentedString(errors)).append("\n");
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

