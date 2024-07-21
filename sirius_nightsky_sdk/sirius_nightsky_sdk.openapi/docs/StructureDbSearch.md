

# StructureDbSearch

User/developer friendly parameter subset for the CSI:FingerID structure db search tool.  Needs results from FingerprintPrediction and Canopus Tool.  Non-Null parameters in this Object well override their equivalent value in the config map.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**enabled** | **Boolean** | tags whether the tool is enabled |  [optional] |
|**structureSearchDBs** | **List&lt;String&gt;** | Structure databases to search in, If expansive search is enabled this DB selection will be expanded to PubChem  if not high confidence hit was found in the selected databases.  &lt;p&gt;  Defaults to BIO + Custom Databases. Possible values are available to Database API. |  [optional] |
|**tagStructuresWithLipidClass** | **Boolean** | Candidates matching the lipid class estimated by El Gordo will be tagged.  The lipid class will only be available if El Gordo predicts that the MS/MS is a lipid spectrum.  If this parameter is set to &#39;false&#39; El Gordo will still be executed and e.g. improve the fragmentation  tree, but the matching structure candidates will not be tagged if they match lipid class. |  [optional] |
|**expansiveSearchConfidenceMode** | **ConfidenceMode** |  |  [optional] |



