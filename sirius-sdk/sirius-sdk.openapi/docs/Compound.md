

# Compound


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**compoundId** | **String** | Unique id of the compound entity. |  [optional] |
|**name** | **String** | Some (optional) human-readable name |  [optional] |
|**rtStartSeconds** | **Double** | The merged/consensus retention time start (earliest rt) of the compound |  [optional] |
|**rtEndSeconds** | **Double** | The merged/consensus retention time end (latest rt) of the compound |  [optional] |
|**neutralMass** | **Double** | Neutral mass of the compound. Ion mass minus the mass of the assigned adduct of each feature of  the compound should result in the same neutral mass |  [optional] |
|**features** | [**List&lt;AlignedFeature&gt;**](AlignedFeature.md) | List of aligned features (adducts) that belong to the compound |  [optional] |
|**consensusAnnotations** | [**ConsensusAnnotationsCSI**](ConsensusAnnotationsCSI.md) |  |  [optional] |
|**consensusAnnotationsDeNovo** | [**ConsensusAnnotationsDeNovo**](ConsensusAnnotationsDeNovo.md) |  |  [optional] |
|**customAnnotations** | [**ConsensusAnnotationsCSI**](ConsensusAnnotationsCSI.md) |  |  [optional] |
|**tags** | [**Map&lt;String, Tag&gt;**](Tag.md) | Key: tagName, value: tag |  [optional] |



