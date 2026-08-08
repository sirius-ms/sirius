

# QuantTable

Quantification of features or compounds within the runs they have been detected in. Rows refer to the quantified objects, columns to the runs. Values that could not be quantified are NaN.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**quantificationMeasure** | **QuantMeasure** |  |  [optional] |
|**rowType** | **QuantRowType** |  |  [optional] |
|**rowIds** | **List&lt;String&gt;** | Ids of the quantified objects, features or compounds depending on the row type. |  [optional] |
|**columnIds** | **List&lt;String&gt;** | Ids of the runs the objects are quantified in. |  [optional] |
|**columnNames** | **List&lt;String&gt;** |  |  [optional] |
|**values** | **List&lt;List&lt;Double&gt;&gt;** |  |  [optional] |



