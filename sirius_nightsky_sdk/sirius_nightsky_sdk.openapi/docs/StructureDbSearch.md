

# StructureDbSearch

User/developer friendly parameter subset for the CSI:FingerID structure db search tool.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**enabled** | **Boolean** | tags whether the tool is enabled |  [optional] |
|**structureSearchDBs** | [**List&lt;StructureSearchDBsEnum&gt;**](#List&lt;StructureSearchDBsEnum&gt;) | Structure databases to search in |  [optional] |
|**tagLipids** | **Boolean** | Candidates matching the lipid class estimated by El Gordo will be tagged.  The lipid class will only be available if El Gordo predicts that the MS/MS is a lipid spectrum.  If this parameter is set to &#39;false&#39; El Gordo will still be executed and e.g. improve the fragmentation  tree, but the matching structure candidates will not be tagged if they match lipid class. |  [optional] |



## Enum: List&lt;StructureSearchDBsEnum&gt;

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



