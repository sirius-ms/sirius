

# JobProgress

Progress information of a computation job that has already been submitted to SIRIUS.  If currentProgress == maxProgress, the job is finished and should change to state DONE soon.  If a job is DONE, all results can be accessed via the project-space API.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**indeterminate** | **Boolean** | Is the progress indeterminate or not |  [optional] |
|**state** | **JobState** |  |  [optional] |
|**currentProgress** | **Long** | Current progress value of the job. |  [optional] |
|**maxProgress** | **Long** | Progress value to reach (might also change during execution) |  [optional] |
|**message** | **String** | Progress information and warnings. |  [optional] |
|**errorMessage** | **String** | Error message if the job did not finish successfully. |  [optional] |



