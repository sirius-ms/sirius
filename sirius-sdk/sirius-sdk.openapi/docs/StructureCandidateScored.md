

# StructureCandidateScored


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**inchiKey** | **String** |  |  [optional] |
|**smiles** | **String** |  |  [optional] |
|**structureName** | **String** |  |  [optional] |
|**structureSvg** | **String** | SVG graphics of the structure candidate  OPTIONAL: needs to be added by parameter |  [optional] |
|**dbLinks** | [**List&lt;DBLink&gt;**](DBLink.md) | List of structure database links belonging to this structure candidate  OPTIONAL: needs to be added by parameter |  [optional] |
|**spectralLibraryMatches** | [**List&lt;SpectralLibraryMatch&gt;**](SpectralLibraryMatch.md) | List of spectral library matches belonging to this structure candidate  OPTIONAL: needs to be added by parameter |  [optional] |
|**xlogP** | **Double** |  |  [optional] |
|**rank** | **Integer** | the overall rank of this candidate among all candidates of this feature |  [optional] |
|**csiScore** | **Double** | CSI:FingerID score of the fingerprint of this compound to the predicted fingerprint of CSI:FingerID  This is the score used for ranking structure candidates |  [optional] |
|**tanimotoSimilarity** | **Double** | Tanimoto similarly of the fingerprint of this compound to the predicted fingerprint of CSI:FingerID |  [optional] |
|**mcesDistToTopHit** | **Double** | Maximum Common Edge Subgraph (MCES) distance to the top scoring hit (CSI:FingerID) in a candidate list. |  [optional] |
|**fingerprint** | [**BinaryFingerprint**](BinaryFingerprint.md) |  |  [optional] |



