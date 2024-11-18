

# CompoundFoldChange


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**leftGroup** | **String** |  |  [optional] |
|**rightGroup** | **String** |  |  [optional] |
|**aggregation** | [**AggregationEnum**](#AggregationEnum) |  |  [optional] |
|**quantification** | [**QuantificationEnum**](#QuantificationEnum) |  |  [optional] |
|**foldChange** | **Double** |  |  [optional] |
|**compoundId** | **String** |  |  [optional] |



## Enum: AggregationEnum

| Name | Value |
|---- | -----|
| AVG | &quot;AVG&quot; |
| MIN | &quot;MIN&quot; |
| MAX | &quot;MAX&quot; |
| MEDIAN | &quot;MEDIAN&quot; |



## Enum: QuantificationEnum

| Name | Value |
|---- | -----|
| APEX_INTENSITY | &quot;APEX_INTENSITY&quot; |
| AREA_UNDER_CURVE | &quot;AREA_UNDER_CURVE&quot; |
| APEX_MASS | &quot;APEX_MASS&quot; |
| AVERAGE_MASS | &quot;AVERAGE_MASS&quot; |
| APEX_RT | &quot;APEX_RT&quot; |
| FULL_WIDTH_HALF_MAX | &quot;FULL_WIDTH_HALF_MAX&quot; |



