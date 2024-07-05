

# MsData

The MsData wraps all spectral input data belonging to a (aligned) feature. All spectra fields are optional.  However, at least one Spectrum field needs to be set to create a valid MsData Object.  The different types of spectra fields can be extended to adapt to other MassSpec measurement techniques not covered yet.  <p>  Each Feature can have:  - One merged MS/MS spectrum (optional)  - One merged MS spectrum (optional)  - many MS/MS spectra (optional)  - many MS spectra (optional)  <p>  Each non-merged spectrum has an index which can be used to access the spectrum.  <p>  In the future we might add some additional information like chromatographic peak or something similar

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**mergedMs1** | [**BasicSpectrum**](BasicSpectrum.md) |  |  [optional] |
|**mergedMs2** | [**BasicSpectrum**](BasicSpectrum.md) |  |  [optional] |
|**ms1Spectra** | [**List&lt;BasicSpectrum&gt;**](BasicSpectrum.md) |  |  [optional] |
|**ms2Spectra** | [**List&lt;BasicSpectrum&gt;**](BasicSpectrum.md) |  |  [optional] |



