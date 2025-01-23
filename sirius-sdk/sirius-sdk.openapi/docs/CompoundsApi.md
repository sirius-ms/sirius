# CompoundsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addCompounds**](CompoundsApi.md#addCompounds) | **POST** /api/projects/{projectId}/compounds | Import Compounds and its contained features. |
| [**addGroup1**](CompoundsApi.md#addGroup1) | **PUT** /api/projects/{projectId}/compounds/groups/{groupName} | **EXPERIMENTAL** Group tags in the project |
| [**addTags1**](CompoundsApi.md#addTags1) | **PUT** /api/projects/{projectId}/compounds/tags/{compoundId} | Tags with the same category name will be overwritten |
| [**deleteCompound**](CompoundsApi.md#deleteCompound) | **DELETE** /api/projects/{projectId}/compounds/{compoundId} | Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space. |
| [**deleteGroup1**](CompoundsApi.md#deleteGroup1) | **DELETE** /api/projects/{projectId}/compounds/groups/{groupName} | **EXPERIMENTAL** Delete tag groups with the given name from the specified project-space |
| [**deleteTags1**](CompoundsApi.md#deleteTags1) | **DELETE** /api/projects/{projectId}/compounds/tags/{compoundId}/{categoryName} | **EXPERIMENTAL** Delete tag with the given category from the compound (group of ion identities) with the specified ID in the specified project-space |
| [**getCompound**](CompoundsApi.md#getCompound) | **GET** /api/projects/{projectId}/compounds/{compoundId} | Get compound (group of ion identities) with the given identifier from the specified project-space. |
| [**getCompoundTracesExperimental**](CompoundsApi.md#getCompoundTracesExperimental) | **GET** /api/projects/{projectId}/compounds/{compoundId}/traces | EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable. |
| [**getCompounds**](CompoundsApi.md#getCompounds) | **GET** /api/projects/{projectId}/compounds | List of all available compounds (group of ion identities) in the given project-space. |
| [**getCompoundsPaged**](CompoundsApi.md#getCompoundsPaged) | **GET** /api/projects/{projectId}/compounds/page | Page of available compounds (group of ion identities) in the given project-space. |
| [**getGroupByName1**](CompoundsApi.md#getGroupByName1) | **GET** /api/projects/{projectId}/compounds/groups/{groupName} | **EXPERIMENTAL** Get tag group by name in the given project-space |
| [**getGroups1**](CompoundsApi.md#getGroups1) | **GET** /api/projects/{projectId}/compounds/groups | **EXPERIMENTAL** Get all tag category groups in the given project-space |
| [**getGroupsByType1**](CompoundsApi.md#getGroupsByType1) | **GET** /api/projects/{projectId}/compounds/groups/type/{groupType} | **EXPERIMENTAL** Get tag groups by type in the given project-space |
| [**getQuantification**](CompoundsApi.md#getQuantification) | **GET** /api/projects/{projectId}/compounds/quantification | Returns the full quantification table. |
| [**getQuantificationRow**](CompoundsApi.md#getQuantificationRow) | **GET** /api/projects/{projectId}/compounds/{compoundId}/quantification | Returns a single quantification table row for the given feature. |
| [**objectsByGroup1**](CompoundsApi.md#objectsByGroup1) | **GET** /api/projects/{projectId}/compounds/grouped | **EXPERIMENTAL** Get compounds (group of ion identities) by tag group |
| [**objectsByTag1**](CompoundsApi.md#objectsByTag1) | **GET** /api/projects/{projectId}/compounds/tagged | **EXPERIMENTAL** Get compounds (group of ion identities) by tag |



## addCompounds

> List&lt;Compound&gt; addCompounds(projectId, compoundImport, profile, optFields, optFieldsFeatures)

Import Compounds and its contained features.

Import Compounds and its contained features. Compounds and Features must not exist in the project.  Otherwise, they will exist twice.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to import into.
        List<CompoundImport> compoundImport = Arrays.asList(); // List<CompoundImport> | the compound data to be imported
        InstrumentProfile profile = InstrumentProfile.fromValue("QTOF"); // InstrumentProfile | profile describing the instrument used to measure the data. Used to merge spectra.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' to override defaults.
        List<AlignedFeatureOptField> optFieldsFeatures = Arrays.asList(); // List<AlignedFeatureOptField> | set of optional fields of the nested features to be included. Use 'none' to override defaults.
        try {
            List<Compound> result = apiInstance.addCompounds(projectId, compoundImport, profile, optFields, optFieldsFeatures);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#addCompounds");
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
| **projectId** | **String**| project-space to import into. | |
| **compoundImport** | [**List&lt;CompoundImport&gt;**](CompoundImport.md)| the compound data to be imported | |
| **profile** | [**InstrumentProfile**](.md)| profile describing the instrument used to measure the data. Used to merge spectra. | [optional] [enum: QTOF, ORBITRAP] |
| **optFields** | [**List&lt;CompoundOptField&gt;**](CompoundOptField.md)| set of optional fields to be included. Use &#39;none&#39; to override defaults. | [optional] |
| **optFieldsFeatures** | [**List&lt;AlignedFeatureOptField&gt;**](AlignedFeatureOptField.md)| set of optional fields of the nested features to be included. Use &#39;none&#39; to override defaults. | [optional] |

### Return type

[**List&lt;Compound&gt;**](Compound.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the Compounds that have been imported with specified optional fields |  -  |


## addGroup1

> TagGroup addGroup1(projectId, groupName, filter, type)

**EXPERIMENTAL** Group tags in the project

**EXPERIMENTAL** Group tags in the project. The group name must not exist in the project.   &lt;p&gt;  See &lt;code&gt;/tagged&lt;/code&gt; for filter syntax.  &lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to add to.
        String groupName = "groupName_example"; // String | name of the new group
        String filter = "filter_example"; // String | filter query to create the group
        String type = "type_example"; // String | type of the group
        try {
            TagGroup result = apiInstance.addGroup1(projectId, groupName, filter, type);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#addGroup1");
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
| **projectId** | **String**| project-space to add to. | |
| **groupName** | **String**| name of the new group | |
| **filter** | **String**| filter query to create the group | |
| **type** | **String**| type of the group | |

### Return type

[**TagGroup**](TagGroup.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the tag group that was added |  -  |


## addTags1

> List&lt;Tag&gt; addTags1(projectId, compoundId, tag)

Tags with the same category name will be overwritten

Tags with the same category name will be overwritten.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to add to.
        String compoundId = "compoundId_example"; // String | compound (group of ion identities) to add tags to.
        List<Tag> tag = Arrays.asList(); // List<Tag> | tags to add.
        try {
            List<Tag> result = apiInstance.addTags1(projectId, compoundId, tag);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#addTags1");
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
| **projectId** | **String**| project-space to add to. | |
| **compoundId** | **String**| compound (group of ion identities) to add tags to. | |
| **tag** | [**List&lt;Tag&gt;**](Tag.md)| tags to add. | |

### Return type

[**List&lt;Tag&gt;**](Tag.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the tags that have been added |  -  |


## deleteCompound

> deleteCompound(projectId, compoundId)

Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.

Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String compoundId = "compoundId_example"; // String | identifier of the compound to delete.
        try {
            apiInstance.deleteCompound(projectId, compoundId);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#deleteCompound");
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
| **compoundId** | **String**| identifier of the compound to delete. | |

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


## deleteGroup1

> deleteGroup1(projectId, groupName)

**EXPERIMENTAL** Delete tag groups with the given name from the specified project-space

**EXPERIMENTAL** Delete tag groups with the given name from the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String groupName = "groupName_example"; // String | name of group to delete.
        try {
            apiInstance.deleteGroup1(projectId, groupName);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#deleteGroup1");
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
| **groupName** | **String**| name of group to delete. | |

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


## deleteTags1

> deleteTags1(projectId, compoundId, categoryName)

**EXPERIMENTAL** Delete tag with the given category from the compound (group of ion identities) with the specified ID in the specified project-space

**EXPERIMENTAL** Delete tag with the given category from the compound (group of ion identities) with the specified ID in the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String compoundId = "compoundId_example"; // String | compound (group of ion identities) to delete tag from.
        String categoryName = "categoryName_example"; // String | category name of the tag to delete.
        try {
            apiInstance.deleteTags1(projectId, compoundId, categoryName);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#deleteTags1");
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
| **compoundId** | **String**| compound (group of ion identities) to delete tag from. | |
| **categoryName** | **String**| category name of the tag to delete. | |

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


## getCompound

> Compound getCompound(projectId, compoundId, optFields, optFieldsFeatures)

Get compound (group of ion identities) with the given identifier from the specified project-space.

Get compound (group of ion identities) with the given identifier from the specified project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String compoundId = "compoundId_example"; // String | identifier of the compound (group of ion identities) to access.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        List<AlignedFeatureOptField> optFieldsFeatures = Arrays.asList(); // List<AlignedFeatureOptField> | 
        try {
            Compound result = apiInstance.getCompound(projectId, compoundId, optFields, optFieldsFeatures);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getCompound");
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
| **compoundId** | **String**| identifier of the compound (group of ion identities) to access. | |
| **optFields** | [**List&lt;CompoundOptField&gt;**](CompoundOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |
| **optFieldsFeatures** | [**List&lt;AlignedFeatureOptField&gt;**](AlignedFeatureOptField.md)|  | [optional] |

### Return type

[**Compound**](Compound.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Compounds with additional optional fields (if specified). |  -  |


## getCompoundTracesExperimental

> TraceSetExperimental getCompoundTracesExperimental(projectId, compoundId, featureId)

EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.

Returns the traces of the given compound. A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String compoundId = "compoundId_example"; // String | compound which intensities should be read out
        String featureId = ""; // String | 
        try {
            TraceSetExperimental result = apiInstance.getCompoundTracesExperimental(projectId, compoundId, featureId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getCompoundTracesExperimental");
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
| **compoundId** | **String**| compound which intensities should be read out | |
| **featureId** | **String**|  | [optional] [default to ] |

### Return type

[**TraceSetExperimental**](TraceSetExperimental.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Traces of the given compound. |  -  |


## getCompounds

> List&lt;Compound&gt; getCompounds(projectId, optFields, optFieldsFeatures)

List of all available compounds (group of ion identities) in the given project-space.

List of all available compounds (group of ion identities) in the given project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        List<AlignedFeatureOptField> optFieldsFeatures = Arrays.asList(); // List<AlignedFeatureOptField> | 
        try {
            List<Compound> result = apiInstance.getCompounds(projectId, optFields, optFieldsFeatures);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getCompounds");
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
| **optFields** | [**List&lt;CompoundOptField&gt;**](CompoundOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |
| **optFieldsFeatures** | [**List&lt;AlignedFeatureOptField&gt;**](AlignedFeatureOptField.md)|  | [optional] |

### Return type

[**List&lt;Compound&gt;**](Compound.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Compounds with additional optional fields (if specified). |  -  |


## getCompoundsPaged

> PagedModelCompound getCompoundsPaged(projectId, page, size, sort, optFields, optFieldsFeatures)

Page of available compounds (group of ion identities) in the given project-space.

Page of available compounds (group of ion identities) in the given project-space.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        List<AlignedFeatureOptField> optFieldsFeatures = Arrays.asList(); // List<AlignedFeatureOptField> | 
        try {
            PagedModelCompound result = apiInstance.getCompoundsPaged(projectId, page, size, sort, optFields, optFieldsFeatures);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getCompoundsPaged");
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
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |
| **optFields** | [**List&lt;CompoundOptField&gt;**](CompoundOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |
| **optFieldsFeatures** | [**List&lt;AlignedFeatureOptField&gt;**](AlignedFeatureOptField.md)|  | [optional] |

### Return type

[**PagedModelCompound**](PagedModelCompound.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Compounds with additional optional fields (if specified). |  -  |


## getGroupByName1

> TagGroup getGroupByName1(projectId, groupName)

**EXPERIMENTAL** Get tag group by name in the given project-space

**EXPERIMENTAL** Get tag group by name in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String groupName = "groupName_example"; // String | name of the group
        try {
            TagGroup result = apiInstance.getGroupByName1(projectId, groupName);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getGroupByName1");
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
| **groupName** | **String**| name of the group | |

### Return type

[**TagGroup**](TagGroup.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Tag group. |  -  |


## getGroups1

> List&lt;TagGroup&gt; getGroups1(projectId)

**EXPERIMENTAL** Get all tag category groups in the given project-space

**EXPERIMENTAL** Get all tag category groups in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        try {
            List<TagGroup> result = apiInstance.getGroups1(projectId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getGroups1");
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

### Return type

[**List&lt;TagGroup&gt;**](TagGroup.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Tag category groups. |  -  |


## getGroupsByType1

> List&lt;TagGroup&gt; getGroupsByType1(projectId, groupType)

**EXPERIMENTAL** Get tag groups by type in the given project-space

**EXPERIMENTAL** Get tag groups by type in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String groupType = "groupType_example"; // String | type of the group
        try {
            List<TagGroup> result = apiInstance.getGroupsByType1(projectId, groupType);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getGroupsByType1");
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
| **groupType** | **String**| type of the group | |

### Return type

[**List&lt;TagGroup&gt;**](TagGroup.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Tag groups. |  -  |


## getQuantification

> QuantTableExperimental getQuantification(projectId, type)

Returns the full quantification table.

Returns the full quantification table. The quantification table contains a quantification of the features within all  runs they are contained in.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        QuantMeasure type = QuantMeasure.fromValue("APEX_INTENSITY"); // QuantMeasure | quantification type.
        try {
            QuantTableExperimental result = apiInstance.getQuantification(projectId, type);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getQuantification");
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
| **type** | [**QuantMeasure**](.md)| quantification type. | [optional] [enum: APEX_INTENSITY, AREA_UNDER_CURVE] |

### Return type

[**QuantTableExperimental**](QuantTableExperimental.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getQuantificationRow

> QuantTableExperimental getQuantificationRow(projectId, compoundId, type)

Returns a single quantification table row for the given feature.

Returns a single quantification table row for the given feature. The quantification table contains a quantification of the feature within all  samples it is contained in.

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String compoundId = "compoundId_example"; // String | compound which should be read out
        QuantMeasure type = QuantMeasure.fromValue("APEX_INTENSITY"); // QuantMeasure | quantification type.
        try {
            QuantTableExperimental result = apiInstance.getQuantificationRow(projectId, compoundId, type);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getQuantificationRow");
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
| **compoundId** | **String**| compound which should be read out | |
| **type** | [**QuantMeasure**](.md)| quantification type. | [optional] [enum: APEX_INTENSITY, AREA_UNDER_CURVE] |

### Return type

[**QuantTableExperimental**](QuantTableExperimental.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## objectsByGroup1

> PagedModelCompound objectsByGroup1(projectId, group, page, size, sort, optFields)

**EXPERIMENTAL** Get compounds (group of ion identities) by tag group

**EXPERIMENTAL** Get compounds (group of ion identities) by tag group.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String group = "group_example"; // String | tag group name.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PagedModelCompound result = apiInstance.objectsByGroup1(projectId, group, page, size, sort, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#objectsByGroup1");
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
| **group** | **String**| tag group name. | |
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |
| **optFields** | [**List&lt;CompoundOptField&gt;**](CompoundOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**PagedModelCompound**](PagedModelCompound.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | tagged compounds (group of ion identities) |  -  |


## objectsByTag1

> PagedModelCompound objectsByTag1(projectId, filter, page, size, sort, optFields)

**EXPERIMENTAL** Get compounds (group of ion identities) by tag

**EXPERIMENTAL** Get compounds (group of ion identities) by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name. Possible field names are:&lt;/p&gt;   &lt;ul&gt;    &lt;li&gt;&lt;strong&gt;category&lt;/strong&gt; - category name&lt;/li&gt;    &lt;li&gt;&lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;category:my_category&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;text:&amp;quot;new york&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;text:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;integer&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;integer:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;(category:hello || category:world) &amp;amp;&amp;amp; text:&amp;quot;new york&amp;quot; AND text:/[mb]oat/ AND integer:[1 TO *] OR real&amp;lt;&#x3D;3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date&amp;lt;2022-01-01 OR time:12\\:00\\:00 OR time:[12\\:00\\:00 TO 14\\:00\\:00] OR time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;

### Example

```java
// Import classes:
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.client.ApiException;
import io.sirius.ms.sdk.client.Configuration;
import io.sirius.ms.sdk.client.models.*;
import io.sirius.ms.sdk.api.CompoundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8888");

        CompoundsApi apiInstance = new CompoundsApi(defaultClient);
        String projectId = "projectId_example"; // String | project space to get compounds (group of ion identities) from.
        String filter = ""; // String | tag filter.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PagedModelCompound result = apiInstance.objectsByTag1(projectId, filter, page, size, sort, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#objectsByTag1");
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
| **projectId** | **String**| project space to get compounds (group of ion identities) from. | |
| **filter** | **String**| tag filter. | [optional] [default to ] |
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |
| **optFields** | [**List&lt;CompoundOptField&gt;**](CompoundOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**PagedModelCompound**](PagedModelCompound.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | tagged compounds (group of ion identities) |  -  |

