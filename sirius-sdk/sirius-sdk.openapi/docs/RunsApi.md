# RunsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addTagsToRunExperimental**](RunsApi.md#addTagsToRunExperimental) | **PUT** /api/projects/{projectId}/runs/tags/{runId} | [EXPERIMENTAL] Add tags to a run in the project |
| [**computeFoldChangeForBlankSubtraction**](RunsApi.md#computeFoldChangeForBlankSubtraction) | **PUT** /api/projects/{projectId}/runs/blanksubtract/compute | **EXPERIMENTAL** Compute the fold changes that are required for the fold change filter |
| [**getRunExperimental**](RunsApi.md#getRunExperimental) | **GET** /api/projects/{projectId}/runs/{runId} | [EXPERIMENTAL] Get run with the given identifier from the specified project-space |
| [**getRunPageExperimental**](RunsApi.md#getRunPageExperimental) | **GET** /api/projects/{projectId}/runs/page | [EXPERIMENTAL] Get all available runs in the given project-space |
| [**getRunsByGroupExperimental**](RunsApi.md#getRunsByGroupExperimental) | **GET** /api/projects/{projectId}/runs/grouped | [EXPERIMENTAL] Get runs by tag group |
| [**getRunsByTagExperimental**](RunsApi.md#getRunsByTagExperimental) | **GET** /api/projects/{projectId}/runs/tagged | [EXPERIMENTAL] Get runs by tag |
| [**getTagsForRunExperimental**](RunsApi.md#getTagsForRunExperimental) | **GET** /api/projects/{projectId}/runs/tags/{objectId} | [EXPERIMENTAL] Get all tags associated with this Run |
| [**removeTagFromRunExperimental**](RunsApi.md#removeTagFromRunExperimental) | **DELETE** /api/projects/{projectId}/runs/tags/{runId}/{tagName} | [EXPERIMENTAL] Delete tag with the given name from the run with the specified ID in the specified project-space |



## addTagsToRunExperimental

> List&lt;Tag&gt; addTagsToRunExperimental(projectId, runId, tag)

[EXPERIMENTAL] Add tags to a run in the project

[EXPERIMENTAL] Add tags to a run in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        String runId = "runId_example"; // String | run to add tags to.
        List<Tag> tag = Arrays.asList(); // List<Tag> | tags to add.
        try {
            List<Tag> result = apiInstance.addTagsToRunExperimental(projectId, runId, tag);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#addTagsToRunExperimental");
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
| **runId** | **String**| run to add tags to. | |
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


## computeFoldChangeForBlankSubtraction

> Job computeFoldChangeForBlankSubtraction(projectId, sampleTypeFoldChangeRequest, optFields)

**EXPERIMENTAL** Compute the fold changes that are required for the fold change filter

**EXPERIMENTAL** Compute the fold changes that are required for the fold change filter.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

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
        String projectId = "projectId_example"; // String | project-space to compute the fold change in.
        SampleTypeFoldChangeRequest sampleTypeFoldChangeRequest = new SampleTypeFoldChangeRequest(); // SampleTypeFoldChangeRequest | request with lists of run IDs that are sample, blank, and control runs
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | job opt fields.
        try {
            Job result = apiInstance.computeFoldChangeForBlankSubtraction(projectId, sampleTypeFoldChangeRequest, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#computeFoldChangeForBlankSubtraction");
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
| **projectId** | **String**| project-space to compute the fold change in. | |
| **sampleTypeFoldChangeRequest** | [**SampleTypeFoldChangeRequest**](SampleTypeFoldChangeRequest.md)| request with lists of run IDs that are sample, blank, and control runs | |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| job opt fields. | [optional] |

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
| **200** | OK |  -  |


## getRunExperimental

> Run getRunExperimental(projectId, runId, optFields)

[EXPERIMENTAL] Get run with the given identifier from the specified project-space

[EXPERIMENTAL] Get run with the given identifier from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
            Run result = apiInstance.getRunExperimental(projectId, runId, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#getRunExperimental");
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


## getRunPageExperimental

> PagedModelRun getRunPageExperimental(projectId, page, size, sort, optFields)

[EXPERIMENTAL] Get all available runs in the given project-space

[EXPERIMENTAL] Get all available runs in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
            PagedModelRun result = apiInstance.getRunPageExperimental(projectId, page, size, sort, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#getRunPageExperimental");
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

[**PagedModelRun**](PagedModelRun.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Runs with tags (if specified). |  -  |


## getRunsByGroupExperimental

> PagedModelRun getRunsByGroupExperimental(projectId, groupName, page, size, sort, optFields)

[EXPERIMENTAL] Get runs by tag group

[EXPERIMENTAL] Get runs by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        String groupName = "groupName_example"; // String | tag group name.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<RunOptField> optFields = Arrays.asList(); // List<RunOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PagedModelRun result = apiInstance.getRunsByGroupExperimental(projectId, groupName, page, size, sort, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#getRunsByGroupExperimental");
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
| **groupName** | **String**| tag group name. | |
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |
| **optFields** | [**List&lt;RunOptField&gt;**](RunOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**PagedModelRun**](PagedModelRun.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | tagged runs |  -  |


## getRunsByTagExperimental

> PagedModelRun getRunsByTagExperimental(projectId, filter, page, size, sort, optFields)

[EXPERIMENTAL] Get runs by tag

[EXPERIMENTAL] Get runs by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name.  &lt;/p&gt;   Currently the only searchable fields are names of tags (&lt;code&gt;tagName&lt;/code&gt;) followed by a clause that is valued for the value type of the tag (See TagDefinition).  Tag name based field need to be prefixed with the namespace &lt;code&gt;tags.&lt;/code&gt;.  Possible value types of tags are &lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;tags.MyTagA:sample&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;tags.MyTagA:&amp;quot;Some Text&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;tags.MyTagB&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;tags.MyTagB:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;tags.city:&amp;quot;new york&amp;quot; AND tags.ATextTag:/[mb]oat/ AND tags.count:[1 TO *] OR tags.realNumberTag&amp;lt;&#x3D;3.2 OR tags.MyDateTag:2024-01-01 OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.MyDateTag&amp;lt;2022-01-01 OR tags.time:12\\:00\\:00 OR tags.time:[12\\:00\\:00 TO 14\\:00\\:00] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        String projectId = "projectId_example"; // String | project space to get runs from.
        String filter = ""; // String | tag filter.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<RunOptField> optFields = Arrays.asList(); // List<RunOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PagedModelRun result = apiInstance.getRunsByTagExperimental(projectId, filter, page, size, sort, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#getRunsByTagExperimental");
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
| **projectId** | **String**| project space to get runs from. | |
| **filter** | **String**| tag filter. | [optional] [default to ] |
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |
| **optFields** | [**List&lt;RunOptField&gt;**](RunOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**PagedModelRun**](PagedModelRun.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | tagged runs |  -  |


## getTagsForRunExperimental

> List&lt;Tag&gt; getTagsForRunExperimental(projectId, objectId)

[EXPERIMENTAL] Get all tags associated with this Run

[EXPERIMENTAL] Get all tags associated with this Run

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
        String projectId = "projectId_example"; // String | project-space to get from.
        String objectId = "objectId_example"; // String | RunId to get tags for.
        try {
            List<Tag> result = apiInstance.getTagsForRunExperimental(projectId, objectId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#getTagsForRunExperimental");
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
| **projectId** | **String**| project-space to get from. | |
| **objectId** | **String**| RunId to get tags for. | |

### Return type

[**List&lt;Tag&gt;**](Tag.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the tags of the requested object |  -  |


## removeTagFromRunExperimental

> removeTagFromRunExperimental(projectId, runId, tagName)

[EXPERIMENTAL] Delete tag with the given name from the run with the specified ID in the specified project-space

[EXPERIMENTAL] Delete tag with the given name from the run with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        String runId = "runId_example"; // String | run to delete tag from.
        String tagName = "tagName_example"; // String | name of the tag to delete.
        try {
            apiInstance.removeTagFromRunExperimental(projectId, runId, tagName);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#removeTagFromRunExperimental");
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
| **runId** | **String**| run to delete tag from. | |
| **tagName** | **String**| name of the tag to delete. | |

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

