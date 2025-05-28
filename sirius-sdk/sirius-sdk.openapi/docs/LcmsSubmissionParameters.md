

# LcmsSubmissionParameters


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**alignLCMSRuns** | **Boolean** | Specifies whether LC/MS runs should be aligned |  [optional] |
|**noiseIntensity** | **Double** | Noise level under which all peaks are considered to be likely noise. A peak has to be at least 3x noise level  to be picked as feature. Peaks with MS/MS are still picked even though they might be below noise level.  If not specified, the noise intensity is detected automatically from data. We recommend to NOT specify  this parameter, as the autmated detection is usually sufficient. |  [optional] |
|**ms1MassDeviation** | [**Deviation**](Deviation.md) |  |  [optional] |
|**alignMassDeviation** | [**Deviation**](Deviation.md) |  |  [optional] |
|**alignRetentionTimeError** | **Double** | Maximal allowed retention time error in seconds for aligning features. If not specified, this parameter is estimated from data. |  [optional] |
|**minSNR** | **Double** | Minimum ratio between peak height and noise intensity for detecting features. By default, this value is 3. Features with good MS/MS are always picked independent of their intensity. For picking very low intensive features we recommend a min-snr of 2, but this will increase runtime and storage memory |  [optional] |



