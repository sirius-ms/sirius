# JobsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteJob**](JobsApi.md#deleteJob) | **DELETE** /api/projects/{projectId}/jobs/{jobId} | Delete job. |
| [**deleteJobConfig**](JobsApi.md#deleteJobConfig) | **DELETE** /api/job-configs/{name} | Delete job configuration with given name. |
| [**deleteJobs**](JobsApi.md#deleteJobs) | **DELETE** /api/projects/{projectId}/jobs | * Delete ALL jobs. |
| [**getDefaultJobConfig**](JobsApi.md#getDefaultJobConfig) | **GET** /api/default-job-config | Request default job configuration |
| [**getJob**](JobsApi.md#getJob) | **GET** /api/projects/{projectId}/jobs/{jobId} | Get job information and its current state and progress (if available). |
| [**getJobConfig**](JobsApi.md#getJobConfig) | **GET** /api/job-configs/{name} | Request job configuration with given name. |
| [**getJobConfigs**](JobsApi.md#getJobConfigs) | **GET** /api/job-configs | Request all available job configurations |
| [**getJobs**](JobsApi.md#getJobs) | **GET** /api/projects/{projectId}/jobs | Get List of all available jobs with information such as current state and progress (if available). |
| [**getJobsPaged**](JobsApi.md#getJobsPaged) | **GET** /api/projects/{projectId}/jobs/page | Get Page of jobs with information such as current state and progress (if available). |
| [**hasJobs**](JobsApi.md#hasJobs) | **GET** /api/projects/{projectId}/has-jobs |  |
| [**saveJobConfig**](JobsApi.md#saveJobConfig) | **POST** /api/job-configs/{name} | Add new job configuration with given name. |
| [**startCommand**](JobsApi.md#startCommand) | **POST** /api/projects/{projectId}/jobs/run-command | Start computation for given command and input. |
| [**startJob**](JobsApi.md#startJob) | **POST** /api/projects/{projectId}/jobs | Start computation for given compounds and with given parameters. |
| [**startJobFromConfig**](JobsApi.md#startJobFromConfig) | **POST** /api/projects/{projectId}/jobs/from-config | Start computation for given compounds and with parameters from a stored job-config. |



## deleteJob

> deleteJob(projectId, jobId, cancelIfRunning, awaitDeletion)

Delete job.

Delete job. Specify how to behave for running jobs.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete job from
        String jobId = "jobId_example"; // String | of the job to be deleted
        Boolean cancelIfRunning = true; // Boolean | If true job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished.
        Boolean awaitDeletion = true; // Boolean | If true request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished.
        try {
            apiInstance.deleteJob(projectId, jobId, cancelIfRunning, awaitDeletion);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#deleteJob");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **projectId** | **String**| project-space to delete job from | |
| **jobId** | **String**| of the job to be deleted | |
| **cancelIfRunning** | **Boolean**| If true job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished. | [optional] [default to true] |
| **awaitDeletion** | **Boolean**| If true request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished. | [optional] [default to true] |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Accepted |  -  |


## deleteJobConfig

> deleteJobConfig(name)

Delete job configuration with given name.

Delete job configuration with given name.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String name = "name_example"; // String | name of the job-config to delete
        try {
            apiInstance.deleteJobConfig(name);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#deleteJobConfig");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **name** | **String**| name of the job-config to delete | |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Accepted |  -  |


## deleteJobs

> deleteJobs(projectId, cancelIfRunning, awaitDeletion)

* Delete ALL jobs.

* Delete ALL jobs. Specify how to behave for running jobs.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete jobs from
        Boolean cancelIfRunning = true; // Boolean | If true job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished.
        Boolean awaitDeletion = true; // Boolean | If true request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished.
        try {
            apiInstance.deleteJobs(projectId, cancelIfRunning, awaitDeletion);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#deleteJobs");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **projectId** | **String**| project-space to delete jobs from | |
| **cancelIfRunning** | **Boolean**| If true job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished. | [optional] [default to true] |
| **awaitDeletion** | **Boolean**| If true request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished. | [optional] [default to true] |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Accepted |  -  |


## getDefaultJobConfig

> JobSubmission getDefaultJobConfig(includeConfigMap)

Request default job configuration

Request default job configuration

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        Boolean includeConfigMap = false; // Boolean | if true, generic configmap with-defaults will be included
        try {
            JobSubmission result = apiInstance.getDefaultJobConfig(includeConfigMap);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#getDefaultJobConfig");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **includeConfigMap** | **Boolean**| if true, generic configmap with-defaults will be included | [optional] [default to false] |

### Return type

[**JobSubmission**](JobSubmission.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | {@link JobSubmission JobSubmission} with all parameters set to default values. |  -  |


## getJob

> Job getJob(projectId, jobId, optFields)

Get job information and its current state and progress (if available).

Get job information and its current state and progress (if available).

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        String jobId = "jobId_example"; // String | of the job to be returned
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            Job result = apiInstance.getJob(projectId, jobId, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#getJob");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **projectId** | **String**| project-space to run jobs on | |
| **jobId** | **String**| of the job to be returned | |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**Job**](Job.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getJobConfig

> JobSubmission getJobConfig(name, includeConfigMap)

Request job configuration with given name.

Request job configuration with given name.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String name = "name_example"; // String | name of the job-config to return
        Boolean includeConfigMap = false; // Boolean | if true the generic configmap will be part of the output
        try {
            JobSubmission result = apiInstance.getJobConfig(name, includeConfigMap);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#getJobConfig");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **name** | **String**| name of the job-config to return | |
| **includeConfigMap** | **Boolean**| if true the generic configmap will be part of the output | [optional] [default to false] |

### Return type

[**JobSubmission**](JobSubmission.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | {@link JobSubmission JobSubmission} for given name. |  -  |


## getJobConfigs

> List&lt;JobSubmission&gt; getJobConfigs(includeConfigMap)

Request all available job configurations

Request all available job configurations

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        Boolean includeConfigMap = false; // Boolean | if true the generic configmap will be part of the output
        try {
            List<JobSubmission> result = apiInstance.getJobConfigs(includeConfigMap);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#getJobConfigs");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **includeConfigMap** | **Boolean**| if true the generic configmap will be part of the output | [optional] [default to false] |

### Return type

[**List&lt;JobSubmission&gt;**](JobSubmission.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | list of available {@link JobSubmission JobSubmission}s |  -  |


## getJobs

> List&lt;Job&gt; getJobs(projectId, optFields)

Get List of all available jobs with information such as current state and progress (if available).

Get List of all available jobs with information such as current state and progress (if available).

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            List<Job> result = apiInstance.getJobs(projectId, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#getJobs");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **projectId** | **String**| project-space to run jobs on | |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**List&lt;Job&gt;**](Job.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getJobsPaged

> PageJob getJobsPaged(projectId, page, size, sort, optFields)

Get Page of jobs with information such as current state and progress (if available).

Get Page of jobs with information such as current state and progress (if available).

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PageJob result = apiInstance.getJobsPaged(projectId, page, size, sort, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#getJobsPaged");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **projectId** | **String**| project-space to run jobs on | |
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**PageJob**](PageJob.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## hasJobs

> Boolean hasJobs(projectId, includeFinished)



### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | 
        Boolean includeFinished = false; // Boolean | 
        try {
            Boolean result = apiInstance.hasJobs(projectId, includeFinished);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#hasJobs");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **projectId** | **String**|  | |
| **includeFinished** | **Boolean**|  | [optional] [default to false] |

### Return type

**Boolean**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## saveJobConfig

> String saveJobConfig(name, jobSubmission, overrideExisting)

Add new job configuration with given name.

Add new job configuration with given name.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String name = "name_example"; // String | name of the job-config to add
        JobSubmission jobSubmission = new JobSubmission(); // JobSubmission | to add
        Boolean overrideExisting = false; // Boolean | 
        try {
            String result = apiInstance.saveJobConfig(name, jobSubmission, overrideExisting);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#saveJobConfig");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **name** | **String**| name of the job-config to add | |
| **jobSubmission** | [**JobSubmission**](JobSubmission.md)| to add | |
| **overrideExisting** | **Boolean**|  | [optional] [default to false] |

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: text/plain


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Probably modified name of the config (to ensure filesystem path compatibility). |  -  |


## startCommand

> Job startCommand(projectId, commandSubmission, optFields)

Start computation for given command and input.

Start computation for given command and input.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to perform the command for.
        CommandSubmission commandSubmission = new CommandSubmission(); // CommandSubmission | the command and the input to be executed
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            Job result = apiInstance.startCommand(projectId, commandSubmission, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#startCommand");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **projectId** | **String**| project-space to perform the command for. | |
| **commandSubmission** | [**CommandSubmission**](CommandSubmission.md)| the command and the input to be executed | |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**Job**](Job.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Job of the command to be executed.   DEPRECATED: this endpoint is based on local file paths and will likely be removed in future versions of this API. |  -  |


## startJob

> Job startJob(projectId, jobSubmission, optFields)

Start computation for given compounds and with given parameters.

Start computation for given compounds and with given parameters.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        JobSubmission jobSubmission = new JobSubmission(); // JobSubmission | configuration of the job that will be submitted of the job to be returned
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            Job result = apiInstance.startJob(projectId, jobSubmission, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#startJob");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **projectId** | **String**| project-space to run jobs on | |
| **jobSubmission** | [**JobSubmission**](JobSubmission.md)| configuration of the job that will be submitted of the job to be returned | |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**Job**](Job.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Accepted |  -  |


## startJobFromConfig

> Job startJobFromConfig(projectId, jobConfigName, requestBody, recompute, optFields)

Start computation for given compounds and with parameters from a stored job-config.

Start computation for given compounds and with parameters from a stored job-config.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        String jobConfigName = "jobConfigName_example"; // String | name if the config to be used
        List<String> requestBody = Arrays.asList(); // List<String> | List of alignedFeatureIds to be computed
        Boolean recompute = true; // Boolean | enable or disable recompute. If null the stored value will be used.
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            Job result = apiInstance.startJobFromConfig(projectId, jobConfigName, requestBody, recompute, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#startJobFromConfig");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **projectId** | **String**| project-space to run jobs on | |
| **jobConfigName** | **String**| name if the config to be used | |
| **requestBody** | [**List&lt;String&gt;**](String.md)| List of alignedFeatureIds to be computed | |
| **recompute** | **Boolean**| enable or disable recompute. If null the stored value will be used. | [optional] |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**Job**](Job.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Accepted |  -  |

