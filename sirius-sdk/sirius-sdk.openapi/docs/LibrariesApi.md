# LibrariesApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getLibraries**](LibrariesApi.md#getLibraries) | **GET** /api/libraries | Get SIRIUS libraries. |



## getLibraries

> List&lt;LibraryInfo&gt; getLibraries()

Get SIRIUS libraries.

Get SIRIUS libraries.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.LibrariesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        LibrariesApi apiInstance = new LibrariesApi(defaultClient);
        try {
            List<LibraryInfo> result = apiInstance.getLibraries();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling LibrariesApi#getLibraries");
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

[**List&lt;LibraryInfo&gt;**](LibraryInfo.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | list of libraries available for downloading. |  -  |

