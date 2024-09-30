# TagCategoriesApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addCategories**](TagCategoriesApi.md#addCategories) | **POST** /api/projects/{projectId}/categories/add | Add tag category to the project. |
| [**deleteCategories**](TagCategoriesApi.md#deleteCategories) | **PUT** /api/projects/{projectId}/categories/delete | Delete tag categories with the given names from the specified project-space. |
| [**getCategories**](TagCategoriesApi.md#getCategories) | **GET** /api/projects/{projectId}/categories | Get all tag categories in the given project-space. |
| [**getCategoriesByType**](TagCategoriesApi.md#getCategoriesByType) | **GET** /api/projects/{projectId}/categories/type/{categoryType} | Get tag categories by type in the given project-space. |
| [**getCategoryByName**](TagCategoriesApi.md#getCategoryByName) | **GET** /api/projects/{projectId}/categories/name/{categoryName} | Get tag category by name in the given project-space. |



## addCategories

> List&lt;TagCategory&gt; addCategories(projectId, tagCategory)

Add tag category to the project.

Add tag category to the project. Category name must not exist in the project.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.TagCategoriesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesApi apiInstance = new TagCategoriesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to add to.
        List<TagCategory> tagCategory = Arrays.asList(); // List<TagCategory> | the tag categories to be added
        try {
            List<TagCategory> result = apiInstance.addCategories(projectId, tagCategory);
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
| **tagCategory** | [**List&lt;TagCategory&gt;**](TagCategory.md)| the tag categories to be added | |

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


## deleteCategories

> deleteCategories(projectId, requestBody)

Delete tag categories with the given names from the specified project-space.

Delete tag categories with the given names from the specified project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.TagCategoriesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesApi apiInstance = new TagCategoriesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        List<String> requestBody = Arrays.asList(); // List<String> | names of categories to delete.
        try {
            apiInstance.deleteCategories(projectId, requestBody);
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
| **requestBody** | [**List&lt;String&gt;**](String.md)| names of categories to delete. | |

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


## getCategories

> List&lt;TagCategory&gt; getCategories(projectId)

Get all tag categories in the given project-space.

Get all tag categories in the given project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.TagCategoriesApi;

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

Get tag categories by type in the given project-space.

Get tag categories by type in the given project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.TagCategoriesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        TagCategoriesApi apiInstance = new TagCategoriesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String categoryType = "categoryType_example"; // String | name of the category
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
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.TagCategoriesApi;

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
| **200** | Tag categories. |  -  |

