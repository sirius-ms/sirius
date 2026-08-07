package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.AggregationType;
import io.sirius.ms.sdk.model.FoldChangeJobSubmission;
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

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.17.0")
public class NpcClassStatisticsApi {
    private ApiClient apiClient;

    public NpcClassStatisticsApi() {
        this(new ApiClient());
    }

    public NpcClassStatisticsApi(ApiClient apiClient) {
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
     * @param foldChangeJobSubmission Parameters of fold change job
     * @param optFields job opt fields.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec computeNpcClassFoldChangesExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull FoldChangeJobSubmission foldChangeJobSubmission, @jakarta.annotation.Nullable List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = foldChangeJobSubmission;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling computeNpcClassFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'foldChangeJobSubmission' is set
        if (foldChangeJobSubmission == null) {
            throw new WebClientResponseException("Missing the required parameter 'foldChangeJobSubmission' when calling computeNpcClassFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/npc-classes/statistics/foldchange/compute", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Compute the fold change between two groups of runs
     * [EXPERIMENTAL] Compute the fold change between two groups of runs.  &lt;p&gt;  The runs need to be tagged and grouped.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param foldChangeJobSubmission Parameters of fold change job
     * @param optFields job opt fields.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job computeNpcClassFoldChangesExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull FoldChangeJobSubmission foldChangeJobSubmission, @jakarta.annotation.Nullable List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return computeNpcClassFoldChangesExperimentalRequestCreation(projectId, foldChangeJobSubmission, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Compute the fold change between two groups of runs
     * [EXPERIMENTAL] Compute the fold change between two groups of runs.  &lt;p&gt;  The runs need to be tagged and grouped.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param foldChangeJobSubmission Parameters of fold change job
     * @param optFields job opt fields.
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> computeNpcClassFoldChangesExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull FoldChangeJobSubmission foldChangeJobSubmission, @jakarta.annotation.Nullable List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return computeNpcClassFoldChangesExperimentalRequestCreation(projectId, foldChangeJobSubmission, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Compute the fold change between two groups of runs
     * [EXPERIMENTAL] Compute the fold change between two groups of runs.  &lt;p&gt;  The runs need to be tagged and grouped.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param foldChangeJobSubmission Parameters of fold change job
     * @param optFields job opt fields.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec computeNpcClassFoldChangesExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull FoldChangeJobSubmission foldChangeJobSubmission, @jakarta.annotation.Nullable List<JobOptField> optFields) throws WebClientResponseException {
        return computeNpcClassFoldChangesExperimentalRequestCreation(projectId, foldChangeJobSubmission, optFields);
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
    private ResponseSpec deleteNpcClassFoldChangesExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String leftGroupName, @jakarta.annotation.Nonnull String rightGroupName, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteNpcClassFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'leftGroupName' is set
        if (leftGroupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'leftGroupName' when calling deleteNpcClassFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'rightGroupName' is set
        if (rightGroupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'rightGroupName' when calling deleteNpcClassFoldChangesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/npc-classes/statistics/foldchanges", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
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
    public void deleteNpcClassFoldChangesExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String leftGroupName, @jakarta.annotation.Nonnull String rightGroupName, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteNpcClassFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification).bodyToMono(localVarReturnType).block();
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
    public ResponseEntity<Void> deleteNpcClassFoldChangesExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String leftGroupName, @jakarta.annotation.Nonnull String rightGroupName, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteNpcClassFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification).toEntity(localVarReturnType).block();
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
    public ResponseSpec deleteNpcClassFoldChangesExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String leftGroupName, @jakarta.annotation.Nonnull String rightGroupName, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        return deleteNpcClassFoldChangesExperimentalRequestCreation(projectId, leftGroupName, rightGroupName, aggregation, quantification);
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
    private ResponseSpec getNpcClassFoldChangeTableExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getNpcClassFoldChangeTableExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/npc-classes/statistics/foldchanges/stats-table", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
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
    public StatisticsTable getNpcClassFoldChangeTableExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        ParameterizedTypeReference<StatisticsTable> localVarReturnType = new ParameterizedTypeReference<StatisticsTable>() {};
        return getNpcClassFoldChangeTableExperimentalRequestCreation(projectId, aggregation, quantification).bodyToMono(localVarReturnType).block();
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
    public ResponseEntity<StatisticsTable> getNpcClassFoldChangeTableExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        ParameterizedTypeReference<StatisticsTable> localVarReturnType = new ParameterizedTypeReference<StatisticsTable>() {};
        return getNpcClassFoldChangeTableExperimentalRequestCreation(projectId, aggregation, quantification).toEntity(localVarReturnType).block();
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
    public ResponseSpec getNpcClassFoldChangeTableExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable AggregationType aggregation, @jakarta.annotation.Nullable QuantMeasure quantification) throws WebClientResponseException {
        return getNpcClassFoldChangeTableExperimentalRequestCreation(projectId, aggregation, quantification);
    }
}
