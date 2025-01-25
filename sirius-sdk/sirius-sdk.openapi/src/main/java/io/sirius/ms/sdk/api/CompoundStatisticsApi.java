package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.AggregationType;
import io.sirius.ms.sdk.model.CompoundFoldChange;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobOptField;
import io.sirius.ms.sdk.model.QuantMeasure;
import io.sirius.ms.sdk.model.StatisticsTable;

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
    private ResponseSpec computeCompoundFoldChangesExperimentalRequestCreation(String projectId, String leftGroupName, String rightGroupName, AggregationType aggregation, QuantMeasure quantification, List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling computeCompoundFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'leftGroupName' is set
        if (leftGroupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'leftGroupName' when calling computeCompoundFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'rightGroupName' is set
        if (rightGroupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'rightGroupName' when calling computeCompoundFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/statistics/foldchange/compute", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
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
    public Job computeCompoundFoldChangesExperimental(String projectId, String leftGroupName, String rightGroupName, AggregationType aggregation, QuantMeasure quantification, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return computeCompoundFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification, optFields).bodyToMono(localVarReturnType).block();
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
    public ResponseEntity<Job> computeCompoundFoldChangesExperimentalWithHttpInfo(String projectId, String leftGroupName, String rightGroupName, AggregationType aggregation, QuantMeasure quantification, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return computeCompoundFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification, optFields).toEntity(localVarReturnType).block();
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
    public ResponseSpec computeCompoundFoldChangesExperimentalWithResponseSpec(String projectId, String leftGroupName, String rightGroupName, AggregationType aggregation, QuantMeasure quantification, List<JobOptField> optFields) throws WebClientResponseException {
        return computeCompoundFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification, optFields);
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
    private ResponseSpec deleteCompoundFoldChangesExperimentalRequestCreation(String projectId, String leftGroupName, String rightGroupName, AggregationType aggregation, QuantMeasure quantification) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteCompoundFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'leftGroupName' is set
        if (leftGroupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'leftGroupName' when calling deleteCompoundFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'rightGroupName' is set
        if (rightGroupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'rightGroupName' when calling deleteCompoundFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/statistics/foldchanges", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
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
    public void deleteCompoundFoldChangesExperimental(String projectId, String leftGroupName, String rightGroupName, AggregationType aggregation, QuantMeasure quantification) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteCompoundFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification).bodyToMono(localVarReturnType).block();
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
    public ResponseEntity<Void> deleteCompoundFoldChangesExperimentalWithHttpInfo(String projectId, String leftGroupName, String rightGroupName, AggregationType aggregation, QuantMeasure quantification) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteCompoundFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification).toEntity(localVarReturnType).block();
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
    public ResponseSpec deleteCompoundFoldChangesExperimentalWithResponseSpec(String projectId, String leftGroupName, String rightGroupName, AggregationType aggregation, QuantMeasure quantification) throws WebClientResponseException {
        return deleteCompoundFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification);
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
    private ResponseSpec getCompoundFoldChangeTableExperimentalRequestCreation(String projectId, AggregationType aggregation, QuantMeasure quantification) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundFoldChangeTableExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/statistics/foldchanges/stats-table", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
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
    public StatisticsTable getCompoundFoldChangeTableExperimental(String projectId, AggregationType aggregation, QuantMeasure quantification) throws WebClientResponseException {
        ParameterizedTypeReference<StatisticsTable> localVarReturnType = new ParameterizedTypeReference<StatisticsTable>() {};
        return getCompoundFoldChangeTableExperimentalRequestCreation(projectId, aggregation, quantification).bodyToMono(localVarReturnType).block();
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
    public ResponseEntity<StatisticsTable> getCompoundFoldChangeTableExperimentalWithHttpInfo(String projectId, AggregationType aggregation, QuantMeasure quantification) throws WebClientResponseException {
        ParameterizedTypeReference<StatisticsTable> localVarReturnType = new ParameterizedTypeReference<StatisticsTable>() {};
        return getCompoundFoldChangeTableExperimentalRequestCreation(projectId, aggregation, quantification).toEntity(localVarReturnType).block();
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
    public ResponseSpec getCompoundFoldChangeTableExperimentalWithResponseSpec(String projectId, AggregationType aggregation, QuantMeasure quantification) throws WebClientResponseException {
        return getCompoundFoldChangeTableExperimentalRequestCreation(projectId, aggregation, quantification);
    }
    /**
     * [EXPERIMENTAL] Get fold changes
     * [EXPERIMENTAL] Get fold changes.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param leftGroupName name of the left group.
     * @param rightGroupName name of the right group.
     * @param aggregation The aggregation parameter
     * @param quantification The quantification parameter
     * @return List&lt;CompoundFoldChange&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundFoldChangesExperimentalRequestCreation(String projectId, String leftGroupName, String rightGroupName, AggregationType aggregation, QuantMeasure quantification) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'leftGroupName' is set
        if (leftGroupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'leftGroupName' when calling getCompoundFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'rightGroupName' is set
        if (rightGroupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'rightGroupName' when calling getCompoundFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        
        final String[] localVarAccepts = { 
            "*/*"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<CompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<CompoundFoldChange>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/statistics/foldchanges", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get fold changes
     * [EXPERIMENTAL] Get fold changes.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param leftGroupName name of the left group.
     * @param rightGroupName name of the right group.
     * @param aggregation The aggregation parameter
     * @param quantification The quantification parameter
     * @return List&lt;CompoundFoldChange&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<CompoundFoldChange> getCompoundFoldChangesExperimental(String projectId, String leftGroupName, String rightGroupName, AggregationType aggregation, QuantMeasure quantification) throws WebClientResponseException {
        ParameterizedTypeReference<CompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<CompoundFoldChange>() {};
        return getCompoundFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] Get fold changes
     * [EXPERIMENTAL] Get fold changes.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param leftGroupName name of the left group.
     * @param rightGroupName name of the right group.
     * @param aggregation The aggregation parameter
     * @param quantification The quantification parameter
     * @return ResponseEntity&lt;List&lt;CompoundFoldChange&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<CompoundFoldChange>> getCompoundFoldChangesExperimentalWithHttpInfo(String projectId, String leftGroupName, String rightGroupName, AggregationType aggregation, QuantMeasure quantification) throws WebClientResponseException {
        ParameterizedTypeReference<CompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<CompoundFoldChange>() {};
        return getCompoundFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification).toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get fold changes
     * [EXPERIMENTAL] Get fold changes.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param leftGroupName name of the left group.
     * @param rightGroupName name of the right group.
     * @param aggregation The aggregation parameter
     * @param quantification The quantification parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundFoldChangesExperimentalWithResponseSpec(String projectId, String leftGroupName, String rightGroupName, AggregationType aggregation, QuantMeasure quantification) throws WebClientResponseException {
        return getCompoundFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification);
    }
}
