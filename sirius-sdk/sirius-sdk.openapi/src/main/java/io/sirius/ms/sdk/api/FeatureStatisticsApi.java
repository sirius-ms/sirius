package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.AggregationType;
import io.sirius.ms.sdk.model.FoldChange;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobOptField;
import io.sirius.ms.sdk.model.QuantMeasure;
import io.sirius.ms.sdk.model.StatisticsTable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.14.0")
public class FeatureStatisticsApi {
    private ApiClient apiClient;

    public FeatureStatisticsApi() {
        this(new ApiClient());
    }

    public FeatureStatisticsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    
    /**
     * [EXPERIMENTAL] Compute the fold change between two groups of runs
     * [EXPERIMENTAL] Compute the fold change between two groups of runs.  &lt;p&gt;  The runs need to be tagged and grouped.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param leftGroupName name of the left tag group.
     * @param rightGroupName name of the right tag group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @param optFields job opt fields.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec computeAlignedFeatureFoldChangesExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String leftGroupName, @jakarta.annotation.Nonnull String rightGroupName, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification, @jakarta.annotation.Nullable List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling computeAlignedFeatureFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'leftGroupName' is set
        if (leftGroupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'leftGroupName' when calling computeAlignedFeatureFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'rightGroupName' is set
        if (rightGroupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'rightGroupName' when calling computeAlignedFeatureFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "leftGroupName", leftGroupName));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "rightGroupName", rightGroupName));
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
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/statistics/foldchange/compute", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Compute the fold change between two groups of runs
     * [EXPERIMENTAL] Compute the fold change between two groups of runs.  &lt;p&gt;  The runs need to be tagged and grouped.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param leftGroupName name of the left tag group.
     * @param rightGroupName name of the right tag group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @param optFields job opt fields.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job computeAlignedFeatureFoldChangesExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String leftGroupName, @jakarta.annotation.Nonnull String rightGroupName, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification, @jakarta.annotation.Nullable List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return computeAlignedFeatureFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Compute the fold change between two groups of runs
     * [EXPERIMENTAL] Compute the fold change between two groups of runs.  &lt;p&gt;  The runs need to be tagged and grouped.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param leftGroupName name of the left tag group.
     * @param rightGroupName name of the right tag group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @param optFields job opt fields.
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> computeAlignedFeatureFoldChangesExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String leftGroupName, @jakarta.annotation.Nonnull String rightGroupName, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification, @jakarta.annotation.Nullable List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return computeAlignedFeatureFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Compute the fold change between two groups of runs
     * [EXPERIMENTAL] Compute the fold change between two groups of runs.  &lt;p&gt;  The runs need to be tagged and grouped.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param leftGroupName name of the left tag group.
     * @param rightGroupName name of the right tag group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @param optFields job opt fields.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec computeAlignedFeatureFoldChangesExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String leftGroupName, @jakarta.annotation.Nonnull String rightGroupName, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification, @jakarta.annotation.Nullable List<JobOptField> optFields) throws WebClientResponseException {
        return computeAlignedFeatureFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification, optFields);
    }

    /**
     * [EXPERIMENTAL] Delete fold changes
     * [EXPERIMENTAL] Delete fold changes.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param leftGroupName name of the left group.
     * @param rightGroupName name of the right group.
     * @param aggregation The aggregation parameter
     * @param quantification The quantification parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteAlignedFeatureFoldChangesExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String leftGroupName, @jakarta.annotation.Nonnull String rightGroupName, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteAlignedFeatureFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'leftGroupName' is set
        if (leftGroupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'leftGroupName' when calling deleteAlignedFeatureFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'rightGroupName' is set
        if (rightGroupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'rightGroupName' when calling deleteAlignedFeatureFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "leftGroupName", leftGroupName));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "rightGroupName", rightGroupName));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "aggregation", aggregation));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "quantification", quantification));
        
        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/statistics/foldchanges", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Delete fold changes
     * [EXPERIMENTAL] Delete fold changes.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param leftGroupName name of the left group.
     * @param rightGroupName name of the right group.
     * @param aggregation The aggregation parameter
     * @param quantification The quantification parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteAlignedFeatureFoldChangesExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String leftGroupName, @jakarta.annotation.Nonnull String rightGroupName, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteAlignedFeatureFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete fold changes
     * [EXPERIMENTAL] Delete fold changes.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param leftGroupName name of the left group.
     * @param rightGroupName name of the right group.
     * @param aggregation The aggregation parameter
     * @param quantification The quantification parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteAlignedFeatureFoldChangesExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String leftGroupName, @jakarta.annotation.Nonnull String rightGroupName, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteAlignedFeatureFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete fold changes
     * [EXPERIMENTAL] Delete fold changes.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param leftGroupName name of the left group.
     * @param rightGroupName name of the right group.
     * @param aggregation The aggregation parameter
     * @param quantification The quantification parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteAlignedFeatureFoldChangesExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String leftGroupName, @jakarta.annotation.Nonnull String rightGroupName, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        return deleteAlignedFeatureFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification);
    }

    /**
     * [EXPERIMENTAL] Get table of all fold changes in the project space
     * [EXPERIMENTAL] Get table of all fold changes in the project space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - table of fold changes.
     * @param projectId project-space to read from.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @return StatisticsTable
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeatureFoldChangeTableExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeatureFoldChangeTableExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "aggregation", aggregation));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "quantification", quantification));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<StatisticsTable> localVarReturnType = new ParameterizedTypeReference<StatisticsTable>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/statistics/foldchanges/stats-table", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get table of all fold changes in the project space
     * [EXPERIMENTAL] Get table of all fold changes in the project space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - table of fold changes.
     * @param projectId project-space to read from.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @return StatisticsTable
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public StatisticsTable getAlignedFeatureFoldChangeTableExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        ParameterizedTypeReference<StatisticsTable> localVarReturnType = new ParameterizedTypeReference<StatisticsTable>() {};
        return getAlignedFeatureFoldChangeTableExperimentalRequestCreation(projectId, aggregation, quantification).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get table of all fold changes in the project space
     * [EXPERIMENTAL] Get table of all fold changes in the project space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - table of fold changes.
     * @param projectId project-space to read from.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @return ResponseEntity&lt;StatisticsTable&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<StatisticsTable> getAlignedFeatureFoldChangeTableExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        ParameterizedTypeReference<StatisticsTable> localVarReturnType = new ParameterizedTypeReference<StatisticsTable>() {};
        return getAlignedFeatureFoldChangeTableExperimentalRequestCreation(projectId, aggregation, quantification).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get table of all fold changes in the project space
     * [EXPERIMENTAL] Get table of all fold changes in the project space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - table of fold changes.
     * @param projectId project-space to read from.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeatureFoldChangeTableExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        return getAlignedFeatureFoldChangeTableExperimentalRequestCreation(projectId, aggregation, quantification);
    }

    /**
     * [EXPERIMENTAL] List all fold changes that are associated with an object
     * [EXPERIMENTAL] List all fold changes that are associated with an object.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - fold changes
     * @param projectId project-space to read from.
     * @param objectId id of the object the fold changes are assigned to.
     * @return List&lt;FoldChange&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFoldChangesByAlignedFeatureExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFoldChangesByAlignedFeatureExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'objectId' is set
        if (objectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'objectId' when calling getFoldChangesByAlignedFeatureExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("objectId", objectId);

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

        ParameterizedTypeReference<FoldChange> localVarReturnType = new ParameterizedTypeReference<FoldChange>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/statistics/foldchanges/{objectId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] List all fold changes that are associated with an object
     * [EXPERIMENTAL] List all fold changes that are associated with an object.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - fold changes
     * @param projectId project-space to read from.
     * @param objectId id of the object the fold changes are assigned to.
     * @return List&lt;FoldChange&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<FoldChange> getFoldChangesByAlignedFeatureExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        ParameterizedTypeReference<FoldChange> localVarReturnType = new ParameterizedTypeReference<FoldChange>() {};
        return getFoldChangesByAlignedFeatureExperimentalRequestCreation(projectId, objectId).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] List all fold changes that are associated with an object
     * [EXPERIMENTAL] List all fold changes that are associated with an object.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - fold changes
     * @param projectId project-space to read from.
     * @param objectId id of the object the fold changes are assigned to.
     * @return ResponseEntity&lt;List&lt;FoldChange&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<FoldChange>> getFoldChangesByAlignedFeatureExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        ParameterizedTypeReference<FoldChange> localVarReturnType = new ParameterizedTypeReference<FoldChange>() {};
        return getFoldChangesByAlignedFeatureExperimentalRequestCreation(projectId, objectId).toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] List all fold changes that are associated with an object
     * [EXPERIMENTAL] List all fold changes that are associated with an object.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - fold changes
     * @param projectId project-space to read from.
     * @param objectId id of the object the fold changes are assigned to.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFoldChangesByAlignedFeatureExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        return getFoldChangesByAlignedFeatureExperimentalRequestCreation(projectId, objectId);
    }
}
