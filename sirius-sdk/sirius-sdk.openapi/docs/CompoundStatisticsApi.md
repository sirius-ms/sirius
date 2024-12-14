# CompoundStatisticsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**computeFoldChange**](CompoundStatisticsApi.md#computeFoldChange) | **PUT** /api/projects/{projectId}/compounds/statistics/foldchange/compute | **EXPERIMENTAL** Compute the fold change between two groups of runs |
| [**deleteFoldChange**](CompoundStatisticsApi.md#deleteFoldChange) | **DELETE** /api/projects/{projectId}/compounds/statistics/foldchange | **EXPERIMENTAL** Delete fold change |
| [**getFoldChange**](CompoundStatisticsApi.md#getFoldChange) | **GET** /api/projects/{projectId}/compounds/statistics/foldchange/{compoundId} | **EXPERIMENTAL** List all fold changes that are associated with a compound (group of ion identities) |
| [**getFoldChangeTable**](CompoundStatisticsApi.md#getFoldChangeTable) | **GET** /api/projects/{projectId}/compounds/statistics/foldchange | **EXPERIMENTAL** Get table of all fold changes in the project space |
| [**listFoldChange**](CompoundStatisticsApi.md#listFoldChange) | **GET** /api/projects/{projectId}/compounds/statistics/foldchange/page | **EXPERIMENTAL** Page of all fold changes in the project space |



## computeFoldChange

> Job computeFoldChange(projectId, left, right, aggregation, quantification, optFields)

**EXPERIMENTAL** Compute the fold change between two groups of runs

**EXPERIMENTAL** Compute the fold change between two groups of runs.   The runs need to be tagged and grouped.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundStatisticsApi apiInstance = new CompoundStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to compute the fold change in.
        String left = "left_example"; // String | name of the left tag group.
        String right = "right_example"; // String | name of the right tag group.
        String aggregation = "AVG"; // String | aggregation type.
        String quantification = "APEX_INTENSITY"; // String | quantification type.
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | job opt fields.
        try {
            Job result = apiInstance.computeFoldChange(projectId, left, right, aggregation, quantification, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundStatisticsApi#computeFoldChange");
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
| **projectId** | **String**| project-space to compute the fold change in. | |
| **left** | **String**| name of the left tag group. | |
| **right** | **String**| name of the right tag group. | |
| **aggregation** | **String**| aggregation type. | [optional] [default to AVG] [enum: AVG, MIN, MAX] |
| **quantification** | **String**| quantification type. | [optional] [default to APEX_INTENSITY] [enum: APEX_INTENSITY, AREA_UNDER_CURVE] |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| job opt fields. | [optional] |

### Return type

[**Job**](Job.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## deleteFoldChange

> deleteFoldChange(projectId, left, right, aggregation, quantification)

**EXPERIMENTAL** Delete fold change

**EXPERIMENTAL** Delete fold change.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundStatisticsApi apiInstance = new CompoundStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String left = "left_example"; // String | name of the left group.
        String right = "right_example"; // String | name of the right group.
        String aggregation = "AVG"; // String | aggregation type.
        String quantification = "APEX_INTENSITY"; // String | quantification type.
        try {
            apiInstance.deleteFoldChange(projectId, left, right, aggregation, quantification);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundStatisticsApi#deleteFoldChange");
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
| **left** | **String**| name of the left group. | |
| **right** | **String**| name of the right group. | |
| **aggregation** | **String**| aggregation type. | [optional] [default to AVG] [enum: AVG, MIN, MAX] |
| **quantification** | **String**| quantification type. | [optional] [default to APEX_INTENSITY] [enum: APEX_INTENSITY, AREA_UNDER_CURVE] |

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


## getFoldChange

> List&lt;CompoundFoldChange&gt; getFoldChange(projectId, compoundId)

**EXPERIMENTAL** List all fold changes that are associated with a compound (group of ion identities)

**EXPERIMENTAL** List all fold changes that are associated with a compound (group of ion identities).   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundStatisticsApi apiInstance = new CompoundStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String compoundId = "compoundId_example"; // String | id of the compound (group of ion identities) the fold changes are assigned to.
        try {
            List<CompoundFoldChange> result = apiInstance.getFoldChange(projectId, compoundId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundStatisticsApi#getFoldChange");
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
| **compoundId** | **String**| id of the compound (group of ion identities) the fold changes are assigned to. | |

### Return type

[**List&lt;CompoundFoldChange&gt;**](CompoundFoldChange.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | fold changes |  -  |


## getFoldChangeTable

> StatisticsTable getFoldChangeTable(projectId, aggregation, quantification)

**EXPERIMENTAL** Get table of all fold changes in the project space

**EXPERIMENTAL** Get table of all fold changes in the project space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundStatisticsApi apiInstance = new CompoundStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String aggregation = "AVG"; // String | aggregation type.
        String quantification = "APEX_INTENSITY"; // String | quantification type.
        try {
            StatisticsTable result = apiInstance.getFoldChangeTable(projectId, aggregation, quantification);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundStatisticsApi#getFoldChangeTable");
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
| **aggregation** | **String**| aggregation type. | [optional] [default to AVG] [enum: AVG, MIN, MAX] |
| **quantification** | **String**| quantification type. | [optional] [default to APEX_INTENSITY] [enum: APEX_INTENSITY, AREA_UNDER_CURVE] |

### Return type

[**StatisticsTable**](StatisticsTable.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | table of fold changes. |  -  |


## listFoldChange

> PageCompoundFoldChange listFoldChange(projectId, page, size, sort)

**EXPERIMENTAL** Page of all fold changes in the project space

**EXPERIMENTAL** Page of all fold changes in the project space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundStatisticsApi apiInstance = new CompoundStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        try {
            PageCompoundFoldChange result = apiInstance.listFoldChange(projectId, page, size, sort);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundStatisticsApi#listFoldChange");
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

### Return type

[**PageCompoundFoldChange**](PageCompoundFoldChange.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | fold changes. |  -  |

