

# MsData

The MsData wraps all spectral input data belonging to a feature.   Each Feature has:  - One merged MS/MS spectrum (optional)  - One merged MS spectrum (optional)  - many MS/MS spectra  - many MS spectra   Each non-merged spectrum has an index which can be used to access the spectrum.   In the future we might add some additional information like chromatographic peak or something similar

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**mergedMs1** | [**AnnotatedSpectrum**](AnnotatedSpectrum.md) |  |  [optional] |
|**mergedMs2** | [**AnnotatedSpectrum**](AnnotatedSpectrum.md) |  |  [optional] |
|**ms2Spectra** | [**List&lt;AnnotatedSpectrum&gt;**](AnnotatedSpectrum.md) |  |  [optional] |
|**ms1Spectra** | [**List&lt;AnnotatedSpectrum&gt;**](AnnotatedSpectrum.md) |  |  [optional] |



