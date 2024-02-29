package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;

import de.unijena.bioinf.ms.nightsky.sdk.model.ProjectInfo;
import de.unijena.bioinf.ms.nightsky.sdk.model.ProjectInfoOptField;

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
     * Move an existing (opened) project-space to another location.
     * Move an existing (opened) project-space to another location.
     * <p><b>200</b> - ProjectInfo of the newly created project if opened (copyProjectId !&#x3D; null) or the project info of  the source project otherwise
     * @param projectId unique name/identifier of the project-space that shall be copied.
     * @param pathToCopiedProject target location where the source project will be copied to.
     * @param copyProjectId optional id/mame of the newly created project (copy). If given the project will be opened.
     * @param optFields The optFields parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec copyProjectSpaceRequestCreation(String projectId, String pathToCopiedProject, String copyProjectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling copyProjectSpace", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'pathToCopiedProject' is set
        if (pathToCopiedProject == null) {
            throw new WebClientResponseException("Missing the required parameter 'pathToCopiedProject' when calling copyProjectSpace", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "pathToCopiedProject", pathToCopiedProject));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "copyProjectId", copyProjectId));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/copy", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Move an existing (opened) project-space to another location.
     * Move an existing (opened) project-space to another location.
     * <p><b>200</b> - ProjectInfo of the newly created project if opened (copyProjectId !&#x3D; null) or the project info of  the source project otherwise
     * @param projectId unique name/identifier of the project-space that shall be copied.
     * @param pathToCopiedProject target location where the source project will be copied to.
     * @param copyProjectId optional id/mame of the newly created project (copy). If given the project will be opened.
     * @param optFields The optFields parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ProjectInfo copyProjectSpace(String projectId, String pathToCopiedProject, String copyProjectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return copyProjectSpaceRequestCreation(projectId, pathToCopiedProject, copyProjectId, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Move an existing (opened) project-space to another location.
     * Move an existing (opened) project-space to another location.
     * <p><b>200</b> - ProjectInfo of the newly created project if opened (copyProjectId !&#x3D; null) or the project info of  the source project otherwise
     * @param projectId unique name/identifier of the project-space that shall be copied.
     * @param pathToCopiedProject target location where the source project will be copied to.
     * @param copyProjectId optional id/mame of the newly created project (copy). If given the project will be opened.
     * @param optFields The optFields parameter
     * @return ResponseEntity&lt;ProjectInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ProjectInfo> copyProjectSpaceWithHttpInfo(String projectId, String pathToCopiedProject, String copyProjectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return copyProjectSpaceRequestCreation(projectId, pathToCopiedProject, copyProjectId, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Move an existing (opened) project-space to another location.
     * Move an existing (opened) project-space to another location.
     * <p><b>200</b> - ProjectInfo of the newly created project if opened (copyProjectId !&#x3D; null) or the project info of  the source project otherwise
     * @param projectId unique name/identifier of the project-space that shall be copied.
     * @param pathToCopiedProject target location where the source project will be copied to.
     * @param copyProjectId optional id/mame of the newly created project (copy). If given the project will be opened.
     * @param optFields The optFields parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec copyProjectSpaceWithResponseSpec(String projectId, String pathToCopiedProject, String copyProjectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        return copyProjectSpaceRequestCreation(projectId, pathToCopiedProject, copyProjectId, optFields);
    }
    /**
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the newly created project-space. Must consist only of [a-zA-Z0-9_-].
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
     * @param projectId unique name/identifier that shall be used to access the newly created project-space. Must consist only of [a-zA-Z0-9_-].
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
     * @param projectId unique name/identifier that shall be used to access the newly created project-space. Must consist only of [a-zA-Z0-9_-].
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
     * @param projectId unique name/identifier that shall be used to access the newly created project-space. Must consist only of [a-zA-Z0-9_-].
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
     * Get CANOPUS prediction vector definition for ClassyFire classes
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param charge The charge parameter
     * @return String
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCanopusClassyFireDataRequestCreation(String projectId, Integer charge) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCanopusClassyFireData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'charge' is set
        if (charge == null) {
            throw new WebClientResponseException("Missing the required parameter 'charge' when calling getCanopusClassyFireData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "charge", charge));

        final String[] localVarAccepts = { 
            "application/csv"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/cf-data", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get CANOPUS prediction vector definition for ClassyFire classes
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param charge The charge parameter
     * @return String
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public String getCanopusClassyFireData(String projectId, Integer charge) throws WebClientResponseException {
        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return getCanopusClassyFireDataRequestCreation(projectId, charge).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get CANOPUS prediction vector definition for ClassyFire classes
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param charge The charge parameter
     * @return ResponseEntity&lt;String&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<String> getCanopusClassyFireDataWithHttpInfo(String projectId, Integer charge) throws WebClientResponseException {
        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return getCanopusClassyFireDataRequestCreation(projectId, charge).toEntity(localVarReturnType).block();
    }

    /**
     * Get CANOPUS prediction vector definition for ClassyFire classes
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param charge The charge parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCanopusClassyFireDataWithResponseSpec(String projectId, Integer charge) throws WebClientResponseException {
        return getCanopusClassyFireDataRequestCreation(projectId, charge);
    }
    /**
     * Get CANOPUS prediction vector definition for NPC classes
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param charge The charge parameter
     * @return String
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCanopusNpcDataRequestCreation(String projectId, Integer charge) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCanopusNpcData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'charge' is set
        if (charge == null) {
            throw new WebClientResponseException("Missing the required parameter 'charge' when calling getCanopusNpcData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "charge", charge));

        final String[] localVarAccepts = { 
            "application/csv"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/npc-data", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get CANOPUS prediction vector definition for NPC classes
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param charge The charge parameter
     * @return String
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public String getCanopusNpcData(String projectId, Integer charge) throws WebClientResponseException {
        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return getCanopusNpcDataRequestCreation(projectId, charge).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get CANOPUS prediction vector definition for NPC classes
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param charge The charge parameter
     * @return ResponseEntity&lt;String&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<String> getCanopusNpcDataWithHttpInfo(String projectId, Integer charge) throws WebClientResponseException {
        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return getCanopusNpcDataRequestCreation(projectId, charge).toEntity(localVarReturnType).block();
    }

    /**
     * Get CANOPUS prediction vector definition for NPC classes
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param charge The charge parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCanopusNpcDataWithResponseSpec(String projectId, Integer charge) throws WebClientResponseException {
        return getCanopusNpcDataRequestCreation(projectId, charge);
    }
    /**
     * Get CSI:FingerID fingerprint (prediction vector) definition
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param charge The charge parameter
     * @return String
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFingerIdDataRequestCreation(String projectId, Integer charge) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFingerIdData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'charge' is set
        if (charge == null) {
            throw new WebClientResponseException("Missing the required parameter 'charge' when calling getFingerIdData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "charge", charge));

        final String[] localVarAccepts = { 
            "application/csv"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/fingerid-data", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get CSI:FingerID fingerprint (prediction vector) definition
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param charge The charge parameter
     * @return String
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public String getFingerIdData(String projectId, Integer charge) throws WebClientResponseException {
        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return getFingerIdDataRequestCreation(projectId, charge).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get CSI:FingerID fingerprint (prediction vector) definition
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param charge The charge parameter
     * @return ResponseEntity&lt;String&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<String> getFingerIdDataWithHttpInfo(String projectId, Integer charge) throws WebClientResponseException {
        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return getFingerIdDataRequestCreation(projectId, charge).toEntity(localVarReturnType).block();
    }

    /**
     * Get CSI:FingerID fingerprint (prediction vector) definition
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param charge The charge parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFingerIdDataWithResponseSpec(String projectId, Integer charge) throws WebClientResponseException {
        return getFingerIdDataRequestCreation(projectId, charge);
    }
    /**
     * Get project space info by its projectId.
     * Get project space info by its projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier tof the project-space to be accessed.
     * @param optFields The optFields parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getProjectSpaceRequestCreation(String projectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
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

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

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
     * @param optFields The optFields parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ProjectInfo getProjectSpace(String projectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return getProjectSpaceRequestCreation(projectId, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get project space info by its projectId.
     * Get project space info by its projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier tof the project-space to be accessed.
     * @param optFields The optFields parameter
     * @return ResponseEntity&lt;ProjectInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ProjectInfo> getProjectSpaceWithHttpInfo(String projectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return getProjectSpaceRequestCreation(projectId, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Get project space info by its projectId.
     * Get project space info by its projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier tof the project-space to be accessed.
     * @param optFields The optFields parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getProjectSpaceWithResponseSpec(String projectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        return getProjectSpaceRequestCreation(projectId, optFields);
    }
    /**
     * List opened project spaces.
     * List opened project spaces.
     * <p><b>200</b> - OK
     * @return List&lt;ProjectInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getProjectSpacesRequestCreation() throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

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
        return apiClient.invokeAPI("/api/projects", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List opened project spaces.
     * List opened project spaces.
     * <p><b>200</b> - OK
     * @return List&lt;ProjectInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<ProjectInfo> getProjectSpaces() throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return getProjectSpacesRequestCreation().bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List opened project spaces.
     * List opened project spaces.
     * <p><b>200</b> - OK
     * @return ResponseEntity&lt;List&lt;ProjectInfo&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<ProjectInfo>> getProjectSpacesWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return getProjectSpacesRequestCreation().toEntityList(localVarReturnType).block();
    }

    /**
     * List opened project spaces.
     * List opened project spaces.
     * <p><b>200</b> - OK
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getProjectSpacesWithResponseSpec() throws WebClientResponseException {
        return getProjectSpacesRequestCreation();
    }
    /**
     * Open an existing project-space and make it accessible via the given projectId.
     * Open an existing project-space and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the opened project-space. Must consist only of [a-zA-Z0-9_-].
     * @param pathToProject The pathToProject parameter
     * @param optFields The optFields parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec openProjectSpaceRequestCreation(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
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
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

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
     * @param projectId unique name/identifier that shall be used to access the opened project-space. Must consist only of [a-zA-Z0-9_-].
     * @param pathToProject The pathToProject parameter
     * @param optFields The optFields parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ProjectInfo openProjectSpace(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return openProjectSpaceRequestCreation(projectId, pathToProject, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Open an existing project-space and make it accessible via the given projectId.
     * Open an existing project-space and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the opened project-space. Must consist only of [a-zA-Z0-9_-].
     * @param pathToProject The pathToProject parameter
     * @param optFields The optFields parameter
     * @return ResponseEntity&lt;ProjectInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ProjectInfo> openProjectSpaceWithHttpInfo(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return openProjectSpaceRequestCreation(projectId, pathToProject, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Open an existing project-space and make it accessible via the given projectId.
     * Open an existing project-space and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the opened project-space. Must consist only of [a-zA-Z0-9_-].
     * @param pathToProject The pathToProject parameter
     * @param optFields The optFields parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec openProjectSpaceWithResponseSpec(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        return openProjectSpaceRequestCreation(projectId, pathToProject, optFields);
    }
}
