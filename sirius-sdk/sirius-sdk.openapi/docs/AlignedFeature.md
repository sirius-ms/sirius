

# AlignedFeature

The AlignedFeature contains the ID of a feature (aligned over runs) together with some read-only information  that might be displayed in some summary view.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**alignedFeatureId** | **String** | Unique identifier of the aligned feature within the project. |  [optional] |
|**compoundId** | **String** | Identifier of the compound the feature belongs to.  Features that are different adducts or isotopologues of the same molecule share it. |  [optional] |
|**name** | **String** | Informative, human-readable name of the feature. |  [optional] |
|**externalFeatureId** | **String** | Externally provided FeatureId (e.g. by some preprocessing tool).  This FeatureId is NOT used by SIRIUS but is stored to ease mapping information back to the source. |  [optional] |
|**ionMass** | **Double** | Mass-to-charge ratio (m/z) of the precursor ion of the feature. |  [optional] |
|**charge** | **Integer** | Ion mode (charge) the feature has been measured in. |  |
|**detectedAdducts** | **Set&lt;String&gt;** | Adducts that have been detected for the feature during preprocessing.  Never empty: if no adduct could be detected, the unknown ion type matching the feature&#39;s  charge ([M+?]+ or [M+?]-) is reported instead, so every feature is filterable by adduct. |  |
|**rtStartSeconds** | **Double** | Start of the retention time range the feature was detected in, in seconds. |  [optional] |
|**rtEndSeconds** | **Double** | End of the retention time range the feature was detected in, in seconds. |  [optional] |
|**rtApexSeconds** | **Double** | Retention time of the intensity apex of the feature, in seconds. |  [optional] |
|**quality** | **DataQuality** |  |  [optional] |
|**hasMs1** | **Boolean** | If true, the feature has at least one MS1 spectrum |  [optional] |
|**hasMsMs** | **Boolean** | If true, the feature has at least one MS/MS spectrum |  [optional] |
|**msData** | [**MsData**](MsData.md) |  |  [optional] |
|**topAnnotations** | [**FeatureAnnotations**](FeatureAnnotations.md) |  |  [optional] |
|**topAnnotationsDeNovo** | [**FeatureAnnotations**](FeatureAnnotations.md) |  |  [optional] |
|**computing** | **Boolean** | Write lock for the feature. If the feature is locked no write operations are possible.  True if any computation is modifying the feature or its results. |  [optional] |
|**computedTools** | [**ComputedSubtools**](ComputedSubtools.md) |  |  [optional] |
|**qualities** | **Map&lt;String, DataQuality&gt;** | Qualities per top level quality category. |  [optional] |
|**tags** | [**Map&lt;String, Tag&gt;**](Tag.md) | Key: tagName, value: tag |  [optional] |



