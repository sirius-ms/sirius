

# JobProgress

Progress information of a computation job that has already been submitted to SIRIUS.  if  currentProgress == maxProgress job is finished and should change to state done soon.  if a job is DONE all results can be accessed via the Project-Spaces api.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**indeterminate** | **Boolean** | Is the progress indeterminate or not |  [optional] |
|**state** | [**StateEnum**](#StateEnum) | Current state of the Jobs in the SIRIUS internal Job scheduler           WAITING: Waiting for submission to ExecutorService (e.g. due to dependent jobs)          READY: Ready for submission but not yet enqueued for submission to ExecutorService.          QUEUED: Enqueued for submission to ExecutorService.          SUBMITTED: Submitted and waiting to be executed.          RUNNING: Job is running.          CANCELED: Jobs is finished due to cancellation by user or dependent jobs.          FAILED: Job is finished but failed.          DONE: Job finished successfully. |  [optional] |
|**currentProgress** | **Long** | Current progress value of the job. |  [optional] |
|**maxProgress** | **Long** | Progress value to reach (might also change during execution) |  [optional] |
|**message** | **String** | Progress information and warnings. |  [optional] |
|**errorMessage** | **String** | Error message if the job did not finish successfully failed. |  [optional] |



## Enum: StateEnum

| Name | Value |
|---- | -----|
| WAITING | &quot;WAITING&quot; |
| READY | &quot;READY&quot; |
| QUEUED | &quot;QUEUED&quot; |
| SUBMITTED | &quot;SUBMITTED&quot; |
| RUNNING | &quot;RUNNING&quot; |
| CANCELED | &quot;CANCELED&quot; |
| FAILED | &quot;FAILED&quot; |
| DONE | &quot;DONE&quot; |



