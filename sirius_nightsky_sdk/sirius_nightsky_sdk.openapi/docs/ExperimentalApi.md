# ExperimentalApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getAlignedFeaturesQuality**](ExperimentalApi.md#getAlignedFeaturesQuality) | **GET** /api/projects/{projectId}/aligned-features/{alignedFeatureId}/quality-report | Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space. |



## getAlignedFeaturesQuality

> AlignedFeatureQuality getAlignedFeaturesQuality(projectId, alignedFeatureId)

Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.

Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.   EXPERIMENTAL: Endpoint is not part of the stable API specification and might change in minor updates.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ExperimentalApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ExperimentalApi apiInstance = new ExperimentalApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | identifier of feature (aligned over runs) to access.
        try {
            AlignedFeatureQuality result = apiInstance.getAlignedFeaturesQuality(projectId, alignedFeatureId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ExperimentalApi#getAlignedFeaturesQuality");
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

