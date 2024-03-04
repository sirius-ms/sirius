# SearchableDatabasesApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addDatabases**](SearchableDatabasesApi.md#addDatabases) | **POST** /api/databases |  |
| [**createDatabase**](SearchableDatabasesApi.md#createDatabase) | **POST** /api/databases/{databaseId} |  |
| [**getCustomDatabases**](SearchableDatabasesApi.md#getCustomDatabases) | **GET** /api/databases/custom |  |
| [**getDatabase**](SearchableDatabasesApi.md#getDatabase) | **GET** /api/databases/{databaseId} |  |
| [**getDatabases**](SearchableDatabasesApi.md#getDatabases) | **GET** /api/databases |  |
| [**getIncludedDatabases**](SearchableDatabasesApi.md#getIncludedDatabases) | **GET** /api/databases/included |  |
| [**importIntoDatabase**](SearchableDatabasesApi.md#importIntoDatabase) | **POST** /api/databases/{databaseId}/import/from-files | Start import of structure and spectra files into the specified database. |
| [**importIntoDatabaseAsync**](SearchableDatabasesApi.md#importIntoDatabaseAsync) | **POST** /api/databases/{databaseId}/import/from-files-async | Start import of structure and spectra files into the specified database. |
| [**removeDatabase**](SearchableDatabasesApi.md#removeDatabase) | **DELETE** /api/databases/{databaseId} |  |
| [**updateDatabase**](SearchableDatabasesApi.md#updateDatabase) | **PUT** /api/databases/{databaseId} |  |



## addDatabases

> List&lt;SearchableDatabase&gt; addDatabases(requestBody)



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


## getCustomDatabases

> List&lt;SearchableDatabase&gt; getCustomDatabases(includeStats)



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
        Boolean includeStats = false; // Boolean | 
        try {
            List<SearchableDatabase> result = apiInstance.getCustomDatabases(includeStats);
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

### Return type

[**List&lt;SearchableDatabase&gt;**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
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

> List&lt;SearchableDatabase&gt; getDatabases(includeStats)



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
        Boolean includeStats = false; // Boolean | 
        try {
            List<SearchableDatabase> result = apiInstance.getDatabases(includeStats);
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

### Return type

[**List&lt;SearchableDatabase&gt;**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getIncludedDatabases

> List&lt;SearchableDatabase&gt; getIncludedDatabases(includeStats)



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
- **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## importIntoDatabase

> SearchableDatabase importIntoDatabase(databaseId, importPreprocessedDataRequest, bufferSize)

Start import of structure and spectra files into the specified database.

Start import of structure and spectra files into the specified database.

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
        String databaseId = "databaseId_example"; // String | database to import into
        ImportPreprocessedDataRequest importPreprocessedDataRequest = new ImportPreprocessedDataRequest(); // ImportPreprocessedDataRequest | files to be imported
        Integer bufferSize = 1000; // Integer | 
        try {
            SearchableDatabase result = apiInstance.importIntoDatabase(databaseId, importPreprocessedDataRequest, bufferSize);
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
| **importPreprocessedDataRequest** | [**ImportPreprocessedDataRequest**](ImportPreprocessedDataRequest.md)| files to be imported | |
| **bufferSize** | **Integer**|  | [optional] [default to 1000] |

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
| **200** | Job of the import command to be executed. |  -  |


## importIntoDatabaseAsync

> Job importIntoDatabaseAsync(databaseId, importPreprocessedDataRequest, bufferSize, optFields)

Start import of structure and spectra files into the specified database.

Start import of structure and spectra files into the specified database.

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
        String databaseId = "databaseId_example"; // String | database to import into
        ImportPreprocessedDataRequest importPreprocessedDataRequest = new ImportPreprocessedDataRequest(); // ImportPreprocessedDataRequest | files to be imported
        Integer bufferSize = 1000; // Integer | 
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            Job result = apiInstance.importIntoDatabaseAsync(databaseId, importPreprocessedDataRequest, bufferSize, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchableDatabasesApi#importIntoDatabaseAsync");
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
| **importPreprocessedDataRequest** | [**ImportPreprocessedDataRequest**](ImportPreprocessedDataRequest.md)| files to be imported | |
| **bufferSize** | **Integer**|  | [optional] [default to 1000] |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

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
| **200** | Job of the import command to be executed. |  -  |


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

