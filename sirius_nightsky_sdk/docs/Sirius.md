

# Sirius

User/developer friendly parameter subset for the Formula/SIRIUS tool

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**enabled** | **Boolean** | tags whether the tool is enabled |  [optional] |
|**profile** | **Instrument** |  |  [optional] |
|**numberOfCandidates** | **Integer** | Number of formula candidates to keep as result list (Formula Candidates). |  [optional] |
|**numberOfCandidatesPerIon** | **Integer** | Use this parameter if you want to force SIRIUS to report at least  NumberOfCandidatesPerIon results per ionization.  if &lt;&#x3D; 0, this parameter will have no effect and just the top  NumberOfCandidates results will be reported. |  [optional] |
|**massAccuracyMS2ppm** | **Double** | Maximum allowed mass accuracy. Only molecular formulas within this mass window are considered. |  [optional] |
|**isotopeMs2Settings** | [**IsotopeMs2SettingsEnum**](#IsotopeMs2SettingsEnum) | Specify how isotope patterns in MS/MS should be handled.  &lt;p&gt;  FILTER: When filtering is enabled, molecular formulas are excluded if their  theoretical isotope pattern does not match the theoretical one, even if their MS/MS pattern has high score.  &lt;p&gt;  SCORE: Use them for SCORING. To use this the instrument should produce clear MS/MS isotope patterns  &lt;p&gt;  IGNORE: Ignore that there might be isotope patterns in MS/MS |  [optional] |
|**formulaSearchDBs** | [**List&lt;FormulaSearchDBsEnum&gt;**](#List&lt;FormulaSearchDBsEnum&gt;) | List Structure database to extract molecular formulas from to reduce formula search space.  SIRIUS is quite good at de novo formula annotation, so only enable if you have a good reason. |  [optional] |
|**enforcedFormulaConstraints** | **String** | These configurations hold the information how to autodetect elements based on the given formula constraints.  Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.  &lt;p&gt;  Enforced: Enforced elements are always considered |  [optional] |
|**fallbackFormulaConstraints** | **String** | These configurations hold the information how to autodetect elements based on the given formula constraints.  Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.  &lt;p&gt;  Fallback: Fallback elements are used, if the auto-detection fails (e.g. no isotope pattern available) |  [optional] |
|**detectableElements** | **List&lt;String&gt;** | These configurations hold the information how to autodetect elements based on the given formula constraints.  Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.  &lt;p&gt;  Detectable: Detectable elements are added to the chemical alphabet, if there are indications for them (e.g. in isotope pattern) |  [optional] |
|**ilpTimeout** | [**Timeout**](Timeout.md) |  |  [optional] |
|**useHeuristic** | [**UseHeuristic**](UseHeuristic.md) |  |  [optional] |



## Enum: IsotopeMs2SettingsEnum

| Name | Value |
|---- | -----|
| IGNORE | &quot;IGNORE&quot; |
| FILTER | &quot;FILTER&quot; |
| SCORE | &quot;SCORE&quot; |



## Enum: List&lt;FormulaSearchDBsEnum&gt;

| Name | Value |
|---- | -----|
| ALL | &quot;ALL&quot; |
| ALL_BUT_INSILICO | &quot;ALL_BUT_INSILICO&quot; |
| BIO | &quot;BIO&quot; |
| PUBCHEM | &quot;PUBCHEM&quot; |
| MESH | &quot;MESH&quot; |
| HMDB | &quot;HMDB&quot; |
| KNAPSACK | &quot;KNAPSACK&quot; |
| CHEBI | &quot;CHEBI&quot; |
| PUBMED | &quot;PUBMED&quot; |
| KEGG | &quot;KEGG&quot; |
| HSDB | &quot;HSDB&quot; |
| MACONDA | &quot;MACONDA&quot; |
| METACYC | &quot;METACYC&quot; |
| GNPS | &quot;GNPS&quot; |
| ZINCBIO | &quot;ZINCBIO&quot; |
| TRAIN | &quot;TRAIN&quot; |
| YMDB | &quot;YMDB&quot; |
| PLANTCYC | &quot;PLANTCYC&quot; |
| NORMAN | &quot;NORMAN&quot; |
| SUPERNATURAL | &quot;SUPERNATURAL&quot; |
| COCONUT | &quot;COCONUT&quot; |
| BLOODEXPOSOME | &quot;BloodExposome&quot; |
| TEROMOL | &quot;TeroMol&quot; |
| PUBCHEMANNOTATIONBIO | &quot;PUBCHEMANNOTATIONBIO&quot; |
| PUBCHEMANNOTATIONDRUG | &quot;PUBCHEMANNOTATIONDRUG&quot; |
| PUBCHEMANNOTATIONSAFETYANDTOXIC | &quot;PUBCHEMANNOTATIONSAFETYANDTOXIC&quot; |
| PUBCHEMANNOTATIONFOOD | &quot;PUBCHEMANNOTATIONFOOD&quot; |
| LOTUS | &quot;LOTUS&quot; |
| FOODB | &quot;FooDB&quot; |
| MIMEDB | &quot;MiMeDB&quot; |
| LIPIDMAPS | &quot;LIPIDMAPS&quot; |
| LIPID | &quot;LIPID&quot; |
| KEGGMINE | &quot;KEGGMINE&quot; |
| ECOCYCMINE | &quot;ECOCYCMINE&quot; |
| YMDBMINE | &quot;YMDBMINE&quot; |
| MASSBANK | &quot;MASSBANK&quot; |
| DSSTOX | &quot;DSSTox&quot; |



