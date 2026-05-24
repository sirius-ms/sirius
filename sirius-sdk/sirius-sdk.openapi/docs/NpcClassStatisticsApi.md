# NpcClassStatisticsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**computeNpcClassFoldChangesExperimental**](NpcClassStatisticsApi.md#computeNpcClassFoldChangesExperimental) | **PUT** /api/projects/{projectId}/npc-classes/statistics/foldchange/compute | [EXPERIMENTAL] Compute the fold change between two groups of runs |
| [**deleteNpcClassFoldChangesExperimental**](NpcClassStatisticsApi.md#deleteNpcClassFoldChangesExperimental) | **DELETE** /api/projects/{projectId}/npc-classes/statistics/foldchanges | [EXPERIMENTAL] Delete fold changes |
| [**getNpcClassFoldChangeTableExperimental**](NpcClassStatisticsApi.md#getNpcClassFoldChangeTableExperimental) | **GET** /api/projects/{projectId}/npc-classes/statistics/foldchanges/stats-table | [EXPERIMENTAL] Get table of all fold changes in the project space |



## computeNpcClassFoldChangesExperimental

> Job computeNpcClassFoldChangesExperimental(projectId, foldChangeJobSubmission, optFields)

[EXPERIMENTAL] Compute the fold change between two groups of runs

[EXPERIMENTAL] Compute the fold change between two groups of runs.  &lt;p&gt;  The runs need to be tagged and grouped.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.NpcClassStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        NpcClassStatisticsApi apiInstance = new NpcClassStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to compute the fold change in.
        FoldChangeJobSubmission foldChangeJobSubmission = new FoldChangeJobSubmission(); // FoldChangeJobSubmission | Parameters of fold change job
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | job opt fields.
        try {
            Job result = apiInstance.computeNpcClassFoldChangesExperimental(projectId, foldChangeJobSubmission, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling NpcClassStatisticsApi#computeNpcClassFoldChangesExperimental");
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
| **foldChangeJobSubmission** | [**FoldChangeJobSubmission**](FoldChangeJobSubmission.md)| Parameters of fold change job | |
| **optFields** | [**List&lt;JobOptField&gt;**](JobOptField.md)| job opt fields. | [optional] |

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
| **200** | OK |  -  |


## deleteNpcClassFoldChangesExperimental

> deleteNpcClassFoldChangesExperimental(projectId, leftGroupName, rightGroupName, aggregation, quantification)

[EXPERIMENTAL] Delete fold changes

[EXPERIMENTAL] Delete fold changes.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.NpcClassStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        NpcClassStatisticsApi apiInstance = new NpcClassStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String leftGroupName = "leftGroupName_example"; // String | name of the left group.
        String rightGroupName = "rightGroupName_example"; // String | name of the right group.
        AggregationType aggregation = AggregationType.fromValue("AVG"); // AggregationType | 
        QuantMeasure quantification = QuantMeasure.fromValue("APEX_INTENSITY"); // QuantMeasure | 
        try {
            apiInstance.deleteNpcClassFoldChangesExperimental(projectId, leftGroupName, rightGroupName, aggregation, quantification);
        } catch (ApiException e) {
            System.err.println("Exception when calling NpcClassStatisticsApi#deleteNpcClassFoldChangesExperimental");
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
| **aggregation** | [**AggregationType**](.md)|  | [optional] [enum: AVG, MIN, MAX] |
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


## getNpcClassFoldChangeTableExperimental

> StatisticsTable getNpcClassFoldChangeTableExperimental(projectId, aggregation, quantification)

[EXPERIMENTAL] Get table of all fold changes in the project space

[EXPERIMENTAL] Get table of all fold changes in the project space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.NpcClassStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        NpcClassStatisticsApi apiInstance = new NpcClassStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        AggregationType aggregation = AggregationType.fromValue("AVG"); // AggregationType | aggregation type.
        QuantMeasure quantification = QuantMeasure.fromValue("APEX_INTENSITY"); // QuantMeasure | quantification type.
        try {
            StatisticsTable result = apiInstance.getNpcClassFoldChangeTableExperimental(projectId, aggregation, quantification);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling NpcClassStatisticsApi#getNpcClassFoldChangeTableExperimental");
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
| **aggregation** | [**AggregationType**](.md)| aggregation type. | [optional] [enum: AVG, MIN, MAX] |
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

