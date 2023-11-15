# JobsApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteJob**](JobsApi.md#deleteJob) | **DELETE** /api/projects/{projectId}/jobs/{jobId} | Delete job. |
| [**deleteJobWithHttpInfo**](JobsApi.md#deleteJobWithHttpInfo) | **DELETE** /api/projects/{projectId}/jobs/{jobId} | Delete job. |
| [**deleteJobConfig**](JobsApi.md#deleteJobConfig) | **DELETE** /api/job-configs/{name} | Delete job configuration with given name. |
| [**deleteJobConfigWithHttpInfo**](JobsApi.md#deleteJobConfigWithHttpInfo) | **DELETE** /api/job-configs/{name} | Delete job configuration with given name. |
| [**getDefaultJobConfig**](JobsApi.md#getDefaultJobConfig) | **GET** /api/default-job-config | Request default job configuration |
| [**getDefaultJobConfigWithHttpInfo**](JobsApi.md#getDefaultJobConfigWithHttpInfo) | **GET** /api/default-job-config | Request default job configuration |
| [**getJob**](JobsApi.md#getJob) | **GET** /api/projects/{projectId}/jobs/{jobId} | Get job information and its current state and progress (if available). |
| [**getJobWithHttpInfo**](JobsApi.md#getJobWithHttpInfo) | **GET** /api/projects/{projectId}/jobs/{jobId} | Get job information and its current state and progress (if available). |
| [**getJobConfig**](JobsApi.md#getJobConfig) | **GET** /api/job-configs/{name} | Request job configuration with given name. |
| [**getJobConfigWithHttpInfo**](JobsApi.md#getJobConfigWithHttpInfo) | **GET** /api/job-configs/{name} | Request job configuration with given name. |
| [**getJobConfigs**](JobsApi.md#getJobConfigs) | **GET** /api/job-configs | Request all available job configurations |
| [**getJobConfigsWithHttpInfo**](JobsApi.md#getJobConfigsWithHttpInfo) | **GET** /api/job-configs | Request all available job configurations |
| [**getJobs**](JobsApi.md#getJobs) | **GET** /api/projects/{projectId}/jobs | Get job information and its current state and progress (if available). |
| [**getJobsWithHttpInfo**](JobsApi.md#getJobsWithHttpInfo) | **GET** /api/projects/{projectId}/jobs | Get job information and its current state and progress (if available). |
| [**postJobConfig**](JobsApi.md#postJobConfig) | **POST** /api/job-configs/{name} | Add new job configuration with given name. |
| [**postJobConfigWithHttpInfo**](JobsApi.md#postJobConfigWithHttpInfo) | **POST** /api/job-configs/{name} | Add new job configuration with given name. |
| [**startImportFromPathJob**](JobsApi.md#startImportFromPathJob) | **POST** /api/{projectId}/jobs/import-from-local-path | Import ms/ms data in given format from local filesystem into the specified project |
| [**startImportFromPathJobWithHttpInfo**](JobsApi.md#startImportFromPathJobWithHttpInfo) | **POST** /api/{projectId}/jobs/import-from-local-path | Import ms/ms data in given format from local filesystem into the specified project |
| [**startImportFromStringJob**](JobsApi.md#startImportFromStringJob) | **POST** /api/{projectId}/jobs/import-from-string | Import ms/ms data from the given format into the specified project-space  Possible formats (ms, mgf, cef, msp, mzML, mzXML) |
| [**startImportFromStringJobWithHttpInfo**](JobsApi.md#startImportFromStringJobWithHttpInfo) | **POST** /api/{projectId}/jobs/import-from-string | Import ms/ms data from the given format into the specified project-space  Possible formats (ms, mgf, cef, msp, mzML, mzXML) |
| [**startJob**](JobsApi.md#startJob) | **POST** /api/projects/{projectId}/jobs | Start computation for given compounds and with given parameters. |
| [**startJobWithHttpInfo**](JobsApi.md#startJobWithHttpInfo) | **POST** /api/projects/{projectId}/jobs | Start computation for given compounds and with given parameters. |
| [**startJobFromConfig**](JobsApi.md#startJobFromConfig) | **POST** /api/projects/{projectId}/jobs/from-config | Start computation for given compounds and with parameters from a stored job-config. |
| [**startJobFromConfigWithHttpInfo**](JobsApi.md#startJobFromConfigWithHttpInfo) | **POST** /api/projects/{projectId}/jobs/from-config | Start computation for given compounds and with parameters from a stored job-config. |



## deleteJob

> void deleteJob(projectId, jobId, cancelIfRunning, awaitDeletion)

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
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
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
| **projectId** | **String**| project-space to run jobs on | |
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

## deleteJobWithHttpInfo

> ApiResponse<Void> deleteJob deleteJobWithHttpInfo(projectId, jobId, cancelIfRunning, awaitDeletion)

Delete job.

Delete job. Specify how to behave for running jobs.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        String jobId = "jobId_example"; // String | of the job to be deleted
        Boolean cancelIfRunning = true; // Boolean | If true job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished.
        Boolean awaitDeletion = true; // Boolean | If true request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished.
        try {
            ApiResponse<Void> response = apiInstance.deleteJobWithHttpInfo(projectId, jobId, cancelIfRunning, awaitDeletion);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#deleteJob");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **projectId** | **String**| project-space to run jobs on | |
| **jobId** | **String**| of the job to be deleted | |
| **cancelIfRunning** | **Boolean**| If true job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished. | [optional] [default to true] |
| **awaitDeletion** | **Boolean**| If true request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished. | [optional] [default to true] |

### Return type


ApiResponse<Void>

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

> void deleteJobConfig(name)

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
        defaultClient.setBasePath("http://localhost:8080");

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

## deleteJobConfigWithHttpInfo

> ApiResponse<Void> deleteJobConfig deleteJobConfigWithHttpInfo(name)

Delete job configuration with given name.

Delete job configuration with given name.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String name = "name_example"; // String | name of the job-config to delete
        try {
            ApiResponse<Void> response = apiInstance.deleteJobConfigWithHttpInfo(name);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#deleteJobConfig");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
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


ApiResponse<Void>

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
        defaultClient.setBasePath("http://localhost:8080");

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

## getDefaultJobConfigWithHttpInfo

> ApiResponse<JobSubmission> getDefaultJobConfig getDefaultJobConfigWithHttpInfo(includeConfigMap)

Request default job configuration

Request default job configuration

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        Boolean includeConfigMap = false; // Boolean | if true, generic configmap with-defaults will be included
        try {
            ApiResponse<JobSubmission> response = apiInstance.getDefaultJobConfigWithHttpInfo(includeConfigMap);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#getDefaultJobConfig");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
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

ApiResponse<[**JobSubmission**](JobSubmission.md)>


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
        defaultClient.setBasePath("http://localhost:8080");

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

## getJobWithHttpInfo

> ApiResponse<Job> getJob getJobWithHttpInfo(projectId, jobId, optFields)

Get job information and its current state and progress (if available).

Get job information and its current state and progress (if available).

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        String jobId = "jobId_example"; // String | of the job to be returned
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            ApiResponse<Job> response = apiInstance.getJobWithHttpInfo(projectId, jobId, optFields);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#getJob");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
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

ApiResponse<[**Job**](Job.md)>


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
        defaultClient.setBasePath("http://localhost:8080");

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

## getJobConfigWithHttpInfo

> ApiResponse<JobSubmission> getJobConfig getJobConfigWithHttpInfo(name, includeConfigMap)

Request job configuration with given name.

Request job configuration with given name.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String name = "name_example"; // String | name of the job-config to return
        Boolean includeConfigMap = false; // Boolean | if true the generic configmap will be part of the output
        try {
            ApiResponse<JobSubmission> response = apiInstance.getJobConfigWithHttpInfo(name, includeConfigMap);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#getJobConfig");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
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

ApiResponse<[**JobSubmission**](JobSubmission.md)>


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

> List<JobSubmission> getJobConfigs(includeConfigMap)

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
        defaultClient.setBasePath("http://localhost:8080");

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

## getJobConfigsWithHttpInfo

> ApiResponse<List<JobSubmission>> getJobConfigs getJobConfigsWithHttpInfo(includeConfigMap)

Request all available job configurations

Request all available job configurations

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        Boolean includeConfigMap = false; // Boolean | if true the generic configmap will be part of the output
        try {
            ApiResponse<List<JobSubmission>> response = apiInstance.getJobConfigsWithHttpInfo(includeConfigMap);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#getJobConfigs");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
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

ApiResponse<[**List&lt;JobSubmission&gt;**](JobSubmission.md)>


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

> PageJob getJobs(projectId, page, size, sort, optFields)

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
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PageJob result = apiInstance.getJobs(projectId, page, size, sort, optFields);
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

## getJobsWithHttpInfo

> ApiResponse<PageJob> getJobs getJobsWithHttpInfo(projectId, page, size, sort, optFields)

Get job information and its current state and progress (if available).

Get job information and its current state and progress (if available).

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            ApiResponse<PageJob> response = apiInstance.getJobsWithHttpInfo(projectId, page, size, sort, optFields);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#getJobs");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
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

ApiResponse<[**PageJob**](PageJob.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## postJobConfig

> String postJobConfig(name, jobSubmission, overrideExisting)

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
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String name = "name_example"; // String | name of the job-config to add
        JobSubmission jobSubmission = new JobSubmission(); // JobSubmission | to add
        Boolean overrideExisting = false; // Boolean | 
        try {
            String result = apiInstance.postJobConfig(name, jobSubmission, overrideExisting);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#postJobConfig");
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

## postJobConfigWithHttpInfo

> ApiResponse<String> postJobConfig postJobConfigWithHttpInfo(name, jobSubmission, overrideExisting)

Add new job configuration with given name.

Add new job configuration with given name.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String name = "name_example"; // String | name of the job-config to add
        JobSubmission jobSubmission = new JobSubmission(); // JobSubmission | to add
        Boolean overrideExisting = false; // Boolean | 
        try {
            ApiResponse<String> response = apiInstance.postJobConfigWithHttpInfo(name, jobSubmission, overrideExisting);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#postJobConfig");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
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

ApiResponse<**String**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: text/plain

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Probably modified name of the config (to ensure filesystem path compatibility). |  -  |


## startImportFromPathJob

> Job startImportFromPathJob(projectId, importLocalFilesSubmission, optFields)

Import ms/ms data in given format from local filesystem into the specified project

Import ms/ms data in given format from local filesystem into the specified project.  The import will run in a background job  Possible formats (ms, mgf, cef, msp, mzML, mzXML, project-space)  &lt;p&gt;

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
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to import into.
        ImportLocalFilesSubmission importLocalFilesSubmission = new ImportLocalFilesSubmission(); // ImportLocalFilesSubmission | configuration of the job that will be submitted
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            Job result = apiInstance.startImportFromPathJob(projectId, importLocalFilesSubmission, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#startImportFromPathJob");
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
| **projectId** | **String**| project-space to import into. | |
| **importLocalFilesSubmission** | [**ImportLocalFilesSubmission**](ImportLocalFilesSubmission.md)| configuration of the job that will be submitted | |
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
| **200** | JobId of background job that imports given run/compounds/features. |  -  |

## startImportFromPathJobWithHttpInfo

> ApiResponse<Job> startImportFromPathJob startImportFromPathJobWithHttpInfo(projectId, importLocalFilesSubmission, optFields)

Import ms/ms data in given format from local filesystem into the specified project

Import ms/ms data in given format from local filesystem into the specified project.  The import will run in a background job  Possible formats (ms, mgf, cef, msp, mzML, mzXML, project-space)  &lt;p&gt;

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to import into.
        ImportLocalFilesSubmission importLocalFilesSubmission = new ImportLocalFilesSubmission(); // ImportLocalFilesSubmission | configuration of the job that will be submitted
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            ApiResponse<Job> response = apiInstance.startImportFromPathJobWithHttpInfo(projectId, importLocalFilesSubmission, optFields);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#startImportFromPathJob");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **projectId** | **String**| project-space to import into. | |
| **importLocalFilesSubmission** | [**ImportLocalFilesSubmission**](ImportLocalFilesSubmission.md)| configuration of the job that will be submitted | |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

ApiResponse<[**Job**](Job.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | JobId of background job that imports given run/compounds/features. |  -  |


## startImportFromStringJob

> Job startImportFromStringJob(projectId, importStringSubmission, optFields)

Import ms/ms data from the given format into the specified project-space  Possible formats (ms, mgf, cef, msp, mzML, mzXML)

Import ms/ms data from the given format into the specified project-space  Possible formats (ms, mgf, cef, msp, mzML, mzXML)

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
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to import into.
        ImportStringSubmission importStringSubmission = new ImportStringSubmission(); // ImportStringSubmission | configuration of the job that will be submitted
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            Job result = apiInstance.startImportFromStringJob(projectId, importStringSubmission, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#startImportFromStringJob");
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
| **projectId** | **String**| project-space to import into. | |
| **importStringSubmission** | [**ImportStringSubmission**](ImportStringSubmission.md)| configuration of the job that will be submitted | |
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
| **200** | CompoundIds of the imported run/compounds/feature. |  -  |

## startImportFromStringJobWithHttpInfo

> ApiResponse<Job> startImportFromStringJob startImportFromStringJobWithHttpInfo(projectId, importStringSubmission, optFields)

Import ms/ms data from the given format into the specified project-space  Possible formats (ms, mgf, cef, msp, mzML, mzXML)

Import ms/ms data from the given format into the specified project-space  Possible formats (ms, mgf, cef, msp, mzML, mzXML)

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to import into.
        ImportStringSubmission importStringSubmission = new ImportStringSubmission(); // ImportStringSubmission | configuration of the job that will be submitted
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            ApiResponse<Job> response = apiInstance.startImportFromStringJobWithHttpInfo(projectId, importStringSubmission, optFields);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#startImportFromStringJob");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **projectId** | **String**| project-space to import into. | |
| **importStringSubmission** | [**ImportStringSubmission**](ImportStringSubmission.md)| configuration of the job that will be submitted | |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

ApiResponse<[**Job**](Job.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | CompoundIds of the imported run/compounds/feature. |  -  |


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
        defaultClient.setBasePath("http://localhost:8080");

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

## startJobWithHttpInfo

> ApiResponse<Job> startJob startJobWithHttpInfo(projectId, jobSubmission, optFields)

Start computation for given compounds and with given parameters.

Start computation for given compounds and with given parameters.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        JobSubmission jobSubmission = new JobSubmission(); // JobSubmission | configuration of the job that will be submitted of the job to be returned
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            ApiResponse<Job> response = apiInstance.startJobWithHttpInfo(projectId, jobSubmission, optFields);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#startJob");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
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

ApiResponse<[**Job**](Job.md)>


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
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        String jobConfigName = "jobConfigName_example"; // String | name if the config to be used
        List<String> requestBody = Arrays.asList(); // List<String> | compound ids to be computed
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
| **requestBody** | [**List&lt;String&gt;**](String.md)| compound ids to be computed | |
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

## startJobFromConfigWithHttpInfo

> ApiResponse<Job> startJobFromConfig startJobFromConfigWithHttpInfo(projectId, jobConfigName, requestBody, recompute, optFields)

Start computation for given compounds and with parameters from a stored job-config.

Start computation for given compounds and with parameters from a stored job-config.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.JobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        JobsApi apiInstance = new JobsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to run jobs on
        String jobConfigName = "jobConfigName_example"; // String | name if the config to be used
        List<String> requestBody = Arrays.asList(); // List<String> | compound ids to be computed
        Boolean recompute = true; // Boolean | enable or disable recompute. If null the stored value will be used.
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            ApiResponse<Job> response = apiInstance.startJobFromConfigWithHttpInfo(projectId, jobConfigName, requestBody, recompute, optFields);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling JobsApi#startJobFromConfig");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
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
| **requestBody** | [**List&lt;String&gt;**](String.md)| compound ids to be computed | |
| **recompute** | **Boolean**| enable or disable recompute. If null the stored value will be used. | [optional] |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

ApiResponse<[**Job**](Job.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Accepted |  -  |

