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
import de.unijena.bioinf.ms.nightsky.sdk.model.Instrument;
import de.unijena.bioinf.ms.nightsky.sdk.model.Timeout;
import de.unijena.bioinf.ms.nightsky.sdk.model.UseHeuristic;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * User/developer friendly parameter subset for the Formula/SIRIUS tool  Can use results from Spectral library search tool.
 */
@JsonPropertyOrder({
  Sirius.JSON_PROPERTY_ENABLED,
  Sirius.JSON_PROPERTY_PROFILE,
  Sirius.JSON_PROPERTY_NUMBER_OF_CANDIDATES,
  Sirius.JSON_PROPERTY_NUMBER_OF_CANDIDATES_PER_IONIZATION,
  Sirius.JSON_PROPERTY_MASS_ACCURACY_M_S2PPM,
  Sirius.JSON_PROPERTY_ISOTOPE_MS2_SETTINGS,
  Sirius.JSON_PROPERTY_FILTER_BY_ISOTOPE_PATTERN,
  Sirius.JSON_PROPERTY_ENFORCE_EL_GORDO_FORMULA,
  Sirius.JSON_PROPERTY_PERFORM_BOTTOM_UP_SEARCH,
  Sirius.JSON_PROPERTY_PERFORM_DENOVO_BELOW_MZ,
  Sirius.JSON_PROPERTY_FORMULA_SEARCH_D_BS,
  Sirius.JSON_PROPERTY_APPLY_FORMULA_CONSTRAINTS_TO_D_B_AND_BOTTOM_UP_SEARCH,
  Sirius.JSON_PROPERTY_ENFORCED_FORMULA_CONSTRAINTS,
  Sirius.JSON_PROPERTY_FALLBACK_FORMULA_CONSTRAINTS,
  Sirius.JSON_PROPERTY_DETECTABLE_ELEMENTS,
  Sirius.JSON_PROPERTY_ILP_TIMEOUT,
  Sirius.JSON_PROPERTY_USE_HEURISTIC,
  Sirius.JSON_PROPERTY_MIN_SCORE_TO_INJECT_SPEC_LIB_MATCH
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class Sirius {
  public static final String JSON_PROPERTY_ENABLED = "enabled";
  private Boolean enabled;

  public static final String JSON_PROPERTY_PROFILE = "profile";
  private Instrument profile;

  public static final String JSON_PROPERTY_NUMBER_OF_CANDIDATES = "numberOfCandidates";
  private Integer numberOfCandidates;

  public static final String JSON_PROPERTY_NUMBER_OF_CANDIDATES_PER_IONIZATION = "numberOfCandidatesPerIonization";
  private Integer numberOfCandidatesPerIonization;

  public static final String JSON_PROPERTY_MASS_ACCURACY_M_S2PPM = "massAccuracyMS2ppm";
  private Double massAccuracyMS2ppm;

  /**
   * Specify how isotope patterns in MS/MS should be handled.  &lt;p&gt;  FILTER: When filtering is enabled, molecular formulas are excluded if their  theoretical isotope pattern does not match the theoretical one, even if their MS/MS pattern has high score.  &lt;p&gt;  SCORE: Use them for SCORING. To use this the instrument should produce clear MS/MS isotope patterns  &lt;p&gt;  IGNORE: Ignore that there might be isotope patterns in MS/MS
   */
  public enum IsotopeMs2SettingsEnum {
    IGNORE("IGNORE"),
    
    FILTER("FILTER"),
    
    SCORE("SCORE");

    private String value;

    IsotopeMs2SettingsEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static IsotopeMs2SettingsEnum fromValue(String value) {
      for (IsotopeMs2SettingsEnum b : IsotopeMs2SettingsEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      return null;
    }
  }

  public static final String JSON_PROPERTY_ISOTOPE_MS2_SETTINGS = "isotopeMs2Settings";
  private IsotopeMs2SettingsEnum isotopeMs2Settings;

  public static final String JSON_PROPERTY_FILTER_BY_ISOTOPE_PATTERN = "filterByIsotopePattern";
  private Boolean filterByIsotopePattern;

  public static final String JSON_PROPERTY_ENFORCE_EL_GORDO_FORMULA = "enforceElGordoFormula";
  private Boolean enforceElGordoFormula;

  public static final String JSON_PROPERTY_PERFORM_BOTTOM_UP_SEARCH = "performBottomUpSearch";
  private Boolean performBottomUpSearch;

  public static final String JSON_PROPERTY_PERFORM_DENOVO_BELOW_MZ = "performDenovoBelowMz";
  private Double performDenovoBelowMz;

  public static final String JSON_PROPERTY_FORMULA_SEARCH_D_BS = "formulaSearchDBs";
  private List<String> formulaSearchDBs;

  public static final String JSON_PROPERTY_APPLY_FORMULA_CONSTRAINTS_TO_D_B_AND_BOTTOM_UP_SEARCH = "applyFormulaConstraintsToDBAndBottomUpSearch";
  private Boolean applyFormulaConstraintsToDBAndBottomUpSearch;

  public static final String JSON_PROPERTY_ENFORCED_FORMULA_CONSTRAINTS = "enforcedFormulaConstraints";
  private String enforcedFormulaConstraints;

  public static final String JSON_PROPERTY_FALLBACK_FORMULA_CONSTRAINTS = "fallbackFormulaConstraints";
  private String fallbackFormulaConstraints;

  public static final String JSON_PROPERTY_DETECTABLE_ELEMENTS = "detectableElements";
  private List<String> detectableElements;

  public static final String JSON_PROPERTY_ILP_TIMEOUT = "ilpTimeout";
  private Timeout ilpTimeout;

  public static final String JSON_PROPERTY_USE_HEURISTIC = "useHeuristic";
  private UseHeuristic useHeuristic;

  public static final String JSON_PROPERTY_MIN_SCORE_TO_INJECT_SPEC_LIB_MATCH = "minScoreToInjectSpecLibMatch";
  private Double minScoreToInjectSpecLibMatch;

  public Sirius() {
  }

  public Sirius enabled(Boolean enabled) {
    
    this.enabled = enabled;
    return this;
  }

   /**
   * tags whether the tool is enabled
   * @return enabled
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENABLED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isEnabled() {
    return enabled;
  }


  @JsonProperty(JSON_PROPERTY_ENABLED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }


  public Sirius profile(Instrument profile) {
    
    this.profile = profile;
    return this;
  }

   /**
   * Get profile
   * @return profile
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PROFILE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Instrument getProfile() {
    return profile;
  }


  @JsonProperty(JSON_PROPERTY_PROFILE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setProfile(Instrument profile) {
    this.profile = profile;
  }


  public Sirius numberOfCandidates(Integer numberOfCandidates) {
    
    this.numberOfCandidates = numberOfCandidates;
    return this;
  }

   /**
   * Number of formula candidates to keep as result list (Formula Candidates).
   * @return numberOfCandidates
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NUMBER_OF_CANDIDATES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getNumberOfCandidates() {
    return numberOfCandidates;
  }


  @JsonProperty(JSON_PROPERTY_NUMBER_OF_CANDIDATES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setNumberOfCandidates(Integer numberOfCandidates) {
    this.numberOfCandidates = numberOfCandidates;
  }


  public Sirius numberOfCandidatesPerIonization(Integer numberOfCandidatesPerIonization) {
    
    this.numberOfCandidatesPerIonization = numberOfCandidatesPerIonization;
    return this;
  }

   /**
   * Use this parameter if you want to force SIRIUS to report at least  NumberOfCandidatesPerIonization results per ionization.  if &lt;&#x3D; 0, this parameter will have no effect and just the top  NumberOfCandidates results will be reported.
   * @return numberOfCandidatesPerIonization
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NUMBER_OF_CANDIDATES_PER_IONIZATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getNumberOfCandidatesPerIonization() {
    return numberOfCandidatesPerIonization;
  }


  @JsonProperty(JSON_PROPERTY_NUMBER_OF_CANDIDATES_PER_IONIZATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setNumberOfCandidatesPerIonization(Integer numberOfCandidatesPerIonization) {
    this.numberOfCandidatesPerIonization = numberOfCandidatesPerIonization;
  }


  public Sirius massAccuracyMS2ppm(Double massAccuracyMS2ppm) {
    
    this.massAccuracyMS2ppm = massAccuracyMS2ppm;
    return this;
  }

   /**
   * Maximum allowed mass deviation. Only molecular formulas within this mass window are considered.
   * @return massAccuracyMS2ppm
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MASS_ACCURACY_M_S2PPM)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getMassAccuracyMS2ppm() {
    return massAccuracyMS2ppm;
  }


  @JsonProperty(JSON_PROPERTY_MASS_ACCURACY_M_S2PPM)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMassAccuracyMS2ppm(Double massAccuracyMS2ppm) {
    this.massAccuracyMS2ppm = massAccuracyMS2ppm;
  }


  public Sirius isotopeMs2Settings(IsotopeMs2SettingsEnum isotopeMs2Settings) {
    
    this.isotopeMs2Settings = isotopeMs2Settings;
    return this;
  }

   /**
   * Specify how isotope patterns in MS/MS should be handled.  &lt;p&gt;  FILTER: When filtering is enabled, molecular formulas are excluded if their  theoretical isotope pattern does not match the theoretical one, even if their MS/MS pattern has high score.  &lt;p&gt;  SCORE: Use them for SCORING. To use this the instrument should produce clear MS/MS isotope patterns  &lt;p&gt;  IGNORE: Ignore that there might be isotope patterns in MS/MS
   * @return isotopeMs2Settings
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ISOTOPE_MS2_SETTINGS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public IsotopeMs2SettingsEnum getIsotopeMs2Settings() {
    return isotopeMs2Settings;
  }


  @JsonProperty(JSON_PROPERTY_ISOTOPE_MS2_SETTINGS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setIsotopeMs2Settings(IsotopeMs2SettingsEnum isotopeMs2Settings) {
    this.isotopeMs2Settings = isotopeMs2Settings;
  }


  public Sirius filterByIsotopePattern(Boolean filterByIsotopePattern) {
    
    this.filterByIsotopePattern = filterByIsotopePattern;
    return this;
  }

   /**
   * When filtering is enabled, molecular formulas are excluded if their theoretical isotope pattern does not match the theoretical one, even if their MS/MS pattern has high score.
   * @return filterByIsotopePattern
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_FILTER_BY_ISOTOPE_PATTERN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isFilterByIsotopePattern() {
    return filterByIsotopePattern;
  }


  @JsonProperty(JSON_PROPERTY_FILTER_BY_ISOTOPE_PATTERN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setFilterByIsotopePattern(Boolean filterByIsotopePattern) {
    this.filterByIsotopePattern = filterByIsotopePattern;
  }


  public Sirius enforceElGordoFormula(Boolean enforceElGordoFormula) {
    
    this.enforceElGordoFormula = enforceElGordoFormula;
    return this;
  }

   /**
   * El Gordo may predict that an MS/MS spectrum is a lipid spectrum. If enabled, the corresponding molecular formula will be enforeced as molecular formula candidate.
   * @return enforceElGordoFormula
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENFORCE_EL_GORDO_FORMULA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isEnforceElGordoFormula() {
    return enforceElGordoFormula;
  }


  @JsonProperty(JSON_PROPERTY_ENFORCE_EL_GORDO_FORMULA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEnforceElGordoFormula(Boolean enforceElGordoFormula) {
    this.enforceElGordoFormula = enforceElGordoFormula;
  }


  public Sirius performBottomUpSearch(Boolean performBottomUpSearch) {
    
    this.performBottomUpSearch = performBottomUpSearch;
    return this;
  }

   /**
   * If true, molecular formula generation via bottom up search is enabled.
   * @return performBottomUpSearch
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PERFORM_BOTTOM_UP_SEARCH)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isPerformBottomUpSearch() {
    return performBottomUpSearch;
  }


  @JsonProperty(JSON_PROPERTY_PERFORM_BOTTOM_UP_SEARCH)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPerformBottomUpSearch(Boolean performBottomUpSearch) {
    this.performBottomUpSearch = performBottomUpSearch;
  }


  public Sirius performDenovoBelowMz(Double performDenovoBelowMz) {
    
    this.performDenovoBelowMz = performDenovoBelowMz;
    return this;
  }

   /**
   * Specifies the m/z below which de novo molecular formula generation is enabled. Set to 0 to disable de novo molecular formula generation.
   * @return performDenovoBelowMz
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PERFORM_DENOVO_BELOW_MZ)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getPerformDenovoBelowMz() {
    return performDenovoBelowMz;
  }


  @JsonProperty(JSON_PROPERTY_PERFORM_DENOVO_BELOW_MZ)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPerformDenovoBelowMz(Double performDenovoBelowMz) {
    this.performDenovoBelowMz = performDenovoBelowMz;
  }


  public Sirius formulaSearchDBs(List<String> formulaSearchDBs) {
    
    this.formulaSearchDBs = formulaSearchDBs;
    return this;
  }

  public Sirius addFormulaSearchDBsItem(String formulaSearchDBsItem) {
    if (this.formulaSearchDBs == null) {
      this.formulaSearchDBs = new ArrayList<>();
    }
    this.formulaSearchDBs.add(formulaSearchDBsItem);
    return this;
  }

   /**
   * List Structure database to extract molecular formulas from to reduce formula search space.  SIRIUS is quite good at de novo formula annotation, so only enable if you have a good reason.
   * @return formulaSearchDBs
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_FORMULA_SEARCH_D_BS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getFormulaSearchDBs() {
    return formulaSearchDBs;
  }


  @JsonProperty(JSON_PROPERTY_FORMULA_SEARCH_D_BS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setFormulaSearchDBs(List<String> formulaSearchDBs) {
    this.formulaSearchDBs = formulaSearchDBs;
  }


  public Sirius applyFormulaConstraintsToDBAndBottomUpSearch(Boolean applyFormulaConstraintsToDBAndBottomUpSearch) {
    
    this.applyFormulaConstraintsToDBAndBottomUpSearch = applyFormulaConstraintsToDBAndBottomUpSearch;
    return this;
  }

   /**
   * By default, the formula (element) constraints are only applied to de novo molecular formula generation.  If true, the constraints are as well applied to database search and bottom up search.
   * @return applyFormulaConstraintsToDBAndBottomUpSearch
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_APPLY_FORMULA_CONSTRAINTS_TO_D_B_AND_BOTTOM_UP_SEARCH)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isApplyFormulaConstraintsToDBAndBottomUpSearch() {
    return applyFormulaConstraintsToDBAndBottomUpSearch;
  }


  @JsonProperty(JSON_PROPERTY_APPLY_FORMULA_CONSTRAINTS_TO_D_B_AND_BOTTOM_UP_SEARCH)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setApplyFormulaConstraintsToDBAndBottomUpSearch(Boolean applyFormulaConstraintsToDBAndBottomUpSearch) {
    this.applyFormulaConstraintsToDBAndBottomUpSearch = applyFormulaConstraintsToDBAndBottomUpSearch;
  }


  public Sirius enforcedFormulaConstraints(String enforcedFormulaConstraints) {
    
    this.enforcedFormulaConstraints = enforcedFormulaConstraints;
    return this;
  }

   /**
   * These configurations hold the information how to autodetect elements based on the given formula constraints.  Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.  &lt;p&gt;  Enforced: Enforced elements are always considered
   * @return enforcedFormulaConstraints
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENFORCED_FORMULA_CONSTRAINTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getEnforcedFormulaConstraints() {
    return enforcedFormulaConstraints;
  }


  @JsonProperty(JSON_PROPERTY_ENFORCED_FORMULA_CONSTRAINTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEnforcedFormulaConstraints(String enforcedFormulaConstraints) {
    this.enforcedFormulaConstraints = enforcedFormulaConstraints;
  }


  public Sirius fallbackFormulaConstraints(String fallbackFormulaConstraints) {
    
    this.fallbackFormulaConstraints = fallbackFormulaConstraints;
    return this;
  }

   /**
   * These configurations hold the information how to autodetect elements based on the given formula constraints.  Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.  &lt;p&gt;  Fallback: Fallback elements are used, if the auto-detection fails (e.g. no isotope pattern available)
   * @return fallbackFormulaConstraints
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_FALLBACK_FORMULA_CONSTRAINTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getFallbackFormulaConstraints() {
    return fallbackFormulaConstraints;
  }


  @JsonProperty(JSON_PROPERTY_FALLBACK_FORMULA_CONSTRAINTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setFallbackFormulaConstraints(String fallbackFormulaConstraints) {
    this.fallbackFormulaConstraints = fallbackFormulaConstraints;
  }


  public Sirius detectableElements(List<String> detectableElements) {
    
    this.detectableElements = detectableElements;
    return this;
  }

  public Sirius addDetectableElementsItem(String detectableElementsItem) {
    if (this.detectableElements == null) {
      this.detectableElements = new ArrayList<>();
    }
    this.detectableElements.add(detectableElementsItem);
    return this;
  }

   /**
   * These configurations hold the information how to autodetect elements based on the given formula constraints.  Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.  &lt;p&gt;  Detectable: Detectable elements are added to the chemical alphabet, if there are indications for them (e.g. in isotope pattern)
   * @return detectableElements
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DETECTABLE_ELEMENTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getDetectableElements() {
    return detectableElements;
  }


  @JsonProperty(JSON_PROPERTY_DETECTABLE_ELEMENTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDetectableElements(List<String> detectableElements) {
    this.detectableElements = detectableElements;
  }


  public Sirius ilpTimeout(Timeout ilpTimeout) {
    
    this.ilpTimeout = ilpTimeout;
    return this;
  }

   /**
   * Get ilpTimeout
   * @return ilpTimeout
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ILP_TIMEOUT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Timeout getIlpTimeout() {
    return ilpTimeout;
  }


  @JsonProperty(JSON_PROPERTY_ILP_TIMEOUT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setIlpTimeout(Timeout ilpTimeout) {
    this.ilpTimeout = ilpTimeout;
  }


  public Sirius useHeuristic(UseHeuristic useHeuristic) {
    
    this.useHeuristic = useHeuristic;
    return this;
  }

   /**
   * Get useHeuristic
   * @return useHeuristic
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_USE_HEURISTIC)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public UseHeuristic getUseHeuristic() {
    return useHeuristic;
  }


  @JsonProperty(JSON_PROPERTY_USE_HEURISTIC)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setUseHeuristic(UseHeuristic useHeuristic) {
    this.useHeuristic = useHeuristic;
  }


  public Sirius minScoreToInjectSpecLibMatch(Double minScoreToInjectSpecLibMatch) {
    
    this.minScoreToInjectSpecLibMatch = minScoreToInjectSpecLibMatch;
    return this;
  }

   /**
   * Similarity Threshold to inject formula candidates no matter which score/rank they have or which filter settings are applied.  If threshold &gt;&#x3D; 0 formulas candidates with reference spectrum similarity above the threshold will be injected.  If NULL injection is disables.
   * @return minScoreToInjectSpecLibMatch
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MIN_SCORE_TO_INJECT_SPEC_LIB_MATCH)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Double getMinScoreToInjectSpecLibMatch() {
    return minScoreToInjectSpecLibMatch;
  }


  @JsonProperty(JSON_PROPERTY_MIN_SCORE_TO_INJECT_SPEC_LIB_MATCH)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMinScoreToInjectSpecLibMatch(Double minScoreToInjectSpecLibMatch) {
    this.minScoreToInjectSpecLibMatch = minScoreToInjectSpecLibMatch;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Sirius sirius = (Sirius) o;
    return Objects.equals(this.enabled, sirius.enabled) &&
        Objects.equals(this.profile, sirius.profile) &&
        Objects.equals(this.numberOfCandidates, sirius.numberOfCandidates) &&
        Objects.equals(this.numberOfCandidatesPerIonization, sirius.numberOfCandidatesPerIonization) &&
        Objects.equals(this.massAccuracyMS2ppm, sirius.massAccuracyMS2ppm) &&
        Objects.equals(this.isotopeMs2Settings, sirius.isotopeMs2Settings) &&
        Objects.equals(this.filterByIsotopePattern, sirius.filterByIsotopePattern) &&
        Objects.equals(this.enforceElGordoFormula, sirius.enforceElGordoFormula) &&
        Objects.equals(this.performBottomUpSearch, sirius.performBottomUpSearch) &&
        Objects.equals(this.performDenovoBelowMz, sirius.performDenovoBelowMz) &&
        Objects.equals(this.formulaSearchDBs, sirius.formulaSearchDBs) &&
        Objects.equals(this.applyFormulaConstraintsToDBAndBottomUpSearch, sirius.applyFormulaConstraintsToDBAndBottomUpSearch) &&
        Objects.equals(this.enforcedFormulaConstraints, sirius.enforcedFormulaConstraints) &&
        Objects.equals(this.fallbackFormulaConstraints, sirius.fallbackFormulaConstraints) &&
        Objects.equals(this.detectableElements, sirius.detectableElements) &&
        Objects.equals(this.ilpTimeout, sirius.ilpTimeout) &&
        Objects.equals(this.useHeuristic, sirius.useHeuristic) &&
        Objects.equals(this.minScoreToInjectSpecLibMatch, sirius.minScoreToInjectSpecLibMatch);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, profile, numberOfCandidates, numberOfCandidatesPerIonization, massAccuracyMS2ppm, isotopeMs2Settings, filterByIsotopePattern, enforceElGordoFormula, performBottomUpSearch, performDenovoBelowMz, formulaSearchDBs, applyFormulaConstraintsToDBAndBottomUpSearch, enforcedFormulaConstraints, fallbackFormulaConstraints, detectableElements, ilpTimeout, useHeuristic, minScoreToInjectSpecLibMatch);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Sirius {\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    profile: ").append(toIndentedString(profile)).append("\n");
    sb.append("    numberOfCandidates: ").append(toIndentedString(numberOfCandidates)).append("\n");
    sb.append("    numberOfCandidatesPerIonization: ").append(toIndentedString(numberOfCandidatesPerIonization)).append("\n");
    sb.append("    massAccuracyMS2ppm: ").append(toIndentedString(massAccuracyMS2ppm)).append("\n");
    sb.append("    isotopeMs2Settings: ").append(toIndentedString(isotopeMs2Settings)).append("\n");
    sb.append("    filterByIsotopePattern: ").append(toIndentedString(filterByIsotopePattern)).append("\n");
    sb.append("    enforceElGordoFormula: ").append(toIndentedString(enforceElGordoFormula)).append("\n");
    sb.append("    performBottomUpSearch: ").append(toIndentedString(performBottomUpSearch)).append("\n");
    sb.append("    performDenovoBelowMz: ").append(toIndentedString(performDenovoBelowMz)).append("\n");
    sb.append("    formulaSearchDBs: ").append(toIndentedString(formulaSearchDBs)).append("\n");
    sb.append("    applyFormulaConstraintsToDBAndBottomUpSearch: ").append(toIndentedString(applyFormulaConstraintsToDBAndBottomUpSearch)).append("\n");
    sb.append("    enforcedFormulaConstraints: ").append(toIndentedString(enforcedFormulaConstraints)).append("\n");
    sb.append("    fallbackFormulaConstraints: ").append(toIndentedString(fallbackFormulaConstraints)).append("\n");
    sb.append("    detectableElements: ").append(toIndentedString(detectableElements)).append("\n");
    sb.append("    ilpTimeout: ").append(toIndentedString(ilpTimeout)).append("\n");
    sb.append("    useHeuristic: ").append(toIndentedString(useHeuristic)).append("\n");
    sb.append("    minScoreToInjectSpecLibMatch: ").append(toIndentedString(minScoreToInjectSpecLibMatch)).append("\n");
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
