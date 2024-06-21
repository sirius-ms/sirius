package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;

import de.unijena.bioinf.ms.nightsky.sdk.model.AlignedFeatureOptField;
import de.unijena.bioinf.ms.nightsky.sdk.model.Compound;
import de.unijena.bioinf.ms.nightsky.sdk.model.CompoundImport;
import de.unijena.bioinf.ms.nightsky.sdk.model.CompoundOptField;
import de.unijena.bioinf.ms.nightsky.sdk.model.PageCompound;
import de.unijena.bioinf.ms.nightsky.sdk.model.TraceSet;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class CompoundsApi {
    private ApiClient apiClient;

    public CompoundsApi() {
        this(new ApiClient());
    }

    @Autowired
    public CompoundsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Import Compounds and its contained features.
     * Import Compounds and its contained features. Compounds and Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Compounds that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param compoundImport the compound data to be imported
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @param optFieldsFeatures set of optional fields of the nested features to be included. Use &#39;none&#39; to override defaults.
     * @return List&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addCompoundsRequestCreation(String projectId, List<CompoundImport> compoundImport, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        Object postBody = compoundImport;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addCompounds", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundImport' is set
        if (compoundImport == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundImport' when calling addCompounds", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFieldsFeatures", optFieldsFeatures));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Import Compounds and its contained features.
     * Import Compounds and its contained features. Compounds and Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Compounds that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param compoundImport the compound data to be imported
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @param optFieldsFeatures set of optional fields of the nested features to be included. Use &#39;none&#39; to override defaults.
     * @return List&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Compound> addCompounds(String projectId, List<CompoundImport> compoundImport, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return addCompoundsRequestCreation(projectId, compoundImport, optFields, optFieldsFeatures).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Import Compounds and its contained features.
     * Import Compounds and its contained features. Compounds and Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Compounds that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param compoundImport the compound data to be imported
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @param optFieldsFeatures set of optional fields of the nested features to be included. Use &#39;none&#39; to override defaults.
     * @return ResponseEntity&lt;List&lt;Compound&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Compound>> addCompoundsWithHttpInfo(String projectId, List<CompoundImport> compoundImport, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return addCompoundsRequestCreation(projectId, compoundImport, optFields, optFieldsFeatures).toEntityList(localVarReturnType).block();
    }

    /**
     * Import Compounds and its contained features.
     * Import Compounds and its contained features. Compounds and Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Compounds that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param compoundImport the compound data to be imported
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @param optFieldsFeatures set of optional fields of the nested features to be included. Use &#39;none&#39; to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addCompoundsWithResponseSpec(String projectId, List<CompoundImport> compoundImport, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return addCompoundsRequestCreation(projectId, compoundImport, optFields, optFieldsFeatures);
    }
    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteCompoundRequestCreation(String projectId, String compoundId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteCompound", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling deleteCompound", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/{compoundId}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteCompound(String projectId, String compoundId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteCompoundRequestCreation(projectId, compoundId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteCompoundWithHttpInfo(String projectId, String compoundId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteCompoundRequestCreation(projectId, compoundId).toEntity(localVarReturnType).block();
    }

    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteCompoundWithResponseSpec(String projectId, String compoundId) throws WebClientResponseException {
        return deleteCompoundRequestCreation(projectId, compoundId);
    }
    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return Compound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundRequestCreation(String projectId, String compoundId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompound", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling getCompound", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFieldsFeatures", optFieldsFeatures));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/{compoundId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return Compound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Compound getCompound(String projectId, String compoundId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundRequestCreation(projectId, compoundId, optFields, optFieldsFeatures).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseEntity&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Compound> getCompoundWithHttpInfo(String projectId, String compoundId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundRequestCreation(projectId, compoundId, optFields, optFieldsFeatures).toEntity(localVarReturnType).block();
    }

    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundWithResponseSpec(String projectId, String compoundId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return getCompoundRequestCreation(projectId, compoundId, optFields, optFieldsFeatures);
    }
    /**
     * List of all available compounds (group of ion identities) in the given project-space.
     * List of all available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return List&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundsRequestCreation(String projectId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompounds", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFieldsFeatures", optFieldsFeatures));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List of all available compounds (group of ion identities) in the given project-space.
     * List of all available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return List&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Compound> getCompounds(String projectId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundsRequestCreation(projectId, optFields, optFieldsFeatures).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List of all available compounds (group of ion identities) in the given project-space.
     * List of all available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseEntity&lt;List&lt;Compound&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Compound>> getCompoundsWithHttpInfo(String projectId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundsRequestCreation(projectId, optFields, optFieldsFeatures).toEntityList(localVarReturnType).block();
    }

    /**
     * List of all available compounds (group of ion identities) in the given project-space.
     * List of all available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundsWithResponseSpec(String projectId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return getCompoundsRequestCreation(projectId, optFields, optFieldsFeatures);
    }
    /**
     * Page of available compounds (group of ion identities) in the given project-space.
     * Page of available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return PageCompound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundsPagedRequestCreation(String projectId, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundsPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFieldsFeatures", optFieldsFeatures));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PageCompound> localVarReturnType = new ParameterizedTypeReference<PageCompound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Page of available compounds (group of ion identities) in the given project-space.
     * Page of available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return PageCompound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageCompound getCompoundsPaged(String projectId, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<PageCompound> localVarReturnType = new ParameterizedTypeReference<PageCompound>() {};
        return getCompoundsPagedRequestCreation(projectId, page, size, sort, optFields, optFieldsFeatures).bodyToMono(localVarReturnType).block();
    }

    /**
     * Page of available compounds (group of ion identities) in the given project-space.
     * Page of available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseEntity&lt;PageCompound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageCompound> getCompoundsPagedWithHttpInfo(String projectId, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<PageCompound> localVarReturnType = new ParameterizedTypeReference<PageCompound>() {};
        return getCompoundsPagedRequestCreation(projectId, page, size, sort, optFields, optFieldsFeatures).toEntity(localVarReturnType).block();
    }

    /**
     * Page of available compounds (group of ion identities) in the given project-space.
     * Page of available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundsPagedWithResponseSpec(String projectId, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return getCompoundsPagedRequestCreation(projectId, page, size, sort, optFields, optFieldsFeatures);
    }
    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param compoundId The compoundId parameter
     * @return TraceSet
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getTracesRequestCreation(String projectId, String compoundId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getTraces", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling getTraces", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<TraceSet> localVarReturnType = new ParameterizedTypeReference<TraceSet>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/{compoundId}/traces", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param compoundId The compoundId parameter
     * @return TraceSet
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TraceSet getTraces(String projectId, String compoundId) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSet> localVarReturnType = new ParameterizedTypeReference<TraceSet>() {};
        return getTracesRequestCreation(projectId, compoundId).bodyToMono(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param compoundId The compoundId parameter
     * @return ResponseEntity&lt;TraceSet&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TraceSet> getTracesWithHttpInfo(String projectId, String compoundId) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSet> localVarReturnType = new ParameterizedTypeReference<TraceSet>() {};
        return getTracesRequestCreation(projectId, compoundId).toEntity(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param compoundId The compoundId parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getTracesWithResponseSpec(String projectId, String compoundId) throws WebClientResponseException {
        return getTracesRequestCreation(projectId, compoundId);
    }
}
