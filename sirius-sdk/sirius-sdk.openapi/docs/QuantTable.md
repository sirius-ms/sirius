

# QuantTable

Quantification of features or compounds within the runs they have been detected in. Rows refer to the quantified objects, columns to the runs. Values that could not be quantified are NaN.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**quantificationMeasure** | **QuantMeasure** |  |  [optional] |
|**rowType** | **QuantRowType** |  |  [optional] |
|**rowIds** | **List&lt;String&gt;** | Ids of the quantified objects, features or compounds depending on the row type. |  [optional] |
|**columnIds** | **List&lt;String&gt;** | Ids of the runs the objects are quantified in. |  [optional] |
|**columnNames** | **List&lt;String&gt;** | Names of the runs the objects are quantified in, in the order of columnIds.  &lt;p&gt;  Optional field, only present if requested, since the table can be read by run id alone.  &lt;p&gt;  The name is the one the run carries in the project. It is either the sample name given when the data was  imported, or, if none was given, derived from the measurement itself: the run id inside the file, and only  failing that the file name. A name is therefore not necessarily the name of the file the run came from, and  it is not guaranteed to be unique. Use columnSources to identify the input file, and  columnIds to identify the run. |  [optional] |
|**columnSources** | **List&lt;String&gt;** | Files the runs were imported from, in the order of columnIds, to relate a column back to the input  data. Same value as the source of the corresponding run.  &lt;p&gt;  Optional field, only present if requested, since it is not needed to read the table. |  [optional] |
|**values** | **List&lt;List&lt;Double&gt;&gt;** |  |  [optional] |



