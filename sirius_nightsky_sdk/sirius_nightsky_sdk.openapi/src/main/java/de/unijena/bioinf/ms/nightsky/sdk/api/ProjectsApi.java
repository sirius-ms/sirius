package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;

import de.unijena.bioinf.ms.nightsky.sdk.model.PageProjectInfo;
import de.unijena.bioinf.ms.nightsky.sdk.model.ProjectInfo;
import de.unijena.bioinf.ms.nightsky.sdk.model.SearchQueryType;

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
public class ProjectsApi {
    private ApiClient apiClient;

    public ProjectsApi() {
        this(new ApiClient());
    }

    @Autowired
    public ProjectsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Close project-space and remove it from application.
     * Close project-space and remove it from application. Project will NOT be deleted from disk.   ATTENTION: This will cancel and remove all jobs running on this Project before closing it.  If there are many jobs, this might take some time.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier of the  project-space to be closed.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec closeProjectSpaceRequestCreation(String projectId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling closeProjectSpace", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Close project-space and remove it from application.
     * Close project-space and remove it from application. Project will NOT be deleted from disk.   ATTENTION: This will cancel and remove all jobs running on this Project before closing it.  If there are many jobs, this might take some time.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier of the  project-space to be closed.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void closeProjectSpace(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        closeProjectSpaceRequestCreation(projectId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Close project-space and remove it from application.
     * Close project-space and remove it from application. Project will NOT be deleted from disk.   ATTENTION: This will cancel and remove all jobs running on this Project before closing it.  If there are many jobs, this might take some time.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier of the  project-space to be closed.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> closeProjectSpaceWithHttpInfo(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return closeProjectSpaceRequestCreation(projectId).toEntity(localVarReturnType).block();
    }

    /**
     * Close project-space and remove it from application.
     * Close project-space and remove it from application. Project will NOT be deleted from disk.   ATTENTION: This will cancel and remove all jobs running on this Project before closing it.  If there are many jobs, this might take some time.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier of the  project-space to be closed.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec closeProjectSpaceWithResponseSpec(String projectId) throws WebClientResponseException {
        return closeProjectSpaceRequestCreation(projectId);
    }
    /**
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the newly created project-space.
     * @param pathToProject The pathToProject parameter
     * @param pathToSourceProject The pathToSourceProject parameter
     * @param awaitImport The awaitImport parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec createProjectSpaceRequestCreation(String projectId, String pathToProject, String pathToSourceProject, Boolean awaitImport) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling createProjectSpace", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'pathToProject' is set
        if (pathToProject == null) {
            throw new WebClientResponseException("Missing the required parameter 'pathToProject' when calling createProjectSpace", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "pathToProject", pathToProject));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "pathToSourceProject", pathToSourceProject));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "awaitImport", awaitImport));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the newly created project-space.
     * @param pathToProject The pathToProject parameter
     * @param pathToSourceProject The pathToSourceProject parameter
     * @param awaitImport The awaitImport parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ProjectInfo createProjectSpace(String projectId, String pathToProject, String pathToSourceProject, Boolean awaitImport) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return createProjectSpaceRequestCreation(projectId, pathToProject, pathToSourceProject, awaitImport).bodyToMono(localVarReturnType).block();
    }

    /**
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the newly created project-space.
     * @param pathToProject The pathToProject parameter
     * @param pathToSourceProject The pathToSourceProject parameter
     * @param awaitImport The awaitImport parameter
     * @return ResponseEntity&lt;ProjectInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ProjectInfo> createProjectSpaceWithHttpInfo(String projectId, String pathToProject, String pathToSourceProject, Boolean awaitImport) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return createProjectSpaceRequestCreation(projectId, pathToProject, pathToSourceProject, awaitImport).toEntity(localVarReturnType).block();
    }

    /**
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the newly created project-space.
     * @param pathToProject The pathToProject parameter
     * @param pathToSourceProject The pathToSourceProject parameter
     * @param awaitImport The awaitImport parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec createProjectSpaceWithResponseSpec(String projectId, String pathToProject, String pathToSourceProject, Boolean awaitImport) throws WebClientResponseException {
        return createProjectSpaceRequestCreation(projectId, pathToProject, pathToSourceProject, awaitImport);
    }
    /**
     * Get project space info by its projectId.
     * Get project space info by its projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier tof the project-space to be accessed.
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getProjectSpaceRequestCreation(String projectId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getProjectSpace", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

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

        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get project space info by its projectId.
     * Get project space info by its projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier tof the project-space to be accessed.
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ProjectInfo getProjectSpace(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return getProjectSpaceRequestCreation(projectId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get project space info by its projectId.
     * Get project space info by its projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier tof the project-space to be accessed.
     * @return ResponseEntity&lt;ProjectInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ProjectInfo> getProjectSpaceWithHttpInfo(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return getProjectSpaceRequestCreation(projectId).toEntity(localVarReturnType).block();
    }

    /**
     * Get project space info by its projectId.
     * Get project space info by its projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier tof the project-space to be accessed.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getProjectSpaceWithResponseSpec(String projectId) throws WebClientResponseException {
        return getProjectSpaceRequestCreation(projectId);
    }
    /**
     * List opened project spaces.
     * List opened project spaces.
     * <p><b>200</b> - OK
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @return PageProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getProjectSpacesRequestCreation(Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "searchQuery", searchQuery));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "querySyntax", querySyntax));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PageProjectInfo> localVarReturnType = new ParameterizedTypeReference<PageProjectInfo>() {};
        return apiClient.invokeAPI("/api/projects", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List opened project spaces.
     * List opened project spaces.
     * <p><b>200</b> - OK
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @return PageProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageProjectInfo getProjectSpaces(Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax) throws WebClientResponseException {
        ParameterizedTypeReference<PageProjectInfo> localVarReturnType = new ParameterizedTypeReference<PageProjectInfo>() {};
        return getProjectSpacesRequestCreation(page, size, sort, searchQuery, querySyntax).bodyToMono(localVarReturnType).block();
    }

    /**
     * List opened project spaces.
     * List opened project spaces.
     * <p><b>200</b> - OK
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @return ResponseEntity&lt;PageProjectInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageProjectInfo> getProjectSpacesWithHttpInfo(Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax) throws WebClientResponseException {
        ParameterizedTypeReference<PageProjectInfo> localVarReturnType = new ParameterizedTypeReference<PageProjectInfo>() {};
        return getProjectSpacesRequestCreation(page, size, sort, searchQuery, querySyntax).toEntity(localVarReturnType).block();
    }

    /**
     * List opened project spaces.
     * List opened project spaces.
     * <p><b>200</b> - OK
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getProjectSpacesWithResponseSpec(Integer page, Integer size, List<String> sort, String searchQuery, SearchQueryType querySyntax) throws WebClientResponseException {
        return getProjectSpacesRequestCreation(page, size, sort, searchQuery, querySyntax);
    }
    /**
     * Open an existing project-space and make it accessible via the given projectId.
     * Open an existing project-space and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the opened project-space.
     * @param pathToProject The pathToProject parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec openProjectSpaceRequestCreation(String projectId, String pathToProject) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling openProjectSpace", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'pathToProject' is set
        if (pathToProject == null) {
            throw new WebClientResponseException("Missing the required parameter 'pathToProject' when calling openProjectSpace", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "pathToProject", pathToProject));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Open an existing project-space and make it accessible via the given projectId.
     * Open an existing project-space and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the opened project-space.
     * @param pathToProject The pathToProject parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ProjectInfo openProjectSpace(String projectId, String pathToProject) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return openProjectSpaceRequestCreation(projectId, pathToProject).bodyToMono(localVarReturnType).block();
    }

    /**
     * Open an existing project-space and make it accessible via the given projectId.
     * Open an existing project-space and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the opened project-space.
     * @param pathToProject The pathToProject parameter
     * @return ResponseEntity&lt;ProjectInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ProjectInfo> openProjectSpaceWithHttpInfo(String projectId, String pathToProject) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return openProjectSpaceRequestCreation(projectId, pathToProject).toEntity(localVarReturnType).block();
    }

    /**
     * Open an existing project-space and make it accessible via the given projectId.
     * Open an existing project-space and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the opened project-space.
     * @param pathToProject The pathToProject parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec openProjectSpaceWithResponseSpec(String projectId, String pathToProject) throws WebClientResponseException {
        return openProjectSpaceRequestCreation(projectId, pathToProject);
    }
}
