# TagCategoriesExperimentalApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addCategories**](TagCategoriesExperimentalApi.md#addCategories) | **PUT** /api/projects/{projectId}/categories | Add tag categories to the project. |
| [**addPossibleValuesToCategory**](TagCategoriesExperimentalApi.md#addPossibleValuesToCategory) | **PUT** /api/projects/{projectId}/categories/{categoryName} | Add a possible value to the tag category in the project. |
| [**deleteCategories**](TagCategoriesExperimentalApi.md#deleteCategories) | **DELETE** /api/projects/{projectId}/categories/{categoryName} | Delete tag categories with the given names from the specified project-space. |
| [**getCategories**](TagCategoriesExperimentalApi.md#getCategories) | **GET** /api/projects/{projectId}/categories | Get all tag categories in the given project-space. |
| [**getCategoriesByType**](TagCategoriesExperimentalApi.md#getCategoriesByType) | **GET** /api/projects/{projectId}/categories/type/{categoryType} | Get tag categories by type in the given project-space. |
| [**getCategoryByName**](TagCategoriesExperimentalApi.md#getCategoryByName) | **GET** /api/projects/{projectId}/categories/{categoryName} | Get tag category by name in the given project-space. |



## addCategories

> List&lt;TagCategory&gt; addCategories(projectId, tagCategoryImport)

Add tag categories to the project.

Add tag categories to the project. Category names must not exist in the project.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagCategoriesExperimentalApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesExperimentalApi apiInstance = new TagCategoriesExperimentalApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to add to.
        List<TagCategoryImport> tagCategoryImport = Arrays.asList(); // List<TagCategoryImport> | the tag categories to be added
        try {
            List<TagCategory> result = apiInstance.addCategories(projectId, tagCategoryImport);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagCategoriesExperimentalApi#addCategories");
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
| **tagCategoryImport** | [**List&lt;TagCategoryImport&gt;**](TagCategoryImport.md)| the tag categories to be added | |

### Return type

[**List&lt;TagCategory&gt;**](TagCategory.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the tag categories that have been added |  -  |


## addPossibleValuesToCategory

> TagCategory addPossibleValuesToCategory(projectId, categoryName, requestBody)

Add a possible value to the tag category in the project.

Add a possible value to the tag category in the project.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagCategoriesExperimentalApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesExperimentalApi apiInstance = new TagCategoriesExperimentalApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to add to.
        String categoryName = "categoryName_example"; // String | the tag category to add to
        List<Object> requestBody = null; // List<Object> | 
        try {
            TagCategory result = apiInstance.addPossibleValuesToCategory(projectId, categoryName, requestBody);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagCategoriesExperimentalApi#addPossibleValuesToCategory");
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
| **categoryName** | **String**| the tag category to add to | |
| **requestBody** | [**List&lt;Object&gt;**](Object.md)|  | |

### Return type

[**TagCategory**](TagCategory.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the tag categories that have been added |  -  |


## deleteCategories

> deleteCategories(projectId, categoryName)

Delete tag categories with the given names from the specified project-space.

Delete tag categories with the given names from the specified project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagCategoriesExperimentalApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesExperimentalApi apiInstance = new TagCategoriesExperimentalApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String categoryName = "categoryName_example"; // String | name of category to delete.
        try {
            apiInstance.deleteCategories(projectId, categoryName);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagCategoriesExperimentalApi#deleteCategories");
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
| **categoryName** | **String**| name of category to delete. | |

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


## getCategories

> List&lt;TagCategory&gt; getCategories(projectId)

Get all tag categories in the given project-space.

Get all tag categories in the given project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagCategoriesExperimentalApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesExperimentalApi apiInstance = new TagCategoriesExperimentalApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        try {
            List<TagCategory> result = apiInstance.getCategories(projectId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagCategoriesExperimentalApi#getCategories");
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

### Return type

[**List&lt;TagCategory&gt;**](TagCategory.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Tag categories. |  -  |


## getCategoriesByType

> List&lt;TagCategory&gt; getCategoriesByType(projectId, categoryType)

Get tag categories by type in the given project-space.

Get tag categories by type in the given project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagCategoriesExperimentalApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesExperimentalApi apiInstance = new TagCategoriesExperimentalApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String categoryType = "categoryType_example"; // String | name of the category
        try {
            List<TagCategory> result = apiInstance.getCategoriesByType(projectId, categoryType);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagCategoriesExperimentalApi#getCategoriesByType");
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
| **categoryType** | **String**| name of the category | |

### Return type

[**List&lt;TagCategory&gt;**](TagCategory.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Tag categories. |  -  |


## getCategoryByName

> TagCategory getCategoryByName(projectId, categoryName)

Get tag category by name in the given project-space.

Get tag category by name in the given project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagCategoriesExperimentalApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesExperimentalApi apiInstance = new TagCategoriesExperimentalApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String categoryName = "categoryName_example"; // String | name of the category
        try {
            TagCategory result = apiInstance.getCategoryByName(projectId, categoryName);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagCategoriesExperimentalApi#getCategoryByName");
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
| **categoryName** | **String**| name of the category | |

### Return type

[**TagCategory**](TagCategory.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Tag category. |  -  |

