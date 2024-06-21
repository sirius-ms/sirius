

# FeatureImport


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**name** | **String** |  |  [optional] |
|**externalFeatureId** | **String** | Externally provided FeatureId (by some preprocessing tool). This FeatureId is NOT used by SIRIUS but is stored to ease mapping information back to the source. |  [optional] |
|**ionMass** | **Double** |  |  |
|**charge** | **Integer** |  |  |
|**detectedAdducts** | **Set&lt;String&gt;** | Detected adducts of this feature. Can be NULL or empty if no adducts are known. |  [optional] |
|**rtStartSeconds** | **Double** |  |  [optional] |
|**rtEndSeconds** | **Double** |  |  [optional] |
|**mergedMs1** | [**BasicSpectrum**](BasicSpectrum.md) |  |  [optional] |
|**ms1Spectra** | [**List&lt;BasicSpectrum&gt;**](BasicSpectrum.md) |  |  |
|**ms2Spectra** | [**List&lt;BasicSpectrum&gt;**](BasicSpectrum.md) |  |  |



