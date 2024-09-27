

# SpectralLibrarySearch

User/developer friendly parameter subset for the Spectral library search tool.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**enabled** | **Boolean** | tags whether the tool is enabled |  [optional] |
|**spectraSearchDBs** | **List&lt;String&gt;** | Structure Databases with Reference spectra to search in.  &lt;p&gt;  Defaults to BIO + Custom Databases. Possible values are available to Database API. |  [optional] |
|**peakDeviationPpm** | **Double** | Maximum allowed mass deviation in ppm for matching peaks. |  [optional] |
|**precursorDeviationPpm** | **Double** | Maximum allowed mass deviation in ppm for matching the precursor. If not specified, the same value as for the peaks is used. |  [optional] |
|**scoring** | **SpectralMatchingType** |  |  [optional] |



