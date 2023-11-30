# ServerSentEventSseApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**listenToEvents**](ServerSentEventSseApi.md#listenToEvents) | **GET** /sse |  |



## listenToEvents

> SseEmitter listenToEvents(eventsToListenOn)



### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.ServerSentEventSseApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        ServerSentEventSseApi apiInstance = new ServerSentEventSseApi(defaultClient);
        List<String> eventsToListenOn = Arrays.asList(); // List<String> | 
        try {
            SseEmitter result = apiInstance.listenToEvents(eventsToListenOn);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ServerSentEventSseApi#listenToEvents");
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
| **eventsToListenOn** | [**List&lt;String&gt;**](String.md)|  | [optional] [enum: JOB, PROJECT, GUI_STATE] |

### Return type

[**SseEmitter**](SseEmitter.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: text/event-stream


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

