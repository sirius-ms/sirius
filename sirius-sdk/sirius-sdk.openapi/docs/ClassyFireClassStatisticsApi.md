# ClassyFireClassStatisticsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**computeClassyfireClassFoldChangesExperimental**](ClassyFireClassStatisticsApi.md#computeClassyfireClassFoldChangesExperimental) | **PUT** /api/projects/{projectId}/classyfire-classes/statistics/foldchange/compute | [EXPERIMENTAL] Compute the fold change between two groups of runs |
| [**deleteClassyfireClassFoldChangesExperimental**](ClassyFireClassStatisticsApi.md#deleteClassyfireClassFoldChangesExperimental) | **DELETE** /api/projects/{projectId}/classyfire-classes/statistics/foldchanges | [EXPERIMENTAL] Delete fold changes |
| [**getClassyfireClassFoldChangeTableExperimental**](ClassyFireClassStatisticsApi.md#getClassyfireClassFoldChangeTableExperimental) | **GET** /api/projects/{projectId}/classyfire-classes/statistics/foldchanges/stats-table | [EXPERIMENTAL] Get table of all fold changes in the project space |



## computeClassyfireClassFoldChangesExperimental

> Job computeClassyfireClassFoldChangesExperimental(projectId, foldChangeJobSubmission, optFields)

[EXPERIMENTAL] Compute the fold change between two groups of runs

[EXPERIMENTAL] Compute the fold change between two groups of runs.  &lt;p&gt;  The runs need to be tagged and grouped.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.ClassyFireClassStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ClassyFireClassStatisticsApi apiInstance = new ClassyFireClassStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to compute the fold change in.
        FoldChangeJobSubmission foldChangeJobSubmission = new FoldChangeJobSubmission(); // FoldChangeJobSubmission | Parameters of fold change job
        List<JobOptField> optFields = Arrays.asList(); // List<JobOptField> | job opt fields.
        try {
            Job result = apiInstance.computeClassyfireClassFoldChangesExperimental(projectId, foldChangeJobSubmission, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ClassyFireClassStatisticsApi#computeClassyfireClassFoldChangesExperimental");
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
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## deleteClassyfireClassFoldChangesExperimental

> deleteClassyfireClassFoldChangesExperimental(projectId, leftGroupName, rightGroupName, aggregation, quantification)

[EXPERIMENTAL] Delete fold changes

[EXPERIMENTAL] Delete fold changes.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.ClassyFireClassStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ClassyFireClassStatisticsApi apiInstance = new ClassyFireClassStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String leftGroupName = "leftGroupName_example"; // String | name of the left group.
        String rightGroupName = "rightGroupName_example"; // String | name of the right group.
        AggregationType aggregation = AggregationType.fromValue("AVG"); // AggregationType | 
        QuantMeasure quantification = QuantMeasure.fromValue("APEX_INTENSITY"); // QuantMeasure | 
        try {
            apiInstance.deleteClassyfireClassFoldChangesExperimental(projectId, leftGroupName, rightGroupName, aggregation, quantification);
        } catch (ApiException e) {
            System.err.println("Exception when calling ClassyFireClassStatisticsApi#deleteClassyfireClassFoldChangesExperimental");
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
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |


## getClassyfireClassFoldChangeTableExperimental

> StatisticsTable getClassyfireClassFoldChangeTableExperimental(projectId, aggregation, quantification)

[EXPERIMENTAL] Get table of all fold changes in the project space

[EXPERIMENTAL] Get table of all fold changes in the project space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.ClassyFireClassStatisticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ClassyFireClassStatisticsApi apiInstance = new ClassyFireClassStatisticsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        AggregationType aggregation = AggregationType.fromValue("AVG"); // AggregationType | aggregation type.
        QuantMeasure quantification = QuantMeasure.fromValue("APEX_INTENSITY"); // QuantMeasure | quantification type.
        try {
            StatisticsTable result = apiInstance.getClassyfireClassFoldChangeTableExperimental(projectId, aggregation, quantification);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ClassyFireClassStatisticsApi#getClassyfireClassFoldChangeTableExperimental");
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
- **Accept**: application/json, application/problem+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | table of fold changes. |  -  |
| **500** | Unexpected server-side error. The problem detail carries the reason. |  -  |
| **404** | The referenced object does not exist in this SIRIUS instance or project. |  -  |
| **400** | The request body or a parameter is malformed or violates a constraint. |  -  |

