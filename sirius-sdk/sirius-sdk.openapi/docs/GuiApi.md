# GuiApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**closeGui**](GuiApi.md#closeGui) | **DELETE** /api/projects/{projectId}/gui | Close GUI instance of given project-space if available. |
| [**getGuis**](GuiApi.md#getGuis) | **GET** /api/guis | Get list of currently running gui windows, managed by this SIRIUS instance. |
| [**openGui**](GuiApi.md#openGui) | **POST** /api/projects/{projectId}/gui | Open GUI instance on specified project-space and bring the GUI window to foreground. |



## closeGui

> Boolean closeGui(projectId, closeProject)

Close GUI instance of given project-space if available.

Close GUI instance of given project-space if available.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.GuiApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        GuiApi apiInstance = new GuiApi(defaultClient);
        String projectId = "projectId_example"; // String | if project-space the GUI instance is connected to.
        Boolean closeProject = true; // Boolean | 
        try {
            Boolean result = apiInstance.closeGui(projectId, closeProject);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling GuiApi#closeGui");
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
| **projectId** | **String**| if project-space the GUI instance is connected to. | |
| **closeProject** | **Boolean**|  | [optional] |

### Return type

**Boolean**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getGuis

> List&lt;GuiInfo&gt; getGuis()

Get list of currently running gui windows, managed by this SIRIUS instance.

Get list of currently running gui windows, managed by this SIRIUS instance.  Note this will not show any Clients that are connected from a separate process!

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.GuiApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        GuiApi apiInstance = new GuiApi(defaultClient);
        try {
            List<GuiInfo> result = apiInstance.getGuis();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling GuiApi#getGuis");
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

[**List&lt;GuiInfo&gt;**](GuiInfo.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | List of GUI windows that are currently managed by this SIRIUS instance. |  -  |


## openGui

> openGui(projectId)

Open GUI instance on specified project-space and bring the GUI window to foreground.

Open GUI instance on specified project-space and bring the GUI window to foreground.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.GuiApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        GuiApi apiInstance = new GuiApi(defaultClient);
        String projectId = "projectId_example"; // String | of project-space the GUI instance will connect to.
        try {
            apiInstance.openGui(projectId);
        } catch (ApiException e) {
            System.err.println("Exception when calling GuiApi#openGui");
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
| **projectId** | **String**| of project-space the GUI instance will connect to. | |

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
| **201** | Created |  -  |

