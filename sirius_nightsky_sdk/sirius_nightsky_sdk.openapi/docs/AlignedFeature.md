

# AlignedFeature

The AlignedFeature contains the ID of a feature (aligned over runs) together with some read-only information  that might be displayed in some summary view.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**alignedFeatureId** | **String** |  |  [optional] |
|**compoundId** | **String** |  |  [optional] |
|**name** | **String** |  |  [optional] |
|**ionMass** | **Double** |  |  [optional] |
|**charge** | **Integer** |  |  |
|**detectedAdducts** | **Set&lt;String&gt;** |  |  |
|**rtStartSeconds** | **Double** |  |  [optional] |
|**rtEndSeconds** | **Double** |  |  [optional] |
|**quality** | [**QualityEnum**](#QualityEnum) | Quality of this feature. |  [optional] |
|**msData** | [**MsData**](MsData.md) |  |  [optional] |
|**topAnnotations** | [**FeatureAnnotations**](FeatureAnnotations.md) |  |  [optional] |
|**topAnnotationsDeNovo** | [**FeatureAnnotations**](FeatureAnnotations.md) |  |  [optional] |
|**computing** | **Boolean** | Write lock for this feature. If the feature is locked no write operations are possible.  True if any computation is modifying this feature or its results |  [optional] |



## Enum: QualityEnum

| Name | Value |
|---- | -----|
| NOT_APPLICABLE | &quot;NOT_APPLICABLE&quot; |
| LOWEST | &quot;LOWEST&quot; |
| BAD | &quot;BAD&quot; |
| DECENT | &quot;DECENT&quot; |
| GOOD | &quot;GOOD&quot; |



