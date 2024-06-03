

# WorkerInfo


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **Long** |  |  |
|**type** | [**TypeEnum**](#TypeEnum) |  |  |
|**supportedPredictors** | [**List&lt;SupportedPredictorsEnum&gt;**](#List&lt;SupportedPredictorsEnum&gt;) |  |  |
|**version** | **String** |  |  [optional] |
|**host** | **String** |  |  [optional] |
|**prefix** | **String** |  |  [optional] |
|**state** | **Integer** |  |  |
|**alive** | **Long** |  |  |
|**serverTime** | **Long** |  |  |



## Enum: TypeEnum

| Name | Value |
|---- | -----|
| FORMULA_ID | &quot;FORMULA_ID&quot; |
| FINGER_ID | &quot;FINGER_ID&quot; |
| IOKR | &quot;IOKR&quot; |
| CANOPUS | &quot;CANOPUS&quot; |
| COVTREE | &quot;COVTREE&quot; |



## Enum: List&lt;SupportedPredictorsEnum&gt;

| Name | Value |
|---- | -----|
| POSITIVE | &quot;CSI_FINGERID_POSITIVE&quot; |
| NEGATIVE | &quot;CSI_FINGERID_NEGATIVE&quot; |



