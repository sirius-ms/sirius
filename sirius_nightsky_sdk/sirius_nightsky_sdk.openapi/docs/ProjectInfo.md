

# ProjectInfo


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**projectId** | **String** | a user selected unique name of the project for easy access. |  [optional] |
|**location** | **String** | storage location of the project. |  [optional] |
|**description** | **String** | Description of this project. |  [optional] |
|**compatible** | **Boolean** | Indicates whether computed results (e.g. fingerprints, compounds classes) are compatible with the backend.  If true project is up-to-date and there are no restrictions regarding usage.  If false project is incompatible and therefore \&quot;read only\&quot; until the incompatible results have been removed. See updateProject endpoint for further information  If NULL the information has not been requested. |  [optional] |
|**numOfFeatures** | **Integer** | Number of features (aligned over runs) in this project. If NULL, information has not been requested (See OptField &#39;sizeInformation&#39;). |  [optional] |
|**numOfCompounds** | **Integer** | Number of compounds (group of ion identities) in this project. If NULL, Information has not been requested (See OptField &#39;sizeInformation&#39;) or might be unavailable for this project type. |  [optional] |
|**numOfBytes** | **Long** | Size in Bytes this project consumes on disk If NULL, Information has not been requested (See OptField &#39;sizeInformation&#39;). |  [optional] |



