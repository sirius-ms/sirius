

# AnnotatedSpectrum



## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**msLevel** | **Integer** | MS level of the measured spectrum.  Artificial spectra with no msLevel (e.g. Simulated Isotope patterns) use null or zero |  [optional] |
|**collisionEnergy** | **String** | Collision energy used for MS/MS spectra  Null for spectra where collision energy is not applicable |  [optional] |
|**scanNumber** | **Integer** | Scan number of the spectrum.  Might be null for artificial spectra with no scan number (e.g. Simulated Isotope patterns or merged spectra) |  [optional] |
|**peaks** | [**List&lt;AnnotatedPeak&gt;**](AnnotatedPeak.md) | The peaks of this spectrum which might contain additional annotations such as molecular formulas. |  |
|**empty** | **Boolean** |  |  [optional] |



