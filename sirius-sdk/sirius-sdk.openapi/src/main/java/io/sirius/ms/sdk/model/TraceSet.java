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
import io.sirius.ms.sdk.model.Axes;
import io.sirius.ms.sdk.model.Trace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * TraceSet
 */
@JsonPropertyOrder({
  TraceSet.JSON_PROPERTY_SAMPLE_ID,
  TraceSet.JSON_PROPERTY_SAMPLE_NAME,
  TraceSet.JSON_PROPERTY_AXES,
  TraceSet.JSON_PROPERTY_TRACES
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class TraceSet {
  public static final String JSON_PROPERTY_SAMPLE_ID = "sampleId";
  private Long sampleId;

  public static final String JSON_PROPERTY_SAMPLE_NAME = "sampleName";
  private String sampleName;

  public static final String JSON_PROPERTY_AXES = "axes";
  private Axes axes;

  public static final String JSON_PROPERTY_TRACES = "traces";
  private List<Trace> traces = new ArrayList<>();

  public TraceSet() {
  }

  public TraceSet sampleId(Long sampleId) {
    
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

  public TraceSet sampleName(String sampleName) {
    
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

  public TraceSet axes(Axes axes) {
    
    this.axes = axes;
    return this;
  }

   /**
   * Get axes
   * @return axes
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_AXES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Axes getAxes() {
    return axes;
  }


  @JsonProperty(JSON_PROPERTY_AXES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAxes(Axes axes) {
    this.axes = axes;
  }

  public TraceSet traces(List<Trace> traces) {
    
    this.traces = traces;
    return this;
  }

  public TraceSet addTracesItem(Trace tracesItem) {
    if (this.traces == null) {
      this.traces = new ArrayList<>();
    }
    this.traces.add(tracesItem);
    return this;
  }

   /**
   * Get traces
   * @return traces
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TRACES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<Trace> getTraces() {
    return traces;
  }


  @JsonProperty(JSON_PROPERTY_TRACES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTraces(List<Trace> traces) {
    this.traces = traces;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TraceSet traceSet = (TraceSet) o;
    return Objects.equals(this.sampleId, traceSet.sampleId) &&
        Objects.equals(this.sampleName, traceSet.sampleName) &&
        Objects.equals(this.axes, traceSet.axes) &&
        Objects.equals(this.traces, traceSet.traces);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sampleId, sampleName, axes, traces);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TraceSet {\n");
    sb.append("    sampleId: ").append(toIndentedString(sampleId)).append("\n");
    sb.append("    sampleName: ").append(toIndentedString(sampleName)).append("\n");
    sb.append("    axes: ").append(toIndentedString(axes)).append("\n");
    sb.append("    traces: ").append(toIndentedString(traces)).append("\n");
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

