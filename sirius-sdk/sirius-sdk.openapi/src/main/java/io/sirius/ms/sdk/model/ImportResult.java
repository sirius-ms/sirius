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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * ImportResult
 */
@JsonPropertyOrder({
  ImportResult.JSON_PROPERTY_AFFECTED_COMPOUND_IDS,
  ImportResult.JSON_PROPERTY_AFFECTED_ALIGNED_FEATURE_IDS
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class ImportResult {
  public static final String JSON_PROPERTY_AFFECTED_COMPOUND_IDS = "affectedCompoundIds";
  private List<String> affectedCompoundIds = new ArrayList<>();

  public static final String JSON_PROPERTY_AFFECTED_ALIGNED_FEATURE_IDS = "affectedAlignedFeatureIds";
  private List<String> affectedAlignedFeatureIds = new ArrayList<>();

  public ImportResult() {
  }

  public ImportResult affectedCompoundIds(List<String> affectedCompoundIds) {
    
    this.affectedCompoundIds = affectedCompoundIds;
    return this;
  }

  public ImportResult addAffectedCompoundIdsItem(String affectedCompoundIdsItem) {
    if (this.affectedCompoundIds == null) {
      this.affectedCompoundIds = new ArrayList<>();
    }
    this.affectedCompoundIds.add(affectedCompoundIdsItem);
    return this;
  }

   /**
   * List of compoundIds that have been imported.
   * @return affectedCompoundIds
  **/
  @jakarta.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_AFFECTED_COMPOUND_IDS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public List<String> getAffectedCompoundIds() {
    return affectedCompoundIds;
  }


  @JsonProperty(JSON_PROPERTY_AFFECTED_COMPOUND_IDS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setAffectedCompoundIds(List<String> affectedCompoundIds) {
    this.affectedCompoundIds = affectedCompoundIds;
  }

  public ImportResult affectedAlignedFeatureIds(List<String> affectedAlignedFeatureIds) {
    
    this.affectedAlignedFeatureIds = affectedAlignedFeatureIds;
    return this;
  }

  public ImportResult addAffectedAlignedFeatureIdsItem(String affectedAlignedFeatureIdsItem) {
    if (this.affectedAlignedFeatureIds == null) {
      this.affectedAlignedFeatureIds = new ArrayList<>();
    }
    this.affectedAlignedFeatureIds.add(affectedAlignedFeatureIdsItem);
    return this;
  }

   /**
   * List of alignedFeatureIds that have been imported..
   * @return affectedAlignedFeatureIds
  **/
  @jakarta.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_AFFECTED_ALIGNED_FEATURE_IDS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public List<String> getAffectedAlignedFeatureIds() {
    return affectedAlignedFeatureIds;
  }


  @JsonProperty(JSON_PROPERTY_AFFECTED_ALIGNED_FEATURE_IDS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setAffectedAlignedFeatureIds(List<String> affectedAlignedFeatureIds) {
    this.affectedAlignedFeatureIds = affectedAlignedFeatureIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImportResult importResult = (ImportResult) o;
    return Objects.equals(this.affectedCompoundIds, importResult.affectedCompoundIds) &&
        Objects.equals(this.affectedAlignedFeatureIds, importResult.affectedAlignedFeatureIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(affectedCompoundIds, affectedAlignedFeatureIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ImportResult {\n");
    sb.append("    affectedCompoundIds: ").append(toIndentedString(affectedCompoundIds)).append("\n");
    sb.append("    affectedAlignedFeatureIds: ").append(toIndentedString(affectedAlignedFeatureIds)).append("\n");
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

