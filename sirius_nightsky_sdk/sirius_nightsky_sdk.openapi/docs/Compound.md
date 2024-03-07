

# Compound


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**compoundId** | **String** | uid of this compound Entity |  [optional] |
|**name** | **String** | Some (optional) human-readable name |  [optional] |
|**rtStartSeconds** | **Double** | The merged/consensus retention time start (earliest rt) of this compound |  [optional] |
|**rtEndSeconds** | **Double** | The merged/consensus retention time end (latest rt) of this compound |  [optional] |
|**neutralMass** | **Double** | Neutral mass of this compound. Ion masse minus the mass of the assigned adduct of each feature of  this compound should result in the same neutral mass |  [optional] |
|**features** | [**List&lt;AlignedFeature&gt;**](AlignedFeature.md) | List of aligned features (adducts) that belong to the same (this) compound |  [optional] |
|**consensusAnnotations** | [**ConsensusAnnotationsCSI**](ConsensusAnnotationsCSI.md) |  |  [optional] |
|**consensusAnnotationsDeNovo** | [**ConsensusAnnotationsDeNovo**](ConsensusAnnotationsDeNovo.md) |  |  [optional] |
|**customAnnotations** | [**ConsensusAnnotationsCSI**](ConsensusAnnotationsCSI.md) |  |  [optional] |



