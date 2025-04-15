

# SpectralLibrarySearch

User/developer friendly parameter subset for the Spectral library search tool.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**enabled** | **Boolean** | tags whether the tool is enabled |  [optional] |
|**spectraSearchDBs** | **List&lt;String&gt;** | Structure Databases with Reference spectra to search in.  &lt;p&gt;  Defaults to BIO + Custom Databases. Possible values are available to Database API. |  [optional] |
|**precursorDeviationPpm** | **Double** | Maximum allowed mass deviation in ppm for matching the precursor. If not specified, the same value as for the peaks is used. |  [optional] |
|**minSimilarity** | **Float** | Minimal spectral similarity of a spectral match to be considered a hit. |  [optional] |
|**minNumOfPeaks** | **Integer** | Minimal number of matching peaks of a spectral match to be considered a hit. |  [optional] |
|**enableAnalogueSearch** | **Boolean** | Enable analogue search in addition to the identity spectral library search |  [optional] |
|**minSimilarityAnalogue** | **Float** | Minimal spectral similarity of a spectral match to be considered an analogue hit. |  [optional] |
|**minNumOfPeaksAnalogue** | **Integer** | Minimal number of matching peaks of a spectral match to be considered an analogue hit. |  [optional] |
|**scoring** | **SpectralMatchingType** |  |  [optional] |
|**peakDeviationPpm** | **Double** | NO LONGER SUPPORTED (IGNORED)  Maximum allowed mass deviation in ppm for matching peaks. |  [optional] |



