

# AlignedFeature

The AlignedFeature contains the ID of a feature (aligned over runs) together with some read-only information  that might be displayed in some summary view.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**alignedFeatureId** | **String** |  |  [optional] |
|**compoundId** | **String** |  |  [optional] |
|**name** | **String** |  |  [optional] |
|**externalFeatureId** | **String** | Externally provided FeatureId (e.g. by some preprocessing tool).  This FeatureId is NOT used by SIRIUS but is stored to ease mapping information back to the source. |  [optional] |
|**ionMass** | **Double** |  |  [optional] |
|**charge** | **Integer** | Ion mode (charge) this feature has been measured in. |  |
|**detectedAdducts** | **Set&lt;String&gt;** | Adducts of this feature that have been detected during preprocessing. |  |
|**rtStartSeconds** | **Double** |  |  [optional] |
|**rtEndSeconds** | **Double** |  |  [optional] |
|**quality** | **DataQuality** |  |  [optional] |
|**hasMs1** | **Boolean** | If true, the feature has at lease one MS1 spectrum |  [optional] |
|**hasMsMs** | **Boolean** | If true, the feature has at lease one MS/MS spectrum |  [optional] |
|**msData** | [**MsData**](MsData.md) |  |  [optional] |
|**topAnnotations** | [**FeatureAnnotations**](FeatureAnnotations.md) |  |  [optional] |
|**topAnnotationsDeNovo** | [**FeatureAnnotations**](FeatureAnnotations.md) |  |  [optional] |
|**computing** | **Boolean** | Write lock for this feature. If the feature is locked no write operations are possible.  True if any computation is modifying this feature or its results |  [optional] |



