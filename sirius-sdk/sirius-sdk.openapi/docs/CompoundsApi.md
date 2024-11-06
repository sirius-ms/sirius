# CompoundsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addCompounds**](CompoundsApi.md#addCompounds) | **POST** /api/projects/{projectId}/compounds | Import Compounds and its contained features. |
| [**deleteCompound**](CompoundsApi.md#deleteCompound) | **DELETE** /api/projects/{projectId}/compounds/{compoundId} | Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space. |
| [**getCompound**](CompoundsApi.md#getCompound) | **GET** /api/projects/{projectId}/compounds/{compoundId} | Get compound (group of ion identities) with the given identifier from the specified project-space. |
| [**getCompoundTraces**](CompoundsApi.md#getCompoundTraces) | **GET** /api/projects/{projectId}/compounds/{compoundId}/traces |  |
| [**getCompounds**](CompoundsApi.md#getCompounds) | **GET** /api/projects/{projectId}/compounds | List of all available compounds (group of ion identities) in the given project-space. |
| [**getCompoundsPaged**](CompoundsApi.md#getCompoundsPaged) | **GET** /api/projects/{projectId}/compounds/page | Page of available compounds (group of ion identities) in the given project-space. |
| [**getQuantification**](CompoundsApi.md#getQuantification) | **GET** /api/projects/{projectId}/compounds/quantification | Returns the full quantification table. |
| [**getQuantificationRow**](CompoundsApi.md#getQuantificationRow) | **GET** /api/projects/{projectId}/compounds/{compoundId}/quantification | Returns a single quantification table row for the given feature. |



## addCompounds

> List&lt;Compound&gt; addCompounds(projectId, compoundImport, profile, optFields, optFieldsFeatures)

Import Compounds and its contained features.

Import Compounds and its contained features. Compounds and Features must not exist in the project.  Otherwise, they will exist twice.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to import into.
        List<CompoundImport> compoundImport = Arrays.asList(); // List<CompoundImport> | the compound data to be imported
        InstrumentProfile profile = InstrumentProfile.fromValue("QTOF"); // InstrumentProfile | profile describing the instrument used to measure the data. Used to merge spectra.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' to override defaults.
        List<AlignedFeatureOptField> optFieldsFeatures = Arrays.asList(); // List<AlignedFeatureOptField> | set of optional fields of the nested features to be included. Use 'none' to override defaults.
        try {
            List<Compound> result = apiInstance.addCompounds(projectId, compoundImport, profile, optFields, optFieldsFeatures);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#addCompounds");
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
| **projectId** | **String**| project-space to import into. | |
| **compoundImport** | [**List&lt;CompoundImport&gt;**](CompoundImport.md)| the compound data to be imported | |
| **profile** | [**InstrumentProfile**](.md)| profile describing the instrument used to measure the data. Used to merge spectra. | [optional] [enum: QTOF, ORBITRAP] |
| **optFields** | [**List&lt;CompoundOptField&gt;**](CompoundOptField.md)| set of optional fields to be included. Use &#39;none&#39; to override defaults. | [optional] |
| **optFieldsFeatures** | [**List&lt;AlignedFeatureOptField&gt;**](AlignedFeatureOptField.md)| set of optional fields of the nested features to be included. Use &#39;none&#39; to override defaults. | [optional] |

### Return type

[**List&lt;Compound&gt;**](Compound.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the Compounds that have been imported with specified optional fields |  -  |


## deleteCompound

> deleteCompound(projectId, compoundId)

Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.

Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

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


## getCompound

> Compound getCompound(projectId, compoundId, optFields, optFieldsFeatures)

Get compound (group of ion identities) with the given identifier from the specified project-space.

Get compound (group of ion identities) with the given identifier from the specified project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

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


## getCompoundTraces

> TraceSet getCompoundTraces(projectId, compoundId, featureId)



### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | 
        String compoundId = "compoundId_example"; // String | 
        String featureId = ""; // String | 
        try {
            TraceSet result = apiInstance.getCompoundTraces(projectId, compoundId, featureId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getCompoundTraces");
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
| **projectId** | **String**|  | |
| **compoundId** | **String**|  | |
| **featureId** | **String**|  | [optional] [default to ] |

### Return type

[**TraceSet**](TraceSet.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getCompounds

> List&lt;Compound&gt; getCompounds(projectId, optFields, optFieldsFeatures)

List of all available compounds (group of ion identities) in the given project-space.

List of all available compounds (group of ion identities) in the given project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        List<AlignedFeatureOptField> optFieldsFeatures = Arrays.asList(); // List<AlignedFeatureOptField> | 
        try {
            List<Compound> result = apiInstance.getCompounds(projectId, optFields, optFieldsFeatures);
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
| **optFields** | [**List&lt;CompoundOptField&gt;**](CompoundOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |
| **optFieldsFeatures** | [**List&lt;AlignedFeatureOptField&gt;**](AlignedFeatureOptField.md)|  | [optional] |

### Return type

[**List&lt;Compound&gt;**](Compound.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Compounds with additional optional fields (if specified). |  -  |


## getCompoundsPaged

> PageCompound getCompoundsPaged(projectId, page, size, sort, optFields, optFieldsFeatures)

Page of available compounds (group of ion identities) in the given project-space.

Page of available compounds (group of ion identities) in the given project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        List<AlignedFeatureOptField> optFieldsFeatures = Arrays.asList(); // List<AlignedFeatureOptField> | 
        try {
            PageCompound result = apiInstance.getCompoundsPaged(projectId, page, size, sort, optFields, optFieldsFeatures);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getCompoundsPaged");
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


## getQuantification

> QuantificationTable getQuantification(projectId, type)

Returns the full quantification table.

Returns the full quantification table. The quantification table contains a quantification of the features within all  runs they are contained in.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String type = "APEX_HEIGHT"; // String | quantification type.
        try {
            QuantificationTable result = apiInstance.getQuantification(projectId, type);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getQuantification");
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
| **type** | **String**| quantification type. | [optional] [default to APEX_HEIGHT] [enum: APEX_HEIGHT, AREA_UNDER_CURVE, APEX_MASS, AVERAGE_MASS, APEX_RT, FULL_WIDTH_HALF_MAX] |

### Return type

[**QuantificationTable**](QuantificationTable.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getQuantificationRow

> QuantificationTable getQuantificationRow(projectId, compoundId, type)

Returns a single quantification table row for the given feature.

Returns a single quantification table row for the given feature. The quantification table contains a quantification of the feature within all  samples it is contained in.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String compoundId = "compoundId_example"; // String | compound which should be read out
        String type = "APEX_HEIGHT"; // String | quantification type.
        try {
            QuantificationTable result = apiInstance.getQuantificationRow(projectId, compoundId, type);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getQuantificationRow");
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
| **compoundId** | **String**| compound which should be read out | |
| **type** | **String**| quantification type. | [optional] [default to APEX_HEIGHT] [enum: APEX_HEIGHT, AREA_UNDER_CURVE, APEX_MASS, AVERAGE_MASS, APEX_RT, FULL_WIDTH_HALF_MAX] |

### Return type

[**QuantificationTable**](QuantificationTable.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

