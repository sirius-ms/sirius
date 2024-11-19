package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.CompoundFoldChange;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobOptField;
import io.sirius.ms.sdk.model.PageCompoundFoldChange;

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
public class CompoundStatisticsApi {
    private ApiClient apiClient;

    public CompoundStatisticsApi() {
        this(new ApiClient());
    }

    @Autowired
    public CompoundStatisticsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * **EXPERIMENTAL** Compute the fold change between two groups of runs
     * **EXPERIMENTAL** Compute the fold change between two groups of runs.   The runs need to be tagged and grouped.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param left name of the left tag group.
     * @param right name of the right tag group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @param optFields job opt fields.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec computeFoldChangeRequestCreation(String projectId, String left, String right, String aggregation, String quantification, List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling computeFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'left' is set
        if (left == null) {
            throw new WebClientResponseException("Missing the required parameter 'left' when calling computeFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'right' is set
        if (right == null) {
            throw new WebClientResponseException("Missing the required parameter 'right' when calling computeFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "left", left));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "right", right));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "aggregation", aggregation));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "quantification", quantification));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/statistics/foldchange/compute", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * **EXPERIMENTAL** Compute the fold change between two groups of runs
     * **EXPERIMENTAL** Compute the fold change between two groups of runs.   The runs need to be tagged and grouped.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param left name of the left tag group.
     * @param right name of the right tag group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @param optFields job opt fields.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job computeFoldChange(String projectId, String left, String right, String aggregation, String quantification, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return computeFoldChangeRequestCreation(projectId, left, right, aggregation, quantification, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Compute the fold change between two groups of runs
     * **EXPERIMENTAL** Compute the fold change between two groups of runs.   The runs need to be tagged and grouped.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param left name of the left tag group.
     * @param right name of the right tag group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @param optFields job opt fields.
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> computeFoldChangeWithHttpInfo(String projectId, String left, String right, String aggregation, String quantification, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return computeFoldChangeRequestCreation(projectId, left, right, aggregation, quantification, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Compute the fold change between two groups of runs
     * **EXPERIMENTAL** Compute the fold change between two groups of runs.   The runs need to be tagged and grouped.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param left name of the left tag group.
     * @param right name of the right tag group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @param optFields job opt fields.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec computeFoldChangeWithResponseSpec(String projectId, String left, String right, String aggregation, String quantification, List<JobOptField> optFields) throws WebClientResponseException {
        return computeFoldChangeRequestCreation(projectId, left, right, aggregation, quantification, optFields);
    }
    /**
     * **EXPERIMENTAL** Delete fold change
     * **EXPERIMENTAL** Delete fold change.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param left name of the left group.
     * @param right name of the right group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteFoldChangeRequestCreation(String projectId, String left, String right, String aggregation, String quantification) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'left' is set
        if (left == null) {
            throw new WebClientResponseException("Missing the required parameter 'left' when calling deleteFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'right' is set
        if (right == null) {
            throw new WebClientResponseException("Missing the required parameter 'right' when calling deleteFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "left", left));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "right", right));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "aggregation", aggregation));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "quantification", quantification));
        
        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/statistics/foldchange", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * **EXPERIMENTAL** Delete fold change
     * **EXPERIMENTAL** Delete fold change.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param left name of the left group.
     * @param right name of the right group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteFoldChange(String projectId, String left, String right, String aggregation, String quantification) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteFoldChangeRequestCreation(projectId, left, right, aggregation, quantification).bodyToMono(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Delete fold change
     * **EXPERIMENTAL** Delete fold change.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param left name of the left group.
     * @param right name of the right group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteFoldChangeWithHttpInfo(String projectId, String left, String right, String aggregation, String quantification) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteFoldChangeRequestCreation(projectId, left, right, aggregation, quantification).toEntity(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Delete fold change
     * **EXPERIMENTAL** Delete fold change.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param left name of the left group.
     * @param right name of the right group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteFoldChangeWithResponseSpec(String projectId, String left, String right, String aggregation, String quantification) throws WebClientResponseException {
        return deleteFoldChangeRequestCreation(projectId, left, right, aggregation, quantification);
    }
    /**
     * **EXPERIMENTAL** List all fold changes that are associated with a compound (group of ion identities)
     * **EXPERIMENTAL** List all fold changes that are associated with a compound (group of ion identities).   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - fold changes
     * @param projectId project-space to read from.
     * @param compoundId id of the compound (group of ion identities) the fold changes are assigned to.
     * @return List&lt;CompoundFoldChange&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFoldChangeRequestCreation(String projectId, String compoundId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling getFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<CompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<CompoundFoldChange>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/statistics/foldchange/{compoundId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * **EXPERIMENTAL** List all fold changes that are associated with a compound (group of ion identities)
     * **EXPERIMENTAL** List all fold changes that are associated with a compound (group of ion identities).   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - fold changes
     * @param projectId project-space to read from.
     * @param compoundId id of the compound (group of ion identities) the fold changes are assigned to.
     * @return List&lt;CompoundFoldChange&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<CompoundFoldChange> getFoldChange(String projectId, String compoundId) throws WebClientResponseException {
        ParameterizedTypeReference<CompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<CompoundFoldChange>() {};
        return getFoldChangeRequestCreation(projectId, compoundId).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * **EXPERIMENTAL** List all fold changes that are associated with a compound (group of ion identities)
     * **EXPERIMENTAL** List all fold changes that are associated with a compound (group of ion identities).   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - fold changes
     * @param projectId project-space to read from.
     * @param compoundId id of the compound (group of ion identities) the fold changes are assigned to.
     * @return ResponseEntity&lt;List&lt;CompoundFoldChange&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<CompoundFoldChange>> getFoldChangeWithHttpInfo(String projectId, String compoundId) throws WebClientResponseException {
        ParameterizedTypeReference<CompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<CompoundFoldChange>() {};
        return getFoldChangeRequestCreation(projectId, compoundId).toEntityList(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** List all fold changes that are associated with a compound (group of ion identities)
     * **EXPERIMENTAL** List all fold changes that are associated with a compound (group of ion identities).   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - fold changes
     * @param projectId project-space to read from.
     * @param compoundId id of the compound (group of ion identities) the fold changes are assigned to.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFoldChangeWithResponseSpec(String projectId, String compoundId) throws WebClientResponseException {
        return getFoldChangeRequestCreation(projectId, compoundId);
    }
    /**
     * **EXPERIMENTAL** Page of all fold changes in the project space
     * **EXPERIMENTAL** Page of all fold changes in the project space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - fold changes.
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return PageCompoundFoldChange
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec listFoldChangeRequestCreation(String projectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling listFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PageCompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<PageCompoundFoldChange>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/statistics/foldchange/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * **EXPERIMENTAL** Page of all fold changes in the project space
     * **EXPERIMENTAL** Page of all fold changes in the project space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - fold changes.
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return PageCompoundFoldChange
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageCompoundFoldChange listFoldChange(String projectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        ParameterizedTypeReference<PageCompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<PageCompoundFoldChange>() {};
        return listFoldChangeRequestCreation(projectId, page, size, sort).bodyToMono(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Page of all fold changes in the project space
     * **EXPERIMENTAL** Page of all fold changes in the project space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - fold changes.
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return ResponseEntity&lt;PageCompoundFoldChange&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageCompoundFoldChange> listFoldChangeWithHttpInfo(String projectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        ParameterizedTypeReference<PageCompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<PageCompoundFoldChange>() {};
        return listFoldChangeRequestCreation(projectId, page, size, sort).toEntity(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Page of all fold changes in the project space
     * **EXPERIMENTAL** Page of all fold changes in the project space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - fold changes.
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec listFoldChangeWithResponseSpec(String projectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        return listFoldChangeRequestCreation(projectId, page, size, sort);
    }
}
