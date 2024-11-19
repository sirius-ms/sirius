

# StatisticsTable


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**statisticsType** | [**StatisticsTypeEnum**](#StatisticsTypeEnum) |  |  [optional] |
|**aggregationType** | [**AggregationTypeEnum**](#AggregationTypeEnum) |  |  [optional] |
|**quantificationType** | [**QuantificationTypeEnum**](#QuantificationTypeEnum) |  |  [optional] |
|**rowType** | [**RowTypeEnum**](#RowTypeEnum) |  |  [optional] |
|**rowIds** | **List&lt;Long&gt;** |  |  [optional] |
|**columnNames** | **List&lt;String&gt;** |  |  [optional] |
|**columnLeftGroups** | **List&lt;String&gt;** |  |  [optional] |
|**columnRightGroups** | **List&lt;String&gt;** |  |  [optional] |
|**values** | **List&lt;List&lt;Double&gt;&gt;** |  |  [optional] |



## Enum: StatisticsTypeEnum

| Name | Value |
|---- | -----|
| FOLD_CHANGE | &quot;FOLD_CHANGE&quot; |



## Enum: AggregationTypeEnum

| Name | Value |
|---- | -----|
| AVG | &quot;AVG&quot; |
| MIN | &quot;MIN&quot; |
| MAX | &quot;MAX&quot; |
| MEDIAN | &quot;MEDIAN&quot; |



## Enum: QuantificationTypeEnum

| Name | Value |
|---- | -----|
| APEX_INTENSITY | &quot;APEX_INTENSITY&quot; |
| AREA_UNDER_CURVE | &quot;AREA_UNDER_CURVE&quot; |



## Enum: RowTypeEnum

| Name | Value |
|---- | -----|
| FEATURES | &quot;FEATURES&quot; |
| COMPOUNDS | &quot;COMPOUNDS&quot; |



