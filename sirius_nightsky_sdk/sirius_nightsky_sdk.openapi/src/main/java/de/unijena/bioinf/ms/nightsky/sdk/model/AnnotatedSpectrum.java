/*
 * SIRIUS Nightsky API
 * REST API that provides the full functionality of SIRIUS and its web services as background service. It is intended as entry-point for scripting languages and software integration SDKs.This API is exposed by SIRIUS 6.0.0-SNAPSHOT
 *
 * The version of the OpenAPI document: 2.0
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
import de.unijena.bioinf.ms.nightsky.sdk.model.AnnotatedPeak;
import de.unijena.bioinf.ms.nightsky.sdk.model.SpectrumAnnotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * 
 */
@JsonPropertyOrder({
  AnnotatedSpectrum.JSON_PROPERTY_NAME,
  AnnotatedSpectrum.JSON_PROPERTY_MS_LEVEL,
  AnnotatedSpectrum.JSON_PROPERTY_COLLISION_ENERGY,
  AnnotatedSpectrum.JSON_PROPERTY_PRECURSOR_MZ,
  AnnotatedSpectrum.JSON_PROPERTY_SCAN_NUMBER,
  AnnotatedSpectrum.JSON_PROPERTY_PEAKS,
  AnnotatedSpectrum.JSON_PROPERTY_SPECTRUM_ANNOTATION
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class AnnotatedSpectrum {
  public static final String JSON_PROPERTY_NAME = "name";
  private String name;

  public static final String JSON_PROPERTY_MS_LEVEL = "msLevel";
  private Integer msLevel;

  public static final String JSON_PROPERTY_COLLISION_ENERGY = "collisionEnergy";
  private String collisionEnergy;

  public static final String JSON_PROPERTY_PRECURSOR_MZ = "precursorMz";
  private Double precursorMz;

  public static final String JSON_PROPERTY_SCAN_NUMBER = "scanNumber";
  private Integer scanNumber;

  public static final String JSON_PROPERTY_PEAKS = "peaks";
  private List<AnnotatedPeak> peaks = new ArrayList<>();

  public static final String JSON_PROPERTY_SPECTRUM_ANNOTATION = "spectrumAnnotation";
  private SpectrumAnnotation spectrumAnnotation;

  public AnnotatedSpectrum() {
  }

  public AnnotatedSpectrum name(String name) {
    
    this.name = name;
    return this;
  }

   /**
   * Optional Displayable name of this spectrum.
   * @return name
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getName() {
    return name;
  }


  @JsonProperty(JSON_PROPERTY_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setName(String name) {
    this.name = name;
  }


  public AnnotatedSpectrum msLevel(Integer msLevel) {
    
    this.msLevel = msLevel;
    return this;
  }

   /**
   * MS level of the measured spectrum.  Artificial spectra with no msLevel (e.g. Simulated Isotope patterns) use null or zero
   * @return msLevel
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MS_LEVEL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getMsLevel() {
    return msLevel;
  }


  @JsonProperty(JSON_PROPERTY_MS_LEVEL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMsLevel(Integer msLevel) {
    this.msLevel = msLevel;
  }


  public AnnotatedSpectrum collisionEnergy(String collisionEnergy) {
    
    this.collisionEnergy = collisionEnergy;
    return this;
  }

   /**
   * Collision energy used for MS/MS spectra  Null for spectra where collision energy is not applicable
   * @return collisionEnergy
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_COLLISION_ENERGY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getCollisionEnergy() {
    return collisionEnergy;
  }


  @JsonProperty(JSON_PROPERTY_COLLISION_ENERGY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCollisionEnergy(String collisionEnergy) {
    this.collisionEnergy = collisionEnergy;
  }


  public AnnotatedSpectrum precursorMz(Double precursorMz) {
    
    this.precursorMz = precursorMz;
    return this;
  }

   /**
   * Precursor m/z of the MS/MS spectrum  Null for spectra where precursor m/z is not applicable
   * @return precursorMz
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PRECURSOR_MZ)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getPrecursorMz() {
    return precursorMz;
  }


  @JsonProperty(JSON_PROPERTY_PRECURSOR_MZ)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPrecursorMz(Double precursorMz) {
    this.precursorMz = precursorMz;
  }


  public AnnotatedSpectrum scanNumber(Integer scanNumber) {
    
    this.scanNumber = scanNumber;
    return this;
  }

   /**
   * Scan number of the spectrum.  Might be null for artificial spectra with no scan number (e.g. Simulated Isotope patterns or merged spectra)
   * @return scanNumber
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SCAN_NUMBER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getScanNumber() {
    return scanNumber;
  }


  @JsonProperty(JSON_PROPERTY_SCAN_NUMBER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setScanNumber(Integer scanNumber) {
    this.scanNumber = scanNumber;
  }


  public AnnotatedSpectrum peaks(List<AnnotatedPeak> peaks) {
    
    this.peaks = peaks;
    return this;
  }

  public AnnotatedSpectrum addPeaksItem(AnnotatedPeak peaksItem) {
    if (this.peaks == null) {
      this.peaks = new ArrayList<>();
    }
    this.peaks.add(peaksItem);
    return this;
  }

   /**
   * The peaks of this spectrum which might contain additional annotations such as molecular formulas.
   * @return peaks
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_PEAKS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public List<AnnotatedPeak> getPeaks() {
    return peaks;
  }


  @JsonProperty(JSON_PROPERTY_PEAKS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setPeaks(List<AnnotatedPeak> peaks) {
    this.peaks = peaks;
  }


  public AnnotatedSpectrum spectrumAnnotation(SpectrumAnnotation spectrumAnnotation) {
    
    this.spectrumAnnotation = spectrumAnnotation;
    return this;
  }

   /**
   * Get spectrumAnnotation
   * @return spectrumAnnotation
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SPECTRUM_ANNOTATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public SpectrumAnnotation getSpectrumAnnotation() {
    return spectrumAnnotation;
  }


  @JsonProperty(JSON_PROPERTY_SPECTRUM_ANNOTATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSpectrumAnnotation(SpectrumAnnotation spectrumAnnotation) {
    this.spectrumAnnotation = spectrumAnnotation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnnotatedSpectrum annotatedSpectrum = (AnnotatedSpectrum) o;
    return Objects.equals(this.name, annotatedSpectrum.name) &&
        Objects.equals(this.msLevel, annotatedSpectrum.msLevel) &&
        Objects.equals(this.collisionEnergy, annotatedSpectrum.collisionEnergy) &&
        Objects.equals(this.precursorMz, annotatedSpectrum.precursorMz) &&
        Objects.equals(this.scanNumber, annotatedSpectrum.scanNumber) &&
        Objects.equals(this.peaks, annotatedSpectrum.peaks) &&
        Objects.equals(this.spectrumAnnotation, annotatedSpectrum.spectrumAnnotation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, msLevel, collisionEnergy, precursorMz, scanNumber, peaks, spectrumAnnotation);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AnnotatedSpectrum {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    msLevel: ").append(toIndentedString(msLevel)).append("\n");
    sb.append("    collisionEnergy: ").append(toIndentedString(collisionEnergy)).append("\n");
    sb.append("    precursorMz: ").append(toIndentedString(precursorMz)).append("\n");
    sb.append("    scanNumber: ").append(toIndentedString(scanNumber)).append("\n");
    sb.append("    peaks: ").append(toIndentedString(peaks)).append("\n");
    sb.append("    spectrumAnnotation: ").append(toIndentedString(spectrumAnnotation)).append("\n");
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

