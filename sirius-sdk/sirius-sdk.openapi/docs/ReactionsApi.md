# ReactionsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addReaction**](ReactionsApi.md#addReaction) | **POST** /api/reactions/library | [EXPERIMENTAL] Add a new reaction to the library. |
| [**addSequence**](ReactionsApi.md#addSequence) | **POST** /api/reactions/sequences/library | [EXPERIMENTAL] Add a new reaction sequence to the library. |
| [**applyReactions**](ReactionsApi.md#applyReactions) | **POST** /api/reactions | [EXPERIMENTAL] Apply a sequence of reactions to a list of SMILES strings or structures from a database. |
| [**deleteReaction**](ReactionsApi.md#deleteReaction) | **DELETE** /api/reactions/library/{name} | [EXPERIMENTAL] Delete a reaction from the library. |
| [**deleteSequence**](ReactionsApi.md#deleteSequence) | **DELETE** /api/reactions/sequences/library/{name} | [EXPERIMENTAL] Delete a reaction sequence from the library. |
| [**getReaction**](ReactionsApi.md#getReaction) | **GET** /api/reactions/library/{name} | [EXPERIMENTAL] Get a specific reaction from the library by name. |
| [**getReactions**](ReactionsApi.md#getReactions) | **GET** /api/reactions | [EXPERIMENTAL] Get all reactions from the library. |
| [**getSequence**](ReactionsApi.md#getSequence) | **GET** /api/reactions/sequences/{name} | [EXPERIMENTAL] Get a specific reaction sequence from the library by name. |
| [**getSequences**](ReactionsApi.md#getSequences) | **GET** /api/reactions/sequences | [EXPERIMENTAL] Get all reaction sequences from the library. |



## addReaction

> addReaction(reaction)

[EXPERIMENTAL] Add a new reaction to the library.

[EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        Reaction reaction = new Reaction(); // Reaction | 
        try {
            apiInstance.addReaction(reaction);
        } catch (ApiException e) {
            System.err.println("Exception when calling ReactionsApi#addReaction");
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
| **reaction** | [**Reaction**](Reaction.md)|  | |

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


## addSequence

> addSequence(reactionSequence)

[EXPERIMENTAL] Add a new reaction sequence to the library.

[EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        ReactionSequence reactionSequence = new ReactionSequence(); // ReactionSequence | 
        try {
            apiInstance.addSequence(reactionSequence);
        } catch (ApiException e) {
            System.err.println("Exception when calling ReactionsApi#addSequence");
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
| **reactionSequence** | [**ReactionSequence**](ReactionSequence.md)|  | |

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


## applyReactions

> PagedModelString applyReactions(reactionRequest, limit)

[EXPERIMENTAL] Apply a sequence of reactions to a list of SMILES strings or structures from a database.

[EXPERIMENTAL] Returns the final pool of SMILES strings. [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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


## deleteReaction

> deleteReaction(name)

[EXPERIMENTAL] Delete a reaction from the library.

[EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        String name = "name_example"; // String | 
        try {
            apiInstance.deleteReaction(name);
        } catch (ApiException e) {
            System.err.println("Exception when calling ReactionsApi#deleteReaction");
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
| **name** | **String**|  | |

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


## deleteSequence

> deleteSequence(name)

[EXPERIMENTAL] Delete a reaction sequence from the library.

[EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        String name = "name_example"; // String | 
        try {
            apiInstance.deleteSequence(name);
        } catch (ApiException e) {
            System.err.println("Exception when calling ReactionsApi#deleteSequence");
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
| **name** | **String**|  | |

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


## getReaction

> Reaction getReaction(name)

[EXPERIMENTAL] Get a specific reaction from the library by name.

[EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        String name = "name_example"; // String | 
        try {
            Reaction result = apiInstance.getReaction(name);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ReactionsApi#getReaction");
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
| **name** | **String**|  | |

### Return type

[**Reaction**](Reaction.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getReactions

> List&lt;Reaction&gt; getReactions()

[EXPERIMENTAL] Get all reactions from the library.

[EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        try {
            List<Reaction> result = apiInstance.getReactions();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ReactionsApi#getReactions");
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

[**List&lt;Reaction&gt;**](Reaction.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getSequence

> ReactionSequence getSequence(name)

[EXPERIMENTAL] Get a specific reaction sequence from the library by name.

[EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        String name = "name_example"; // String | 
        try {
            ReactionSequence result = apiInstance.getSequence(name);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ReactionsApi#getSequence");
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
| **name** | **String**|  | |

### Return type

[**ReactionSequence**](ReactionSequence.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getSequences

> List&lt;ReactionSequence&gt; getSequences()

[EXPERIMENTAL] Get all reaction sequences from the library.

[EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        try {
            List<ReactionSequence> result = apiInstance.getSequences();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ReactionsApi#getSequences");
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

[**List&lt;ReactionSequence&gt;**](ReactionSequence.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

