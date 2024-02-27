# SearchableDatabasesApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addDatabase**](SearchableDatabasesApi.md#addDatabase) | **POST** /api/databases/{databaseId}/add |  |
| [**createDatabase**](SearchableDatabasesApi.md#createDatabase) | **POST** /api/databases/{databaseId} |  |
| [**getDatabase**](SearchableDatabasesApi.md#getDatabase) | **GET** /api/databases/{databaseId} |  |
| [**getDatabases**](SearchableDatabasesApi.md#getDatabases) | **GET** /api/databases |  |
| [**removeDatabase**](SearchableDatabasesApi.md#removeDatabase) | **DELETE** /api/databases/{databaseId} |  |
| [**updateDatabase**](SearchableDatabasesApi.md#updateDatabase) | **PUT** /api/databases/{databaseId} |  |



## addDatabase

> SearchableDatabase addDatabase(databaseId, pathToProject)



### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        SearchableDatabasesApi apiInstance = new SearchableDatabasesApi(defaultClient);
        String databaseId = "databaseId_example"; // String | 
        String pathToProject = "pathToProject_example"; // String | 
        try {
            SearchableDatabase result = apiInstance.addDatabase(databaseId, pathToProject);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchableDatabasesApi#addDatabase");
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
| **databaseId** | **String**|  | |
| **pathToProject** | **String**|  | |

### Return type

[**SearchableDatabase**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## createDatabase

> SearchableDatabase createDatabase(databaseId, searchableDatabaseParameters)



### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        SearchableDatabasesApi apiInstance = new SearchableDatabasesApi(defaultClient);
        String databaseId = "databaseId_example"; // String | 
        SearchableDatabaseParameters searchableDatabaseParameters = new SearchableDatabaseParameters(); // SearchableDatabaseParameters | 
        try {
            SearchableDatabase result = apiInstance.createDatabase(databaseId, searchableDatabaseParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchableDatabasesApi#createDatabase");
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
| **databaseId** | **String**|  | |
| **searchableDatabaseParameters** | [**SearchableDatabaseParameters**](SearchableDatabaseParameters.md)|  | [optional] |

### Return type

[**SearchableDatabase**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getDatabase

> SearchableDatabase getDatabase(databaseId, includeStats)



### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        SearchableDatabasesApi apiInstance = new SearchableDatabasesApi(defaultClient);
        String databaseId = "databaseId_example"; // String | 
        Boolean includeStats = true; // Boolean | 
        try {
            SearchableDatabase result = apiInstance.getDatabase(databaseId, includeStats);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchableDatabasesApi#getDatabase");
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
| **databaseId** | **String**|  | |
| **includeStats** | **Boolean**|  | [optional] [default to true] |

### Return type

[**SearchableDatabase**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getDatabases

> PageSearchableDatabase getDatabases(page, size, sort, includeStats)



### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        SearchableDatabasesApi apiInstance = new SearchableDatabasesApi(defaultClient);
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        Boolean includeStats = false; // Boolean | 
        try {
            PageSearchableDatabase result = apiInstance.getDatabases(page, size, sort, includeStats);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchableDatabasesApi#getDatabases");
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
| **includeStats** | **Boolean**|  | [optional] [default to false] |

### Return type

[**PageSearchableDatabase**](PageSearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## removeDatabase

> removeDatabase(databaseId, delete)



### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        SearchableDatabasesApi apiInstance = new SearchableDatabasesApi(defaultClient);
        String databaseId = "databaseId_example"; // String | 
        Boolean delete = false; // Boolean | 
        try {
            apiInstance.removeDatabase(databaseId, delete);
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchableDatabasesApi#removeDatabase");
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
| **databaseId** | **String**|  | |
| **delete** | **Boolean**|  | [optional] [default to false] |

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


## updateDatabase

> SearchableDatabase updateDatabase(databaseId, searchableDatabaseParameters)



### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        SearchableDatabasesApi apiInstance = new SearchableDatabasesApi(defaultClient);
        String databaseId = "databaseId_example"; // String | 
        SearchableDatabaseParameters searchableDatabaseParameters = new SearchableDatabaseParameters(); // SearchableDatabaseParameters | 
        try {
            SearchableDatabase result = apiInstance.updateDatabase(databaseId, searchableDatabaseParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchableDatabasesApi#updateDatabase");
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
| **databaseId** | **String**|  | |
| **searchableDatabaseParameters** | [**SearchableDatabaseParameters**](SearchableDatabaseParameters.md)|  | [optional] |

### Return type

[**SearchableDatabase**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

