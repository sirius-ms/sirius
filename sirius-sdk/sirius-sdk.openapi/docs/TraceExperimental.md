

# TraceExperimental

EXPERIMENTAL: This schema is experimental and may be changed (or even removed) without notice until it is declared stable.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **Long** |  |  [optional] |
|**sampleId** | **Long** |  |  [optional] |
|**sampleName** | **String** |  |  [optional] |
|**label** | **String** |  |  [optional] |
|**intensities** | **List&lt;Double&gt;** |  |  [optional] |
|**annotations** | [**List&lt;TraceAnnotationExperimental&gt;**](TraceAnnotationExperimental.md) |  |  [optional] |
|**mz** | **Double** |  |  [optional] |
|**merged** | **Boolean** |  |  [optional] |
|**normalizationFactor** | **Double** | Traces are stored with raw intensity values. The normalization factor maps them to relative intensities,  such that traces from different samples can be compared. |  [optional] |
|**noiseLevel** | **Double** | The noise level is estimated from the median noise in the surrounding scans. It can be used to  calculate signal-to-noise ratios. |  [optional] |



