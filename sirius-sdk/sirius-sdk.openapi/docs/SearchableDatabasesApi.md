# SearchableDatabasesApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addDatabases**](SearchableDatabasesApi.md#addDatabases) | **POST** /api/databases | [DEPRECATED] This endpoint is based on local file paths and will likely be replaced in future versions of this API. |
| [**createDatabase**](SearchableDatabasesApi.md#createDatabase) | **POST** /api/databases/{databaseId} | Create a new, empty custom database |
| [**getCustomDatabases**](SearchableDatabasesApi.md#getCustomDatabases) | **GET** /api/databases/custom | List only the custom databases, that is the structure databases and spectral libraries the user has  created or added. |
| [**getDatabase**](SearchableDatabasesApi.md#getDatabase) | **GET** /api/databases/{databaseId} | Get a single searchable database by its id. |
| [**getDatabases**](SearchableDatabasesApi.md#getDatabases) | **GET** /api/databases | List all searchable databases, both the ones included in SIRIUS and the custom ones added by the user |
| [**getDownloadableDatabases**](SearchableDatabasesApi.md#getDownloadableDatabases) | **GET** /api/databases/downloadable | Get list of curated custom databases downloadable from the SIRIUS web service for local use |
| [**getIncludedDatabases**](SearchableDatabasesApi.md#getIncludedDatabases) | **GET** /api/databases/included | List only the databases that ship with SIRIUS, such as PubChem and the bio databases. |
| [**getStructures**](SearchableDatabasesApi.md#getStructures) | **GET** /api/databases/{databaseId}/structures | [EXPERIMENTAL] Page through the structures contained in a custom database |
| [**importIntoDatabase**](SearchableDatabasesApi.md#importIntoDatabase) | **POST** /api/databases/{databaseId}/import/from-files | Start import of structure and spectra files into the specified database. |
| [**removeDatabase**](SearchableDatabasesApi.md#removeDatabase) | **DELETE** /api/databases/{databaseId} | Remove a custom database from this SIRIUS instance, and optionally delete it from disk |
| [**updateDatabase**](SearchableDatabasesApi.md#updateDatabase) | **PUT** /api/databases/{databaseId} | Change the settings of an existing custom database |



## addDatabases

> List&lt;SearchableDatabase&gt; addDatabases(requestBody)

[DEPRECATED] This endpoint is based on local file paths and will likely be replaced in future versions of this API.

Register existing custom database files with this SIRIUS instance, so that they become searchable.  &lt;p&gt;  Use this to make databases that already exist on disk available again, for example after reinstalling  SIRIUS or when sharing a database file with a colleague. The files are opened in place, not copied.

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
        List<String> requestBody = Arrays.asList(); // List<String> | local file paths of the database files (.siriusdb) to register. Each must exist,                         must not already be registered, and its name must not collide with an existing                         database.
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
| **requestBody** | [**List&lt;String&gt;**](String.md)| local file paths of the database files (.siriusdb) to register. Each must exist,                         must not already be registered, and its name must not collide with an existing                         database. | |

### Return type

[**List&lt;SearchableDatabase&gt;**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the databases that were successfully registered. Files that exist but could not be opened are          skipped and are absent from the result. |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **400** | A path does not exist or is not a file, is already registered, or its database name is already in use. No database is registered in that case. |  -  |


## createDatabase

> SearchableDatabase createDatabase(databaseId, searchableDatabaseParameters)

Create a new, empty custom database

Create a new, empty custom database.  &lt;p&gt;  The new database is created on disk and registered with this SIRIUS instance, so it can immediately be  used as a search parameter and imported into via the import endpoint. It contains no structures and no  reference spectra until something is imported.

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
        String databaseId = "databaseId_example"; // String | id of the new database. Must be URL-safe, that is letters, digits, '-' and '_' only,                     and must not be in use by another database.
        SearchableDatabaseParameters searchableDatabaseParameters = new SearchableDatabaseParameters(); // SearchableDatabaseParameters | optional settings for the new database. If omitted, the database is created in the                     default custom database directory with default settings. Supply a location to place                     the database file elsewhere, a displayName for the user interface, and                     matchRtOfReferenceSpectra for in-house libraries whose retention times are comparable                     to the measured samples.
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
| **databaseId** | **String**| id of the new database. Must be URL-safe, that is letters, digits, &#39;-&#39; and &#39;_&#39; only,                     and must not be in use by another database. | |
| **searchableDatabaseParameters** | [**SearchableDatabaseParameters**](SearchableDatabaseParameters.md)| optional settings for the new database. If omitted, the database is created in the                     default custom database directory with default settings. Supply a location to place                     the database file elsewhere, a displayName for the user interface, and                     matchRtOfReferenceSpectra for in-house libraries whose retention times are comparable                     to the measured samples. | [optional] |

### Return type

[**SearchableDatabase**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the created database. |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **400** | The database id is not a valid database name. It must consist of letters, digits, &#39;-&#39; and &#39;_&#39; only. |  -  |
| **409** | A database with this id already exists, or a file already exists at the target location. |  -  |


## getCustomDatabases

> List&lt;SearchableDatabase&gt; getCustomDatabases(includeStats, includeWithErrors)

List only the custom databases, that is the structure databases and spectral libraries the user has  created or added.

List only the custom databases, that is the structure databases and spectral libraries the user has  created or added. These are the databases that can be modified and imported into.

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
        Boolean includeStats = false; // Boolean | if true, the number of structures, formulas and reference spectra is included                           per database. Slower, since the database files have to be read.
        Boolean includeWithErrors = false; // Boolean | if true, databases that could not be loaded are listed as well, carrying the                           reason in their errorMessage field.
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
| **includeStats** | **Boolean**| if true, the number of structures, formulas and reference spectra is included                           per database. Slower, since the database files have to be read. | [optional] [default to false] |
| **includeWithErrors** | **Boolean**| if true, databases that could not be loaded are listed as well, carrying the                           reason in their errorMessage field. | [optional] [default to false] |

### Return type

[**List&lt;SearchableDatabase&gt;**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | all custom databases known to this SIRIUS instance. |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## getDatabase

> SearchableDatabase getDatabase(databaseId, includeStats)

Get a single searchable database by its id.

Get a single searchable database by its id.

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
        String databaseId = "databaseId_example"; // String | id of the database to retrieve, as reported by the listing endpoints.
        Boolean includeStats = true; // Boolean | if true (the default here), the number of structures, formulas and reference spectra                      is included.
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
| **databaseId** | **String**| id of the database to retrieve, as reported by the listing endpoints. | |
| **includeStats** | **Boolean**| if true (the default here), the number of structures, formulas and reference spectra                      is included. | [optional] [default to true] |

### Return type

[**SearchableDatabase**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the requested database. |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | No database with the given id exists. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## getDatabases

> List&lt;SearchableDatabase&gt; getDatabases(includeStats, includeWithErrors)

List all searchable databases, both the ones included in SIRIUS and the custom ones added by the user

List all searchable databases, both the ones included in SIRIUS and the custom ones added by the user.  &lt;p&gt;  A searchable database provides structures and reference spectra (optional), and can be selected as a search  parameter for structure database search and spectral library search. Note that every imported spectral  library also acts as a structure database.

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
        Boolean includeStats = false; // Boolean | if true, the number of structures, formulas and reference spectra is included                            per database. Computing these counts touches the database files, so requesting                            them is noticeably slower than a plain listing.
        Boolean includeWithErrors = false; // Boolean | if true, databases that could not be loaded are listed as well, carrying the                            reason in their errorMessage field. Use this to show a broken database to the                            user instead of silently hiding it.
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
| **includeStats** | **Boolean**| if true, the number of structures, formulas and reference spectra is included                            per database. Computing these counts touches the database files, so requesting                            them is noticeably slower than a plain listing. | [optional] [default to false] |
| **includeWithErrors** | **Boolean**| if true, databases that could not be loaded are listed as well, carrying the                            reason in their errorMessage field. Use this to show a broken database to the                            user instead of silently hiding it. | [optional] [default to false] |

### Return type

[**List&lt;SearchableDatabase&gt;**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | all databases known to this SIRIUS instance. |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## getDownloadableDatabases

> List&lt;DownloadableDatabase&gt; getDownloadableDatabases()

Get list of curated custom databases downloadable from the SIRIUS web service for local use

Get list of curated custom databases downloadable from the SIRIUS web service for local use.  &lt;p&gt;  [DEPRECATED] This endpoint will likely be removed or changed in future versions of this API.  [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.

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
        try {
            List<DownloadableDatabase> result = apiInstance.getDownloadableDatabases();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchableDatabasesApi#getDownloadableDatabases");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**List&lt;DownloadableDatabase&gt;**](DownloadableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | list of databases available for downloading. |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |


## getIncludedDatabases

> List&lt;SearchableDatabase&gt; getIncludedDatabases(includeStats)

List only the databases that ship with SIRIUS, such as PubChem and the bio databases.

List only the databases that ship with SIRIUS, such as PubChem and the bio databases. These are  read-only: they cannot be imported into, modified or removed.

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
        Boolean includeStats = false; // Boolean | if true, the number of structures, formulas and reference spectra is included per                      database. Slower, since the database files have to be read.
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
| **includeStats** | **Boolean**| if true, the number of structures, formulas and reference spectra is included per                      database. Slower, since the database files have to be read. | [optional] [default to false] |

### Return type

[**List&lt;SearchableDatabase&gt;**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | all databases included in SIRIUS. |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## getStructures

> PagedModelDatabaseStructure getStructures(databaseId, page, size, sort)

[EXPERIMENTAL] Page through the structures contained in a custom database

[EXPERIMENTAL] Page through the structures contained in a custom database.  &lt;p&gt;  Returns the stored structures with their name, SMILES, InChI, InChI key, molecular formula and mass.  Only custom databases are supported; the databases included in SIRIUS cannot be enumerated this way.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint  can change at any time, even in minor updates.

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
        String databaseId = "databaseId_example"; // String | id of the custom database to read from.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        try {
            PagedModelDatabaseStructure result = apiInstance.getStructures(databaseId, page, size, sort);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchableDatabasesApi#getStructures");
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
| **databaseId** | **String**| id of the custom database to read from. | |
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |

### Return type

[**PagedModelDatabaseStructure**](PagedModelDatabaseStructure.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | a page of the structures in the database. |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | No custom database with the given id exists. Databases included in SIRIUS cannot be enumerated. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


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
        String databaseId = "databaseId_example"; // String | id of the custom database to import into. Must exist.
        List<File> inputFiles = Arrays.asList(); // List<File> | files to import into project
        Integer bufferSize = 1000; // Integer | number of compounds to keep in memory before writing them to the                                  database. Raise it to speed up large imports on machines with enough RAM.
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
| **databaseId** | **String**| id of the custom database to import into. Must exist. | |
| **inputFiles** | **List&lt;File&gt;**| files to import into project | |
| **bufferSize** | **Integer**| number of compounds to keep in memory before writing them to the                                  database. Raise it to speed up large imports on machines with enough RAM. | [optional] [default to 1000] |
| **bioTransformerParameters** | [**BioTransformerParameters**](BioTransformerParameters.md)|  | [optional] |

### Return type

[**SearchableDatabase**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the affected database, including its updated statistics. |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | No database with the given id exists. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## removeDatabase

> removeDatabase(databaseId, delete)

Remove a custom database from this SIRIUS instance, and optionally delete it from disk

Remove a custom database from this SIRIUS instance, and optionally delete it from disk.  &lt;p&gt;  This is idempotent: removing a database that is not registered succeeds and does nothing, so a client  does not have to check first.

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
        String databaseId = "databaseId_example"; // String | id of the database to remove.
        Boolean delete = false; // Boolean | if true, the database file is deleted from disk and the data is lost. If false (the                    default), only the registration is removed and the file is kept, so the database can                    be registered again later.
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
| **databaseId** | **String**| id of the database to remove. | |
| **delete** | **Boolean**| if true, the database file is deleted from disk and the data is lost. If false (the                    default), only the registration is removed and the file is kept, so the database can                    be registered again later. | [optional] [default to false] |

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
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## updateDatabase

> SearchableDatabase updateDatabase(databaseId, searchableDatabaseParameters)

Change the settings of an existing custom database

Change the settings of an existing custom database.  &lt;p&gt;  NOT IMPLEMENTED YET: changing the display name and the retention time matching flag of an existing database  is not supported so far, and every request currently fails. The request and response shape is settled  though, so a client can be written against this endpoint today: it will start succeeding in a future  version without any change on the client side.  &lt;p&gt;  Until then, create a new database with the desired settings and import into it.

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
        String databaseId = "databaseId_example"; // String | id of the database to update.
        SearchableDatabaseParameters searchableDatabaseParameters = new SearchableDatabaseParameters(); // SearchableDatabaseParameters | the settings to apply.
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
| **databaseId** | **String**| id of the database to update. | |
| **searchableDatabaseParameters** | [**SearchableDatabaseParameters**](SearchableDatabaseParameters.md)| the settings to apply. | [optional] |

### Return type

[**SearchableDatabase**](SearchableDatabase.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the updated database. |  -  |
| **500** | Currently always, since updating custom databases is not implemented yet. This will become a normal server-side error once the endpoint is implemented. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |

