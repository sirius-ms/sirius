# CompoundsApi

All URIs are relative to *http://localhost:8888*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**addCompounds**](CompoundsApi.md#addCompounds) | **POST** /api/projects/{projectId}/compounds | Import Compounds and its contained features. |
| [**addTagsToCompoundExperimental**](CompoundsApi.md#addTagsToCompoundExperimental) | **PUT** /api/projects/{projectId}/compounds/tags/{compoundId} | [EXPERIMENTAL] Tags with the same name will be overwritten |
| [**deleteCompound**](CompoundsApi.md#deleteCompound) | **DELETE** /api/projects/{projectId}/compounds/{compoundId} | Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space. |
| [**getCompound**](CompoundsApi.md#getCompound) | **GET** /api/projects/{projectId}/compounds/{compoundId} | Get compound (group of ion identities) with the given identifier from the specified project-space. |
| [**getCompoundQuantTableExperimental**](CompoundsApi.md#getCompoundQuantTableExperimental) | **GET** /api/projects/{projectId}/compounds/quant-table | [EXPERIMENTAL] Returns the full quantification table of compounds |
| [**getCompoundQuantTableRowExperimental**](CompoundsApi.md#getCompoundQuantTableRowExperimental) | **GET** /api/projects/{projectId}/compounds/{compoundId}/quant-table-row | [EXPERIMENTAL] Returns a single quantification table row for the given compound |
| [**getCompoundTracesExperimental**](CompoundsApi.md#getCompoundTracesExperimental) | **GET** /api/projects/{projectId}/compounds/{compoundId}/traces | [EXPERIMENTAL] Returns the traces of the given compound |
| [**getCompounds**](CompoundsApi.md#getCompounds) | **GET** /api/projects/{projectId}/compounds | List of all available compounds (group of ion identities) in the given project-space. |
| [**getCompoundsByGroupExperimental**](CompoundsApi.md#getCompoundsByGroupExperimental) | **GET** /api/projects/{projectId}/compounds/grouped | [EXPERIMENTAL] Get compounds (group of ion identities) by tag group |
| [**getCompoundsByTagExperimental**](CompoundsApi.md#getCompoundsByTagExperimental) | **GET** /api/projects/{projectId}/compounds/tagged | [EXPERIMENTAL] Get compounds (group of ion identities) by tag |
| [**getCompoundsPaged**](CompoundsApi.md#getCompoundsPaged) | **GET** /api/projects/{projectId}/compounds/page | Page of available compounds (group of ion identities) in the given project-space. |
| [**getTagsForCompoundExperimental**](CompoundsApi.md#getTagsForCompoundExperimental) | **GET** /api/projects/{projectId}/compounds/tags/{objectId} | [EXPERIMENTAL] Get all tags associated with this Compound |
| [**removeTagFromCompoundExperimental**](CompoundsApi.md#removeTagFromCompoundExperimental) | **DELETE** /api/projects/{projectId}/compounds/tags/{compoundId}/{tagName} | [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space |



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


## addTagsToCompoundExperimental

> List&lt;Tag&gt; addTagsToCompoundExperimental(projectId, compoundId, tag)

[EXPERIMENTAL] Tags with the same name will be overwritten

[EXPERIMENTAL] Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
            List<Tag> result = apiInstance.addTagsToCompoundExperimental(projectId, compoundId, tag);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#addTagsToCompoundExperimental");
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


## getCompound

> Compound getCompound(projectId, compoundId, msDataSearchPrepared, optFields, optFieldsFeatures)

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
        Boolean msDataSearchPrepared = false; // Boolean | Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        List<AlignedFeatureOptField> optFieldsFeatures = Arrays.asList(); // List<AlignedFeatureOptField> | 
        try {
            Compound result = apiInstance.getCompound(projectId, compoundId, msDataSearchPrepared, optFields, optFieldsFeatures);
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
| **msDataSearchPrepared** | **Boolean**| Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra. | [optional] [default to false] |
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


## getCompoundQuantTableExperimental

> QuantTableExperimental getCompoundQuantTableExperimental(projectId, type)

[EXPERIMENTAL] Returns the full quantification table of compounds

[EXPERIMENTAL] Returns the full quantification table of compounds.  &lt;p&gt;  The quantification table contains a quantification of the features within all  runs they are contained in.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*

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
            QuantTableExperimental result = apiInstance.getCompoundQuantTableExperimental(projectId, type);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getCompoundQuantTableExperimental");
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


## getCompoundQuantTableRowExperimental

> QuantTableExperimental getCompoundQuantTableRowExperimental(projectId, compoundId, type)

[EXPERIMENTAL] Returns a single quantification table row for the given compound

[EXPERIMENTAL] Returns a single quantification table row for the given compound.  &lt;p&gt;  The quantification table contains a quantification of the feature within all  samples it is contained in.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*

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
            QuantTableExperimental result = apiInstance.getCompoundQuantTableRowExperimental(projectId, compoundId, type);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getCompoundQuantTableRowExperimental");
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


## getCompoundTracesExperimental

> TraceSetExperimental getCompoundTracesExperimental(projectId, compoundId, featureId)

[EXPERIMENTAL] Returns the traces of the given compound

[EXPERIMENTAL] Returns the traces of the given compound.  &lt;p&gt;  A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*

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

> List&lt;Compound&gt; getCompounds(projectId, msDataSearchPrepared, optFields, optFieldsFeatures)

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
        Boolean msDataSearchPrepared = false; // Boolean | Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        List<AlignedFeatureOptField> optFieldsFeatures = Arrays.asList(); // List<AlignedFeatureOptField> | 
        try {
            List<Compound> result = apiInstance.getCompounds(projectId, msDataSearchPrepared, optFields, optFieldsFeatures);
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
| **msDataSearchPrepared** | **Boolean**| Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra. | [optional] [default to false] |
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


## getCompoundsByGroupExperimental

> PagedModelCompound getCompoundsByGroupExperimental(projectId, groupName, page, size, sort, optFields)

[EXPERIMENTAL] Get compounds (group of ion identities) by tag group

[EXPERIMENTAL] Get compounds (group of ion identities) by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        String groupName = "groupName_example"; // String | tag group name.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PagedModelCompound result = apiInstance.getCompoundsByGroupExperimental(projectId, groupName, page, size, sort, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getCompoundsByGroupExperimental");
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
| **groupName** | **String**| tag group name. | |
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


## getCompoundsByTagExperimental

> PagedModelCompound getCompoundsByTagExperimental(projectId, filter, page, size, sort, optFields)

[EXPERIMENTAL] Get compounds (group of ion identities) by tag

[EXPERIMENTAL] Get compounds (group of ion identities) by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name.  &lt;/p&gt;   Currently the only searchable fields are names of tags (&lt;code&gt;tagName&lt;/code&gt;) followed by a clause that is valued for the value type of the tag (See TagDefinition).  Tag name based field need to be prefixed with the namespace &lt;code&gt;tags.&lt;/code&gt;.  Possible value types of tags are &lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;tags.MyTagA:sample&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;tags.MyTagA:&amp;quot;Some Text&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;tags.MyTagB&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;tags.MyTagB:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;tags.city:&amp;quot;new york&amp;quot; AND tags.ATextTag:/[mb]oat/ AND tags.count:[1 TO *] OR tags.realNumberTag&amp;lt;&#x3D;3.2 OR tags.MyDateTag:2024-01-01 OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.MyDateTag&amp;lt;2022-01-01 OR tags.time:12\\:00\\:00 OR tags.time:[12\\:00\\:00 TO 14\\:00\\:00] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
            PagedModelCompound result = apiInstance.getCompoundsByTagExperimental(projectId, filter, page, size, sort, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getCompoundsByTagExperimental");
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


## getCompoundsPaged

> PagedModelCompound getCompoundsPaged(projectId, page, size, sort, msDataSearchPrepared, optFields, optFieldsFeatures)

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
        Boolean msDataSearchPrepared = false; // Boolean | Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
        List<CompoundOptField> optFields = Arrays.asList(); // List<CompoundOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        List<AlignedFeatureOptField> optFieldsFeatures = Arrays.asList(); // List<AlignedFeatureOptField> | 
        try {
            PagedModelCompound result = apiInstance.getCompoundsPaged(projectId, page, size, sort, msDataSearchPrepared, optFields, optFieldsFeatures);
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
| **msDataSearchPrepared** | **Boolean**| Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra. | [optional] [default to false] |
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


## getTagsForCompoundExperimental

> List&lt;Tag&gt; getTagsForCompoundExperimental(projectId, objectId)

[EXPERIMENTAL] Get all tags associated with this Compound

[EXPERIMENTAL] Get all tags associated with this Compound

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
        String projectId = "projectId_example"; // String | project-space to get from.
        String objectId = "objectId_example"; // String | CompoundId to get tags for.
        try {
            List<Tag> result = apiInstance.getTagsForCompoundExperimental(projectId, objectId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#getTagsForCompoundExperimental");
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
| **projectId** | **String**| project-space to get from. | |
| **objectId** | **String**| CompoundId to get tags for. | |

### Return type

[**List&lt;Tag&gt;**](Tag.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | the tags of the requested Compound |  -  |


## removeTagFromCompoundExperimental

> removeTagFromCompoundExperimental(projectId, compoundId, tagName)

[EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space

[EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.

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
        String tagName = "tagName_example"; // String | name of the tag to delete.
        try {
            apiInstance.removeTagFromCompoundExperimental(projectId, compoundId, tagName);
        } catch (ApiException e) {
            System.err.println("Exception when calling CompoundsApi#removeTagFromCompoundExperimental");
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
| **tagName** | **String**| name of the tag to delete. | |

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

