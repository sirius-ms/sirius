# ExperimentalGuiApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**applyToGui**](ExperimentalGuiApi.md#applyToGui) | **PATCH** /api/projects/{projectId}/gui | Apply given changes to the running GUI instance. |
| [**closeGui**](ExperimentalGuiApi.md#closeGui) | **DELETE** /api/projects/{projectId}/gui | Close GUI instance of given project-space if available. |
| [**openGui**](ExperimentalGuiApi.md#openGui) | **POST** /api/projects/{projectId}/gui | Open GUI instance on specified project-space and bring the GUI window to foreground. |



## applyToGui

> applyToGui(projectId, guiParameters)

Apply given changes to the running GUI instance.

Apply given changes to the running GUI instance.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ExperimentalGuiApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        ExperimentalGuiApi apiInstance = new ExperimentalGuiApi(defaultClient);
        String projectId = "projectId_example"; // String | of project-space the GUI instance is connected to.
        GuiParameters guiParameters = new GuiParameters(); // GuiParameters | parameters that should be applied.
        try {
            apiInstance.applyToGui(projectId, guiParameters);
        } catch (ApiException e) {
            System.err.println("Exception when calling ExperimentalGuiApi#applyToGui");
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
| **projectId** | **String**| of project-space the GUI instance is connected to. | |
| **guiParameters** | [**GuiParameters**](GuiParameters.md)| parameters that should be applied. | |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## closeGui

> closeGui(projectId)

Close GUI instance of given project-space if available.

Close GUI instance of given project-space if available.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ExperimentalGuiApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        ExperimentalGuiApi apiInstance = new ExperimentalGuiApi(defaultClient);
        String projectId = "projectId_example"; // String | if project-space the GUI instance is connected to.
        try {
            apiInstance.closeGui(projectId);
        } catch (ApiException e) {
            System.err.println("Exception when calling ExperimentalGuiApi#closeGui");
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


## openGui

> openGui(projectId, readOnly, guiParameters)

Open GUI instance on specified project-space and bring the GUI window to foreground.

Open GUI instance on specified project-space and bring the GUI window to foreground.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ExperimentalGuiApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        ExperimentalGuiApi apiInstance = new ExperimentalGuiApi(defaultClient);
        String projectId = "projectId_example"; // String | of project-space the GUI instance will connect to.
        Boolean readOnly = true; // Boolean | open in read-only mode.
        GuiParameters guiParameters = new GuiParameters(); // GuiParameters | 
        try {
            apiInstance.openGui(projectId, readOnly, guiParameters);
        } catch (ApiException e) {
            System.err.println("Exception when calling ExperimentalGuiApi#openGui");
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
| **readOnly** | **Boolean**| open in read-only mode. | [optional] [default to true] |
| **guiParameters** | [**GuiParameters**](GuiParameters.md)|  | [optional] |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Created |  -  |

