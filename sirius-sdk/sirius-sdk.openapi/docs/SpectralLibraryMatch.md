

# SpectralLibraryMatch


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**specMatchId** | **String** |  |  [optional] |
|**rank** | **Integer** |  |  [optional] |
|**similarity** | **Double** | Similarity between query and reference spectrum |  |
|**sharedPeaks** | **Integer** | Number of shared/matched peaks |  [optional] |
|**sharedPeakMapping** | [**List&lt;PeakPair&gt;**](PeakPair.md) | List of paired/matched peak indices.   Maps indices of peaks from the query spectrum (mass sorted)  to indices of matched peaks in the reference spectrum (mass sorted) |  [optional] |
|**querySpectrumIndex** | **Integer** |  |  |
|**dbName** | **String** |  |  [optional] |
|**dbId** | **String** |  |  [optional] |
|**uuid** | **Long** |  |  |
|**splash** | **String** |  |  [optional] |
|**molecularFormula** | **String** |  |  [optional] |
|**adduct** | **String** |  |  [optional] |
|**exactMass** | **Double** |  |  [optional] |
|**smiles** | **String** |  |  [optional] |
|**type** | **SpectralMatchType** |  |  [optional] |
|**inchiKey** | **String** |  |  |
|**referenceSpectrumType** | **SpectrumType** |  |  [optional] |
|**referenceSpectrum** | [**BasicSpectrum**](BasicSpectrum.md) |  |  [optional] |
|**querySpectrumType** | **SpectrumType** |  |  [optional] |



