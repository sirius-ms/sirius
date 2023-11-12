# CompoundsApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteCompound**](CompoundsApi.md#deleteCompound) | **DELETE** /api/projects/{projectId}/compounds/{compoundId} | Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space. |
| [**deleteCompoundWithHttpInfo**](CompoundsApi.md#deleteCompoundWithHttpInfo) | **DELETE** /api/projects/{projectId}/compounds/{compoundId} | Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space. |
| [**getCompound**](CompoundsApi.md#getCompound) | **GET** /api/projects/{projectId}/compounds/{compoundId} | Get compound (group of ion identities) with the given identifier from the specified project-space. |
| [**getCompoundWithHttpInfo**](CompoundsApi.md#getCompoundWithHttpInfo) | **GET** /api/projects/{projectId}/compounds/{compoundId} | Get compound (group of ion identities) with the given identifier from the specified project-space. |
| [**getCompounds**](CompoundsApi.md#getCompounds) | **GET** /api/projects/{projectId}/compounds | Get all available compounds (group of ion identities) in the given project-space. |
| [**getCompoundsWithHttpInfo**](CompoundsApi.md#getCompoundsWithHttpInfo) | **GET** /api/projects/{projectId}/compounds | Get all available compounds (group of ion identities) in the given project-space. |



## deleteCompound

> void deleteCompound(projectId, compoundId)

Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.

Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String compoundId = "compoundId_example"; // String | identifier of the compound to delete.
        try {
            apiInstance.deleteCompound(projectId, compoundId);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#deleteCompound");
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
| **compoundId** | **String**| identifier of the compound to delete. | |

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

## deleteCompoundWithHttpInfo

> ApiResponse<Void> deleteCompound deleteCompoundWithHttpInfo(projectId, compoundId)

Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.

Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String compoundId = "compoundId_example"; // String | identifier of the compound to delete.
        try {
            ApiResponse<Void> response = apiInstance.deleteCompoundWithHttpInfo(projectId, compoundId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#deleteCompound");
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
| **projectId** | **String**| project-space to delete from. | |
| **compoundId** | **String**| identifier of the compound to delete. | |

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


## getCompound

> Compound getCompound(projectId, compoundId, optFields, optFieldsFeatures)

Get compound (group of ion identities) with the given identifier from the specified project-space.

Get compound (group of ion identities) with the given identifier from the specified project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String compoundId = "compoundId_example"; // String | identifier of the compound (group of ion identities) to access.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        List<AlignedFeatureOptField> optFieldsFeatures = Arrays.asList(); // List<AlignedFeatureOptField> | 
        try {
            Compound result = apiInstance.getCompound(projectId, compoundId, optFields, optFieldsFeatures);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getCompound");
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
| **compoundId** | **String**| identifier of the compound (group of ion identities) to access. | |
| **optFields** | [**List&lt;CompoundOptField&gt;**](CompoundOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |
| **optFieldsFeatures** | [**List&lt;AlignedFeatureOptField&gt;**](AlignedFeatureOptField.md)|  | [optional] |

### Return type

[**Compound**](Compound.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Compounds with additional optional fields (if specified). |  -  |

## getCompoundWithHttpInfo

> ApiResponse<Compound> getCompound getCompoundWithHttpInfo(projectId, compoundId, optFields, optFieldsFeatures)

Get compound (group of ion identities) with the given identifier from the specified project-space.

Get compound (group of ion identities) with the given identifier from the specified project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String compoundId = "compoundId_example"; // String | identifier of the compound (group of ion identities) to access.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        List<AlignedFeatureOptField> optFieldsFeatures = Arrays.asList(); // List<AlignedFeatureOptField> | 
        try {
            ApiResponse<Compound> response = apiInstance.getCompoundWithHttpInfo(projectId, compoundId, optFields, optFieldsFeatures);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getCompound");
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
| **projectId** | **String**| project-space to read from. | |
| **compoundId** | **String**| identifier of the compound (group of ion identities) to access. | |
| **optFields** | [**List&lt;CompoundOptField&gt;**](CompoundOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |
| **optFieldsFeatures** | [**List&lt;AlignedFeatureOptField&gt;**](AlignedFeatureOptField.md)|  | [optional] |

### Return type

ApiResponse<[**Compound**](Compound.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Compounds with additional optional fields (if specified). |  -  |


## getCompounds

> PageCompound getCompounds(projectId, page, size, sort, searchQuery, querySyntax, optFields, optFieldsFeatures)

Get all available compounds (group of ion identities) in the given project-space.

Get all available compounds (group of ion identities) in the given project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        String searchQuery = "searchQuery_example"; // String | optional search query in specified format
        SearchQueryType querySyntax = SearchQueryType.fromValue("LUCENE"); // SearchQueryType | query syntax used fpr searchQuery
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        List<AlignedFeatureOptField> optFieldsFeatures = Arrays.asList(); // List<AlignedFeatureOptField> | 
        try {
            PageCompound result = apiInstance.getCompounds(projectId, page, size, sort, searchQuery, querySyntax, optFields, optFieldsFeatures);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getCompounds");
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
| **searchQuery** | **String**| optional search query in specified format | [optional] |
| **querySyntax** | [**SearchQueryType**](.md)| query syntax used fpr searchQuery | [optional] [enum: LUCENE] |
| **optFields** | [**List&lt;CompoundOptField&gt;**](CompoundOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |
| **optFieldsFeatures** | [**List&lt;AlignedFeatureOptField&gt;**](AlignedFeatureOptField.md)|  | [optional] |

### Return type

[**PageCompound**](PageCompound.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Compounds with additional optional fields (if specified). |  -  |

## getCompoundsWithHttpInfo

> ApiResponse<PageCompound> getCompounds getCompoundsWithHttpInfo(projectId, page, size, sort, searchQuery, querySyntax, optFields, optFieldsFeatures)

Get all available compounds (group of ion identities) in the given project-space.

Get all available compounds (group of ion identities) in the given project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiResponse;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        String searchQuery = "searchQuery_example"; // String | optional search query in specified format
        SearchQueryType querySyntax = SearchQueryType.fromValue("LUCENE"); // SearchQueryType | query syntax used fpr searchQuery
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        List<AlignedFeatureOptField> optFieldsFeatures = Arrays.asList(); // List<AlignedFeatureOptField> | 
        try {
            ApiResponse<PageCompound> response = apiInstance.getCompoundsWithHttpInfo(projectId, page, size, sort, searchQuery, querySyntax, optFields, optFieldsFeatures);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getCompounds");
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
| **projectId** | **String**| project-space to read from. | |
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |
| **searchQuery** | **String**| optional search query in specified format | [optional] |
| **querySyntax** | [**SearchQueryType**](.md)| query syntax used fpr searchQuery | [optional] [enum: LUCENE] |
| **optFields** | [**List&lt;CompoundOptField&gt;**](CompoundOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |
| **optFieldsFeatures** | [**List&lt;AlignedFeatureOptField&gt;**](AlignedFeatureOptField.md)|  | [optional] |

### Return type

ApiResponse<[**PageCompound**](PageCompound.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Compounds with additional optional fields (if specified). |  -  |

