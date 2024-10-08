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
import io.sirius.ms.sdk.model.Category;
import io.sirius.ms.sdk.model.DataQuality;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * AlignedFeatureQuality
 */
@JsonPropertyOrder({
  AlignedFeatureQuality.JSON_PROPERTY_ALIGNED_FEATURE_ID,
  AlignedFeatureQuality.JSON_PROPERTY_OVERALL_QUALITY,
  AlignedFeatureQuality.JSON_PROPERTY_CATEGORIES
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class AlignedFeatureQuality {
  public static final String JSON_PROPERTY_ALIGNED_FEATURE_ID = "alignedFeatureId";
  private String alignedFeatureId;

  public static final String JSON_PROPERTY_OVERALL_QUALITY = "overallQuality";
  private DataQuality overallQuality;

  public static final String JSON_PROPERTY_CATEGORIES = "categories";
  private Map<String, Category> categories = new HashMap<>();

  public AlignedFeatureQuality() {
  }

  public AlignedFeatureQuality alignedFeatureId(String alignedFeatureId) {
    
    this.alignedFeatureId = alignedFeatureId;
    return this;
  }

   /**
   * Id of the feature (aligned over runs) this quality information belongs to.
   * @return alignedFeatureId
  **/
  @jakarta.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_ALIGNED_FEATURE_ID)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getAlignedFeatureId() {
    return alignedFeatureId;
  }


  @JsonProperty(JSON_PROPERTY_ALIGNED_FEATURE_ID)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setAlignedFeatureId(String alignedFeatureId) {
    this.alignedFeatureId = alignedFeatureId;
  }

  public AlignedFeatureQuality overallQuality(DataQuality overallQuality) {
    
    this.overallQuality = overallQuality;
    return this;
  }

   /**
   * Get overallQuality
   * @return overallQuality
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_OVERALL_QUALITY)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public DataQuality getOverallQuality() {
    return overallQuality;
  }


  @JsonProperty(JSON_PROPERTY_OVERALL_QUALITY)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setOverallQuality(DataQuality overallQuality) {
    this.overallQuality = overallQuality;
  }

  public AlignedFeatureQuality categories(Map<String, Category> categories) {
    
    this.categories = categories;
    return this;
  }

  public AlignedFeatureQuality putCategoriesItem(String key, Category categoriesItem) {
    this.categories.put(key, categoriesItem);
    return this;
  }

   /**
   * Contains all pre-computation quality information that belong to  this feature (aligned over runs), such as information about the quality of the peak shape, MS2 spectrum etc.,
   * @return categories
  **/
  @jakarta.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_CATEGORIES)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public Map<String, Category> getCategories() {
    return categories;
  }


  @JsonProperty(JSON_PROPERTY_CATEGORIES)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setCategories(Map<String, Category> categories) {
    this.categories = categories;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AlignedFeatureQuality alignedFeatureQuality = (AlignedFeatureQuality) o;
    return Objects.equals(this.alignedFeatureId, alignedFeatureQuality.alignedFeatureId) &&
        Objects.equals(this.overallQuality, alignedFeatureQuality.overallQuality) &&
        Objects.equals(this.categories, alignedFeatureQuality.categories);
  }

  @Override
  public int hashCode() {
    return Objects.hash(alignedFeatureId, overallQuality, categories);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AlignedFeatureQuality {\n");
    sb.append("    alignedFeatureId: ").append(toIndentedString(alignedFeatureId)).append("\n");
    sb.append("    overallQuality: ").append(toIndentedString(overallQuality)).append("\n");
    sb.append("    categories: ").append(toIndentedString(categories)).append("\n");
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

