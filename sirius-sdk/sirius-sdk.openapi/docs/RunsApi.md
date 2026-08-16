# RunsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addTagsToRunExperimental**](RunsApi.md#addTagsToRunExperimental) | **PUT** /api/projects/{projectId}/runs/tags/{runId} | [EXPERIMENTAL] Add tags to a run in the project |
| [**addTagsToRunsExperimental**](RunsApi.md#addTagsToRunsExperimental) | **PUT** /api/projects/{projectId}/runs/tags | [EXPERIMENTAL] Add tags to a run in the project |
| [**getRun**](RunsApi.md#getRun) | **GET** /api/projects/{projectId}/runs/{runId} | Get run with the given identifier from the specified project-space. |
| [**getRunsByGroupExperimental**](RunsApi.md#getRunsByGroupExperimental) | **GET** /api/projects/{projectId}/runs/grouped | [EXPERIMENTAL] Get runs by tag group |
| [**getRunsPage**](RunsApi.md#getRunsPage) | **GET** /api/projects/{projectId}/runs/page | Get runs in the given project-space |
| [**getRunsSearchableFields**](RunsApi.md#getRunsSearchableFields) | **GET** /api/projects/{projectId}/runs/searchable-fields | Get all fields that can be used in the searchQuery parameter of run endpoints |
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
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the tags that have been added |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## addTagsToRunsExperimental

> addTagsToRunsExperimental(projectId, tagSubmission)

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
        List<TagSubmission> tagSubmission = Arrays.asList(); // List<TagSubmission> | tags with the id of run they shall be added to.
        try {
            apiInstance.addTagsToRunsExperimental(projectId, tagSubmission);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#addTagsToRunsExperimental");
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
| **tagSubmission** | [**List&lt;TagSubmission&gt;**](TagSubmission.md)| tags with the id of run they shall be added to. | |

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
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


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
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Run with tags (if specified). |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


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
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | tagged runs |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## getRunsPage

> PagedModelRun getRunsPage(projectId, searchQuery, page, size, sort, optFields)

Get runs in the given project-space

Get runs in the given project-space.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefixed  by a field name.  &lt;/p&gt;   Searchable fields are the indexed properties of the run (e.g. &lt;code&gt;name&lt;/code&gt;, &lt;code&gt;source&lt;/code&gt;,  &lt;code&gt;chromatography&lt;/code&gt;, &lt;code&gt;ionization&lt;/code&gt;) and project tags prefixed with the namespace  &lt;code&gt;tags.&lt;/code&gt; (e.g. &lt;code&gt;tags.MyTag&lt;/code&gt;).  Use the &lt;code&gt;searchable-fields&lt;/code&gt; endpoint (getRunsSearchableFields) to list all fields that can be  searched, including their value type, whether they support word based (full text) search, and whether  results can be sorted by them.  Possible value types are &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;double&lt;/strong&gt;,  &lt;strong&gt;boolean&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt;.   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;chromatography:LC&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;name:&amp;quot;Blank Sample 25&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;tags.MyTagB&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;tags.MyTagB:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;name:&amp;quot;Blank&amp;quot; AND chromatography:LC AND tags.city:&amp;quot;new york&amp;quot; OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;

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
        String searchQuery = "searchQuery_example"; // String | Optional search query in lucene syntax. Omit this parameter to page over all runs.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<RunOptField> optFields = Arrays.asList(); // List<RunOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PagedModelRun result = apiInstance.getRunsPage(projectId, searchQuery, page, size, sort, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#getRunsPage");
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
| **searchQuery** | **String**| Optional search query in lucene syntax. Omit this parameter to page over all runs. | [optional] |
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
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | tagged runs |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## getRunsSearchableFields

> List&lt;SearchableField&gt; getRunsSearchableFields(projectId)

Get all fields that can be used in the searchQuery parameter of run endpoints

Get all fields that can be used in the searchQuery parameter of run endpoints.  &lt;p&gt;  Use this to build valid lucene queries: the field type determines which clauses are supported  (e.g. range queries for numeric fields, word based search for full-text fields). Includes the  dynamic tag fields (&lt;code&gt;tags.&amp;lt;tagName&amp;gt;&lt;/code&gt;) of this project. An empty list means there are no  searchable fields.

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
        String projectId = "projectId_example"; // String | project space to read the searchable run fields from.
        try {
            List<SearchableField> result = apiInstance.getRunsSearchableFields(projectId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RunsApi#getRunsSearchableFields");
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
| **projectId** | **String**| project space to read the searchable run fields from. | |

### Return type

[**List&lt;SearchableField&gt;**](SearchableField.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | fields usable in searchQuery parameters of run endpoints. |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


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
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the tags of the requested object |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


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
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |

