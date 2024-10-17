# RunsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addTags**](RunsApi.md#addTags) | **POST** /api/projects/{projectId}/runs/tags/add/{objectId} | Add tags to an object in the project. |
| [**deleteTags**](RunsApi.md#deleteTags) | **PUT** /api/projects/{projectId}/runs/tags/delete/{objectId} | Delete tags with the given IDs from the specified project-space. |
| [**getRun**](RunsApi.md#getRun) | **GET** /api/projects/{projectId}/runs/{runId} | Get run with the given identifier from the specified project-space. |
| [**getRunsPaged**](RunsApi.md#getRunsPaged) | **GET** /api/projects/{projectId}/runs/page | Get all available runs in the given project-space. |
| [**objectsByTag**](RunsApi.md#objectsByTag) | **POST** /api/projects/{projectId}/runs/tags/tagged/{categoryName} | Get objects by tag. |



## addTags

> List&lt;Tag&gt; addTags(projectId, objectId, tag)

Add tags to an object in the project.

Add tags to an object in the project. Tags with the same category name will be overwritten.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.RunsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        RunsApi apiInstance = new RunsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to add to.
        String objectId = "objectId_example"; // String | object to tag.
        List<Tag> tag = Arrays.asList(); // List<Tag> | tags to add.
        try {
            List<Tag> result = apiInstance.addTags(projectId, objectId, tag);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#addTags");
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
| **projectId** | **String**| project-space to add to. | |
| **objectId** | **String**| object to tag. | |
| **tag** | [**List&lt;Tag&gt;**](Tag.md)| tags to add. | |

### Return type

[**List&lt;Tag&gt;**](Tag.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the tags that have been added |  -  |


## deleteTags

> deleteTags(projectId, objectId, requestBody)

Delete tags with the given IDs from the specified project-space.

Delete tags with the given IDs from the specified project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.RunsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        RunsApi apiInstance = new RunsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String objectId = "objectId_example"; // String | object to delete tags from.
        List<String> requestBody = Arrays.asList(); // List<String> | Category names of the tags to delete.
        try {
            apiInstance.deleteTags(projectId, objectId, requestBody);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#deleteTags");
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
| **projectId** | **String**| project-space to delete from. | |
| **objectId** | **String**| object to delete tags from. | |
| **requestBody** | [**List&lt;String&gt;**](String.md)| Category names of the tags to delete. | |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getRun

> Run getRun(projectId, runId, optFields)

Get run with the given identifier from the specified project-space.

Get run with the given identifier from the specified project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.RunsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        RunsApi apiInstance = new RunsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String runId = "runId_example"; // String | identifier of run to access.
        List<RunOptField> optFields = Arrays.asList(); // List<RunOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            Run result = apiInstance.getRun(projectId, runId, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#getRun");
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
| **projectId** | **String**| project-space to read from. | |
| **runId** | **String**| identifier of run to access. | |
| **optFields** | [**List&lt;RunOptField&gt;**](RunOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**Run**](Run.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Run with tags (if specified). |  -  |


## getRunsPaged

> PageRun getRunsPaged(projectId, page, size, sort, optFields)

Get all available runs in the given project-space.

Get all available runs in the given project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.RunsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        RunsApi apiInstance = new RunsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<RunOptField> optFields = Arrays.asList(); // List<RunOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PageRun result = apiInstance.getRunsPaged(projectId, page, size, sort, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#getRunsPaged");
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
| **projectId** | **String**| project-space to read from. | |
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |
| **optFields** | [**List&lt;RunOptField&gt;**](RunOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**PageRun**](PageRun.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Runs with tags (if specified). |  -  |


## objectsByTag

> PageRun objectsByTag(projectId, categoryName, objectsByTagRequest, page, size, sort, optFields)

Get objects by tag.

Get objects by tag.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.RunsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        RunsApi apiInstance = new RunsApi(defaultClient);
        String projectId = "projectId_example"; // String | project space to get objects from.
        String categoryName = "categoryName_example"; // String | category of the tag.
        ObjectsByTagRequest objectsByTagRequest = new ObjectsByTagRequest(); // ObjectsByTagRequest | tag filter.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<RunOptField> optFields = Arrays.asList(); // List<RunOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PageRun result = apiInstance.objectsByTag(projectId, categoryName, objectsByTagRequest, page, size, sort, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#objectsByTag");
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
| **projectId** | **String**| project space to get objects from. | |
| **categoryName** | **String**| category of the tag. | |
| **objectsByTagRequest** | [**ObjectsByTagRequest**](ObjectsByTagRequest.md)| tag filter. | |
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |
| **optFields** | [**List&lt;RunOptField&gt;**](RunOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**PageRun**](PageRun.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

