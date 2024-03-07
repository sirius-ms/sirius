

# MsData

The MsData wraps all spectral input data belonging to a feature.  <p>  Each Feature has:  - One merged MS/MS spectrum (optional)  - One merged MS spectrum (optional)  - many MS/MS spectra  - many MS spectra  <p>  Each non-merged spectrum has an index which can be used to access the spectrum.  <p>  In the future we might add some additional information like chromatographic peak or something similar

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**mergedMs1** | [**BasicSpectrum**](BasicSpectrum.md) |  |  [optional] |
|**mergedMs2** | [**BasicSpectrum**](BasicSpectrum.md) |  |  [optional] |
|**ms1Spectra** | [**List&lt;BasicSpectrum&gt;**](BasicSpectrum.md) |  |  |
|**ms2Spectra** | [**List&lt;BasicSpectrum&gt;**](BasicSpectrum.md) |  |  |



