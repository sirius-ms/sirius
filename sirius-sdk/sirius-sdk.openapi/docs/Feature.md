

# Feature

A feature as it has been detected in one single run, in contrast to an AlignedFeature which combines the  features of the same compound over all runs it was detected in.  <p>  It provides the properties that are specific to the run it was detected in, such as its position on the  retention time axis of that run and its quantity within that run.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**featureId** | **String** | Identifier |  [optional] |
|**alignedFeatureId** | **String** | ID of the AlignedFeature this feature belongs to |  [optional] |
|**runId** | **String** | ID of the run this feature belongs to |  [optional] |
|**averageMz** | **Double** | Average m/z over the whole feature |  [optional] |
|**apexMz** | **Double** | m/z at the apex of the feature, the m/z this feature was measured at in its run |  [optional] |
|**rtStartSeconds** | **Double** | Start of the feature on the retention time axis in seconds |  [optional] |
|**rtEndSeconds** | **Double** | End of the feature on the retention time axis in seconds |  [optional] |
|**rtApexSeconds** | **Double** | Apex of the feature on the retention time axis in seconds |  [optional] |
|**rtFwhmSeconds** | **Double** | Full width at half maximum of the feature on the retention time axis in seconds |  [optional] |
|**apexIntensity** | **Double** | Feature quantity measured as the intensity of the apex of the feature |  [optional] |
|**areaUnderCurve** | **Double** | Feature quantity measured as the area under the curve of the whole feature |  [optional] |



