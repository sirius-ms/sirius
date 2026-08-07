

# LcmsSubmissionParameters


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**sampleNames** | **List&lt;String&gt;** | Sample names for each input file to link imported results, e.g. QuantTable back to the input data.  If NULL or empty sample names will be derived from the input files.  &lt;p&gt;  The names are matched to the input files by index. Partial lists are allowed: a NULL entry and any  input file without a corresponding entry get their name derived from the input file. Surplus entries  that match no input file are ignored.  &lt;p&gt;  Names must neither be blank nor duplicated, otherwise the import is rejected. |  [optional] |
|**sampleTypes** | **List&lt;String&gt;** | Sample type for each input file to be used to compute fold changes between blank and sample runs  If NULL or empty no fold changes will be computed during preprocessing.  &lt;p&gt;  The types are matched to the input files by index. In contrast to sampleNames either all or no sample  types have to be given: if the number of types does not match the number of input files or if any type  is NULL or blank, the import is rejected. |  [optional] |
|**alignLCMSRuns** | **Boolean** | Specifies whether LC/MS runs should be aligned |  [optional] |
|**noiseIntensity** | **Double** | Noise level under which all peaks are considered to be likely noise. A peak has to be at least 3x noise level  to be picked as feature. Peaks with MS/MS are still picked even though they might be below noise level.  If not specified, the noise intensity is detected automatically from data. We recommend to NOT specify  this parameter, as the automated detection is usually sufficient. |  [optional] |
|**traceMaxMassDeviation** | [**Deviation**](Deviation.md) |  |  [optional] |
|**alignMaxMassDeviation** | [**Deviation**](Deviation.md) |  |  [optional] |
|**alignMaxRetentionTimeDeviation** | **Double** | Maximal allowed retention time error in seconds for aligning features. If not specified, this parameter is estimated from data. |  [optional] |
|**minSNR** | **Double** | Minimum ratio between peak height and noise intensity for detecting features. By default, this value is 3. Features with good MS/MS are always picked independent of their intensity. For picking very low intensive features we recommend a min-snr of 2, but this will increase runtime and storage memory |  [optional] |



