

# Run


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**runId** | **String** | Identifier |  [optional] |
|**name** | **String** | Informative, human-readable name of the run |  [optional] |
|**source** | **String** | Source location |  [optional] |
|**chromatography** | **String** | Chromatography the run was measured with, e.g. &#39;Liquid Chromatography&#39;. |  [optional] |
|**ionization** | **String** | Ionization the run was measured with, named as in the HUPO PSI-MS controlled vocabulary,  e.g. &#39;electrospray ionization&#39;. |  [optional] |
|**fragmentation** | **String** | Fragmentation the run was measured with, named as in the HUPO PSI-MS controlled vocabulary,  e.g. &#39;beam-type collision-induced dissociation&#39;. |  [optional] |
|**massAnalyzers** | **List&lt;String&gt;** | Mass analyzers of the instrument the run was measured on, named as in the HUPO PSI-MS  controlled vocabulary, e.g. &#39;orbitrap&#39;. |  [optional] |
|**tags** | [**Map&lt;String, Tag&gt;**](Tag.md) | Key: tagName, value: tag |  [optional] |



