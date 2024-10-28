# RunsExperimentalApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addTags**](RunsExperimentalApi.md#addTags) | **PUT** /api/projects/{projectId}/runs/tags/{objectId} | Add tags to an object in the project. |
| [**deleteTags**](RunsExperimentalApi.md#deleteTags) | **DELETE** /api/projects/{projectId}/runs/tags/{objectId}/{categoryName} | Delete tag with the given category from the object with the specified ID in the specified project-space. |
| [**getRun**](RunsExperimentalApi.md#getRun) | **GET** /api/projects/{projectId}/runs/{runId} | Get run with the given identifier from the specified project-space. |
| [**getRunsPaged**](RunsExperimentalApi.md#getRunsPaged) | **GET** /api/projects/{projectId}/runs/page | Get all available runs in the given project-space. |
| [**objectsByTag**](RunsExperimentalApi.md#objectsByTag) | **GET** /api/projects/{projectId}/runs/tagged | Get objects by tag |



## addTags

> List&lt;AddTagsRequestInner&gt; addTags(projectId, objectId, addTagsRequestInner)

Add tags to an object in the project.

Add tags to an object in the project. Tags with the same category name will be overwritten.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.RunsExperimentalApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        RunsExperimentalApi apiInstance = new RunsExperimentalApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to add to.
        String objectId = "objectId_example"; // String | object to tag.
        List<AddTagsRequestInner> addTagsRequestInner = Arrays.asList(); // List<AddTagsRequestInner> | tags to add.
        try {
            List<AddTagsRequestInner> result = apiInstance.addTags(projectId, objectId, addTagsRequestInner);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsExperimentalApi#addTags");
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
| **addTagsRequestInner** | [**List&lt;AddTagsRequestInner&gt;**](AddTagsRequestInner.md)| tags to add. | |

### Return type

[**List&lt;AddTagsRequestInner&gt;**](AddTagsRequestInner.md)

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

> deleteTags(projectId, objectId, categoryName)

Delete tag with the given category from the object with the specified ID in the specified project-space.

Delete tag with the given category from the object with the specified ID in the specified project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.RunsExperimentalApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        RunsExperimentalApi apiInstance = new RunsExperimentalApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String objectId = "objectId_example"; // String | object to delete tag from.
        String categoryName = "categoryName_example"; // String | category name of the tag to delete.
        try {
            apiInstance.deleteTags(projectId, objectId, categoryName);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsExperimentalApi#deleteTags");
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
| **objectId** | **String**| object to delete tag from. | |
| **categoryName** | **String**| category name of the tag to delete. | |

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
import io.sirius.ms.sdk.api.RunsExperimentalApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        RunsExperimentalApi apiInstance = new RunsExperimentalApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String runId = "runId_example"; // String | identifier of run to access.
        List<RunOptField> optFields = Arrays.asList(); // List<RunOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            Run result = apiInstance.getRun(projectId, runId, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsExperimentalApi#getRun");
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
import io.sirius.ms.sdk.api.RunsExperimentalApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        RunsExperimentalApi apiInstance = new RunsExperimentalApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<RunOptField> optFields = Arrays.asList(); // List<RunOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PageRun result = apiInstance.getRunsPaged(projectId, page, size, sort, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsExperimentalApi#getRunsPaged");
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

> PageRun objectsByTag(projectId, filter, page, size, sort, optFields)

Get objects by tag

Get objects by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name. Possible field names are:&lt;/p&gt;   &lt;ul&gt;    &lt;li&gt;&lt;strong&gt;category&lt;/strong&gt; - category name&lt;/li&gt;    &lt;li&gt;&lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;category:my_category&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;text:&amp;quot;new york&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;text:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;integer&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;integer:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;(category:hello || category:world) &amp;amp;&amp;amp; text:&amp;quot;new york&amp;quot; AND text:/[mb]oat/ AND integer:[1 TO *] OR real&amp;lt;&#x3D;3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date&amp;lt;2022-01-01 OR time:12\\:00\\:00 OR time:[12\\:00\\:00 TO 14\\:00\\:00] OR time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.RunsExperimentalApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        RunsExperimentalApi apiInstance = new RunsExperimentalApi(defaultClient);
        String projectId = "projectId_example"; // String | project space to get objects from.
        String filter = ""; // String | tag filter.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<RunOptField> optFields = Arrays.asList(); // List<RunOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PageRun result = apiInstance.objectsByTag(projectId, filter, page, size, sort, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsExperimentalApi#objectsByTag");
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
| **filter** | **String**| tag filter. | [optional] [default to ] |
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
| **200** | OK |  -  |

