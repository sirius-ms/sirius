/*
 *  This file is part of the SIRIUS libraries for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 *  https://openapi-generator.tech
 *  Do not edit the class manually.
 */


package io.sirius.ms.sdk.model;

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * EXPERIMENTAL: This schema is experimental and may be changed (or even removed) without notice until it is declared stable.
 */
@JsonPropertyOrder({
  AdductNodeExperimental.JSON_PROPERTY_ALIGNED_FEATURE_ID,
  AdductNodeExperimental.JSON_PROPERTY_MZ,
  AdductNodeExperimental.JSON_PROPERTY_ADDUCT_ANNOTATIONS
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class AdductNodeExperimental {
  public static final String JSON_PROPERTY_ALIGNED_FEATURE_ID = "alignedFeatureId";
  private String alignedFeatureId;

  public static final String JSON_PROPERTY_MZ = "mz";
  private Double mz;

  public static final String JSON_PROPERTY_ADDUCT_ANNOTATIONS = "adductAnnotations";
  private Map<String, Double> adductAnnotations = new HashMap<>();

  public AdductNodeExperimental() {
  }

  public AdductNodeExperimental alignedFeatureId(String alignedFeatureId) {
    
    this.alignedFeatureId = alignedFeatureId;
    return this;
  }

   /**
   * Get alignedFeatureId
   * @return alignedFeatureId
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ALIGNED_FEATURE_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getAlignedFeatureId() {
    return alignedFeatureId;
  }


  @JsonProperty(JSON_PROPERTY_ALIGNED_FEATURE_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAlignedFeatureId(String alignedFeatureId) {
    this.alignedFeatureId = alignedFeatureId;
  }

  public AdductNodeExperimental mz(Double mz) {
    
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

  public AdductNodeExperimental adductAnnotations(Map<String, Double> adductAnnotations) {
    
    this.adductAnnotations = adductAnnotations;
    return this;
  }

  public AdductNodeExperimental putAdductAnnotationsItem(String key, Double adductAnnotationsItem) {
    if (this.adductAnnotations == null) {
      this.adductAnnotations = new HashMap<>();
    }
    this.adductAnnotations.put(key, adductAnnotationsItem);
    return this;
  }

   /**
   * Get adductAnnotations
   * @return adductAnnotations
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ADDUCT_ANNOTATIONS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Map<String, Double> getAdductAnnotations() {
    return adductAnnotations;
  }


  @JsonProperty(JSON_PROPERTY_ADDUCT_ANNOTATIONS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAdductAnnotations(Map<String, Double> adductAnnotations) {
    this.adductAnnotations = adductAnnotations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AdductNodeExperimental adductNodeExperimental = (AdductNodeExperimental) o;
    return Objects.equals(this.alignedFeatureId, adductNodeExperimental.alignedFeatureId) &&
        Objects.equals(this.mz, adductNodeExperimental.mz) &&
        Objects.equals(this.adductAnnotations, adductNodeExperimental.adductAnnotations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(alignedFeatureId, mz, adductAnnotations);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AdductNodeExperimental {\n");
    sb.append("    alignedFeatureId: ").append(toIndentedString(alignedFeatureId)).append("\n");
    sb.append("    mz: ").append(toIndentedString(mz)).append("\n");
    sb.append("    adductAnnotations: ").append(toIndentedString(adductAnnotations)).append("\n");
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
