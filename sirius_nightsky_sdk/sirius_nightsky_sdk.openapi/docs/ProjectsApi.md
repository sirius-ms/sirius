# ProjectsApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**closeProjectSpace**](ProjectsApi.md#closeProjectSpace) | **DELETE** /api/projects/{projectId} | Close project-space and remove it from application. |
| [**closeProjectSpaceWithHttpInfo**](ProjectsApi.md#closeProjectSpaceWithHttpInfo) | **DELETE** /api/projects/{projectId} | Close project-space and remove it from application. |
| [**createProjectSpace**](ProjectsApi.md#createProjectSpace) | **POST** /api/projects/{projectId} | Create and open a new project-space at given location and make it accessible via the given projectId. |
| [**createProjectSpaceWithHttpInfo**](ProjectsApi.md#createProjectSpaceWithHttpInfo) | **POST** /api/projects/{projectId} | Create and open a new project-space at given location and make it accessible via the given projectId. |
| [**getProjectSpace**](ProjectsApi.md#getProjectSpace) | **GET** /api/projects/{projectId} | Get project space info by its projectId. |
| [**getProjectSpaceWithHttpInfo**](ProjectsApi.md#getProjectSpaceWithHttpInfo) | **GET** /api/projects/{projectId} | Get project space info by its projectId. |
| [**getProjectSpaces**](ProjectsApi.md#getProjectSpaces) | **GET** /api/projects | List opened project spaces. |
| [**getProjectSpacesWithHttpInfo**](ProjectsApi.md#getProjectSpacesWithHttpInfo) | **GET** /api/projects | List opened project spaces. |
| [**openProjectSpace**](ProjectsApi.md#openProjectSpace) | **PUT** /api/projects/{projectId} | Open an existing project-space and make it accessible via the given projectId. |
| [**openProjectSpaceWithHttpInfo**](ProjectsApi.md#openProjectSpaceWithHttpInfo) | **PUT** /api/projects/{projectId} | Open an existing project-space and make it accessible via the given projectId. |



## closeProjectSpace

> void closeProjectSpace(projectId)

Close project-space and remove it from application.

Close project-space and remove it from application. Project will NOT be deleted from disk.   ATTENTION: This will cancel and remove all jobs running on this Project before closing it.  If there are many jobs, this might take some time.

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
        defaultClient.setBasePath("http://localhost:8080");

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

## closeProjectSpaceWithHttpInfo

> ApiResponse<Void> closeProjectSpace closeProjectSpaceWithHttpInfo(projectId)

Close project-space and remove it from application.

Close project-space and remove it from application. Project will NOT be deleted from disk.   ATTENTION: This will cancel and remove all jobs running on this Project before closing it.  If there are many jobs, this might take some time.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | unique name/identifier of the  project-space to be closed.
        try {
            ApiResponse<Void> response = apiInstance.closeProjectSpaceWithHttpInfo(projectId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#closeProjectSpace");
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
| **projectId** | **String**| unique name/identifier of the  project-space to be closed. | |

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
| **200** | OK |  -  |


## createProjectSpace

> ProjectInfo createProjectSpace(projectId, pathToProject, pathToSourceProject, awaitImport)

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
        defaultClient.setBasePath("http://localhost:8080");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | unique name/identifier that shall be used to access the newly created project-space.
        String pathToProject = "pathToProject_example"; // String | 
        String pathToSourceProject = "pathToSourceProject_example"; // String | 
        Boolean awaitImport = true; // Boolean | 
        try {
            ProjectInfo result = apiInstance.createProjectSpace(projectId, pathToProject, pathToSourceProject, awaitImport);
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
| **projectId** | **String**| unique name/identifier that shall be used to access the newly created project-space. | |
| **pathToProject** | **String**|  | |
| **pathToSourceProject** | **String**|  | [optional] |
| **awaitImport** | **Boolean**|  | [optional] [default to true] |

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

## createProjectSpaceWithHttpInfo

> ApiResponse<ProjectInfo> createProjectSpace createProjectSpaceWithHttpInfo(projectId, pathToProject, pathToSourceProject, awaitImport)

Create and open a new project-space at given location and make it accessible via the given projectId.

Create and open a new project-space at given location and make it accessible via the given projectId.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | unique name/identifier that shall be used to access the newly created project-space.
        String pathToProject = "pathToProject_example"; // String | 
        String pathToSourceProject = "pathToSourceProject_example"; // String | 
        Boolean awaitImport = true; // Boolean | 
        try {
            ApiResponse<ProjectInfo> response = apiInstance.createProjectSpaceWithHttpInfo(projectId, pathToProject, pathToSourceProject, awaitImport);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#createProjectSpace");
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
| **projectId** | **String**| unique name/identifier that shall be used to access the newly created project-space. | |
| **pathToProject** | **String**|  | |
| **pathToSourceProject** | **String**|  | [optional] |
| **awaitImport** | **Boolean**|  | [optional] [default to true] |

### Return type

ApiResponse<[**ProjectInfo**](ProjectInfo.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getProjectSpace

> ProjectInfo getProjectSpace(projectId)

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
        defaultClient.setBasePath("http://localhost:8080");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | unique name/identifier tof the project-space to be accessed.
        try {
            ProjectInfo result = apiInstance.getProjectSpace(projectId);
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

## getProjectSpaceWithHttpInfo

> ApiResponse<ProjectInfo> getProjectSpace getProjectSpaceWithHttpInfo(projectId)

Get project space info by its projectId.

Get project space info by its projectId.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | unique name/identifier tof the project-space to be accessed.
        try {
            ApiResponse<ProjectInfo> response = apiInstance.getProjectSpaceWithHttpInfo(projectId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#getProjectSpace");
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
| **projectId** | **String**| unique name/identifier tof the project-space to be accessed. | |

### Return type

ApiResponse<[**ProjectInfo**](ProjectInfo.md)>


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

> PageProjectInfo getProjectSpaces(page, size, sort, searchQuery, querySyntax)

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
        defaultClient.setBasePath("http://localhost:8080");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        String searchQuery = "searchQuery_example"; // String | optional search query in specified format
        SearchQueryType querySyntax = SearchQueryType.fromValue("LUCENE"); // SearchQueryType | query syntax used fpr searchQuery
        try {
            PageProjectInfo result = apiInstance.getProjectSpaces(page, size, sort, searchQuery, querySyntax);
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


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |
| **searchQuery** | **String**| optional search query in specified format | [optional] |
| **querySyntax** | [**SearchQueryType**](.md)| query syntax used fpr searchQuery | [optional] [enum: LUCENE] |

### Return type

[**PageProjectInfo**](PageProjectInfo.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## getProjectSpacesWithHttpInfo

> ApiResponse<PageProjectInfo> getProjectSpaces getProjectSpacesWithHttpInfo(page, size, sort, searchQuery, querySyntax)

List opened project spaces.

List opened project spaces.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        String searchQuery = "searchQuery_example"; // String | optional search query in specified format
        SearchQueryType querySyntax = SearchQueryType.fromValue("LUCENE"); // SearchQueryType | query syntax used fpr searchQuery
        try {
            ApiResponse<PageProjectInfo> response = apiInstance.getProjectSpacesWithHttpInfo(page, size, sort, searchQuery, querySyntax);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#getProjectSpaces");
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
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |
| **searchQuery** | **String**| optional search query in specified format | [optional] |
| **querySyntax** | [**SearchQueryType**](.md)| query syntax used fpr searchQuery | [optional] [enum: LUCENE] |

### Return type

ApiResponse<[**PageProjectInfo**](PageProjectInfo.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## openProjectSpace

> ProjectInfo openProjectSpace(projectId, pathToProject)

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
        defaultClient.setBasePath("http://localhost:8080");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | unique name/identifier that shall be used to access the opened project-space.
        String pathToProject = "pathToProject_example"; // String | 
        try {
            ProjectInfo result = apiInstance.openProjectSpace(projectId, pathToProject);
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
| **projectId** | **String**| unique name/identifier that shall be used to access the opened project-space. | |
| **pathToProject** | **String**|  | |

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

## openProjectSpaceWithHttpInfo

> ApiResponse<ProjectInfo> openProjectSpace openProjectSpaceWithHttpInfo(projectId, pathToProject)

Open an existing project-space and make it accessible via the given projectId.

Open an existing project-space and make it accessible via the given projectId.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ProjectsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        ProjectsApi apiInstance = new ProjectsApi(defaultClient);
        String projectId = "projectId_example"; // String | unique name/identifier that shall be used to access the opened project-space.
        String pathToProject = "pathToProject_example"; // String | 
        try {
            ApiResponse<ProjectInfo> response = apiInstance.openProjectSpaceWithHttpInfo(projectId, pathToProject);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectsApi#openProjectSpace");
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
| **projectId** | **String**| unique name/identifier that shall be used to access the opened project-space. | |
| **pathToProject** | **String**|  | |

### Return type

ApiResponse<[**ProjectInfo**](ProjectInfo.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

