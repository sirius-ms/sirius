# FeaturesApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteAlignedFeature**](FeaturesApi.md#deleteAlignedFeature) | **DELETE** /api/projects/{projectId}/aligned-features/{alignedFeatureId} | Delete feature (aligned over runs) with the given identifier from the specified project-space. |
| [**getAlignedFeature**](FeaturesApi.md#getAlignedFeature) | **GET** /api/projects/{projectId}/aligned-features/{alignedFeatureId} | Get feature (aligned over runs) with the given identifier from the specified project-space. |
| [**getAlignedFeatures**](FeaturesApi.md#getAlignedFeatures) | **GET** /api/projects/{projectId}/aligned-features | Get all available features (aligned over runs) in the given project-space. |
| [**getBestMatchingCompoundClasses**](FeaturesApi.md#getBestMatchingCompoundClasses) | **GET** /api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/best-compound-classes | Best matching compound classes,  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology, |
| [**getCanopusPrediction**](FeaturesApi.md#getCanopusPrediction) | **GET** /api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/canopus-prediction | All predicted compound classes (CANOPUS) from ClassyFire and NPC and their probabilities, |
| [**getFingerprintPrediction**](FeaturesApi.md#getFingerprintPrediction) | **GET** /api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/fingerprint | Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier  This fingerprint is used to perform structure database search and predict compound classes. |
| [**getFormulaCandidate**](FeaturesApi.md#getFormulaCandidate) | **GET** /api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId} | FormulaResultContainers for the given &#39;formulaId&#39; with minimal information. |
| [**getFormulaCandidates**](FeaturesApi.md#getFormulaCandidates) | **GET** /api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas | List of all FormulaResultContainers available for this feature with minimal information. |
| [**getFragTree**](FeaturesApi.md#getFragTree) | **GET** /api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/tree | Returns fragmentation tree (SIRIUS) for the given formula result identifier  This tree is used to rank formula candidates (treeScore). |
| [**getLipidAnnotation**](FeaturesApi.md#getLipidAnnotation) | **GET** /api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/lipid-annotation | Returns Lipid annotation (ElGordo) for the given formula result identifier. |
| [**getMsData**](FeaturesApi.md#getMsData) | **GET** /api/projects/{projectId}/aligned-features/{alignedFeatureId}/ms-data | Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; . |
| [**getSimulatedIsotopePattern**](FeaturesApi.md#getSimulatedIsotopePattern) | **GET** /api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/isotope-pattern | Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier. |
| [**getSiriusFragTree**](FeaturesApi.md#getSiriusFragTree) | **GET** /api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/sirius-tree |  |
| [**getStructureCandidates**](FeaturesApi.md#getStructureCandidates) | **GET** /api/projects/{projectId}/aligned-features/{alignedFeatureId}/structures | List of StructureCandidates for the given &#39;alignedFeatureId&#39; with minimal information. |
| [**getStructureCandidatesByFormula**](FeaturesApi.md#getStructureCandidatesByFormula) | **GET** /api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/structures | List of StructureCandidates the given &#39;formulaId&#39; with minimal information. |



## deleteAlignedFeature

> deleteAlignedFeature(projectId, alignedFeatureId)

Delete feature (aligned over runs) with the given identifier from the specified project-space.

Delete feature (aligned over runs) with the given identifier from the specified project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to delete from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | identifier of feature (aligned over runs) to delete.
        try {
            apiInstance.deleteAlignedFeature(projectId, alignedFeatureId);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#deleteAlignedFeature");
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
| **alignedFeatureId** | **String**| identifier of feature (aligned over runs) to delete. | |

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


## getAlignedFeature

> AlignedFeature getAlignedFeature(projectId, alignedFeatureId, optFields)

Get feature (aligned over runs) with the given identifier from the specified project-space.

Get feature (aligned over runs) with the given identifier from the specified project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | identifier of feature (aligned over runs) to access.
        List<AlignedFeatureOptField> optFields = Arrays.asList(); // List<AlignedFeatureOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            AlignedFeature result = apiInstance.getAlignedFeature(projectId, alignedFeatureId, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#getAlignedFeature");
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
| **alignedFeatureId** | **String**| identifier of feature (aligned over runs) to access. | |
| **optFields** | [**List&lt;AlignedFeatureOptField&gt;**](AlignedFeatureOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**AlignedFeature**](AlignedFeature.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | AlignedFeature with additional annotations and MS/MS data (if specified). |  -  |


## getAlignedFeatures

> PageAlignedFeature getAlignedFeatures(projectId, page, size, sort, searchQuery, querySyntax, optFields)

Get all available features (aligned over runs) in the given project-space.

Get all available features (aligned over runs) in the given project-space.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        String searchQuery = "searchQuery_example"; // String | optional search query in specified format
        SearchQueryType querySyntax = SearchQueryType.fromValue("LUCENE"); // SearchQueryType | query syntax used fpr searchQuery
        List<AlignedFeatureOptField> optFields = Arrays.asList(); // List<AlignedFeatureOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PageAlignedFeature result = apiInstance.getAlignedFeatures(projectId, page, size, sort, searchQuery, querySyntax, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#getAlignedFeatures");
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
| **searchQuery** | **String**| optional search query in specified format | [optional] |
| **querySyntax** | [**SearchQueryType**](.md)| query syntax used fpr searchQuery | [optional] [enum: LUCENE] |
| **optFields** | [**List&lt;AlignedFeatureOptField&gt;**](AlignedFeatureOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**PageAlignedFeature**](PageAlignedFeature.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | AlignedFeatures with additional annotations and MS/MS data (if specified). |  -  |


## getBestMatchingCompoundClasses

> CompoundClasses getBestMatchingCompoundClasses(projectId, alignedFeatureId, formulaId)

Best matching compound classes,  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,

Best matching compound classes,  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | feature (aligned over runs) the formula result belongs to.
        String formulaId = "formulaId_example"; // String | identifier of the requested formula result
        try {
            CompoundClasses result = apiInstance.getBestMatchingCompoundClasses(projectId, alignedFeatureId, formulaId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#getBestMatchingCompoundClasses");
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
| **alignedFeatureId** | **String**| feature (aligned over runs) the formula result belongs to. | |
| **formulaId** | **String**| identifier of the requested formula result | |

### Return type

[**CompoundClasses**](CompoundClasses.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Best matching Predicted compound classes |  -  |


## getCanopusPrediction

> CanopusPrediction getCanopusPrediction(projectId, alignedFeatureId, formulaId)

All predicted compound classes (CANOPUS) from ClassyFire and NPC and their probabilities,

All predicted compound classes (CANOPUS) from ClassyFire and NPC and their probabilities,

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | feature (aligned over runs) the formula result belongs to.
        String formulaId = "formulaId_example"; // String | identifier of the requested formula result
        try {
            CanopusPrediction result = apiInstance.getCanopusPrediction(projectId, alignedFeatureId, formulaId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#getCanopusPrediction");
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
| **alignedFeatureId** | **String**| feature (aligned over runs) the formula result belongs to. | |
| **formulaId** | **String**| identifier of the requested formula result | |

### Return type

[**CanopusPrediction**](CanopusPrediction.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Predicted compound classes |  -  |


## getFingerprintPrediction

> List&lt;Double&gt; getFingerprintPrediction(projectId, alignedFeatureId, formulaId)

Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier  This fingerprint is used to perform structure database search and predict compound classes.

Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier  This fingerprint is used to perform structure database search and predict compound classes.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | feature (aligned over runs) the formula result belongs to.
        String formulaId = "formulaId_example"; // String | identifier of the requested formula result
        try {
            List<Double> result = apiInstance.getFingerprintPrediction(projectId, alignedFeatureId, formulaId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#getFingerprintPrediction");
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
| **alignedFeatureId** | **String**| feature (aligned over runs) the formula result belongs to. | |
| **formulaId** | **String**| identifier of the requested formula result | |

### Return type

**List&lt;Double&gt;**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | probabilistic fingerprint predicted by CSI:FingerID |  -  |


## getFormulaCandidate

> FormulaCandidate getFormulaCandidate(projectId, alignedFeatureId, formulaId, optFields)

FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.

FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.  Can be enriched with an optional results overview and formula candidate information.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | feature (aligned over runs) the formula result belongs to.
        String formulaId = "formulaId_example"; // String | identifier of the requested formula result
        List<FormulaCandidateOptField> optFields = Arrays.asList(); // List<FormulaCandidateOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            FormulaCandidate result = apiInstance.getFormulaCandidate(projectId, alignedFeatureId, formulaId, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#getFormulaCandidate");
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
| **alignedFeatureId** | **String**| feature (aligned over runs) the formula result belongs to. | |
| **formulaId** | **String**| identifier of the requested formula result | |
| **optFields** | [**List&lt;FormulaCandidateOptField&gt;**](FormulaCandidateOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**FormulaCandidate**](FormulaCandidate.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | FormulaCandidate of this feature (aligned over runs) with. |  -  |


## getFormulaCandidates

> PageFormulaCandidate getFormulaCandidates(projectId, alignedFeatureId, page, size, sort, searchQuery, querySyntax, optFields)

List of all FormulaResultContainers available for this feature with minimal information.

List of all FormulaResultContainers available for this feature with minimal information.  Can be enriched with an optional results overview.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | feature (aligned over runs) the formula result belongs to.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        String searchQuery = "searchQuery_example"; // String | optional search query in specified format
        SearchQueryType querySyntax = SearchQueryType.fromValue("LUCENE"); // SearchQueryType | query syntax used fpr searchQuery
        List<FormulaCandidateOptField> optFields = Arrays.asList(); // List<FormulaCandidateOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PageFormulaCandidate result = apiInstance.getFormulaCandidates(projectId, alignedFeatureId, page, size, sort, searchQuery, querySyntax, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#getFormulaCandidates");
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
| **alignedFeatureId** | **String**| feature (aligned over runs) the formula result belongs to. | |
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |
| **searchQuery** | **String**| optional search query in specified format | [optional] |
| **querySyntax** | [**SearchQueryType**](.md)| query syntax used fpr searchQuery | [optional] [enum: LUCENE] |
| **optFields** | [**List&lt;FormulaCandidateOptField&gt;**](FormulaCandidateOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**PageFormulaCandidate**](PageFormulaCandidate.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | All FormulaCandidate of this feature with. |  -  |


## getFragTree

> FragmentationTree getFragTree(projectId, alignedFeatureId, formulaId)

Returns fragmentation tree (SIRIUS) for the given formula result identifier  This tree is used to rank formula candidates (treeScore).

Returns fragmentation tree (SIRIUS) for the given formula result identifier  This tree is used to rank formula candidates (treeScore).

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | feature (aligned over runs) the formula result belongs to.
        String formulaId = "formulaId_example"; // String | identifier of the requested formula result
        try {
            FragmentationTree result = apiInstance.getFragTree(projectId, alignedFeatureId, formulaId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#getFragTree");
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
| **alignedFeatureId** | **String**| feature (aligned over runs) the formula result belongs to. | |
| **formulaId** | **String**| identifier of the requested formula result | |

### Return type

[**FragmentationTree**](FragmentationTree.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Fragmentation Tree |  -  |


## getLipidAnnotation

> LipidAnnotation getLipidAnnotation(projectId, alignedFeatureId, formulaId)

Returns Lipid annotation (ElGordo) for the given formula result identifier.

Returns Lipid annotation (ElGordo) for the given formula result identifier.  ElGordo lipid annotation runs as part of the SIRIUS formula identification step.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | feature (aligned over runs) the formula result belongs to.
        String formulaId = "formulaId_example"; // String | identifier of the requested formula result
        try {
            LipidAnnotation result = apiInstance.getLipidAnnotation(projectId, alignedFeatureId, formulaId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#getLipidAnnotation");
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
| **alignedFeatureId** | **String**| feature (aligned over runs) the formula result belongs to. | |
| **formulaId** | **String**| identifier of the requested formula result | |

### Return type

[**LipidAnnotation**](LipidAnnotation.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | LipidAnnotation |  -  |


## getMsData

> MsData getMsData(projectId, alignedFeatureId)

Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .

Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | feature (aligned over runs) the Mass Spec data belong sto.
        try {
            MsData result = apiInstance.getMsData(projectId, alignedFeatureId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#getMsData");
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
| **alignedFeatureId** | **String**| feature (aligned over runs) the Mass Spec data belong sto. | |

### Return type

[**MsData**](MsData.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Mass Spec data of this feature (aligned over runs). |  -  |


## getSimulatedIsotopePattern

> IsotopePatternAnnotation getSimulatedIsotopePattern(projectId, alignedFeatureId, formulaId)

Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier.

Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier. This simulated isotope pattern is used to rank formula candidates (treeScore).

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | feature (aligned over runs) the formula result belongs to.
        String formulaId = "formulaId_example"; // String | identifier of the requested formula result
        try {
            IsotopePatternAnnotation result = apiInstance.getSimulatedIsotopePattern(projectId, alignedFeatureId, formulaId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#getSimulatedIsotopePattern");
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
| **alignedFeatureId** | **String**| feature (aligned over runs) the formula result belongs to. | |
| **formulaId** | **String**| identifier of the requested formula result | |

### Return type

[**IsotopePatternAnnotation**](IsotopePatternAnnotation.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Isotope pattern information |  -  |


## getSiriusFragTree

> String getSiriusFragTree(projectId, alignedFeatureId, formulaId)



### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | 
        String alignedFeatureId = "alignedFeatureId_example"; // String | 
        String formulaId = "formulaId_example"; // String | 
        try {
            String result = apiInstance.getSiriusFragTree(projectId, alignedFeatureId, formulaId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#getSiriusFragTree");
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
| **projectId** | **String**|  | |
| **alignedFeatureId** | **String**|  | |
| **formulaId** | **String**|  | |

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getStructureCandidates

> PageStructureCandidateFormula getStructureCandidates(projectId, alignedFeatureId, page, size, sort, searchQuery, querySyntax, optFields)

List of StructureCandidates for the given &#39;alignedFeatureId&#39; with minimal information.

List of StructureCandidates for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | feature (aligned over runs) the structure candidates belong to.
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        String searchQuery = "searchQuery_example"; // String | optional search query in specified format
        SearchQueryType querySyntax = SearchQueryType.fromValue("LUCENE"); // SearchQueryType | query syntax used fpr searchQuery
        List<StructureCandidateOptField> optFields = Arrays.asList(); // List<StructureCandidateOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PageStructureCandidateFormula result = apiInstance.getStructureCandidates(projectId, alignedFeatureId, page, size, sort, searchQuery, querySyntax, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#getStructureCandidates");
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
| **alignedFeatureId** | **String**| feature (aligned over runs) the structure candidates belong to. | |
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |
| **searchQuery** | **String**| optional search query in specified format | [optional] |
| **querySyntax** | [**SearchQueryType**](.md)| query syntax used fpr searchQuery | [optional] [enum: LUCENE] |
| **optFields** | [**List&lt;StructureCandidateOptField&gt;**](StructureCandidateOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**PageStructureCandidateFormula**](PageStructureCandidateFormula.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | StructureCandidate of this feature (aligned over runs) candidate with specified optional fields. |  -  |


## getStructureCandidatesByFormula

> PageStructureCandidateScored getStructureCandidatesByFormula(projectId, alignedFeatureId, formulaId, page, size, sort, searchQuery, querySyntax, optFields)

List of StructureCandidates the given &#39;formulaId&#39; with minimal information.

List of StructureCandidates the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.

### Example

```java
// Import classes:
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.client.Configuration;
import de.unijena.bioinf.ms.nightsky.sdk.client.models.*;
import de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");

        FeaturesApi apiInstance = new FeaturesApi(defaultClient);
        String projectId = "projectId_example"; // String | project-space to read from.
        String alignedFeatureId = "alignedFeatureId_example"; // String | feature (aligned over runs) the formula result belongs to.
        String formulaId = "formulaId_example"; // String | identifier of the requested formula result
        Integer page = 0; // Integer | Zero-based page index (0..N)
        Integer size = 20; // Integer | The size of the page to be returned
        List<String> sort = Arrays.asList(); // List<String> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
        String searchQuery = "searchQuery_example"; // String | optional search query in specified format
        SearchQueryType querySyntax = SearchQueryType.fromValue("LUCENE"); // SearchQueryType | query syntax used fpr searchQuery
        List<StructureCandidateOptField> optFields = Arrays.asList(); // List<StructureCandidateOptField> | set of optional fields to be included. Use 'none' only to override defaults.
        try {
            PageStructureCandidateScored result = apiInstance.getStructureCandidatesByFormula(projectId, alignedFeatureId, formulaId, page, size, sort, searchQuery, querySyntax, optFields);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling FeaturesApi#getStructureCandidatesByFormula");
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
| **alignedFeatureId** | **String**| feature (aligned over runs) the formula result belongs to. | |
| **formulaId** | **String**| identifier of the requested formula result | |
| **page** | **Integer**| Zero-based page index (0..N) | [optional] [default to 0] |
| **size** | **Integer**| The size of the page to be returned | [optional] [default to 20] |
| **sort** | [**List&lt;String&gt;**](String.md)| Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [optional] |
| **searchQuery** | **String**| optional search query in specified format | [optional] |
| **querySyntax** | [**SearchQueryType**](.md)| query syntax used fpr searchQuery | [optional] [enum: LUCENE] |
| **optFields** | [**List&lt;StructureCandidateOptField&gt;**](StructureCandidateOptField.md)| set of optional fields to be included. Use &#39;none&#39; only to override defaults. | [optional] |

### Return type

[**PageStructureCandidateScored**](PageStructureCandidateScored.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | StructureCandidate of this formula candidate with specified optional fields. |  -  |

