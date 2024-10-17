

# Sirius

User/developer friendly parameter subset for the Formula/SIRIUS tool  Can use results from Spectral library search tool.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**enabled** | **Boolean** | tags whether the tool is enabled |  [optional] |
|**profile** | **InstrumentProfile** |  |  [optional] |
|**numberOfCandidates** | **Integer** | Number of formula candidates to keep as result list (Formula Candidates). |  [optional] |
|**numberOfCandidatesPerIonization** | **Integer** | Use this parameter if you want to force SIRIUS to report at least  NumberOfCandidatesPerIonization results per ionization.  if &lt;&#x3D; 0, this parameter will have no effect and just the top  NumberOfCandidates results will be reported. |  [optional] |
|**massAccuracyMS2ppm** | **Double** | Maximum allowed mass deviation. Only molecular formulas within this mass window are considered. |  [optional] |
|**isotopeMs2Settings** | [**IsotopeMs2SettingsEnum**](#IsotopeMs2SettingsEnum) | Specify how isotope patterns in MS/MS should be handled.  &lt;p&gt;  FILTER: When filtering is enabled, molecular formulas are excluded if their  theoretical isotope pattern does not match the theoretical one, even if their MS/MS pattern has high score.  &lt;p&gt;  SCORE: Use them for SCORING. To use this the instrument should produce clear MS/MS isotope patterns  &lt;p&gt;  IGNORE: Ignore that there might be isotope patterns in MS/MS |  [optional] |
|**filterByIsotopePattern** | **Boolean** | When filtering is enabled, molecular formulas are excluded if their theoretical isotope pattern does not match the theoretical one, even if their MS/MS pattern has high score. |  [optional] |
|**enforceElGordoFormula** | **Boolean** | El Gordo may predict that an MS/MS spectrum is a lipid spectrum. If enabled, the corresponding molecular formula will be enforeced as molecular formula candidate. |  [optional] |
|**performBottomUpSearch** | **Boolean** | If true, molecular formula generation via bottom up search is enabled. |  [optional] |
|**performDenovoBelowMz** | **Double** | Specifies the m/z below which de novo molecular formula generation is enabled. Set to 0 to disable de novo molecular formula generation. |  [optional] |
|**formulaSearchDBs** | **List&lt;String&gt;** | List Structure database to extract molecular formulas from to reduce formula search space.  SIRIUS is quite good at de novo formula annotation, so only enable if you have a good reason. |  [optional] |
|**applyFormulaConstraintsToDBAndBottomUpSearch** | **Boolean** | By default, the formula (element) constraints are only applied to de novo molecular formula generation.  If true, the constraints are as well applied to database search and bottom up search. |  [optional] |
|**enforcedFormulaConstraints** | **String** | These configurations hold the information how to autodetect elements based on the given formula constraints.  Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.  &lt;p&gt;  Enforced: Enforced elements are always considered |  [optional] |
|**fallbackFormulaConstraints** | **String** | These configurations hold the information how to autodetect elements based on the given formula constraints.  Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.  &lt;p&gt;  Fallback: Fallback elements are used, if the auto-detection fails (e.g. no isotope pattern available) |  [optional] |
|**detectableElements** | **List&lt;String&gt;** | These configurations hold the information how to autodetect elements based on the given formula constraints.  Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.  &lt;p&gt;  Detectable: Detectable elements are added to the chemical alphabet, if there are indications for them (e.g. in isotope pattern) |  [optional] |
|**ilpTimeout** | [**Timeout**](Timeout.md) |  |  [optional] |
|**useHeuristic** | [**UseHeuristic**](UseHeuristic.md) |  |  [optional] |
|**injectSpecLibMatchFormulas** | **Boolean** | If true formula candidates that belong to spectral library matches above a certain threshold will  we inject/preserved for further analyses no matter which score they have or which filter is applied |  [optional] |
|**minScoreToInjectSpecLibMatch** | **Double** | Similarity Threshold to inject formula candidates no matter which score/rank they have or which filter settings are applied.  If threshold &gt;&#x3D; 0 formulas candidates with reference spectrum similarity above the threshold will be injected. |  [optional] |
|**minPeaksToInjectSpecLibMatch** | **Integer** | Matching peaks threshold to inject formula candidates no matter which score they have or which filter is applied. |  [optional] |



## Enum: IsotopeMs2SettingsEnum

| Name | Value |
|---- | -----|
| IGNORE | &quot;IGNORE&quot; |
| FILTER | &quot;FILTER&quot; |
| SCORE | &quot;SCORE&quot; |



