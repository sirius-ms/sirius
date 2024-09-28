

# ConnectionError


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**errorType** | [**ErrorTypeEnum**](#ErrorTypeEnum) |  |  |
|**errorKlass** | [**ErrorKlassEnum**](#ErrorKlassEnum) |  |  |
|**siriusErrorCode** | **Integer** |  |  |
|**siriusMessage** | **String** |  |  |
|**serverResponseErrorCode** | **Integer** |  |  [optional] |
|**serverResponseErrorMessage** | **String** |  |  [optional] |
|**error** | **Boolean** |  |  [optional] |
|**warning** | **Boolean** |  |  [optional] |



## Enum: ErrorTypeEnum

| Name | Value |
|---- | -----|
| WARNING | &quot;WARNING&quot; |
| ERROR | &quot;ERROR&quot; |



## Enum: ErrorKlassEnum

| Name | Value |
|---- | -----|
| UNKNOWN | &quot;UNKNOWN&quot; |
| INTERNET | &quot;INTERNET&quot; |
| LOGIN_SERVER | &quot;LOGIN_SERVER&quot; |
| LICENSE_SERVER | &quot;LICENSE_SERVER&quot; |
| TOKEN | &quot;TOKEN&quot; |
| LOGIN | &quot;LOGIN&quot; |
| LICENSE | &quot;LICENSE&quot; |
| TERMS | &quot;TERMS&quot; |
| APP_SERVER | &quot;APP_SERVER&quot; |



