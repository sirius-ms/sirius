package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;

import de.unijena.bioinf.ms.nightsky.sdk.model.AlignedFeature;
import de.unijena.bioinf.ms.nightsky.sdk.model.AlignedFeatureOptField;
import de.unijena.bioinf.ms.nightsky.sdk.model.FormulaCandidate;
import de.unijena.bioinf.ms.nightsky.sdk.model.FormulaCandidateOptField;
import de.unijena.bioinf.ms.nightsky.sdk.model.PageAlignedFeature;
import de.unijena.bioinf.ms.nightsky.sdk.model.PageFormulaCandidate;
import de.unijena.bioinf.ms.nightsky.sdk.model.PageStructureCandidateFormula;
import de.unijena.bioinf.ms.nightsky.sdk.model.PageStructureCandidateScored;
import de.unijena.bioinf.ms.nightsky.sdk.model.SearchQueryType;
import de.unijena.bioinf.ms.nightsky.sdk.model.StructureCandidateOptField;

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

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class FeaturesApi {
    private ApiClient apiClient;

    public FeaturesApi() {
        this(new ApiClient());
    }

    @Autowired
    public FeaturesApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteAlignedFeatureRequestCreation(String projectId, String alignedFeatureId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteAlignedFeature", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling deleteAlignedFeature", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

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
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteAlignedFeature(String projectId, String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteAlignedFeatureRequestCreation(projectId, alignedFeatureId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteAlignedFeatureWithHttpInfo(String projectId, String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteAlignedFeatureRequestCreation(projectId, alignedFeatureId).toEntity(localVarReturnType).block();
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteAlignedFeatureWithResponseSpec(String projectId, String alignedFeatureId) throws WebClientResponseException {
        return deleteAlignedFeatureRequestCreation(projectId, alignedFeatureId);
    }
    /**
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeature with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return AlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeatureRequestCreation(String projectId, String alignedFeatureId, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeature", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getAlignedFeature", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeature with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return AlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AlignedFeature getAlignedFeature(String projectId, String alignedFeatureId, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return getAlignedFeatureRequestCreation(projectId, alignedFeatureId, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeature with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;AlignedFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AlignedFeature> getAlignedFeatureWithHttpInfo(String projectId, String alignedFeatureId, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return getAlignedFeatureRequestCreation(projectId, alignedFeatureId, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeature with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeatureWithResponseSpec(String projectId, String alignedFeatureId, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        return getAlignedFeatureRequestCreation(projectId, alignedFeatureId, optFields);
    }
    /**
     * Get all available features (aligned over runs) in the given project-space.
     * Get all available features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PageAlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeaturesRequestCreation(String projectId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeatures", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "searchQuery", searchQuery));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "querySyntax", querySyntax));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PageAlignedFeature> localVarReturnType = new ParameterizedTypeReference<PageAlignedFeature>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get all available features (aligned over runs) in the given project-space.
     * Get all available features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PageAlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageAlignedFeature getAlignedFeatures(String projectId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageAlignedFeature> localVarReturnType = new ParameterizedTypeReference<PageAlignedFeature>() {};
        return getAlignedFeaturesRequestCreation(projectId, page, size, sort, searchQuery, querySyntax, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get all available features (aligned over runs) in the given project-space.
     * Get all available features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PageAlignedFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageAlignedFeature> getAlignedFeaturesWithHttpInfo(String projectId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageAlignedFeature> localVarReturnType = new ParameterizedTypeReference<PageAlignedFeature>() {};
        return getAlignedFeaturesRequestCreation(projectId, page, size, sort, searchQuery, querySyntax, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Get all available features (aligned over runs) in the given project-space.
     * Get all available features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeaturesWithResponseSpec(String projectId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        return getAlignedFeaturesRequestCreation(projectId, page, size, sort, searchQuery, querySyntax, optFields);
    }
    /**
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.  Can be enriched with an optional results overview and formula candidate information.
     * <p><b>200</b> - FormulaCandidate of this feature (aligned over runs) with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return FormulaCandidate
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFormulaCandidateRequestCreation(String projectId, String alignedFeatureId, String formulaId, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFormulaCandidate", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getFormulaCandidate", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getFormulaCandidate", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<FormulaCandidate> localVarReturnType = new ParameterizedTypeReference<FormulaCandidate>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.  Can be enriched with an optional results overview and formula candidate information.
     * <p><b>200</b> - FormulaCandidate of this feature (aligned over runs) with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return FormulaCandidate
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public FormulaCandidate getFormulaCandidate(String projectId, String alignedFeatureId, String formulaId, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<FormulaCandidate> localVarReturnType = new ParameterizedTypeReference<FormulaCandidate>() {};
        return getFormulaCandidateRequestCreation(projectId, alignedFeatureId, formulaId, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.  Can be enriched with an optional results overview and formula candidate information.
     * <p><b>200</b> - FormulaCandidate of this feature (aligned over runs) with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;FormulaCandidate&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<FormulaCandidate> getFormulaCandidateWithHttpInfo(String projectId, String alignedFeatureId, String formulaId, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<FormulaCandidate> localVarReturnType = new ParameterizedTypeReference<FormulaCandidate>() {};
        return getFormulaCandidateRequestCreation(projectId, alignedFeatureId, formulaId, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.  Can be enriched with an optional results overview and formula candidate information.
     * <p><b>200</b> - FormulaCandidate of this feature (aligned over runs) with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFormulaCandidateWithResponseSpec(String projectId, String alignedFeatureId, String formulaId, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        return getFormulaCandidateRequestCreation(projectId, alignedFeatureId, formulaId, optFields);
    }
    /**
     * List of all FormulaResultContainers available for this feature with minimal information.
     * List of all FormulaResultContainers available for this feature with minimal information.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PageFormulaCandidate
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFormulaCandidatesRequestCreation(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFormulaCandidates", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getFormulaCandidates", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "searchQuery", searchQuery));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "querySyntax", querySyntax));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PageFormulaCandidate> localVarReturnType = new ParameterizedTypeReference<PageFormulaCandidate>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List of all FormulaResultContainers available for this feature with minimal information.
     * List of all FormulaResultContainers available for this feature with minimal information.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PageFormulaCandidate
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageFormulaCandidate getFormulaCandidates(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageFormulaCandidate> localVarReturnType = new ParameterizedTypeReference<PageFormulaCandidate>() {};
        return getFormulaCandidatesRequestCreation(projectId, alignedFeatureId, page, size, sort, searchQuery, querySyntax, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * List of all FormulaResultContainers available for this feature with minimal information.
     * List of all FormulaResultContainers available for this feature with minimal information.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PageFormulaCandidate&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageFormulaCandidate> getFormulaCandidatesWithHttpInfo(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageFormulaCandidate> localVarReturnType = new ParameterizedTypeReference<PageFormulaCandidate>() {};
        return getFormulaCandidatesRequestCreation(projectId, alignedFeatureId, page, size, sort, searchQuery, querySyntax, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * List of all FormulaResultContainers available for this feature with minimal information.
     * List of all FormulaResultContainers available for this feature with minimal information.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFormulaCandidatesWithResponseSpec(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        return getFormulaCandidatesRequestCreation(projectId, alignedFeatureId, page, size, sort, searchQuery, querySyntax, optFields);
    }
    /**
     * List of StructureCandidates for the given &#39;alignedFeatureId&#39; with minimal information.
     * List of StructureCandidates for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PageStructureCandidateFormula
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getStructureCandidatesRequestCreation(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getStructureCandidates", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getStructureCandidates", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "searchQuery", searchQuery));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "querySyntax", querySyntax));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PageStructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<PageStructureCandidateFormula>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/structures", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List of StructureCandidates for the given &#39;alignedFeatureId&#39; with minimal information.
     * List of StructureCandidates for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PageStructureCandidateFormula
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageStructureCandidateFormula getStructureCandidates(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageStructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<PageStructureCandidateFormula>() {};
        return getStructureCandidatesRequestCreation(projectId, alignedFeatureId, page, size, sort, searchQuery, querySyntax, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * List of StructureCandidates for the given &#39;alignedFeatureId&#39; with minimal information.
     * List of StructureCandidates for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PageStructureCandidateFormula&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageStructureCandidateFormula> getStructureCandidatesWithHttpInfo(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageStructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<PageStructureCandidateFormula>() {};
        return getStructureCandidatesRequestCreation(projectId, alignedFeatureId, page, size, sort, searchQuery, querySyntax, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * List of StructureCandidates for the given &#39;alignedFeatureId&#39; with minimal information.
     * List of StructureCandidates for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStructureCandidatesWithResponseSpec(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getStructureCandidatesRequestCreation(projectId, alignedFeatureId, page, size, sort, searchQuery, querySyntax, optFields);
    }
    /**
     * List of StructureCandidates the given &#39;formulaId&#39; with minimal information.
     * List of StructureCandidates the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PageStructureCandidateScored
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getStructureCandidatesByFormulaRequestCreation(String projectId, String alignedFeatureId, String formulaId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getStructureCandidatesByFormula", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getStructureCandidatesByFormula", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getStructureCandidatesByFormula", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "searchQuery", searchQuery));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "querySyntax", querySyntax));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PageStructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<PageStructureCandidateScored>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/structures", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List of StructureCandidates the given &#39;formulaId&#39; with minimal information.
     * List of StructureCandidates the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PageStructureCandidateScored
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageStructureCandidateScored getStructureCandidatesByFormula(String projectId, String alignedFeatureId, String formulaId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageStructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<PageStructureCandidateScored>() {};
        return getStructureCandidatesByFormulaRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, searchQuery, querySyntax, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * List of StructureCandidates the given &#39;formulaId&#39; with minimal information.
     * List of StructureCandidates the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PageStructureCandidateScored&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageStructureCandidateScored> getStructureCandidatesByFormulaWithHttpInfo(String projectId, String alignedFeatureId, String formulaId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageStructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<PageStructureCandidateScored>() {};
        return getStructureCandidatesByFormulaRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, searchQuery, querySyntax, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * List of StructureCandidates the given &#39;formulaId&#39; with minimal information.
     * List of StructureCandidates the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStructureCandidatesByFormulaWithResponseSpec(String projectId, String alignedFeatureId, String formulaId, Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getStructureCandidatesByFormulaRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, searchQuery, querySyntax, optFields);
    }
}
