

# StructureCandidateFormula


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**inchiKey** | **String** |  |  [optional] |
|**smiles** | **String** |  |  [optional] |
|**structureName** | **String** |  |  [optional] |
|**xlogP** | **Double** |  |  [optional] |
|**dbLinks** | [**List&lt;DBLink&gt;**](DBLink.md) | List of structure database links belonging to this structure candidate  OPTIONAL: needs to be added by parameter |  [optional] |
|**spectralLibraryMatches** | [**List&lt;SpectralLibraryMatch&gt;**](SpectralLibraryMatch.md) | List of spectral library matches belonging to this structure candidate  OPTIONAL: needs to be added by parameter |  [optional] |
|**csiScore** | **Double** | CSI:FingerID score of the fingerprint of this compound to the predicted fingerprint of CSI:FingerID  This is the score used for ranking structure candidates |  [optional] |
|**tanimotoSimilarity** | **Double** | Tanimoto similarly of the fingerprint of this compound to the predicted fingerprint of CSI:FingerID |  [optional] |
|**confidenceExactMatch** | **Double** | Confidence Score that represents the confidence whether the top hit is correct. |  [optional] |
|**confidenceApproxMatch** | **Double** | Confidence Score that represents the confidence whether the top hit or a very similar hit (estimated by MCES distance) is correct. |  [optional] |
|**mcesDistToTopHit** | **Double** | Maximum Common Edge Subgraph (MCES) distance to the top scoring hit (CSI:FingerID) in a candidate list. |  [optional] |
|**fingerprint** | [**BinaryFingerprint**](BinaryFingerprint.md) |  |  [optional] |
|**molecularFormula** | **String** | Molecular formula of this candidate |  [optional] |
|**adduct** | **String** | Adduct of this candidate |  [optional] |
|**formulaId** | **String** | Id of the corresponding Formula candidate |  [optional] |



