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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import de.unijena.bioinf.ms.nightsky.sdk.model.DataSmoothing;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * LcmsSubmissionParameters
 */
@JsonPropertyOrder({
  LcmsSubmissionParameters.JSON_PROPERTY_ALIGN_L_C_M_S_RUNS,
  LcmsSubmissionParameters.JSON_PROPERTY_NOISE,
  LcmsSubmissionParameters.JSON_PROPERTY_PERSISTENCE,
  LcmsSubmissionParameters.JSON_PROPERTY_MERGE,
  LcmsSubmissionParameters.JSON_PROPERTY_FILTER,
  LcmsSubmissionParameters.JSON_PROPERTY_GAUSSIAN_SIGMA,
  LcmsSubmissionParameters.JSON_PROPERTY_WAVELET_SCALE,
  LcmsSubmissionParameters.JSON_PROPERTY_WAVELET_WINDOW
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class LcmsSubmissionParameters {
  public static final String JSON_PROPERTY_ALIGN_L_C_M_S_RUNS = "alignLCMSRuns";
  private Boolean alignLCMSRuns = true;

  public static final String JSON_PROPERTY_NOISE = "noise";
  private Double noise = 2.0d;

  public static final String JSON_PROPERTY_PERSISTENCE = "persistence";
  private Double persistence = 0.1d;

  public static final String JSON_PROPERTY_MERGE = "merge";
  private Double merge = 0.8d;

  public static final String JSON_PROPERTY_FILTER = "filter";
  private DataSmoothing filter = DataSmoothing.AUTO;

  public static final String JSON_PROPERTY_GAUSSIAN_SIGMA = "gaussianSigma";
  private Double gaussianSigma = 3.0d;

  public static final String JSON_PROPERTY_WAVELET_SCALE = "waveletScale";
  private Integer waveletScale = 20;

  public static final String JSON_PROPERTY_WAVELET_WINDOW = "waveletWindow";
  private Double waveletWindow = 11d;

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

  public LcmsSubmissionParameters noise(Double noise) {
    
    this.noise = noise;
    return this;
  }

   /**
   * Features must be larger than &lt;value&gt; * detected noise level.
   * @return noise
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NOISE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getNoise() {
    return noise;
  }


  @JsonProperty(JSON_PROPERTY_NOISE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setNoise(Double noise) {
    this.noise = noise;
  }

  public LcmsSubmissionParameters persistence(Double persistence) {
    
    this.persistence = persistence;
    return this;
  }

   /**
   * Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
   * @return persistence
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PERSISTENCE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getPersistence() {
    return persistence;
  }


  @JsonProperty(JSON_PROPERTY_PERSISTENCE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPersistence(Double persistence) {
    this.persistence = persistence;
  }

  public LcmsSubmissionParameters merge(Double merge) {
    
    this.merge = merge;
    return this;
  }

   /**
   * Merge neighboring features with valley less than &lt;value&gt; * intensity.
   * @return merge
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MERGE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getMerge() {
    return merge;
  }


  @JsonProperty(JSON_PROPERTY_MERGE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMerge(Double merge) {
    this.merge = merge;
  }

  public LcmsSubmissionParameters filter(DataSmoothing filter) {
    
    this.filter = filter;
    return this;
  }

   /**
   * Get filter
   * @return filter
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_FILTER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public DataSmoothing getFilter() {
    return filter;
  }


  @JsonProperty(JSON_PROPERTY_FILTER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setFilter(DataSmoothing filter) {
    this.filter = filter;
  }

  public LcmsSubmissionParameters gaussianSigma(Double gaussianSigma) {
    
    this.gaussianSigma = gaussianSigma;
    return this;
  }

   /**
   * Sigma (kernel width) for gaussian filter algorithm.
   * @return gaussianSigma
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_GAUSSIAN_SIGMA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getGaussianSigma() {
    return gaussianSigma;
  }


  @JsonProperty(JSON_PROPERTY_GAUSSIAN_SIGMA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setGaussianSigma(Double gaussianSigma) {
    this.gaussianSigma = gaussianSigma;
  }

  public LcmsSubmissionParameters waveletScale(Integer waveletScale) {
    
    this.waveletScale = waveletScale;
    return this;
  }

   /**
   * Number of coefficients for wavelet filter algorithm.
   * @return waveletScale
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_WAVELET_SCALE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getWaveletScale() {
    return waveletScale;
  }


  @JsonProperty(JSON_PROPERTY_WAVELET_SCALE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setWaveletScale(Integer waveletScale) {
    this.waveletScale = waveletScale;
  }

  public LcmsSubmissionParameters waveletWindow(Double waveletWindow) {
    
    this.waveletWindow = waveletWindow;
    return this;
  }

   /**
   * Wavelet window size (%) for wavelet filter algorithm.
   * @return waveletWindow
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_WAVELET_WINDOW)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getWaveletWindow() {
    return waveletWindow;
  }


  @JsonProperty(JSON_PROPERTY_WAVELET_WINDOW)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setWaveletWindow(Double waveletWindow) {
    this.waveletWindow = waveletWindow;
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
        Objects.equals(this.noise, lcmsSubmissionParameters.noise) &&
        Objects.equals(this.persistence, lcmsSubmissionParameters.persistence) &&
        Objects.equals(this.merge, lcmsSubmissionParameters.merge) &&
        Objects.equals(this.filter, lcmsSubmissionParameters.filter) &&
        Objects.equals(this.gaussianSigma, lcmsSubmissionParameters.gaussianSigma) &&
        Objects.equals(this.waveletScale, lcmsSubmissionParameters.waveletScale) &&
        Objects.equals(this.waveletWindow, lcmsSubmissionParameters.waveletWindow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(alignLCMSRuns, noise, persistence, merge, filter, gaussianSigma, waveletScale, waveletWindow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LcmsSubmissionParameters {\n");
    sb.append("    alignLCMSRuns: ").append(toIndentedString(alignLCMSRuns)).append("\n");
    sb.append("    noise: ").append(toIndentedString(noise)).append("\n");
    sb.append("    persistence: ").append(toIndentedString(persistence)).append("\n");
    sb.append("    merge: ").append(toIndentedString(merge)).append("\n");
    sb.append("    filter: ").append(toIndentedString(filter)).append("\n");
    sb.append("    gaussianSigma: ").append(toIndentedString(gaussianSigma)).append("\n");
    sb.append("    waveletScale: ").append(toIndentedString(waveletScale)).append("\n");
    sb.append("    waveletWindow: ").append(toIndentedString(waveletWindow)).append("\n");
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
