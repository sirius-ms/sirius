

# AnnotatedSpectrum

Spectrum model with peak annotations based on the fragmentation tree and Epimetheus substructure annotations.  Molecular formula and adduct of the spectrum are identical to the ones of the corresponding molecular formula candidate and FragmentationTree.  Fragment molecular formulas and adducts correspond to the FragmentationTree's FragmentNodes

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**name** | **String** | Optional Displayable name of this spectrum. |  [optional] |
|**msLevel** | **Integer** | MS level of the measured spectrum.  Artificial spectra with no msLevel (e.g. Simulated Isotope patterns) use null or zero |  [optional] |
|**collisionEnergy** | **String** | Collision energy used for MS/MS spectra  Null for spectra where collision energy is not applicable |  [optional] |
|**instrument** | **String** | Instrument information. |  [optional] |
|**precursorMz** | **Double** | Precursor m/z of the MS/MS spectrum  Null for spectra where precursor m/z is not applicable |  [optional] |
|**scanNumber** | **Integer** | Scan number of the spectrum.  Might be null for artificial spectra with no scan number (e.g. Simulated Isotope patterns or merged spectra) |  [optional] |
|**peaks** | [**List&lt;AnnotatedPeak&gt;**](AnnotatedPeak.md) | The peaks of this spectrum which might contain additional annotations such as molecular formulas. |  |
|**absIntensityFactor** | **Double** | Factor to convert relative intensities to absolute intensities.  Might be null or 1 for spectra where absolute intensities are not available (E.g. artificial or merged spectra)  &lt;p&gt;  DEPRECATED: Spectra are always returned with raw intensities.  Use provided normalization factors to normalize on the fly. |  [optional] |
|**maxNormFactor** | **Double** | Factor to convert absolute intensities to MAX norm. |  [optional] |
|**sumNormFactor** | **Double** | Factor to convert absolute intensities to SUM norm. |  [optional] |
|**l2NormFactor** | **Double** | Factor to convert absolute intensities to L2 (Euclidean) norm. |  [optional] |
|**firstPeakNormFactor** | **Double** | Factor to convert absolute intensities to normalize intensities by first peak intensity. |  [optional] |
|**spectrumAnnotation** | [**SpectrumAnnotation**](SpectrumAnnotation.md) |  |  [optional] |



