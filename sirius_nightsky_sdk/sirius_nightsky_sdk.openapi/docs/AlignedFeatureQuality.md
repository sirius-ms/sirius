

# AlignedFeatureQuality


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**alignedFeatureId** | **String** | Id of the feature (aligned over runs) this quality information belongs to. |  |
|**overallQuality** | [**OverallQualityEnum**](#OverallQualityEnum) | Overall Quality |  |
|**categories** | [**Map&lt;String, Category&gt;**](Category.md) | Contains all pre-computation quality information that belong to  this feature (aligned over runs), such as information about the quality of the peak shape, MS2 spectrum etc., |  |



## Enum: OverallQualityEnum

| Name | Value |
|---- | -----|
| LOWEST | &quot;LOWEST&quot; |
| LOWEST_WITH_DEPENDENCIES | &quot;LOWEST_WITH_DEPENDENCIES&quot; |
| NOT_APPLICABLE | &quot;NOT_APPLICABLE&quot; |
| BAD | &quot;BAD&quot; |
| DECENT | &quot;DECENT&quot; |
| GOOD | &quot;GOOD&quot; |



