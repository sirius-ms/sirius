

# StructureCandidateFormula


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**inchiKey** | **String** | InChIKey of the structure candidate.  Searching with a full 27 character key matches its 2D (skeleton) part, so stereoisomers of the  searched structure are found alike. |  [optional] |
|**smiles** | **String** |  |  [optional] |
|**structureName** | **String** | Name of the structure candidate.  Searching also resolves the searched term as a PubChem synonym, so a structure is found by any  of its common names. |  [optional] |
|**structureSvg** | **String** | SVG graphics of the structure candidate  OPTIONAL: needs to be added by parameter |  [optional] |
|**dbLinks** | [**List&lt;DBLink&gt;**](DBLink.md) | List of structure database links belonging to the structure candidate  OPTIONAL: needs to be added by parameter |  [optional] |
|**spectralLibraryMatches** | [**List&lt;SpectralLibraryMatch&gt;**](SpectralLibraryMatch.md) | List of spectral library matches belonging to the structure candidate  OPTIONAL: needs to be added by parameter |  [optional] |
|**xlogP** | **Double** |  |  [optional] |
|**rank** | **Integer** | The overall rank of this candidate among all candidates of this feature. |  [optional] |
|**csiScore** | **Double** | CSI:FingerID score of the fingerprint of this compound to the predicted fingerprint of CSI:FingerID  This is the score used for ranking structure candidates |  [optional] |
|**tanimotoSimilarity** | **Double** | Tanimoto similarity of the fingerprint of this compound to the predicted fingerprint of CSI:FingerID |  [optional] |
|**mcesDistToTopHit** | **Double** | Maximum Common Edge Subgraph (MCES) distance to the top scoring hit (CSI:FingerID) in a candidate list. |  [optional] |
|**fingerprint** | [**BinaryFingerprint**](BinaryFingerprint.md) |  |  [optional] |
|**molecularFormula** | **String** | Molecular formula of this candidate |  [optional] |
|**adduct** | **String** | Adduct of this candidate |  [optional] |
|**formulaId** | **String** | Id of the corresponding Formula candidate |  [optional] |



