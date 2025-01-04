

# JobProgress

Progress information of a computation job that has already been submitted to SIRIUS.  if  currentProgress == maxProgress job is finished and should change to state done soon.  if a job is DONE all results can be accessed via the Project-Spaces api.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**indeterminate** | **Boolean** | Is the progress indeterminate or not |  [optional] |
|**state** | **JobState** |  |  [optional] |
|**currentProgress** | **Long** | Current progress value of the job. |  [optional] |
|**maxProgress** | **Long** | Progress value to reach (might also change during execution) |  [optional] |
|**message** | **String** | Progress information and warnings. |  [optional] |
|**errorMessage** | **String** | Error message if the job did not finish successfully failed. |  [optional] |



