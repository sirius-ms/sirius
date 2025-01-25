# TagsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addGroup**](TagsApi.md#addGroup) | **PUT** /api/projects/{projectId}/groups/{groupName} | [EXPERIMENTAL] Group tags in the project |
| [**addPossibleValuesToTagDefinition**](TagsApi.md#addPossibleValuesToTagDefinition) | **PUT** /api/projects/{projectId}/tags/{tagName} | [EXPERIMENTAL] Add a possible value to the tag definition in the project |
| [**createTags**](TagsApi.md#createTags) | **PUT** /api/projects/{projectId}/tags | [EXPERIMENTAL] Add tags to the project |
| [**deleteGroup**](TagsApi.md#deleteGroup) | **DELETE** /api/projects/{projectId}/groups/{groupName} | [EXPERIMENTAL] Delete tag groups with the given name from the specified project-space |
| [**deleteTag**](TagsApi.md#deleteTag) | **DELETE** /api/projects/{projectId}/tags/{tagName} | [EXPERIMENTAL] Delete tag definition with the given name from the specified project-space |
| [**getGroupByName**](TagsApi.md#getGroupByName) | **GET** /api/projects/{projectId}/groups/{groupName} | [EXPERIMENTAL] Get tag group by name in the given project-space |
| [**getGroups**](TagsApi.md#getGroups) | **GET** /api/projects/{projectId}/groups | [EXPERIMENTAL] Get all tag based groups in the given project-space |
| [**getGroupsByType**](TagsApi.md#getGroupsByType) | **GET** /api/projects/{projectId}/groups/type/{groupType} | [EXPERIMENTAL] Get tag groups by type in the given project-space |
| [**getTag**](TagsApi.md#getTag) | **GET** /api/projects/{projectId}/tags/{tagName} | [EXPERIMENTAL] Get tag definition by its name in the given project-space |
| [**getTags**](TagsApi.md#getTags) | **GET** /api/projects/{projectId}/tags | [EXPERIMENTAL] Get all tag definitions in the given project-space |



## addGroup

> TagGroup addGroup(projectId, groupName, filter, type)

[EXPERIMENTAL] Group tags in the project

[EXPERIMENTAL] Group tags in the project. The group name must not exist in the project.   &lt;p&gt;  See &lt;code&gt;/tagged&lt;/code&gt; for filter syntax.  &lt;/p&gt;   [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagsApi apiInstance = new TagsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to add to.
        String groupName = "groupName_example"; // String | name of the new group
        String filter = "filter_example"; // String | filter query to create the group
        String type = "type_example"; // String | type of the group
        try {
            TagGroup result = apiInstance.addGroup(projectId, groupName, filter, type);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagsApi#addGroup");
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
| **groupName** | **String**| name of the new group | |
| **filter** | **String**| filter query to create the group | |
| **type** | **String**| type of the group | |

### Return type

[**TagGroup**](TagGroup.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the tag group that was added |  -  |


## addPossibleValuesToTagDefinition

> TagDefinition addPossibleValuesToTagDefinition(projectId, tagName, requestBody)

[EXPERIMENTAL] Add a possible value to the tag definition in the project

[EXPERIMENTAL] Add a possible value to the tag definition in the project.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagsApi apiInstance = new TagsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to add to.
        String tagName = "tagName_example"; // String | the tag definition to add the values to
        List<Object> requestBody = null; // List<Object> | 
        try {
            TagDefinition result = apiInstance.addPossibleValuesToTagDefinition(projectId, tagName, requestBody);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagsApi#addPossibleValuesToTagDefinition");
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
| **tagName** | **String**| the tag definition to add the values to | |
| **requestBody** | [**List&lt;Object&gt;**](Object.md)|  | |

### Return type

[**TagDefinition**](TagDefinition.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the definitions of the tags that have been added |  -  |


## createTags

> List&lt;TagDefinition&gt; createTags(projectId, tagDefinitionImport)

[EXPERIMENTAL] Add tags to the project

[EXPERIMENTAL] Add tags to the project. Tag names must not exist in the project.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagsApi apiInstance = new TagsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to add to.
        List<TagDefinitionImport> tagDefinitionImport = Arrays.asList(); // List<TagDefinitionImport> | the tag definitions to be created
        try {
            List<TagDefinition> result = apiInstance.createTags(projectId, tagDefinitionImport);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagsApi#createTags");
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
| **tagDefinitionImport** | [**List&lt;TagDefinitionImport&gt;**](TagDefinitionImport.md)| the tag definitions to be created | |

### Return type

[**List&lt;TagDefinition&gt;**](TagDefinition.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the definitions of the tags that have been created |  -  |


## deleteGroup

> deleteGroup(projectId, groupName)

[EXPERIMENTAL] Delete tag groups with the given name from the specified project-space

[EXPERIMENTAL] Delete tag groups with the given name from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagsApi apiInstance = new TagsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String groupName = "groupName_example"; // String | name of group to delete.
        try {
            apiInstance.deleteGroup(projectId, groupName);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagsApi#deleteGroup");
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
| **groupName** | **String**| name of group to delete. | |

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


## deleteTag

> deleteTag(projectId, tagName)

[EXPERIMENTAL] Delete tag definition with the given name from the specified project-space

[EXPERIMENTAL] Delete tag definition with the given name from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagsApi apiInstance = new TagsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String tagName = "tagName_example"; // String | name of the tag definition to delete.
        try {
            apiInstance.deleteTag(projectId, tagName);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagsApi#deleteTag");
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
| **tagName** | **String**| name of the tag definition to delete. | |

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


## getGroupByName

> TagGroup getGroupByName(projectId, groupName)

[EXPERIMENTAL] Get tag group by name in the given project-space

[EXPERIMENTAL] Get tag group by name in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagsApi apiInstance = new TagsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String groupName = "groupName_example"; // String | name of the group
        try {
            TagGroup result = apiInstance.getGroupByName(projectId, groupName);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagsApi#getGroupByName");
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
| **groupName** | **String**| name of the group | |

### Return type

[**TagGroup**](TagGroup.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Tag group. |  -  |


## getGroups

> List&lt;TagGroup&gt; getGroups(projectId, groupType)

[EXPERIMENTAL] Get all tag based groups in the given project-space

[EXPERIMENTAL] Get all tag based groups in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagsApi apiInstance = new TagsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String groupType = "groupType_example"; // String | type of the group
        try {
            List<TagGroup> result = apiInstance.getGroups(projectId, groupType);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagsApi#getGroups");
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
| **groupType** | **String**| type of the group | [optional] |

### Return type

[**List&lt;TagGroup&gt;**](TagGroup.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Groups. |  -  |


## getGroupsByType

> List&lt;TagGroup&gt; getGroupsByType(projectId, groupType)

[EXPERIMENTAL] Get tag groups by type in the given project-space

[EXPERIMENTAL] Get tag groups by type in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagsApi apiInstance = new TagsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String groupType = "groupType_example"; // String | type of the group
        try {
            List<TagGroup> result = apiInstance.getGroupsByType(projectId, groupType);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagsApi#getGroupsByType");
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
| **groupType** | **String**| type of the group | |

### Return type

[**List&lt;TagGroup&gt;**](TagGroup.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Tag groups. |  -  |


## getTag

> TagDefinition getTag(projectId, tagName)

[EXPERIMENTAL] Get tag definition by its name in the given project-space

[EXPERIMENTAL] Get tag definition by its name in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagsApi apiInstance = new TagsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String tagName = "tagName_example"; // String | name of the tag
        try {
            TagDefinition result = apiInstance.getTag(projectId, tagName);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagsApi#getTag");
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
| **tagName** | **String**| name of the tag | |

### Return type

[**TagDefinition**](TagDefinition.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Tag definition. |  -  |


## getTags

> List&lt;TagDefinition&gt; getTags(projectId, tagScope)

[EXPERIMENTAL] Get all tag definitions in the given project-space

[EXPERIMENTAL] Get all tag definitions in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagsApi apiInstance = new TagsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String tagScope = "tagScope_example"; // String | scope of the tag (optional)
        try {
            List<TagDefinition> result = apiInstance.getTags(projectId, tagScope);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagsApi#getTags");
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
| **tagScope** | **String**| scope of the tag (optional) | [optional] |

### Return type

[**List&lt;TagDefinition&gt;**](TagDefinition.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Tag definitions. |  -  |

