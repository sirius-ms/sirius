

# AlignedFeatureQualityExperimental

EXPERIMENTAL: This schema is experimental and may be changed (or even removed) without notice until it is declared stable.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**alignedFeatureId** | **String** | Id of the feature (aligned over runs) this quality information belongs to. |  |
|**overallQuality** | **DataQuality** |  |  |
|**categories** | [**Map&lt;String, Category&gt;**](Category.md) | Contains all pre-computation quality information that belong to  this feature (aligned over runs), such as information about the quality of the peak shape, MS2 spectrum etc., |  |



