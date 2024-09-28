# LoginAndAccountApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getAccountInfo**](LoginAndAccountApi.md#getAccountInfo) | **GET** /api/account/ | Get information about the account currently logged in. |
| [**getSignUpURL**](LoginAndAccountApi.md#getSignUpURL) | **GET** /api/account/signUpURL | Get SignUp URL (For signUp via web browser) |
| [**getSubscriptions**](LoginAndAccountApi.md#getSubscriptions) | **GET** /api/account/subscriptions | Get available subscriptions of the account currently logged in. |
| [**isLoggedIn**](LoginAndAccountApi.md#isLoggedIn) | **GET** /api/account/isLoggedIn | Check if a user is logged in. |
| [**login**](LoginAndAccountApi.md#login) | **POST** /api/account/login | Login into SIRIUS web services and activate default subscription if available. |
| [**logout**](LoginAndAccountApi.md#logout) | **POST** /api/account/logout | Logout from SIRIUS web services. |
| [**openPortal**](LoginAndAccountApi.md#openPortal) | **GET** /api/account/openPortal | Open User portal in browser. |
| [**selectSubscription**](LoginAndAccountApi.md#selectSubscription) | **PUT** /api/account/subscriptions/select-active | Select a subscription as active subscription to be used for computations. |
| [**signUp**](LoginAndAccountApi.md#signUp) | **GET** /api/account/signUp | Open SignUp window in system browser and return signUp link. |



## getAccountInfo

> AccountInfo getAccountInfo(includeSubs)

Get information about the account currently logged in.

Get information about the account currently logged in. Fails if not logged in.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.LoginAndAccountApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        LoginAndAccountApi apiInstance = new LoginAndAccountApi(defaultClient);
        Boolean includeSubs = false; // Boolean | include available and active subscriptions in {@link AccountInfo AccountInfo}.
        try {
            AccountInfo result = apiInstance.getAccountInfo(includeSubs);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling LoginAndAccountApi#getAccountInfo");
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
| **includeSubs** | **Boolean**| include available and active subscriptions in {@link AccountInfo AccountInfo}. | [optional] [default to false] |

### Return type

[**AccountInfo**](AccountInfo.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Basic information about the account that has been logged in and its subscriptions. |  -  |


## getSignUpURL

> String getSignUpURL()

Get SignUp URL (For signUp via web browser)

Get SignUp URL (For signUp via web browser)

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.LoginAndAccountApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        LoginAndAccountApi apiInstance = new LoginAndAccountApi(defaultClient);
        try {
            String result = apiInstance.getSignUpURL();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling LoginAndAccountApi#getSignUpURL");
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

**String**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: text/plain;charset=UTF-8


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getSubscriptions

> List&lt;Subscription&gt; getSubscriptions()

Get available subscriptions of the account currently logged in.

Get available subscriptions of the account currently logged in. Fails if not logged in.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.LoginAndAccountApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        LoginAndAccountApi apiInstance = new LoginAndAccountApi(defaultClient);
        try {
            List<Subscription> result = apiInstance.getSubscriptions();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling LoginAndAccountApi#getSubscriptions");
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

[**List&lt;Subscription&gt;**](Subscription.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## isLoggedIn

> Boolean isLoggedIn()

Check if a user is logged in.

Check if a user is logged in.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.LoginAndAccountApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        LoginAndAccountApi apiInstance = new LoginAndAccountApi(defaultClient);
        try {
            Boolean result = apiInstance.isLoggedIn();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling LoginAndAccountApi#isLoggedIn");
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

**Boolean**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | true if the user is logged in |  -  |


## login

> AccountInfo login(acceptTerms, accountCredentials, failWhenLoggedIn, includeSubs)

Login into SIRIUS web services and activate default subscription if available.

Login into SIRIUS web services and activate default subscription if available.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.LoginAndAccountApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        LoginAndAccountApi apiInstance = new LoginAndAccountApi(defaultClient);
        Boolean acceptTerms = true; // Boolean | 
        AccountCredentials accountCredentials = new AccountCredentials(); // AccountCredentials | used to log in.
        Boolean failWhenLoggedIn = false; // Boolean | if true request fails if an active login already exists.
        Boolean includeSubs = false; // Boolean | include available and active subscriptions in {@link AccountInfo AccountInfo}.
        try {
            AccountInfo result = apiInstance.login(acceptTerms, accountCredentials, failWhenLoggedIn, includeSubs);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling LoginAndAccountApi#login");
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
| **acceptTerms** | **Boolean**|  | |
| **accountCredentials** | [**AccountCredentials**](AccountCredentials.md)| used to log in. | |
| **failWhenLoggedIn** | **Boolean**| if true request fails if an active login already exists. | [optional] [default to false] |
| **includeSubs** | **Boolean**| include available and active subscriptions in {@link AccountInfo AccountInfo}. | [optional] [default to false] |

### Return type

[**AccountInfo**](AccountInfo.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Basic information about the account that has been logged in and its subscriptions. |  -  |


## logout

> logout()

Logout from SIRIUS web services.

Logout from SIRIUS web services.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.LoginAndAccountApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        LoginAndAccountApi apiInstance = new LoginAndAccountApi(defaultClient);
        try {
            apiInstance.logout();
        } catch (ApiException e) {
            System.err.println("Exception when calling LoginAndAccountApi#logout");
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


## openPortal

> openPortal()

Open User portal in browser.

Open User portal in browser. If user is logged in SIRIUS tries to transfer the login state to the browser.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.LoginAndAccountApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        LoginAndAccountApi apiInstance = new LoginAndAccountApi(defaultClient);
        try {
            apiInstance.openPortal();
        } catch (ApiException e) {
            System.err.println("Exception when calling LoginAndAccountApi#openPortal");
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


## selectSubscription

> AccountInfo selectSubscription(sid)

Select a subscription as active subscription to be used for computations.

Select a subscription as active subscription to be used for computations.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.LoginAndAccountApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        LoginAndAccountApi apiInstance = new LoginAndAccountApi(defaultClient);
        String sid = "sid_example"; // String | 
        try {
            AccountInfo result = apiInstance.selectSubscription(sid);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling LoginAndAccountApi#selectSubscription");
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
| **sid** | **String**|  | |

### Return type

[**AccountInfo**](AccountInfo.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Account information with updated active subscription |  -  |


## signUp

> String signUp()

Open SignUp window in system browser and return signUp link.

Open SignUp window in system browser and return signUp link.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.LoginAndAccountApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        LoginAndAccountApi apiInstance = new LoginAndAccountApi(defaultClient);
        try {
            String result = apiInstance.signUp();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling LoginAndAccountApi#signUp");
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

**String**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: text/plain;charset=UTF-8


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

