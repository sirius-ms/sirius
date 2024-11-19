# TagCategoriesApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addCategories**](TagCategoriesApi.md#addCategories) | **PUT** /api/projects/{projectId}/categories | **EXPERIMENTAL** Add tag categories to the project. |
| [**addPossibleValuesToCategory**](TagCategoriesApi.md#addPossibleValuesToCategory) | **PUT** /api/projects/{projectId}/categories/{categoryName} | **EXPERIMENTAL** Add a possible value to the tag category in the project. |
| [**deleteCategories**](TagCategoriesApi.md#deleteCategories) | **DELETE** /api/projects/{projectId}/categories/{categoryName} | **EXPERIMENTAL** Delete tag categories with the given name from the specified project-space. |
| [**getCategories**](TagCategoriesApi.md#getCategories) | **GET** /api/projects/{projectId}/categories | **EXPERIMENTAL** Get all tag categories in the given project-space. |
| [**getCategoriesByType**](TagCategoriesApi.md#getCategoriesByType) | **GET** /api/projects/{projectId}/categories/type/{categoryType} | **EXPERIMENTAL** Get tag categories by type in the given project-space. |
| [**getCategoryByName**](TagCategoriesApi.md#getCategoryByName) | **GET** /api/projects/{projectId}/categories/{categoryName} | **EXPERIMENTAL** Get tag category by name in the given project-space. |



## addCategories

> List&lt;TagCategory&gt; addCategories(projectId, tagCategoryImport)

**EXPERIMENTAL** Add tag categories to the project.

**EXPERIMENTAL** Add tag categories to the project. Category names must not exist in the project.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagCategoriesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesApi apiInstance = new TagCategoriesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to add to.
        List<TagCategoryImport> tagCategoryImport = Arrays.asList(); // List<TagCategoryImport> | the tag categories to be added
        try {
            List<TagCategory> result = apiInstance.addCategories(projectId, tagCategoryImport);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagCategoriesApi#addCategories");
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

**EXPERIMENTAL** Add a possible value to the tag category in the project.

**EXPERIMENTAL** Add a possible value to the tag category in the project.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagCategoriesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesApi apiInstance = new TagCategoriesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to add to.
        String categoryName = "categoryName_example"; // String | the tag category to add to
        List<Object> requestBody = null; // List<Object> | 
        try {
            TagCategory result = apiInstance.addPossibleValuesToCategory(projectId, categoryName, requestBody);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagCategoriesApi#addPossibleValuesToCategory");
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

**EXPERIMENTAL** Delete tag categories with the given name from the specified project-space.

**EXPERIMENTAL** Delete tag categories with the given name from the specified project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagCategoriesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesApi apiInstance = new TagCategoriesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String categoryName = "categoryName_example"; // String | name of category to delete.
        try {
            apiInstance.deleteCategories(projectId, categoryName);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagCategoriesApi#deleteCategories");
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

**EXPERIMENTAL** Get all tag categories in the given project-space.

**EXPERIMENTAL** Get all tag categories in the given project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagCategoriesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesApi apiInstance = new TagCategoriesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        try {
            List<TagCategory> result = apiInstance.getCategories(projectId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagCategoriesApi#getCategories");
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

**EXPERIMENTAL** Get tag categories by type in the given project-space.

**EXPERIMENTAL** Get tag categories by type in the given project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagCategoriesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesApi apiInstance = new TagCategoriesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String categoryType = "categoryType_example"; // String | type of the category
        try {
            List<TagCategory> result = apiInstance.getCategoriesByType(projectId, categoryType);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagCategoriesApi#getCategoriesByType");
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
| **categoryType** | **String**| type of the category | |

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

**EXPERIMENTAL** Get tag category by name in the given project-space.

**EXPERIMENTAL** Get tag category by name in the given project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.TagCategoriesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesApi apiInstance = new TagCategoriesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String categoryName = "categoryName_example"; // String | name of the category
        try {
            TagCategory result = apiInstance.getCategoryByName(projectId, categoryName);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TagCategoriesApi#getCategoryByName");
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

