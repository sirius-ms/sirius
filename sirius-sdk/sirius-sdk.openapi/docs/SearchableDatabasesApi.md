# SearchableDatabasesApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addDatabases**](SearchableDatabasesApi.md#addDatabases) | **POST** /api/databases | DEPRECATED: this endpoint is based on local file paths and will likely be replaced in future versions of this API. |
| [**createDatabase**](SearchableDatabasesApi.md#createDatabase) | **POST** /api/databases/{databaseId} |  |
| [**getCustomDatabases**](SearchableDatabasesApi.md#getCustomDatabases) | **GET** /api/databases/custom |  |
| [**getDatabase**](SearchableDatabasesApi.md#getDatabase) | **GET** /api/databases/{databaseId} |  |
| [**getDatabases**](SearchableDatabasesApi.md#getDatabases) | **GET** /api/databases |  |
| [**getIncludedDatabases**](SearchableDatabasesApi.md#getIncludedDatabases) | **GET** /api/databases/included |  |
| [**importIntoDatabase**](SearchableDatabasesApi.md#importIntoDatabase) | **POST** /api/databases/{databaseId}/import/from-files | Start import of structure and spectra files into the specified database. |
| [**removeDatabase**](SearchableDatabasesApi.md#removeDatabase) | **DELETE** /api/databases/{databaseId} |  |
| [**updateDatabase**](SearchableDatabasesApi.md#updateDatabase) | **PUT** /api/databases/{databaseId} |  |



## addDatabases

> List&lt;SearchableDatabase&gt; addDatabases(requestBody)

DEPRECATED: this endpoint is based on local file paths and will likely be replaced in future versions of this API.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        SearchableDatabasesApi apiInstance = new SearchableDatabasesApi(defaultClient);
        List<String> requestBody = Arrays.asList(); // List<String> | 
        try {
            List<SearchableDatabase> result = apiInstance.addDatabases(requestBody);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchableDatabasesApi#addDatabases");
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
| **requestBody** | [**List&lt;String&gt;**](String.md)|  | |

### Return type

[**List&lt;SearchableDatabase&gt;**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## createDatabase

> SearchableDatabase createDatabase(databaseId, searchableDatabaseParameters)



### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

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
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getCustomDatabases

> List&lt;SearchableDatabase&gt; getCustomDatabases(includeStats, includeWithErrors)



### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        SearchableDatabasesApi apiInstance = new SearchableDatabasesApi(defaultClient);
        Boolean includeStats = false; // Boolean | 
        Boolean includeWithErrors = false; // Boolean | 
        try {
            List<SearchableDatabase> result = apiInstance.getCustomDatabases(includeStats, includeWithErrors);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchableDatabasesApi#getCustomDatabases");
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
| **includeStats** | **Boolean**|  | [optional] [default to false] |
| **includeWithErrors** | **Boolean**|  | [optional] [default to false] |

### Return type

[**List&lt;SearchableDatabase&gt;**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getDatabase

> SearchableDatabase getDatabase(databaseId, includeStats)



### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

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
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getDatabases

> List&lt;SearchableDatabase&gt; getDatabases(includeStats, includeWithErrors)



### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        SearchableDatabasesApi apiInstance = new SearchableDatabasesApi(defaultClient);
        Boolean includeStats = false; // Boolean | 
        Boolean includeWithErrors = false; // Boolean | 
        try {
            List<SearchableDatabase> result = apiInstance.getDatabases(includeStats, includeWithErrors);
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
| **includeStats** | **Boolean**|  | [optional] [default to false] |
| **includeWithErrors** | **Boolean**|  | [optional] [default to false] |

### Return type

[**List&lt;SearchableDatabase&gt;**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getIncludedDatabases

> List&lt;SearchableDatabase&gt; getIncludedDatabases(includeStats)



### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        SearchableDatabasesApi apiInstance = new SearchableDatabasesApi(defaultClient);
        Boolean includeStats = false; // Boolean | 
        try {
            List<SearchableDatabase> result = apiInstance.getIncludedDatabases(includeStats);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchableDatabasesApi#getIncludedDatabases");
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
| **includeStats** | **Boolean**|  | [optional] [default to false] |

### Return type

[**List&lt;SearchableDatabase&gt;**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## importIntoDatabase

> SearchableDatabase importIntoDatabase(databaseId, inputFiles, bufferSize, bioTransformerParameters)

Start import of structure and spectra files into the specified database.

Start import of structure and spectra files into the specified database.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        SearchableDatabasesApi apiInstance = new SearchableDatabasesApi(defaultClient);
        String databaseId = "databaseId_example"; // String | database to import into
        List<File> inputFiles = Arrays.asList(); // List<File> | files to import into project
        Integer bufferSize = 1000; // Integer | 
        BioTransformerParameters bioTransformerParameters = new BioTransformerParameters(); // BioTransformerParameters | 
        try {
            SearchableDatabase result = apiInstance.importIntoDatabase(databaseId, inputFiles, bufferSize, bioTransformerParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchableDatabasesApi#importIntoDatabase");
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
| **databaseId** | **String**| database to import into | |
| **inputFiles** | **List&lt;File&gt;**| files to import into project | |
| **bufferSize** | **Integer**|  | [optional] [default to 1000] |
| **bioTransformerParameters** | [**BioTransformerParameters**](BioTransformerParameters.md)|  | [optional] |

### Return type

[**SearchableDatabase**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Meta-Infomation of the affected database after the import has been performed. |  -  |


## removeDatabase

> removeDatabase(databaseId, delete)



### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

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
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.SearchableDatabasesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

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
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

