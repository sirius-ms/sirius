# FeatureStatisticsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**computeFoldChange1**](FeatureStatisticsApi.md#computeFoldChange1) | **PUT** /api/projects/{projectId}/aligned-features/statistics/foldchange/compute | **EXPERIMENTAL** Compute the fold change between two groups of runs |
| [**deleteFoldChange1**](FeatureStatisticsApi.md#deleteFoldChange1) | **DELETE** /api/projects/{projectId}/aligned-features/statistics/foldchange | **EXPERIMENTAL** Delete fold change |
| [**getFoldChange1**](FeatureStatisticsApi.md#getFoldChange1) | **GET** /api/projects/{projectId}/aligned-features/statistics/foldchange/{alignedFeatureId} | **EXPERIMENTAL** List all fold changes that are associated with a feature (aligned over runs) |
| [**listFoldChange1**](FeatureStatisticsApi.md#listFoldChange1) | **GET** /api/projects/{projectId}/aligned-features/statistics/foldchange/page | **EXPERIMENTAL** Page of all fold changes in the project space |



## computeFoldChange1

> Job computeFoldChange1(projectId, left, right, aggregation, quantification, optFields)

**EXPERIMENTAL** Compute the fold change between two groups of runs

**EXPERIMENTAL** Compute the fold change between two groups of runs.   The runs need to be tagged and grouped.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.FeatureStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        FeatureStatisticsApi apiInstance = new FeatureStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to compute the fold change in.
        String left = "left_example"; // String | name of the left tag group.
        String right = "right_example"; // String | name of the right tag group.
        String aggregation = "AVG"; // String | aggregation type.
        String quantification = "APEX_INTENSITY"; // String | quantification type.
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | job opt fields.
        try {
            Job result = apiInstance.computeFoldChange1(projectId, left, right, aggregation, quantification, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeatureStatisticsApi#computeFoldChange1");
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
| **aggregation** | **String**| aggregation type. | [optional] [default to AVG] [enum: AVG, MIN, MAX, MEDIAN] |
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


## deleteFoldChange1

> deleteFoldChange1(projectId, left, right, aggregation, quantification)

**EXPERIMENTAL** Delete fold change

**EXPERIMENTAL** Delete fold change.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.FeatureStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        FeatureStatisticsApi apiInstance = new FeatureStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String left = "left_example"; // String | name of the left group.
        String right = "right_example"; // String | name of the right group.
        String aggregation = "AVG"; // String | aggregation type.
        String quantification = "APEX_INTENSITY"; // String | quantification type.
        try {
            apiInstance.deleteFoldChange1(projectId, left, right, aggregation, quantification);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeatureStatisticsApi#deleteFoldChange1");
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
| **aggregation** | **String**| aggregation type. | [optional] [default to AVG] [enum: AVG, MIN, MAX, MEDIAN] |
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


## getFoldChange1

> List&lt;AlignedFeatureFoldChange&gt; getFoldChange1(projectId, alignedFeatureId)

**EXPERIMENTAL** List all fold changes that are associated with a feature (aligned over runs)

**EXPERIMENTAL** List all fold changes that are associated with a feature (aligned over runs).   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.FeatureStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        FeatureStatisticsApi apiInstance = new FeatureStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | id of the feature (aligend over runs) the fold changes are assigned to.
        try {
            List<AlignedFeatureFoldChange> result = apiInstance.getFoldChange1(projectId, alignedFeatureId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeatureStatisticsApi#getFoldChange1");
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
| **alignedFeatureId** | **String**| id of the feature (aligend over runs) the fold changes are assigned to. | |

### Return type

[**List&lt;AlignedFeatureFoldChange&gt;**](AlignedFeatureFoldChange.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | fold changes |  -  |


## listFoldChange1

> PageAlignedFeatureFoldChange listFoldChange1(projectId, page, size, sort)

**EXPERIMENTAL** Page of all fold changes in the project space

**EXPERIMENTAL** Page of all fold changes in the project space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.FeatureStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        FeatureStatisticsApi apiInstance = new FeatureStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        try {
            PageAlignedFeatureFoldChange result = apiInstance.listFoldChange1(projectId, page, size, sort);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeatureStatisticsApi#listFoldChange1");
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

[**PageAlignedFeatureFoldChange**](PageAlignedFeatureFoldChange.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | fold changes. |  -  |

