# ExperimentalDataQualityApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getAlignedFeaturesQuality**](ExperimentalDataQualityApi.md#getAlignedFeaturesQuality) | **GET** /api/projects/{projectId}/aligned-features-quality | List of data quality information for features (aligned over runs) in the given project-space. |
| [**getAlignedFeaturesQuality1**](ExperimentalDataQualityApi.md#getAlignedFeaturesQuality1) | **GET** /api/projects/{projectId}/aligned-features-quality/{alignedFeatureId} | Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space. |
| [**getAlignedFeaturesQualityPaged**](ExperimentalDataQualityApi.md#getAlignedFeaturesQualityPaged) | **GET** /api/projects/{projectId}/aligned-features-quality/page | Page of data quality information for features (aligned over runs) in the given project-space. |



## getAlignedFeaturesQuality

> List&lt;AlignedFeatureQuality&gt; getAlignedFeaturesQuality(projectId)

List of data quality information for features (aligned over runs) in the given project-space.

List of data quality information for features (aligned over runs) in the given project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ExperimentalDataQualityApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ExperimentalDataQualityApi apiInstance = new ExperimentalDataQualityApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        try {
            List<AlignedFeatureQuality> result = apiInstance.getAlignedFeaturesQuality(projectId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ExperimentalDataQualityApi#getAlignedFeaturesQuality");
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

[**List&lt;AlignedFeatureQuality&gt;**](AlignedFeatureQuality.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | AlignedFeatureQuality quality information of the respective feature. |  -  |


## getAlignedFeaturesQuality1

> AlignedFeatureQuality getAlignedFeaturesQuality1(projectId, alignedFeatureId)

Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.

Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ExperimentalDataQualityApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ExperimentalDataQualityApi apiInstance = new ExperimentalDataQualityApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | identifier of feature (aligned over runs) to access.
        try {
            AlignedFeatureQuality result = apiInstance.getAlignedFeaturesQuality1(projectId, alignedFeatureId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ExperimentalDataQualityApi#getAlignedFeaturesQuality1");
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
| **alignedFeatureId** | **String**| identifier of feature (aligned over runs) to access. | |

### Return type

[**AlignedFeatureQuality**](AlignedFeatureQuality.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | AlignedFeatureQuality quality information of the respective feature. |  -  |


## getAlignedFeaturesQualityPaged

> PageAlignedFeatureQuality getAlignedFeaturesQualityPaged(projectId, page, size, sort)

Page of data quality information for features (aligned over runs) in the given project-space.

Page of data quality information for features (aligned over runs) in the given project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ExperimentalDataQualityApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ExperimentalDataQualityApi apiInstance = new ExperimentalDataQualityApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        try {
            PageAlignedFeatureQuality result = apiInstance.getAlignedFeaturesQualityPaged(projectId, page, size, sort);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ExperimentalDataQualityApi#getAlignedFeaturesQualityPaged");
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

[**PageAlignedFeatureQuality**](PageAlignedFeatureQuality.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | AlignedFeatureQuality quality information of the respective feature. |  -  |

