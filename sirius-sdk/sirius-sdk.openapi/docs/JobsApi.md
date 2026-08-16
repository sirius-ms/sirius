# JobsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteJob**](JobsApi.md#deleteJob) | **DELETE** /api/projects/{projectId}/jobs/{jobId} | Delete job. |
| [**deleteJobConfig**](JobsApi.md#deleteJobConfig) | **DELETE** /api/job-configs/{name} | Delete job configuration with given name. |
| [**deleteJobs**](JobsApi.md#deleteJobs) | **DELETE** /api/projects/{projectId}/jobs | Delete ALL jobs. |
| [**getCommand**](JobsApi.md#getCommand) | **POST** /api/job-configs/get-command | Get a CLI command for the given job configuration. |
| [**getDefaultJobConfig**](JobsApi.md#getDefaultJobConfig) | **GET** /api/default-job-config | Request default job configuration |
| [**getJob**](JobsApi.md#getJob) | **GET** /api/projects/{projectId}/jobs/{jobId} | Get job information and its current state and progress (if available). |
| [**getJobConfig**](JobsApi.md#getJobConfig) | **GET** /api/job-configs/{name} | Request job configuration with given name. |
| [**getJobConfigNames**](JobsApi.md#getJobConfigNames) | **GET** /api/job-config-names | [DEPRECATED] Get all (non-default) job configuration names   |
| [**getJobConfigs**](JobsApi.md#getJobConfigs) | **GET** /api/job-configs | Request all available job configurations |
| [**getJobs**](JobsApi.md#getJobs) | **GET** /api/projects/{projectId}/jobs | [DEPRECATED] Get list of all available jobs with information such as current state and progress (if available) |
| [**getJobsPage**](JobsApi.md#getJobsPage) | **GET** /api/projects/{projectId}/jobs/page | Get Page of jobs with information such as current state and progress (if available). |
| [**hasJobs**](JobsApi.md#hasJobs) | **GET** /api/projects/{projectId}/has-jobs |  |
| [**saveJobConfig**](JobsApi.md#saveJobConfig) | **POST** /api/job-configs/{name} | Add new job configuration with given name. |
| [**startCommand**](JobsApi.md#startCommand) | **POST** /api/projects/{projectId}/jobs/run-command | [DEPRECATED] Start computation for given command and input |
| [**startJob**](JobsApi.md#startJob) | **POST** /api/projects/{projectId}/jobs | Start computation for given compounds and with given parameters. |
| [**startJobFromConfig**](JobsApi.md#startJobFromConfig) | **POST** /api/projects/{projectId}/jobs/from-config | Start computation for given compounds and with parameters from a stored job-config. |



## deleteJob

> deleteJob(projectId, jobId, cancelIfRunning, awaitDeletion)

Delete job.

Delete job. Specify how to behave for running jobs.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete job from
        String jobId = "jobId_example"; // String | of the job to be deleted
        Boolean cancelIfRunning = true; // Boolean | If true, job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished.
        Boolean awaitDeletion = true; // Boolean | If true, request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished.
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
| **cancelIfRunning** | **Boolean**| If true, job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished. | [optional] [default to true] |
| **awaitDeletion** | **Boolean**| If true, request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished. | [optional] [default to true] |

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
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## deleteJobConfig

> deleteJobConfig(name)

Delete job configuration with given name.

Delete job configuration with given name.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

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
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## deleteJobs

> deleteJobs(projectId, cancelIfRunning, awaitDeletion)

Delete ALL jobs.

Delete ALL jobs. Specify how to behave for running jobs.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete jobs from
        Boolean cancelIfRunning = true; // Boolean | If true, job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished.
        Boolean awaitDeletion = true; // Boolean | If true, request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished.
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
| **cancelIfRunning** | **Boolean**| If true, job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished. | [optional] [default to true] |
| **awaitDeletion** | **Boolean**| If true, request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished. | [optional] [default to true] |

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
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## getCommand

> List&lt;String&gt; getCommand(jobSubmission)

Get a CLI command for the given job configuration.

Get a CLI command for the given job configuration.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        JobSubmission jobSubmission = new JobSubmission(); // JobSubmission | 
        try {
            List<String> result = apiInstance.getCommand(jobSubmission);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#getCommand");
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
| **jobSubmission** | [**JobSubmission**](JobSubmission.md)|  | |

### Return type

**List&lt;String&gt;**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## getDefaultJobConfig

> JobSubmission getDefaultJobConfig(includeConfigMap, moveParametersToConfigMap, includeCustomDbsForStructureSearch)

Request default job configuration

Request default job configuration

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        Boolean includeConfigMap = false; // Boolean | if true, generic configmap with-defaults will be included
        Boolean moveParametersToConfigMap = false; // Boolean | if true, object-based parameters will be converted to and added to the generic configMap parameters
        Boolean includeCustomDbsForStructureSearch = false; // Boolean | if true, default database selection of structure db search                                            and spectral library search also contains all available custom databases.                                            If no custom databases are selected, spectral library search is disabled by default.
        try {
            JobSubmission result = apiInstance.getDefaultJobConfig(includeConfigMap, moveParametersToConfigMap, includeCustomDbsForStructureSearch);
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
| **moveParametersToConfigMap** | **Boolean**| if true, object-based parameters will be converted to and added to the generic configMap parameters | [optional] [default to false] |
| **includeCustomDbsForStructureSearch** | **Boolean**| if true, default database selection of structure db search                                            and spectral library search also contains all available custom databases.                                            If no custom databases are selected, spectral library search is disabled by default. | [optional] [default to false] |

### Return type

[**JobSubmission**](JobSubmission.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | &lt;code&gt;JobSubmission&lt;/code&gt; with all parameters set to default values. |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## getJob

> Job getJob(projectId, jobId, optFields)

Get job information and its current state and progress (if available).

Get job information and its current state and progress (if available).

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        String jobId = "jobId_example"; // String | id of the job to be returned
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
| **jobId** | **String**| id of the job to be returned | |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**Job**](Job.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## getJobConfig

> StoredJobSubmission getJobConfig(name, moveParametersToConfigMap)

Request job configuration with given name.

Request job configuration with given name.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String name = "name_example"; // String | name of the job-config to return
        Boolean moveParametersToConfigMap = false; // Boolean | if true, object-based parameters will be converted to and added to the generic configMap parameters
        try {
            StoredJobSubmission result = apiInstance.getJobConfig(name, moveParametersToConfigMap);
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
| **moveParametersToConfigMap** | **Boolean**| if true, object-based parameters will be converted to and added to the generic configMap parameters | [optional] [default to false] |

### Return type

[**StoredJobSubmission**](StoredJobSubmission.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | &lt;code&gt;JobSubmission&lt;/code&gt; for given name. |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## getJobConfigNames

> List&lt;String&gt; getJobConfigNames()

[DEPRECATED] Get all (non-default) job configuration names  

[DEPRECATED] Get all (non-default) job configuration names  &lt;p&gt;  [DEPRECATED] Use /job-configs to get all configs with names. This endpoint is based on local file paths and will likely be removed in future versions of this API.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        try {
            List<String> result = apiInstance.getJobConfigNames();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#getJobConfigNames");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters

This endpoint does not need any parameter.

### Return type

**List&lt;String&gt;**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |


## getJobConfigs

> List&lt;StoredJobSubmission&gt; getJobConfigs()

Request all available job configurations

Request all available job configurations

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        try {
            List<StoredJobSubmission> result = apiInstance.getJobConfigs();
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

This endpoint does not need any parameter.

### Return type

[**List&lt;StoredJobSubmission&gt;**](StoredJobSubmission.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | list of available &lt;code&gt;JobSubmission&lt;/code&gt;s |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |


## getJobs

> List&lt;Job&gt; getJobs(projectId, optFields)

[DEPRECATED] Get list of all available jobs with information such as current state and progress (if available)

[DEPRECATED] Get list of all available jobs with information such as current state and progress (if available).  &lt;p&gt;  [DEPRECATED] Use /jobs/page instead. Loading all jobs at once does not scale for long running projects.  This endpoint will be removed in the next major version of this API.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

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
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## getJobsPage

> PagedModelJob getJobsPage(projectId, page, size, sort, optFields)

Get Page of jobs with information such as current state and progress (if available).

Get Page of jobs with information such as current state and progress (if available).

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

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
            PagedModelJob result = apiInstance.getJobsPage(projectId, page, size, sort, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#getJobsPage");
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

[**PagedModelJob**](PagedModelJob.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## hasJobs

> Boolean hasJobs(projectId, includeFinished)



### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

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
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## saveJobConfig

> StoredJobSubmission saveJobConfig(name, jobSubmission, overrideExisting, moveParametersToConfigMap)

Add new job configuration with given name.

Add new job configuration with given name.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String name = "name_example"; // String | name of the job-config to add
        JobSubmission jobSubmission = new JobSubmission(); // JobSubmission | to add
        Boolean overrideExisting = false; // Boolean | 
        Boolean moveParametersToConfigMap = false; // Boolean | if true, object-based parameters will be converted to and added to the generic configMap parameters in the return object
        try {
            StoredJobSubmission result = apiInstance.saveJobConfig(name, jobSubmission, overrideExisting, moveParametersToConfigMap);
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
| **moveParametersToConfigMap** | **Boolean**| if true, object-based parameters will be converted to and added to the generic configMap parameters in the return object | [optional] [default to false] |

### Return type

[**StoredJobSubmission**](StoredJobSubmission.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | StoredJobSubmission that contains the JobSubmission and the probably modified name of the config (to ensure path compatibility). |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## startCommand

> Job startCommand(projectId, commandSubmission, optFields)

[DEPRECATED] Start computation for given command and input

[DEPRECATED] Start computation for given command and input.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.  [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

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
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Job of the command to be executed. |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## startJob

> Job startJob(projectId, jobSubmission, optFields)

Start computation for given compounds and with given parameters.

Start computation for given compounds and with given parameters.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        JobSubmission jobSubmission = new JobSubmission(); // JobSubmission | configuration of the job to be submitted
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
| **jobSubmission** | [**JobSubmission**](JobSubmission.md)| configuration of the job to be submitted | |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**Job**](Job.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Accepted |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## startJobFromConfig

> Job startJobFromConfig(projectId, jobConfigName, requestBody, recompute, optFields)

Start computation for given compounds and with parameters from a stored job-config.

Start computation for given compounds and with parameters from a stored job-config.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        String jobConfigName = "jobConfigName_example"; // String | name of the config to be used
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
| **jobConfigName** | **String**| name of the config to be used | |
| **requestBody** | [**List&lt;String&gt;**](String.md)| List of alignedFeatureIds to be computed | |
| **recompute** | **Boolean**| enable or disable recompute. If null the stored value will be used. | [optional] |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**Job**](Job.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Accepted |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |

