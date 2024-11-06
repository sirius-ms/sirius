

# QuantificationTable


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**quantificationType** | [**QuantificationTypeEnum**](#QuantificationTypeEnum) |  |  [optional] |
|**rowType** | [**RowTypeEnum**](#RowTypeEnum) |  |  [optional] |
|**columnType** | [**ColumnTypeEnum**](#ColumnTypeEnum) |  |  [optional] |
|**rowIds** | **List&lt;Long&gt;** |  |  [optional] |
|**columnIds** | **List&lt;Long&gt;** |  |  [optional] |
|**rowNames** | **List&lt;String&gt;** |  |  [optional] |
|**columnNames** | **List&lt;String&gt;** |  |  [optional] |
|**values** | **List&lt;List&lt;Double&gt;&gt;** |  |  [optional] |



## Enum: QuantificationTypeEnum

| Name | Value |
|---- | -----|
| APEX_HEIGHT | &quot;APEX_HEIGHT&quot; |
| AREA_UNDER_CURVE | &quot;AREA_UNDER_CURVE&quot; |
| APEX_MASS | &quot;APEX_MASS&quot; |
| AVERAGE_MASS | &quot;AVERAGE_MASS&quot; |
| APEX_RT | &quot;APEX_RT&quot; |
| FULL_WIDTH_HALF_MAX | &quot;FULL_WIDTH_HALF_MAX&quot; |



## Enum: RowTypeEnum

| Name | Value |
|---- | -----|
| FEATURES | &quot;FEATURES&quot; |
| COMPOUNDS | &quot;COMPOUNDS&quot; |



## Enum: ColumnTypeEnum

| Name | Value |
|---- | -----|
| SAMPLES | &quot;SAMPLES&quot; |



