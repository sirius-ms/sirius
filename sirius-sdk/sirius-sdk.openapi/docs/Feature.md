

# Feature

The AlignedFeature contains the ID of a feature (aligned over runs) together with some read-only information  that might be displayed in some summary view.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**featureId** | **String** | Identifier |  [optional] |
|**alignedFeatureId** | **String** | ID of the AlignedFeature this feature belongs to |  [optional] |
|**runId** | **String** | ID of the run this feature belongs to |  [optional] |
|**averageMz** | **Double** | Average m/z over the whole feature |  [optional] |
|**rtStartSeconds** | **Double** | Start of the feature on the retention time axis in seconds |  [optional] |
|**rtEndSeconds** | **Double** | End of the feature on the retention time axis in seconds |  [optional] |
|**rtApexSeconds** | **Double** | Apex of the feature on the retention time axis in seconds |  [optional] |
|**rtFWHM** | **Double** | Full width at half maximum of the feature on the retention time axis in seconds |  [optional] |
|**apexIntensity** | **Double** | Intensity of the apex of the feature |  [optional] |
|**areaUnderCurve** | **Double** | Area under curve of the whole feature |  [optional] |



