package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.PagedModelRun;
import io.sirius.ms.sdk.model.Run;
import io.sirius.ms.sdk.model.RunOptField;
import io.sirius.ms.sdk.model.SearchableField;
import io.sirius.ms.sdk.model.Tag;
import io.sirius.ms.sdk.model.TagSubmission;

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
public class RunsApi {
    private ApiClient apiClient;

    public RunsApi() {
        this(new ApiClient());
    }

    public RunsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * [EXPERIMENTAL] Add tags to a run in the project
     * [EXPERIMENTAL] Add tags to a run in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to add to.
     * @param runId run to add tags to.
     * @param tag tags to add.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addTagsToRunExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String runId, @jakarta.annotation.Nonnull List<Tag> tag) throws WebClientResponseException {
        Object postBody = tag;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addTagsToRunExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'runId' is set
        if (runId == null) {
            throw new WebClientResponseException("Missing the required parameter 'runId' when calling addTagsToRunExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tag' is set
        if (tag == null) {
            throw new WebClientResponseException("Missing the required parameter 'tag' when calling addTagsToRunExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("runId", runId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json", "application/problem+json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/tags/{runId}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Add tags to a run in the project
     * [EXPERIMENTAL] Add tags to a run in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to add to.
     * @param runId run to add tags to.
     * @param tag tags to add.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Tag> addTagsToRunExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String runId, @jakarta.annotation.Nonnull List<Tag> tag) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return addTagsToRunExperimentalRequestCreation(projectId, runId, tag).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] Add tags to a run in the project
     * [EXPERIMENTAL] Add tags to a run in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to add to.
     * @param runId run to add tags to.
     * @param tag tags to add.
     * @return ResponseEntity&lt;List&lt;Tag&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Tag>> addTagsToRunExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String runId, @jakarta.annotation.Nonnull List<Tag> tag) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return addTagsToRunExperimentalRequestCreation(projectId, runId, tag).toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add tags to a run in the project
     * [EXPERIMENTAL] Add tags to a run in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to add to.
     * @param runId run to add tags to.
     * @param tag tags to add.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addTagsToRunExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String runId, @jakarta.annotation.Nonnull List<Tag> tag) throws WebClientResponseException {
        return addTagsToRunExperimentalRequestCreation(projectId, runId, tag);
    }

    /**
     * [EXPERIMENTAL] Add tags to a run in the project
     * [EXPERIMENTAL] Add tags to a run in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to add to.
     * @param tagSubmission tags with the id of run they shall be added to.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addTagsToRunsExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<TagSubmission> tagSubmission) throws WebClientResponseException {
        Object postBody = tagSubmission;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addTagsToRunsExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tagSubmission' is set
        if (tagSubmission == null) {
            throw new WebClientResponseException("Missing the required parameter 'tagSubmission' when calling addTagsToRunsExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/tags", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Add tags to a run in the project
     * [EXPERIMENTAL] Add tags to a run in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to add to.
     * @param tagSubmission tags with the id of run they shall be added to.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void addTagsToRunsExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<TagSubmission> tagSubmission) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        addTagsToRunsExperimentalRequestCreation(projectId, tagSubmission).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add tags to a run in the project
     * [EXPERIMENTAL] Add tags to a run in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to add to.
     * @param tagSubmission tags with the id of run they shall be added to.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> addTagsToRunsExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<TagSubmission> tagSubmission) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return addTagsToRunsExperimentalRequestCreation(projectId, tagSubmission).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add tags to a run in the project
     * [EXPERIMENTAL] Add tags to a run in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to add to.
     * @param tagSubmission tags with the id of run they shall be added to.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addTagsToRunsExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<TagSubmission> tagSubmission) throws WebClientResponseException {
        return addTagsToRunsExperimentalRequestCreation(projectId, tagSubmission);
    }

    /**
     * Get run with the given identifier from the specified project-space.
     * Get run with the given identifier from the specified project-space.
     * <p><b>200</b> - Run with tags (if specified).
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to read from.
     * @param runId identifier of run to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Run
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getRunRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String runId, @jakarta.annotation.Nullable List<RunOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getRun", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'runId' is set
        if (runId == null) {
            throw new WebClientResponseException("Missing the required parameter 'runId' when calling getRun", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("runId", runId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

        final String[] localVarAccepts = { 
            "application/json", "application/problem+json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Run> localVarReturnType = new ParameterizedTypeReference<Run>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/{runId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get run with the given identifier from the specified project-space.
     * Get run with the given identifier from the specified project-space.
     * <p><b>200</b> - Run with tags (if specified).
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to read from.
     * @param runId identifier of run to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Run
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Run getRun(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String runId, @jakarta.annotation.Nullable List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Run> localVarReturnType = new ParameterizedTypeReference<Run>() {};
        return getRunRequestCreation(projectId, runId, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get run with the given identifier from the specified project-space.
     * Get run with the given identifier from the specified project-space.
     * <p><b>200</b> - Run with tags (if specified).
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to read from.
     * @param runId identifier of run to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;Run&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Run> getRunWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String runId, @jakarta.annotation.Nullable List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Run> localVarReturnType = new ParameterizedTypeReference<Run>() {};
        return getRunRequestCreation(projectId, runId, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Get run with the given identifier from the specified project-space.
     * Get run with the given identifier from the specified project-space.
     * <p><b>200</b> - Run with tags (if specified).
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to read from.
     * @param runId identifier of run to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getRunWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String runId, @jakarta.annotation.Nullable List<RunOptField> optFields) throws WebClientResponseException {
        return getRunRequestCreation(projectId, runId, optFields);
    }

    /**
     * [EXPERIMENTAL] Get runs by tag group
     * [EXPERIMENTAL] Get runs by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - tagged runs
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to delete from.
     * @param groupName tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelRun
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getRunsByGroupExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String groupName, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<RunOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getRunsByGroupExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'groupName' is set
        if (groupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'groupName' when calling getRunsByGroupExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "groupName", groupName));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

        final String[] localVarAccepts = { 
            "application/json", "application/problem+json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelRun> localVarReturnType = new ParameterizedTypeReference<PagedModelRun>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/grouped", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get runs by tag group
     * [EXPERIMENTAL] Get runs by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - tagged runs
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to delete from.
     * @param groupName tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelRun
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelRun getRunsByGroupExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String groupName, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelRun> localVarReturnType = new ParameterizedTypeReference<PagedModelRun>() {};
        return getRunsByGroupExperimentalRequestCreation(projectId, groupName, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get runs by tag group
     * [EXPERIMENTAL] Get runs by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - tagged runs
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to delete from.
     * @param groupName tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PagedModelRun&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelRun> getRunsByGroupExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String groupName, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelRun> localVarReturnType = new ParameterizedTypeReference<PagedModelRun>() {};
        return getRunsByGroupExperimentalRequestCreation(projectId, groupName, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get runs by tag group
     * [EXPERIMENTAL] Get runs by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - tagged runs
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to delete from.
     * @param groupName tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getRunsByGroupExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String groupName, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<RunOptField> optFields) throws WebClientResponseException {
        return getRunsByGroupExperimentalRequestCreation(projectId, groupName, page, size, sort, optFields);
    }

    /**
     * Get runs in the given project-space
     * Get runs in the given project-space.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefixed  by a field name.  &lt;/p&gt;   Searchable fields are the indexed properties of the run (e.g. &lt;code&gt;name&lt;/code&gt;, &lt;code&gt;source&lt;/code&gt;,  &lt;code&gt;chromatography&lt;/code&gt;, &lt;code&gt;ionization&lt;/code&gt;) and project tags prefixed with the namespace  &lt;code&gt;tags.&lt;/code&gt; (e.g. &lt;code&gt;tags.MyTag&lt;/code&gt;).  Use the &lt;code&gt;searchable-fields&lt;/code&gt; endpoint (getRunsSearchableFields) to list all fields that can be  searched, including their value type, whether they support word based (full text) search, and whether  results can be sorted by them.  Possible value types are &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;double&lt;/strong&gt;,  &lt;strong&gt;boolean&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt;.   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;chromatography:LC&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;name:&amp;quot;Blank Sample 25&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;tags.MyTagB&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;tags.MyTagB:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;name:&amp;quot;Blank&amp;quot; AND chromatography:LC AND tags.city:&amp;quot;new york&amp;quot; OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;
     * <p><b>200</b> - tagged runs
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project space to get runs from.
     * @param searchQuery Optional search query in lucene syntax. Omit this parameter to page over all runs.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelRun
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getRunsPageRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<RunOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getRunsPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "searchQuery", searchQuery));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

        final String[] localVarAccepts = { 
            "application/json", "application/problem+json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelRun> localVarReturnType = new ParameterizedTypeReference<PagedModelRun>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get runs in the given project-space
     * Get runs in the given project-space.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefixed  by a field name.  &lt;/p&gt;   Searchable fields are the indexed properties of the run (e.g. &lt;code&gt;name&lt;/code&gt;, &lt;code&gt;source&lt;/code&gt;,  &lt;code&gt;chromatography&lt;/code&gt;, &lt;code&gt;ionization&lt;/code&gt;) and project tags prefixed with the namespace  &lt;code&gt;tags.&lt;/code&gt; (e.g. &lt;code&gt;tags.MyTag&lt;/code&gt;).  Use the &lt;code&gt;searchable-fields&lt;/code&gt; endpoint (getRunsSearchableFields) to list all fields that can be  searched, including their value type, whether they support word based (full text) search, and whether  results can be sorted by them.  Possible value types are &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;double&lt;/strong&gt;,  &lt;strong&gt;boolean&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt;.   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;chromatography:LC&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;name:&amp;quot;Blank Sample 25&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;tags.MyTagB&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;tags.MyTagB:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;name:&amp;quot;Blank&amp;quot; AND chromatography:LC AND tags.city:&amp;quot;new york&amp;quot; OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;
     * <p><b>200</b> - tagged runs
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project space to get runs from.
     * @param searchQuery Optional search query in lucene syntax. Omit this parameter to page over all runs.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelRun
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelRun getRunsPage(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelRun> localVarReturnType = new ParameterizedTypeReference<PagedModelRun>() {};
        return getRunsPageRequestCreation(projectId, searchQuery, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get runs in the given project-space
     * Get runs in the given project-space.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefixed  by a field name.  &lt;/p&gt;   Searchable fields are the indexed properties of the run (e.g. &lt;code&gt;name&lt;/code&gt;, &lt;code&gt;source&lt;/code&gt;,  &lt;code&gt;chromatography&lt;/code&gt;, &lt;code&gt;ionization&lt;/code&gt;) and project tags prefixed with the namespace  &lt;code&gt;tags.&lt;/code&gt; (e.g. &lt;code&gt;tags.MyTag&lt;/code&gt;).  Use the &lt;code&gt;searchable-fields&lt;/code&gt; endpoint (getRunsSearchableFields) to list all fields that can be  searched, including their value type, whether they support word based (full text) search, and whether  results can be sorted by them.  Possible value types are &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;double&lt;/strong&gt;,  &lt;strong&gt;boolean&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt;.   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;chromatography:LC&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;name:&amp;quot;Blank Sample 25&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;tags.MyTagB&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;tags.MyTagB:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;name:&amp;quot;Blank&amp;quot; AND chromatography:LC AND tags.city:&amp;quot;new york&amp;quot; OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;
     * <p><b>200</b> - tagged runs
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project space to get runs from.
     * @param searchQuery Optional search query in lucene syntax. Omit this parameter to page over all runs.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PagedModelRun&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelRun> getRunsPageWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelRun> localVarReturnType = new ParameterizedTypeReference<PagedModelRun>() {};
        return getRunsPageRequestCreation(projectId, searchQuery, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Get runs in the given project-space
     * Get runs in the given project-space.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefixed  by a field name.  &lt;/p&gt;   Searchable fields are the indexed properties of the run (e.g. &lt;code&gt;name&lt;/code&gt;, &lt;code&gt;source&lt;/code&gt;,  &lt;code&gt;chromatography&lt;/code&gt;, &lt;code&gt;ionization&lt;/code&gt;) and project tags prefixed with the namespace  &lt;code&gt;tags.&lt;/code&gt; (e.g. &lt;code&gt;tags.MyTag&lt;/code&gt;).  Use the &lt;code&gt;searchable-fields&lt;/code&gt; endpoint (getRunsSearchableFields) to list all fields that can be  searched, including their value type, whether they support word based (full text) search, and whether  results can be sorted by them.  Possible value types are &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;double&lt;/strong&gt;,  &lt;strong&gt;boolean&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt;.   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;chromatography:LC&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;name:&amp;quot;Blank Sample 25&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;tags.MyTagB&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;tags.MyTagB:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;name:&amp;quot;Blank&amp;quot; AND chromatography:LC AND tags.city:&amp;quot;new york&amp;quot; OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;
     * <p><b>200</b> - tagged runs
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project space to get runs from.
     * @param searchQuery Optional search query in lucene syntax. Omit this parameter to page over all runs.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getRunsPageWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<RunOptField> optFields) throws WebClientResponseException {
        return getRunsPageRequestCreation(projectId, searchQuery, page, size, sort, optFields);
    }

    /**
     * Get all fields that can be used in the searchQuery parameter of run endpoints
     * Get all fields that can be used in the searchQuery parameter of run endpoints.  &lt;p&gt;  Use this to build valid lucene queries: the field type determines which clauses are supported  (e.g. range queries for numeric fields, word based search for full-text fields). Includes the  dynamic tag fields (&lt;code&gt;tags.&amp;lt;tagName&amp;gt;&lt;/code&gt;) of this project. An empty list means there are no  searchable fields.
     * <p><b>200</b> - fields usable in searchQuery parameters of run endpoints.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project space to read the searchable run fields from.
     * @return List&lt;SearchableField&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getRunsSearchableFieldsRequestCreation(@jakarta.annotation.Nonnull String projectId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getRunsSearchableFields", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json", "application/problem+json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SearchableField> localVarReturnType = new ParameterizedTypeReference<SearchableField>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/searchable-fields", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get all fields that can be used in the searchQuery parameter of run endpoints
     * Get all fields that can be used in the searchQuery parameter of run endpoints.  &lt;p&gt;  Use this to build valid lucene queries: the field type determines which clauses are supported  (e.g. range queries for numeric fields, word based search for full-text fields). Includes the  dynamic tag fields (&lt;code&gt;tags.&amp;lt;tagName&amp;gt;&lt;/code&gt;) of this project. An empty list means there are no  searchable fields.
     * <p><b>200</b> - fields usable in searchQuery parameters of run endpoints.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project space to read the searchable run fields from.
     * @return List&lt;SearchableField&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<SearchableField> getRunsSearchableFields(@jakarta.annotation.Nonnull String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableField> localVarReturnType = new ParameterizedTypeReference<SearchableField>() {};
        return getRunsSearchableFieldsRequestCreation(projectId).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Get all fields that can be used in the searchQuery parameter of run endpoints
     * Get all fields that can be used in the searchQuery parameter of run endpoints.  &lt;p&gt;  Use this to build valid lucene queries: the field type determines which clauses are supported  (e.g. range queries for numeric fields, word based search for full-text fields). Includes the  dynamic tag fields (&lt;code&gt;tags.&amp;lt;tagName&amp;gt;&lt;/code&gt;) of this project. An empty list means there are no  searchable fields.
     * <p><b>200</b> - fields usable in searchQuery parameters of run endpoints.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project space to read the searchable run fields from.
     * @return ResponseEntity&lt;List&lt;SearchableField&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<SearchableField>> getRunsSearchableFieldsWithHttpInfo(@jakarta.annotation.Nonnull String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableField> localVarReturnType = new ParameterizedTypeReference<SearchableField>() {};
        return getRunsSearchableFieldsRequestCreation(projectId).toEntityList(localVarReturnType).block();
    }

    /**
     * Get all fields that can be used in the searchQuery parameter of run endpoints
     * Get all fields that can be used in the searchQuery parameter of run endpoints.  &lt;p&gt;  Use this to build valid lucene queries: the field type determines which clauses are supported  (e.g. range queries for numeric fields, word based search for full-text fields). Includes the  dynamic tag fields (&lt;code&gt;tags.&amp;lt;tagName&amp;gt;&lt;/code&gt;) of this project. An empty list means there are no  searchable fields.
     * <p><b>200</b> - fields usable in searchQuery parameters of run endpoints.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project space to read the searchable run fields from.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getRunsSearchableFieldsWithResponseSpec(@jakarta.annotation.Nonnull String projectId) throws WebClientResponseException {
        return getRunsSearchableFieldsRequestCreation(projectId);
    }

    /**
     * [EXPERIMENTAL] Get all tags associated with this Run
     * [EXPERIMENTAL] Get all tags associated with this Run
     * <p><b>200</b> - the tags of the requested object
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to get from.
     * @param objectId RunId to get tags for.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getTagsForRunExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getTagsForRunExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'objectId' is set
        if (objectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'objectId' when calling getTagsForRunExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
            "application/json", "application/problem+json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/tags/{objectId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get all tags associated with this Run
     * [EXPERIMENTAL] Get all tags associated with this Run
     * <p><b>200</b> - the tags of the requested object
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to get from.
     * @param objectId RunId to get tags for.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Tag> getTagsForRunExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return getTagsForRunExperimentalRequestCreation(projectId, objectId).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] Get all tags associated with this Run
     * [EXPERIMENTAL] Get all tags associated with this Run
     * <p><b>200</b> - the tags of the requested object
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to get from.
     * @param objectId RunId to get tags for.
     * @return ResponseEntity&lt;List&lt;Tag&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Tag>> getTagsForRunExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return getTagsForRunExperimentalRequestCreation(projectId, objectId).toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get all tags associated with this Run
     * [EXPERIMENTAL] Get all tags associated with this Run
     * <p><b>200</b> - the tags of the requested object
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to get from.
     * @param objectId RunId to get tags for.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getTagsForRunExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        return getTagsForRunExperimentalRequestCreation(projectId, objectId);
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the run with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the run with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to delete from.
     * @param runId run to delete tag from.
     * @param tagName name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec removeTagFromRunExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String runId, @jakarta.annotation.Nonnull String tagName) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling removeTagFromRunExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'runId' is set
        if (runId == null) {
            throw new WebClientResponseException("Missing the required parameter 'runId' when calling removeTagFromRunExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tagName' is set
        if (tagName == null) {
            throw new WebClientResponseException("Missing the required parameter 'tagName' when calling removeTagFromRunExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("runId", runId);
        pathParams.put("tagName", tagName);

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
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/tags/{runId}/{tagName}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the run with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the run with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to delete from.
     * @param runId run to delete tag from.
     * @param tagName name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void removeTagFromRunExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String runId, @jakarta.annotation.Nonnull String tagName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        removeTagFromRunExperimentalRequestCreation(projectId, runId, tagName).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the run with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the run with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to delete from.
     * @param runId run to delete tag from.
     * @param tagName name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> removeTagFromRunExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String runId, @jakarta.annotation.Nonnull String tagName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return removeTagFromRunExperimentalRequestCreation(projectId, runId, tagName).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the run with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the run with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - The referenced object does not exist in this SIRIUS instance or project.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param projectId project-space to delete from.
     * @param runId run to delete tag from.
     * @param tagName name of the tag to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec removeTagFromRunExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String runId, @jakarta.annotation.Nonnull String tagName) throws WebClientResponseException {
        return removeTagFromRunExperimentalRequestCreation(projectId, runId, tagName);
    }
}
