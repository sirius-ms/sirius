# FeatureStatisticsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**computeAlignedFeatureFoldChangesExperimental**](FeatureStatisticsApi.md#computeAlignedFeatureFoldChangesExperimental) | **PUT** /api/projects/{projectId}/aligned-features/statistics/foldchange/compute | [EXPERIMENTAL] Compute the fold change between two groups of runs |
| [**deleteAlignedFeatureFoldChangesExperimental**](FeatureStatisticsApi.md#deleteAlignedFeatureFoldChangesExperimental) | **DELETE** /api/projects/{projectId}/aligned-features/statistics/foldchanges | [EXPERIMENTAL] Delete fold changes |
| [**getAlignedFeatureFoldChangeTableExperimental**](FeatureStatisticsApi.md#getAlignedFeatureFoldChangeTableExperimental) | **GET** /api/projects/{projectId}/aligned-features/statistics/foldchanges/stats-table | [EXPERIMENTAL] Get table of all fold changes in the project space |
| [**getAlignedFeatureFoldChangesExperimental**](FeatureStatisticsApi.md#getAlignedFeatureFoldChangesExperimental) | **GET** /api/projects/{projectId}/aligned-features/statistics/foldchanges | [EXPERIMENTAL] Get fold changes |



## computeAlignedFeatureFoldChangesExperimental

> Job computeAlignedFeatureFoldChangesExperimental(projectId, leftGroupName, rightGroupName, aggregation, quantification, optFields)

[EXPERIMENTAL] Compute the fold change between two groups of runs

[EXPERIMENTAL] Compute the fold change between two groups of runs.  &lt;p&gt;  The runs need to be tagged and grouped.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        String leftGroupName = "leftGroupName_example"; // String | name of the left tag group.
        String rightGroupName = "rightGroupName_example"; // String | name of the right tag group.
        AggregationType aggregation = AggregationType.fromValue("AVG"); // AggregationType | aggregation type.
        QuantMeasure quantification = QuantMeasure.fromValue("APEX_INTENSITY"); // QuantMeasure | quantification type.
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | job opt fields.
        try {
            Job result = apiInstance.computeAlignedFeatureFoldChangesExperimental(projectId, leftGroupName, rightGroupName, aggregation, quantification, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeatureStatisticsApi#computeAlignedFeatureFoldChangesExperimental");
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
| **leftGroupName** | **String**| name of the left tag group. | |
| **rightGroupName** | **String**| name of the right tag group. | |
| **aggregation** | [**AggregationType**](.md)| aggregation type. | [optional] [enum: AVG, MIN, MAX, MEDIAN] |
| **quantification** | [**QuantMeasure**](.md)| quantification type. | [optional] [enum: APEX_INTENSITY, AREA_UNDER_CURVE] |
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


## deleteAlignedFeatureFoldChangesExperimental

> deleteAlignedFeatureFoldChangesExperimental(projectId, leftGroupName, rightGroupName, aggregation, quantification)

[EXPERIMENTAL] Delete fold changes

[EXPERIMENTAL] Delete fold changes.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        String leftGroupName = "leftGroupName_example"; // String | name of the left group.
        String rightGroupName = "rightGroupName_example"; // String | name of the right group.
        AggregationType aggregation = AggregationType.fromValue("AVG"); // AggregationType | 
        QuantMeasure quantification = QuantMeasure.fromValue("APEX_INTENSITY"); // QuantMeasure | 
        try {
            apiInstance.deleteAlignedFeatureFoldChangesExperimental(projectId, leftGroupName, rightGroupName, aggregation, quantification);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeatureStatisticsApi#deleteAlignedFeatureFoldChangesExperimental");
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
| **leftGroupName** | **String**| name of the left group. | |
| **rightGroupName** | **String**| name of the right group. | |
| **aggregation** | [**AggregationType**](.md)|  | [optional] [enum: AVG, MIN, MAX, MEDIAN] |
| **quantification** | [**QuantMeasure**](.md)|  | [optional] [enum: APEX_INTENSITY, AREA_UNDER_CURVE] |

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


## getAlignedFeatureFoldChangeTableExperimental

> StatisticsTable getAlignedFeatureFoldChangeTableExperimental(projectId, aggregation, quantification)

[EXPERIMENTAL] Get table of all fold changes in the project space

[EXPERIMENTAL] Get table of all fold changes in the project space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        AggregationType aggregation = AggregationType.fromValue("AVG"); // AggregationType | aggregation type.
        QuantMeasure quantification = QuantMeasure.fromValue("APEX_INTENSITY"); // QuantMeasure | quantification type.
        try {
            StatisticsTable result = apiInstance.getAlignedFeatureFoldChangeTableExperimental(projectId, aggregation, quantification);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeatureStatisticsApi#getAlignedFeatureFoldChangeTableExperimental");
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
| **aggregation** | [**AggregationType**](.md)| aggregation type. | [optional] [enum: AVG, MIN, MAX, MEDIAN] |
| **quantification** | [**QuantMeasure**](.md)| quantification type. | [optional] [enum: APEX_INTENSITY, AREA_UNDER_CURVE] |

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


## getAlignedFeatureFoldChangesExperimental

> List&lt;FoldChange&gt; getAlignedFeatureFoldChangesExperimental(projectId, leftGroupName, rightGroupName, aggregation, quantification)

[EXPERIMENTAL] Get fold changes

[EXPERIMENTAL] Get fold changes.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        String leftGroupName = "leftGroupName_example"; // String | name of the left group.
        String rightGroupName = "rightGroupName_example"; // String | name of the right group.
        AggregationType aggregation = AggregationType.fromValue("AVG"); // AggregationType | 
        QuantMeasure quantification = QuantMeasure.fromValue("APEX_INTENSITY"); // QuantMeasure | 
        try {
            List<FoldChange> result = apiInstance.getAlignedFeatureFoldChangesExperimental(projectId, leftGroupName, rightGroupName, aggregation, quantification);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeatureStatisticsApi#getAlignedFeatureFoldChangesExperimental");
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
| **leftGroupName** | **String**| name of the left group. | |
| **rightGroupName** | **String**| name of the right group. | |
| **aggregation** | [**AggregationType**](.md)|  | [optional] [enum: AVG, MIN, MAX, MEDIAN] |
| **quantification** | [**QuantMeasure**](.md)|  | [optional] [enum: APEX_INTENSITY, AREA_UNDER_CURVE] |

### Return type

[**List&lt;FoldChange&gt;**](FoldChange.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

