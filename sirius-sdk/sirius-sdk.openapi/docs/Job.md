

# Job

Identifier created by the SIRIUS Nightsky API for a newly created Job.  Object can be enriched with Job status/progress information ({@link JobProgress JobProgress}) and/or Job command information.  This is a return value of the API. So nullable values can also be NOT_REQUIRED to allow for easy removal.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **String** | Unique identifier to access the job via the API |  [optional] |
|**command** | **String** | Command string of the executed Task |  [optional] |
|**progress** | [**JobProgress**](JobProgress.md) |  |  [optional] |
|**affectedCompoundIds** | **List&lt;String&gt;** | List of compoundIds that are affected by this job.  This lis will also contain compoundIds where not all features of the compound are affected by the job.  If this job is creating compounds (e.g. data import jobs) this value will be NULL until the jobs has finished |  [optional] |
|**affectedAlignedFeatureIds** | **List&lt;String&gt;** | List of alignedFeatureIds that are affected by this job.  If this job is creating features (e.g. data import jobs) this value will be NULL until the jobs has finished |  [optional] |
|**jobEffect** | [**JobEffectEnum**](#JobEffectEnum) | Effect this job has the affected ids are added, removed or modified.  Null if job does not affect features/compounds  Not available/null if affected Ids are not requested |  [optional] |



## Enum: JobEffectEnum

| Name | Value |
|---- | -----|
| IMPORT | &quot;IMPORT&quot; |
| COMPUTATION | &quot;COMPUTATION&quot; |
| DELETION | &quot;DELETION&quot; |



