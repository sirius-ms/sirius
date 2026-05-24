# ReactionsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**applyReactions**](ReactionsApi.md#applyReactions) | **POST** /api/reactions | Apply a sequence of reactions to a list of SMILES strings or structures from a database. |



## applyReactions

> PagedModelString applyReactions(reactionRequest, limit)

Apply a sequence of reactions to a list of SMILES strings or structures from a database.

[EXPERIMENTAL] Returns the final pool of SMILES strings.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.ReactionsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        ReactionsApi apiInstance = new ReactionsApi(defaultClient);
        ReactionRequest reactionRequest = new ReactionRequest(); // ReactionRequest | 
        Integer limit = 1000; // Integer | 
        try {
            PagedModelString result = apiInstance.applyReactions(reactionRequest, limit);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ReactionsApi#applyReactions");
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
| **reactionRequest** | [**ReactionRequest**](ReactionRequest.md)|  | |
| **limit** | **Integer**|  | [optional] [default to 1000] |

### Return type

[**PagedModelString**](PagedModelString.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

