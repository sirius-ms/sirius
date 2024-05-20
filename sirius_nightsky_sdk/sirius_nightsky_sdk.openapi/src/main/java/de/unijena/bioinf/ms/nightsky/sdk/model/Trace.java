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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import de.unijena.bioinf.ms.nightsky.sdk.model.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Trace
 */
@JsonPropertyOrder({
  Trace.JSON_PROPERTY_ID,
  Trace.JSON_PROPERTY_SAMPLE_ID,
  Trace.JSON_PROPERTY_SAMPLE_NAME,
  Trace.JSON_PROPERTY_LABEL,
  Trace.JSON_PROPERTY_INTENSITIES,
  Trace.JSON_PROPERTY_ANNOTATIONS,
  Trace.JSON_PROPERTY_MZ
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class Trace {
  public static final String JSON_PROPERTY_ID = "id";
  private Long id;

  public static final String JSON_PROPERTY_SAMPLE_ID = "sampleId";
  private Long sampleId;

  public static final String JSON_PROPERTY_SAMPLE_NAME = "sampleName";
  private String sampleName;

  public static final String JSON_PROPERTY_LABEL = "label";
  private String label;

  public static final String JSON_PROPERTY_INTENSITIES = "intensities";
  private List<Double> intensities;

  public static final String JSON_PROPERTY_ANNOTATIONS = "annotations";
  private List<Annotation> annotations;

  public static final String JSON_PROPERTY_MZ = "mz";
  private Double mz;

  public Trace() {
  }

  public Trace id(Long id) {
    
    this.id = id;
    return this;
  }

   /**
   * Get id
   * @return id
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getId() {
    return id;
  }


  @JsonProperty(JSON_PROPERTY_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setId(Long id) {
    this.id = id;
  }


  public Trace sampleId(Long sampleId) {
    
    this.sampleId = sampleId;
    return this;
  }

   /**
   * Get sampleId
   * @return sampleId
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SAMPLE_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getSampleId() {
    return sampleId;
  }


  @JsonProperty(JSON_PROPERTY_SAMPLE_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSampleId(Long sampleId) {
    this.sampleId = sampleId;
  }


  public Trace sampleName(String sampleName) {
    
    this.sampleName = sampleName;
    return this;
  }

   /**
   * Get sampleName
   * @return sampleName
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SAMPLE_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getSampleName() {
    return sampleName;
  }


  @JsonProperty(JSON_PROPERTY_SAMPLE_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSampleName(String sampleName) {
    this.sampleName = sampleName;
  }


  public Trace label(String label) {
    
    this.label = label;
    return this;
  }

   /**
   * Get label
   * @return label
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_LABEL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getLabel() {
    return label;
  }


  @JsonProperty(JSON_PROPERTY_LABEL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setLabel(String label) {
    this.label = label;
  }


  public Trace intensities(List<Double> intensities) {
    
    this.intensities = intensities;
    return this;
  }

  public Trace addIntensitiesItem(Double intensitiesItem) {
    if (this.intensities == null) {
      this.intensities = new ArrayList<>();
    }
    this.intensities.add(intensitiesItem);
    return this;
  }

   /**
   * Get intensities
   * @return intensities
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_INTENSITIES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<Double> getIntensities() {
    return intensities;
  }


  @JsonProperty(JSON_PROPERTY_INTENSITIES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setIntensities(List<Double> intensities) {
    this.intensities = intensities;
  }


  public Trace annotations(List<Annotation> annotations) {
    
    this.annotations = annotations;
    return this;
  }

  public Trace addAnnotationsItem(Annotation annotationsItem) {
    if (this.annotations == null) {
      this.annotations = new ArrayList<>();
    }
    this.annotations.add(annotationsItem);
    return this;
  }

   /**
   * Get annotations
   * @return annotations
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ANNOTATIONS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<Annotation> getAnnotations() {
    return annotations;
  }


  @JsonProperty(JSON_PROPERTY_ANNOTATIONS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAnnotations(List<Annotation> annotations) {
    this.annotations = annotations;
  }


  public Trace mz(Double mz) {
    
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Trace trace = (Trace) o;
    return Objects.equals(this.id, trace.id) &&
        Objects.equals(this.sampleId, trace.sampleId) &&
        Objects.equals(this.sampleName, trace.sampleName) &&
        Objects.equals(this.label, trace.label) &&
        Objects.equals(this.intensities, trace.intensities) &&
        Objects.equals(this.annotations, trace.annotations) &&
        Objects.equals(this.mz, trace.mz);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, sampleId, sampleName, label, intensities, annotations, mz);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Trace {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    sampleId: ").append(toIndentedString(sampleId)).append("\n");
    sb.append("    sampleName: ").append(toIndentedString(sampleName)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    intensities: ").append(toIndentedString(intensities)).append("\n");
    sb.append("    annotations: ").append(toIndentedString(annotations)).append("\n");
    sb.append("    mz: ").append(toIndentedString(mz)).append("\n");
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

