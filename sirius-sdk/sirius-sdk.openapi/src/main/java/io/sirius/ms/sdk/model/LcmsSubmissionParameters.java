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
import io.sirius.ms.sdk.model.Deviation;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * LcmsSubmissionParameters
 */
@JsonPropertyOrder({
  LcmsSubmissionParameters.JSON_PROPERTY_ALIGN_L_C_M_S_RUNS,
  LcmsSubmissionParameters.JSON_PROPERTY_NOISE_INTENSITY,
  LcmsSubmissionParameters.JSON_PROPERTY_TRACE_MAX_MASS_DEVIATION,
  LcmsSubmissionParameters.JSON_PROPERTY_ALIGN_MAX_MASS_DEVIATION,
  LcmsSubmissionParameters.JSON_PROPERTY_ALIGN_MAX_RETENTION_TIME_DEVIATION,
  LcmsSubmissionParameters.JSON_PROPERTY_MIN_S_N_R
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class LcmsSubmissionParameters {
  public static final String JSON_PROPERTY_ALIGN_L_C_M_S_RUNS = "alignLCMSRuns";
  private Boolean alignLCMSRuns = true;

  public static final String JSON_PROPERTY_NOISE_INTENSITY = "noiseIntensity";
  private Double noiseIntensity = -1d;

  public static final String JSON_PROPERTY_TRACE_MAX_MASS_DEVIATION = "traceMaxMassDeviation";
  private Deviation traceMaxMassDeviation;

  public static final String JSON_PROPERTY_ALIGN_MAX_MASS_DEVIATION = "alignMaxMassDeviation";
  private Deviation alignMaxMassDeviation;

  public static final String JSON_PROPERTY_ALIGN_MAX_RETENTION_TIME_DEVIATION = "alignMaxRetentionTimeDeviation";
  private Double alignMaxRetentionTimeDeviation = -1d;

  public static final String JSON_PROPERTY_MIN_S_N_R = "minSNR";
  private Double minSNR = 3d;

  public LcmsSubmissionParameters() {
  }

  public LcmsSubmissionParameters alignLCMSRuns(Boolean alignLCMSRuns) {
    
    this.alignLCMSRuns = alignLCMSRuns;
    return this;
  }

   /**
   * Specifies whether LC/MS runs should be aligned
   * @return alignLCMSRuns
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ALIGN_L_C_M_S_RUNS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isAlignLCMSRuns() {
    return alignLCMSRuns;
  }


  @JsonProperty(JSON_PROPERTY_ALIGN_L_C_M_S_RUNS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAlignLCMSRuns(Boolean alignLCMSRuns) {
    this.alignLCMSRuns = alignLCMSRuns;
  }

  public LcmsSubmissionParameters noiseIntensity(Double noiseIntensity) {
    
    this.noiseIntensity = noiseIntensity;
    return this;
  }

   /**
   * Noise level under which all peaks are considered to be likely noise. A peak has to be at least 3x noise level  to be picked as feature. Peaks with MS/MS are still picked even though they might be below noise level.  If not specified, the noise intensity is detected automatically from data. We recommend to NOT specify  this parameter, as the automated detection is usually sufficient.
   * @return noiseIntensity
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NOISE_INTENSITY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getNoiseIntensity() {
    return noiseIntensity;
  }


  @JsonProperty(JSON_PROPERTY_NOISE_INTENSITY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setNoiseIntensity(Double noiseIntensity) {
    this.noiseIntensity = noiseIntensity;
  }

  public LcmsSubmissionParameters traceMaxMassDeviation(Deviation traceMaxMassDeviation) {
    
    this.traceMaxMassDeviation = traceMaxMassDeviation;
    return this;
  }

   /**
   * Get traceMaxMassDeviation
   * @return traceMaxMassDeviation
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TRACE_MAX_MASS_DEVIATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Deviation getTraceMaxMassDeviation() {
    return traceMaxMassDeviation;
  }


  @JsonProperty(JSON_PROPERTY_TRACE_MAX_MASS_DEVIATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTraceMaxMassDeviation(Deviation traceMaxMassDeviation) {
    this.traceMaxMassDeviation = traceMaxMassDeviation;
  }

  public LcmsSubmissionParameters alignMaxMassDeviation(Deviation alignMaxMassDeviation) {
    
    this.alignMaxMassDeviation = alignMaxMassDeviation;
    return this;
  }

   /**
   * Get alignMaxMassDeviation
   * @return alignMaxMassDeviation
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ALIGN_MAX_MASS_DEVIATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Deviation getAlignMaxMassDeviation() {
    return alignMaxMassDeviation;
  }


  @JsonProperty(JSON_PROPERTY_ALIGN_MAX_MASS_DEVIATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAlignMaxMassDeviation(Deviation alignMaxMassDeviation) {
    this.alignMaxMassDeviation = alignMaxMassDeviation;
  }

  public LcmsSubmissionParameters alignMaxRetentionTimeDeviation(Double alignMaxRetentionTimeDeviation) {
    
    this.alignMaxRetentionTimeDeviation = alignMaxRetentionTimeDeviation;
    return this;
  }

   /**
   * Maximal allowed retention time error in seconds for aligning features. If not specified, this parameter is estimated from data.
   * @return alignMaxRetentionTimeDeviation
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ALIGN_MAX_RETENTION_TIME_DEVIATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getAlignMaxRetentionTimeDeviation() {
    return alignMaxRetentionTimeDeviation;
  }


  @JsonProperty(JSON_PROPERTY_ALIGN_MAX_RETENTION_TIME_DEVIATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAlignMaxRetentionTimeDeviation(Double alignMaxRetentionTimeDeviation) {
    this.alignMaxRetentionTimeDeviation = alignMaxRetentionTimeDeviation;
  }

  public LcmsSubmissionParameters minSNR(Double minSNR) {
    
    this.minSNR = minSNR;
    return this;
  }

   /**
   * Minimum ratio between peak height and noise intensity for detecting features. By default, this value is 3. Features with good MS/MS are always picked independent of their intensity. For picking very low intensive features we recommend a min-snr of 2, but this will increase runtime and storage memory
   * @return minSNR
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MIN_S_N_R)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getMinSNR() {
    return minSNR;
  }


  @JsonProperty(JSON_PROPERTY_MIN_S_N_R)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMinSNR(Double minSNR) {
    this.minSNR = minSNR;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LcmsSubmissionParameters lcmsSubmissionParameters = (LcmsSubmissionParameters) o;
    return Objects.equals(this.alignLCMSRuns, lcmsSubmissionParameters.alignLCMSRuns) &&
        Objects.equals(this.noiseIntensity, lcmsSubmissionParameters.noiseIntensity) &&
        Objects.equals(this.traceMaxMassDeviation, lcmsSubmissionParameters.traceMaxMassDeviation) &&
        Objects.equals(this.alignMaxMassDeviation, lcmsSubmissionParameters.alignMaxMassDeviation) &&
        Objects.equals(this.alignMaxRetentionTimeDeviation, lcmsSubmissionParameters.alignMaxRetentionTimeDeviation) &&
        Objects.equals(this.minSNR, lcmsSubmissionParameters.minSNR);
  }

  @Override
  public int hashCode() {
    return Objects.hash(alignLCMSRuns, noiseIntensity, traceMaxMassDeviation, alignMaxMassDeviation, alignMaxRetentionTimeDeviation, minSNR);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LcmsSubmissionParameters {\n");
    sb.append("    alignLCMSRuns: ").append(toIndentedString(alignLCMSRuns)).append("\n");
    sb.append("    noiseIntensity: ").append(toIndentedString(noiseIntensity)).append("\n");
    sb.append("    traceMaxMassDeviation: ").append(toIndentedString(traceMaxMassDeviation)).append("\n");
    sb.append("    alignMaxMassDeviation: ").append(toIndentedString(alignMaxMassDeviation)).append("\n");
    sb.append("    alignMaxRetentionTimeDeviation: ").append(toIndentedString(alignMaxRetentionTimeDeviation)).append("\n");
    sb.append("    minSNR: ").append(toIndentedString(minSNR)).append("\n");
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

