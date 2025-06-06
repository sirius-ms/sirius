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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * SpectrumAnnotation
 */
@JsonPropertyOrder({
  SpectrumAnnotation.JSON_PROPERTY_MOLECULAR_FORMULA,
  SpectrumAnnotation.JSON_PROPERTY_ADDUCT,
  SpectrumAnnotation.JSON_PROPERTY_EXACT_MASS,
  SpectrumAnnotation.JSON_PROPERTY_MASS_DEVIATION_MZ,
  SpectrumAnnotation.JSON_PROPERTY_MASS_DEVIATION_PPM,
  SpectrumAnnotation.JSON_PROPERTY_STRUCTURE_ANNOTATION_SMILES,
  SpectrumAnnotation.JSON_PROPERTY_STRUCTURE_ANNOTATION_NAME,
  SpectrumAnnotation.JSON_PROPERTY_STRUCTURE_ANNOTATION_SVG,
  SpectrumAnnotation.JSON_PROPERTY_STRUCTURE_ANNOTATION_SCORE
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class SpectrumAnnotation {
  public static final String JSON_PROPERTY_MOLECULAR_FORMULA = "molecularFormula";
  private String molecularFormula;

  public static final String JSON_PROPERTY_ADDUCT = "adduct";
  private String adduct;

  public static final String JSON_PROPERTY_EXACT_MASS = "exactMass";
  private Double exactMass;

  public static final String JSON_PROPERTY_MASS_DEVIATION_MZ = "massDeviationMz";
  private Double massDeviationMz;

  public static final String JSON_PROPERTY_MASS_DEVIATION_PPM = "massDeviationPpm";
  private Double massDeviationPpm;

  public static final String JSON_PROPERTY_STRUCTURE_ANNOTATION_SMILES = "structureAnnotationSmiles";
  private String structureAnnotationSmiles;

  public static final String JSON_PROPERTY_STRUCTURE_ANNOTATION_NAME = "structureAnnotationName";
  private String structureAnnotationName;

  public static final String JSON_PROPERTY_STRUCTURE_ANNOTATION_SVG = "structureAnnotationSvg";
  private String structureAnnotationSvg;

  public static final String JSON_PROPERTY_STRUCTURE_ANNOTATION_SCORE = "structureAnnotationScore";
  private Double structureAnnotationScore;

  public SpectrumAnnotation() {
  }

  public SpectrumAnnotation molecularFormula(String molecularFormula) {
    
    this.molecularFormula = molecularFormula;
    return this;
  }

   /**
   * Molecular formula that has been annotated to this spectrum
   * @return molecularFormula
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MOLECULAR_FORMULA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getMolecularFormula() {
    return molecularFormula;
  }


  @JsonProperty(JSON_PROPERTY_MOLECULAR_FORMULA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMolecularFormula(String molecularFormula) {
    this.molecularFormula = molecularFormula;
  }

  public SpectrumAnnotation adduct(String adduct) {
    
    this.adduct = adduct;
    return this;
  }

   /**
   * Adduct that has been annotated to this spectrum
   * @return adduct
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ADDUCT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getAdduct() {
    return adduct;
  }


  @JsonProperty(JSON_PROPERTY_ADDUCT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAdduct(String adduct) {
    this.adduct = adduct;
  }

  public SpectrumAnnotation exactMass(Double exactMass) {
    
    this.exactMass = exactMass;
    return this;
  }

   /**
   * Exact mass based on the annotated molecular formula and ionization
   * @return exactMass
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_EXACT_MASS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getExactMass() {
    return exactMass;
  }


  @JsonProperty(JSON_PROPERTY_EXACT_MASS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setExactMass(Double exactMass) {
    this.exactMass = exactMass;
  }

  public SpectrumAnnotation massDeviationMz(Double massDeviationMz) {
    
    this.massDeviationMz = massDeviationMz;
    return this;
  }

   /**
   * Absolute mass deviation of the exact mass to the precursor mass (precursorMz) of this spectrum in mDa
   * @return massDeviationMz
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MASS_DEVIATION_MZ)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getMassDeviationMz() {
    return massDeviationMz;
  }


  @JsonProperty(JSON_PROPERTY_MASS_DEVIATION_MZ)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMassDeviationMz(Double massDeviationMz) {
    this.massDeviationMz = massDeviationMz;
  }

  public SpectrumAnnotation massDeviationPpm(Double massDeviationPpm) {
    
    this.massDeviationPpm = massDeviationPpm;
    return this;
  }

   /**
   * Relative mass deviation of the exact mass to the precursor mass (precursorMz) of this spectrum in ppm
   * @return massDeviationPpm
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MASS_DEVIATION_PPM)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getMassDeviationPpm() {
    return massDeviationPpm;
  }


  @JsonProperty(JSON_PROPERTY_MASS_DEVIATION_PPM)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMassDeviationPpm(Double massDeviationPpm) {
    this.massDeviationPpm = massDeviationPpm;
  }

  public SpectrumAnnotation structureAnnotationSmiles(String structureAnnotationSmiles) {
    
    this.structureAnnotationSmiles = structureAnnotationSmiles;
    return this;
  }

   /**
   * EXPERIMENTAL: This field is experimental and may be changed (or even removed) without notice until it is declared stable.  &lt;p&gt;  Smiles of the structure candidate used to derive substructure peak annotations via epimetheus insilico fragmentation  Substructure highlighting (bond and atom indices) refer to this specific SMILES.  If you standardize or canonicalize this SMILES in any way the indices of substructure highlighting might  not match correctly anymore.  &lt;p&gt;  Null if substructure annotation not available or not requested.
   * @return structureAnnotationSmiles
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_STRUCTURE_ANNOTATION_SMILES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getStructureAnnotationSmiles() {
    return structureAnnotationSmiles;
  }


  @JsonProperty(JSON_PROPERTY_STRUCTURE_ANNOTATION_SMILES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setStructureAnnotationSmiles(String structureAnnotationSmiles) {
    this.structureAnnotationSmiles = structureAnnotationSmiles;
  }

  public SpectrumAnnotation structureAnnotationName(String structureAnnotationName) {
    
    this.structureAnnotationName = structureAnnotationName;
    return this;
  }

   /**
   * EXPERIMENTAL: This field is experimental and may be changed (or even removed) without notice until it is declared stable.  &lt;p&gt;  Name of the structure candidate used to derive substructure peak annotations via epimetheus insilico fragmentation.  &lt;p&gt;  Null if substructure annotation not available or not requested.
   * @return structureAnnotationName
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_STRUCTURE_ANNOTATION_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getStructureAnnotationName() {
    return structureAnnotationName;
  }


  @JsonProperty(JSON_PROPERTY_STRUCTURE_ANNOTATION_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setStructureAnnotationName(String structureAnnotationName) {
    this.structureAnnotationName = structureAnnotationName;
  }

  public SpectrumAnnotation structureAnnotationSvg(String structureAnnotationSvg) {
    
    this.structureAnnotationSvg = structureAnnotationSvg;
    return this;
  }

   /**
   * EXPERIMENTAL: This field is experimental and may be changed (or even removed) without notice until it is declared stable.  &lt;p&gt;  SVG graphics of the structure candidate used to derive substructure peak annotations via epimetheus insilico fragmentation  Substructure highlighting (bond and atom indices) refers to this SVG.  &lt;p&gt;  Null if substructure annotation not available or not requested.
   * @return structureAnnotationSvg
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_STRUCTURE_ANNOTATION_SVG)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getStructureAnnotationSvg() {
    return structureAnnotationSvg;
  }


  @JsonProperty(JSON_PROPERTY_STRUCTURE_ANNOTATION_SVG)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setStructureAnnotationSvg(String structureAnnotationSvg) {
    this.structureAnnotationSvg = structureAnnotationSvg;
  }

  public SpectrumAnnotation structureAnnotationScore(Double structureAnnotationScore) {
    
    this.structureAnnotationScore = structureAnnotationScore;
    return this;
  }

   /**
   * EXPERIMENTAL: This field is experimental and may be changed (or even removed) without notice until it is declared stable.  &lt;p&gt;  Overall score of all substructure annotations computed for this structure candidate (structureAnnotationSmiles)  &lt;p&gt;  Null if substructure annotation not available or not requested.
   * @return structureAnnotationScore
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_STRUCTURE_ANNOTATION_SCORE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getStructureAnnotationScore() {
    return structureAnnotationScore;
  }


  @JsonProperty(JSON_PROPERTY_STRUCTURE_ANNOTATION_SCORE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setStructureAnnotationScore(Double structureAnnotationScore) {
    this.structureAnnotationScore = structureAnnotationScore;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpectrumAnnotation spectrumAnnotation = (SpectrumAnnotation) o;
    return Objects.equals(this.molecularFormula, spectrumAnnotation.molecularFormula) &&
        Objects.equals(this.adduct, spectrumAnnotation.adduct) &&
        Objects.equals(this.exactMass, spectrumAnnotation.exactMass) &&
        Objects.equals(this.massDeviationMz, spectrumAnnotation.massDeviationMz) &&
        Objects.equals(this.massDeviationPpm, spectrumAnnotation.massDeviationPpm) &&
        Objects.equals(this.structureAnnotationSmiles, spectrumAnnotation.structureAnnotationSmiles) &&
        Objects.equals(this.structureAnnotationName, spectrumAnnotation.structureAnnotationName) &&
        Objects.equals(this.structureAnnotationSvg, spectrumAnnotation.structureAnnotationSvg) &&
        Objects.equals(this.structureAnnotationScore, spectrumAnnotation.structureAnnotationScore);
  }

  @Override
  public int hashCode() {
    return Objects.hash(molecularFormula, adduct, exactMass, massDeviationMz, massDeviationPpm, structureAnnotationSmiles, structureAnnotationName, structureAnnotationSvg, structureAnnotationScore);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SpectrumAnnotation {\n");
    sb.append("    molecularFormula: ").append(toIndentedString(molecularFormula)).append("\n");
    sb.append("    adduct: ").append(toIndentedString(adduct)).append("\n");
    sb.append("    exactMass: ").append(toIndentedString(exactMass)).append("\n");
    sb.append("    massDeviationMz: ").append(toIndentedString(massDeviationMz)).append("\n");
    sb.append("    massDeviationPpm: ").append(toIndentedString(massDeviationPpm)).append("\n");
    sb.append("    structureAnnotationSmiles: ").append(toIndentedString(structureAnnotationSmiles)).append("\n");
    sb.append("    structureAnnotationName: ").append(toIndentedString(structureAnnotationName)).append("\n");
    sb.append("    structureAnnotationSvg: ").append(toIndentedString(structureAnnotationSvg)).append("\n");
    sb.append("    structureAnnotationScore: ").append(toIndentedString(structureAnnotationScore)).append("\n");
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

