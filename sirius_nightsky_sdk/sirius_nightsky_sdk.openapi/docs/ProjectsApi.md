# ProjectsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**closeProjectSpace**](ProjectsApi.md#closeProjectSpace) | **DELETE** /api/projects/{projectId} | Close project-space and remove it from application |
| [**copyProjectSpace**](ProjectsApi.md#copyProjectSpace) | **PUT** /api/projects/{projectId}/copy | Move an existing (opened) project-space to another location. |
| [**createProjectSpace**](ProjectsApi.md#createProjectSpace) | **POST** /api/projects/{projectId} | Create and open a new project-space at given location and make it accessible via the given projectId. |
| [**getCanopusClassyFireData**](ProjectsApi.md#getCanopusClassyFireData) | **GET** /api/projects/{projectId}/cf-data | Get CANOPUS prediction vector definition for ClassyFire classes |
| [**getCanopusNpcData**](ProjectsApi.md#getCanopusNpcData) | **GET** /api/projects/{projectId}/npc-data | Get CANOPUS prediction vector definition for NPC classes |
| [**getFingerIdData**](ProjectsApi.md#getFingerIdData) | **GET** /api/projects/{projectId}/fingerid-data | Get CSI:FingerID fingerprint (prediction vector) definition |
| [**getProjectSpace**](ProjectsApi.md#getProjectSpace) | **GET** /api/projects/{projectId} | Get project space info by its projectId. |
| [**getProjectSpaces**](ProjectsApi.md#getProjectSpaces) | **GET** /api/projects | List opened project spaces. |
| [**importMsRunData**](ProjectsApi.md#importMsRunData) | **POST** /api/projects/{projectId}/import/ms-data-files | Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML) |
| [**importMsRunDataAsJob**](ProjectsApi.md#importMsRunDataAsJob) | **POST** /api/projects/{projectId}/import/ms-data-files-job | Import and Align full MS-Runs from various formats into the specified project as background job. |
| [**importMsRunDataAsJobLocally**](ProjectsApi.md#importMsRunDataAsJobLocally) | **POST** /api/projects/{projectId}/import/ms-data-local-files-job | Import and Align full MS-Runs from various formats into the specified project as background job |
| [**importMsRunDataLocally**](ProjectsApi.md#importMsRunDataLocally) | **POST** /api/projects/{projectId}/import/ms-local-data-files | Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)   |
| [**importPreprocessedData**](ProjectsApi.md#importPreprocessedData) | **POST** /api/projects/{projectId}/import/preprocessed-data-files | Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp) |
| [**importPreprocessedDataAsJob**](ProjectsApi.md#importPreprocessedDataAsJob) | **POST** /api/projects/{projectId}/import/preprocessed-data-files-job | Import ms/ms data from the given format into the specified project-space as background job. |
| [**importPreprocessedDataAsJobLocally**](ProjectsApi.md#importPreprocessedDataAsJobLocally) | **POST** /api/projects/{projectId}/import/preprocessed-local-data-files-job | Import ms/ms data from the given format into the specified project-space as background job |
| [**importPreprocessedDataLocally**](ProjectsApi.md#importPreprocessedDataLocally) | **POST** /api/projects/{projectId}/import/preprocessed-local-data-files | Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)   |
| [**openProjectSpace**](ProjectsApi.md#openProjectSpace) | **PUT** /api/projects/{projectId} | Open an existing project-space and make it accessible via the given projectId. |



## closeProjectSpace

> closeProjectSpace(projectId)

Close project-space and remove it from application

Close project-space and remove it from application. Project will NOT be deleted from disk.  &lt;p&gt;  ATTENTION: This will cancel and remove all jobs running on this Project before closing it.  If there are many jobs, this might take some time.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | unique name/identifier of the  project-space to be closed.
        try {
            apiInstance.closeProjectSpace(projectId);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#closeProjectSpace");
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
| **projectId** | **String**| unique name/identifier of the  project-space to be closed. | |

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
| **200** | OK |  -  |


## copyProjectSpace

> ProjectInfo copyProjectSpace(projectId, pathToCopiedProject, copyProjectId, optFields)

Move an existing (opened) project-space to another location.

Move an existing (opened) project-space to another location.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | unique name/identifier of the project-space that shall be copied.
        String pathToCopiedProject = "pathToCopiedProject_example"; // String | target location where the source project will be copied to.
        String copyProjectId = "copyProjectId_example"; // String | optional id/mame of the newly created project (copy). If given the project will be opened.
        List<ProjectInfoOptField> optFields = Arrays.asList(); // List<ProjectInfoOptField> | 
        try {
            ProjectInfo result = apiInstance.copyProjectSpace(projectId, pathToCopiedProject, copyProjectId, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#copyProjectSpace");
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
| **projectId** | **String**| unique name/identifier of the project-space that shall be copied. | |
| **pathToCopiedProject** | **String**| target location where the source project will be copied to. | |
| **copyProjectId** | **String**| optional id/mame of the newly created project (copy). If given the project will be opened. | [optional] |
| **optFields** | [**List&lt;ProjectInfoOptField&gt;**](ProjectInfoOptField.md)|  | [optional] |

### Return type

[**ProjectInfo**](ProjectInfo.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | ProjectInfo of the newly created project if opened (copyProjectId !&#x3D; null) or the project info of  the source project otherwise  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases. |  -  |


## createProjectSpace

> ProjectInfo createProjectSpace(projectId, pathToProject, optFields)

Create and open a new project-space at given location and make it accessible via the given projectId.

Create and open a new project-space at given location and make it accessible via the given projectId.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | unique name/identifier that shall be used to access the newly created project-space. Must consist only of [a-zA-Z0-9_-].
        String pathToProject = "pathToProject_example"; // String | local file path where the project will be created. If NULL, project will be stored by its projectId in default project location. DEPRECATED: This parameter relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
        List<ProjectInfoOptField> optFields = Arrays.asList(); // List<ProjectInfoOptField> | 
        try {
            ProjectInfo result = apiInstance.createProjectSpace(projectId, pathToProject, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#createProjectSpace");
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
| **projectId** | **String**| unique name/identifier that shall be used to access the newly created project-space. Must consist only of [a-zA-Z0-9_-]. | |
| **pathToProject** | **String**| local file path where the project will be created. If NULL, project will be stored by its projectId in default project location. DEPRECATED: This parameter relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases. | [optional] |
| **optFields** | [**List&lt;ProjectInfoOptField&gt;**](ProjectInfoOptField.md)|  | [optional] |

### Return type

[**ProjectInfo**](ProjectInfo.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getCanopusClassyFireData

> String getCanopusClassyFireData(projectId, charge)

Get CANOPUS prediction vector definition for ClassyFire classes

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | 
        Integer charge = 56; // Integer | 
        try {
            String result = apiInstance.getCanopusClassyFireData(projectId, charge);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#getCanopusClassyFireData");
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
| **charge** | **Integer**|  | |

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/csv


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getCanopusNpcData

> String getCanopusNpcData(projectId, charge)

Get CANOPUS prediction vector definition for NPC classes

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | 
        Integer charge = 56; // Integer | 
        try {
            String result = apiInstance.getCanopusNpcData(projectId, charge);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#getCanopusNpcData");
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
| **charge** | **Integer**|  | |

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/csv


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getFingerIdData

> String getFingerIdData(projectId, charge)

Get CSI:FingerID fingerprint (prediction vector) definition

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | 
        Integer charge = 56; // Integer | 
        try {
            String result = apiInstance.getFingerIdData(projectId, charge);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#getFingerIdData");
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
| **charge** | **Integer**|  | |

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/csv


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getProjectSpace

> ProjectInfo getProjectSpace(projectId, optFields)

Get project space info by its projectId.

Get project space info by its projectId.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | unique name/identifier tof the project-space to be accessed.
        List<ProjectInfoOptField> optFields = Arrays.asList(); // List<ProjectInfoOptField> | 
        try {
            ProjectInfo result = apiInstance.getProjectSpace(projectId, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#getProjectSpace");
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
| **projectId** | **String**| unique name/identifier tof the project-space to be accessed. | |
| **optFields** | [**List&lt;ProjectInfoOptField&gt;**](ProjectInfoOptField.md)|  | [optional] |

### Return type

[**ProjectInfo**](ProjectInfo.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getProjectSpaces

> List&lt;ProjectInfo&gt; getProjectSpaces()

List opened project spaces.

List opened project spaces.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        try {
            List<ProjectInfo> result = apiInstance.getProjectSpaces();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#getProjectSpaces");
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

[**List&lt;ProjectInfo&gt;**](ProjectInfo.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## importMsRunData

> ImportResult importMsRunData(projectId, parameters, allowMs1Only, inputFiles)

Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)

Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | Project-space to import into.
        LcmsSubmissionParameters parameters = new LcmsSubmissionParameters(); // LcmsSubmissionParameters | Parameters for feature alignment and feature finding.
        Boolean allowMs1Only = true; // Boolean | Import data without MS/MS.
        List<File> inputFiles = Arrays.asList(); // List<File> | 
        try {
            ImportResult result = apiInstance.importMsRunData(projectId, parameters, allowMs1Only, inputFiles);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#importMsRunData");
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
| **projectId** | **String**| Project-space to import into. | |
| **parameters** | [**LcmsSubmissionParameters**](.md)| Parameters for feature alignment and feature finding. | |
| **allowMs1Only** | **Boolean**| Import data without MS/MS. | [optional] [default to true] |
| **inputFiles** | **List&lt;File&gt;**|  | [optional] |

### Return type

[**ImportResult**](ImportResult.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## importMsRunDataAsJob

> Job importMsRunDataAsJob(projectId, parameters, allowMs1Only, optFields, inputFiles)

Import and Align full MS-Runs from various formats into the specified project as background job.

Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | Project-space to import into.
        LcmsSubmissionParameters parameters = new LcmsSubmissionParameters(); // LcmsSubmissionParameters | Parameters for feature alignment and feature finding.
        Boolean allowMs1Only = true; // Boolean | Import data without MS/MS.
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | Set of optional fields to be included. Use 'none' only to override defaults.
        List<File> inputFiles = Arrays.asList(); // List<File> | 
        try {
            Job result = apiInstance.importMsRunDataAsJob(projectId, parameters, allowMs1Only, optFields, inputFiles);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#importMsRunDataAsJob");
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
| **projectId** | **String**| Project-space to import into. | |
| **parameters** | [**LcmsSubmissionParameters**](.md)| Parameters for feature alignment and feature finding. | |
| **allowMs1Only** | **Boolean**| Import data without MS/MS. | [optional] [default to true] |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| Set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |
| **inputFiles** | **List&lt;File&gt;**|  | [optional] |

### Return type

[**Job**](Job.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the import job. |  -  |


## importMsRunDataAsJobLocally

> Job importMsRunDataAsJobLocally(projectId, parameters, requestBody, allowMs1Only, optFields)

Import and Align full MS-Runs from various formats into the specified project as background job

Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;ms-data-files-job&#39; instead.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | Project-space to import into.
        LcmsSubmissionParameters parameters = new LcmsSubmissionParameters(); // LcmsSubmissionParameters | Parameters for feature alignment and feature finding.
        List<String> requestBody = Arrays.asList(); // List<String> | 
        Boolean allowMs1Only = true; // Boolean | Import data without MS/MS.
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | Set of optional fields to be included. Use 'none' only to override defaults.
        try {
            Job result = apiInstance.importMsRunDataAsJobLocally(projectId, parameters, requestBody, allowMs1Only, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#importMsRunDataAsJobLocally");
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
| **projectId** | **String**| Project-space to import into. | |
| **parameters** | [**LcmsSubmissionParameters**](.md)| Parameters for feature alignment and feature finding. | |
| **requestBody** | [**List&lt;String&gt;**](String.md)|  | |
| **allowMs1Only** | **Boolean**| Import data without MS/MS. | [optional] [default to true] |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| Set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

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
| **200** | the import job. |  -  |


## importMsRunDataLocally

> ImportResult importMsRunDataLocally(projectId, parameters, requestBody, allowMs1Only)

Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  

Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;ms-data-files&#39; instead.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | Project to import into.
        LcmsSubmissionParameters parameters = new LcmsSubmissionParameters(); // LcmsSubmissionParameters | Parameters for feature alignment and feature finding.
        List<String> requestBody = Arrays.asList(); // List<String> | Local files to import into project
        Boolean allowMs1Only = true; // Boolean | Import data without MS/MS.
        try {
            ImportResult result = apiInstance.importMsRunDataLocally(projectId, parameters, requestBody, allowMs1Only);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#importMsRunDataLocally");
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
| **projectId** | **String**| Project to import into. | |
| **parameters** | [**LcmsSubmissionParameters**](.md)| Parameters for feature alignment and feature finding. | |
| **requestBody** | [**List&lt;String&gt;**](String.md)| Local files to import into project | |
| **allowMs1Only** | **Boolean**| Import data without MS/MS. | [optional] [default to true] |

### Return type

[**ImportResult**](ImportResult.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## importPreprocessedData

> ImportResult importPreprocessedData(projectId, ignoreFormulas, allowMs1Only, inputFiles)

Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)

Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to import into.
        Boolean ignoreFormulas = false; // Boolean | 
        Boolean allowMs1Only = true; // Boolean | 
        List<File> inputFiles = Arrays.asList(); // List<File> | 
        try {
            ImportResult result = apiInstance.importPreprocessedData(projectId, ignoreFormulas, allowMs1Only, inputFiles);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#importPreprocessedData");
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
| **ignoreFormulas** | **Boolean**|  | [optional] [default to false] |
| **allowMs1Only** | **Boolean**|  | [optional] [default to true] |
| **inputFiles** | **List&lt;File&gt;**|  | [optional] |

### Return type

[**ImportResult**](ImportResult.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## importPreprocessedDataAsJob

> Job importPreprocessedDataAsJob(projectId, ignoreFormulas, allowMs1Only, optFields, inputFiles)

Import ms/ms data from the given format into the specified project-space as background job.

Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to import into.
        Boolean ignoreFormulas = false; // Boolean | 
        Boolean allowMs1Only = true; // Boolean | 
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        List<File> inputFiles = Arrays.asList(); // List<File> | 
        try {
            Job result = apiInstance.importPreprocessedDataAsJob(projectId, ignoreFormulas, allowMs1Only, optFields, inputFiles);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#importPreprocessedDataAsJob");
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
| **ignoreFormulas** | **Boolean**|  | [optional] [default to false] |
| **allowMs1Only** | **Boolean**|  | [optional] [default to true] |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |
| **inputFiles** | **List&lt;File&gt;**|  | [optional] |

### Return type

[**Job**](Job.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the import job. |  -  |


## importPreprocessedDataAsJobLocally

> Job importPreprocessedDataAsJobLocally(projectId, requestBody, ignoreFormulas, allowMs1Only, optFields)

Import ms/ms data from the given format into the specified project-space as background job

Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;preprocessed-data-files-job&#39; instead.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to import into.
        List<String> requestBody = Arrays.asList(); // List<String> | 
        Boolean ignoreFormulas = false; // Boolean | 
        Boolean allowMs1Only = true; // Boolean | 
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            Job result = apiInstance.importPreprocessedDataAsJobLocally(projectId, requestBody, ignoreFormulas, allowMs1Only, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#importPreprocessedDataAsJobLocally");
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
| **requestBody** | [**List&lt;String&gt;**](String.md)|  | |
| **ignoreFormulas** | **Boolean**|  | [optional] [default to false] |
| **allowMs1Only** | **Boolean**|  | [optional] [default to true] |
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
| **200** | the import job. |  -  |


## importPreprocessedDataLocally

> ImportResult importPreprocessedDataLocally(projectId, requestBody, ignoreFormulas, allowMs1Only)

Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)  

Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;preprocessed-data-files&#39; instead.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to import into.
        List<String> requestBody = Arrays.asList(); // List<String> | files to import into project
        Boolean ignoreFormulas = false; // Boolean | 
        Boolean allowMs1Only = true; // Boolean | 
        try {
            ImportResult result = apiInstance.importPreprocessedDataLocally(projectId, requestBody, ignoreFormulas, allowMs1Only);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#importPreprocessedDataLocally");
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
| **requestBody** | [**List&lt;String&gt;**](String.md)| files to import into project | |
| **ignoreFormulas** | **Boolean**|  | [optional] [default to false] |
| **allowMs1Only** | **Boolean**|  | [optional] [default to true] |

### Return type

[**ImportResult**](ImportResult.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## openProjectSpace

> ProjectInfo openProjectSpace(projectId, pathToProject, optFields)

Open an existing project-space and make it accessible via the given projectId.

Open an existing project-space and make it accessible via the given projectId.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | unique name/identifier that shall be used to access the opened project-space. Must consist only of [a-zA-Z0-9_-].
        String pathToProject = "pathToProject_example"; // String | local file path to open the project from. If NULL, project will be loaded by it projectId from default project location.  DEPRECATED: This parameter relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
        List<ProjectInfoOptField> optFields = Arrays.asList(); // List<ProjectInfoOptField> | 
        try {
            ProjectInfo result = apiInstance.openProjectSpace(projectId, pathToProject, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#openProjectSpace");
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
| **projectId** | **String**| unique name/identifier that shall be used to access the opened project-space. Must consist only of [a-zA-Z0-9_-]. | |
| **pathToProject** | **String**| local file path to open the project from. If NULL, project will be loaded by it projectId from default project location.  DEPRECATED: This parameter relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases. | [optional] |
| **optFields** | [**List&lt;ProjectInfoOptField&gt;**](ProjectInfoOptField.md)|  | [optional] |

### Return type

[**ProjectInfo**](ProjectInfo.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

