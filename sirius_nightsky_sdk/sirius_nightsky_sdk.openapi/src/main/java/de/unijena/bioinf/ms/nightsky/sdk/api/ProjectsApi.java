package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;

import de.unijena.bioinf.ms.nightsky.sdk.model.DataSmoothing;
import java.io.File;
import de.unijena.bioinf.ms.nightsky.sdk.model.ImportResult;
import de.unijena.bioinf.ms.nightsky.sdk.model.Job;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobOptField;
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

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
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
     * Close project-space and remove it from application
     * Close project-space and remove it from application. Project will NOT be deleted from disk.  &lt;p&gt;  ATTENTION: This will cancel and remove all jobs running on this Project before closing it.  If there are many jobs, this might take some time.
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
     * Close project-space and remove it from application
     * Close project-space and remove it from application. Project will NOT be deleted from disk.  &lt;p&gt;  ATTENTION: This will cancel and remove all jobs running on this Project before closing it.  If there are many jobs, this might take some time.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier of the  project-space to be closed.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void closeProjectSpace(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        closeProjectSpaceRequestCreation(projectId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Close project-space and remove it from application
     * Close project-space and remove it from application. Project will NOT be deleted from disk.  &lt;p&gt;  ATTENTION: This will cancel and remove all jobs running on this Project before closing it.  If there are many jobs, this might take some time.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier of the  project-space to be closed.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> closeProjectSpaceWithHttpInfo(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return closeProjectSpaceRequestCreation(projectId).toEntity(localVarReturnType).block();
    }

    /**
     * Close project-space and remove it from application
     * Close project-space and remove it from application. Project will NOT be deleted from disk.  &lt;p&gt;  ATTENTION: This will cancel and remove all jobs running on this Project before closing it.  If there are many jobs, this might take some time.
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
     * <p><b>200</b> - ProjectInfo of the newly created project if opened (copyProjectId !&#x3D; null) or the project info of  the source project otherwise  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
     * @param projectId unique name/identifier of the project-space that shall be copied.
     * @param pathToCopiedProject target location where the source project will be copied to.
     * @param copyProjectId optional id/mame of the newly created project (copy). If given the project will be opened.
     * @param optFields The optFields parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
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
     * <p><b>200</b> - ProjectInfo of the newly created project if opened (copyProjectId !&#x3D; null) or the project info of  the source project otherwise  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
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
     * <p><b>200</b> - ProjectInfo of the newly created project if opened (copyProjectId !&#x3D; null) or the project info of  the source project otherwise  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
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
     * <p><b>200</b> - ProjectInfo of the newly created project if opened (copyProjectId !&#x3D; null) or the project info of  the source project otherwise  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
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
     * @param pathToProject local file path where the project will be created. If NULL, project will be stored by its projectId in default project location. DEPRECATED: This parameter relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
     * @param optFields The optFields parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec createProjectSpaceRequestCreation(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling createProjectSpace", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the newly created project-space. Must consist only of [a-zA-Z0-9_-].
     * @param pathToProject local file path where the project will be created. If NULL, project will be stored by its projectId in default project location. DEPRECATED: This parameter relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
     * @param optFields The optFields parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ProjectInfo createProjectSpace(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return createProjectSpaceRequestCreation(projectId, pathToProject, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the newly created project-space. Must consist only of [a-zA-Z0-9_-].
     * @param pathToProject local file path where the project will be created. If NULL, project will be stored by its projectId in default project location. DEPRECATED: This parameter relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
     * @param optFields The optFields parameter
     * @return ResponseEntity&lt;ProjectInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ProjectInfo> createProjectSpaceWithHttpInfo(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return createProjectSpaceRequestCreation(projectId, pathToProject, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the newly created project-space. Must consist only of [a-zA-Z0-9_-].
     * @param pathToProject local file path where the project will be created. If NULL, project will be stored by its projectId in default project location. DEPRECATED: This parameter relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
     * @param optFields The optFields parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec createProjectSpaceWithResponseSpec(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        return createProjectSpaceRequestCreation(projectId, pathToProject, optFields);
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
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     * <p><b>200</b> - OK
     * @param projectId Project-space to import into.
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @param inputFiles The inputFiles parameter
     * @return ImportResult
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec importMsRunDataRequestCreation(String projectId, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge, List<File> inputFiles) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling importMsRunData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "tag", tag));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "alignRuns", alignRuns));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "allowMs1Only", allowMs1Only));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "filter", filter));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "sigma", sigma));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "scale", scale));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "window", window));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "noise", noise));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "persistence", persistence));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "merge", merge));
        
        if (inputFiles != null)
            formParams.addAll("inputFiles", inputFiles.stream().map(FileSystemResource::new).collect(Collectors.toList()));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "multipart/form-data"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/import/ms-data-files", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     * <p><b>200</b> - OK
     * @param projectId Project-space to import into.
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @param inputFiles The inputFiles parameter
     * @return ImportResult
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ImportResult importMsRunData(String projectId, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge, List<File> inputFiles) throws WebClientResponseException {
        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return importMsRunDataRequestCreation(projectId, tag, alignRuns, allowMs1Only, filter, sigma, scale, window, noise, persistence, merge, inputFiles).bodyToMono(localVarReturnType).block();
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     * <p><b>200</b> - OK
     * @param projectId Project-space to import into.
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @param inputFiles The inputFiles parameter
     * @return ResponseEntity&lt;ImportResult&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ImportResult> importMsRunDataWithHttpInfo(String projectId, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge, List<File> inputFiles) throws WebClientResponseException {
        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return importMsRunDataRequestCreation(projectId, tag, alignRuns, allowMs1Only, filter, sigma, scale, window, noise, persistence, merge, inputFiles).toEntity(localVarReturnType).block();
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     * <p><b>200</b> - OK
     * @param projectId Project-space to import into.
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @param inputFiles The inputFiles parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importMsRunDataWithResponseSpec(String projectId, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge, List<File> inputFiles) throws WebClientResponseException {
        return importMsRunDataRequestCreation(projectId, tag, alignRuns, allowMs1Only, filter, sigma, scale, window, noise, persistence, merge, inputFiles);
    }
    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job.
     * Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param inputFiles The inputFiles parameter
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec importMsRunDataAsJobRequestCreation(String projectId, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge, List<JobOptField> optFields, List<File> inputFiles) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling importMsRunDataAsJob", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "tag", tag));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "alignRuns", alignRuns));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "allowMs1Only", allowMs1Only));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "filter", filter));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "sigma", sigma));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "scale", scale));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "window", window));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "noise", noise));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "persistence", persistence));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "merge", merge));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        if (inputFiles != null)
            formParams.addAll("inputFiles", inputFiles.stream().map(FileSystemResource::new).collect(Collectors.toList()));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "multipart/form-data"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/jobs/import/ms-data-files-job", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job.
     * Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param inputFiles The inputFiles parameter
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job importMsRunDataAsJob(String projectId, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge, List<JobOptField> optFields, List<File> inputFiles) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importMsRunDataAsJobRequestCreation(projectId, tag, alignRuns, allowMs1Only, filter, sigma, scale, window, noise, persistence, merge, optFields, inputFiles).bodyToMono(localVarReturnType).block();
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job.
     * Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param inputFiles The inputFiles parameter
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> importMsRunDataAsJobWithHttpInfo(String projectId, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge, List<JobOptField> optFields, List<File> inputFiles) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importMsRunDataAsJobRequestCreation(projectId, tag, alignRuns, allowMs1Only, filter, sigma, scale, window, noise, persistence, merge, optFields, inputFiles).toEntity(localVarReturnType).block();
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job.
     * Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param inputFiles The inputFiles parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importMsRunDataAsJobWithResponseSpec(String projectId, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge, List<JobOptField> optFields, List<File> inputFiles) throws WebClientResponseException {
        return importMsRunDataAsJobRequestCreation(projectId, tag, alignRuns, allowMs1Only, filter, sigma, scale, window, noise, persistence, merge, optFields, inputFiles);
    }
    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job
     * Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;ms-data-files-job&#39; instead.
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param requestBody The requestBody parameter
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec importMsRunDataAsJobLocallyRequestCreation(String projectId, List<String> requestBody, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge, List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling importMsRunDataAsJobLocally", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'requestBody' is set
        if (requestBody == null) {
            throw new WebClientResponseException("Missing the required parameter 'requestBody' when calling importMsRunDataAsJobLocally", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "tag", tag));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "alignRuns", alignRuns));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "allowMs1Only", allowMs1Only));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "filter", filter));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "sigma", sigma));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "scale", scale));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "window", window));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "noise", noise));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "persistence", persistence));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "merge", merge));
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
        return apiClient.invokeAPI("/api/projects/{projectId}/jobs/import/ms-data-local-files-job", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job
     * Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;ms-data-files-job&#39; instead.
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param requestBody The requestBody parameter
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job importMsRunDataAsJobLocally(String projectId, List<String> requestBody, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importMsRunDataAsJobLocallyRequestCreation(projectId, requestBody, tag, alignRuns, allowMs1Only, filter, sigma, scale, window, noise, persistence, merge, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job
     * Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;ms-data-files-job&#39; instead.
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param requestBody The requestBody parameter
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> importMsRunDataAsJobLocallyWithHttpInfo(String projectId, List<String> requestBody, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importMsRunDataAsJobLocallyRequestCreation(projectId, requestBody, tag, alignRuns, allowMs1Only, filter, sigma, scale, window, noise, persistence, merge, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job
     * Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;ms-data-files-job&#39; instead.
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param requestBody The requestBody parameter
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importMsRunDataAsJobLocallyWithResponseSpec(String projectId, List<String> requestBody, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge, List<JobOptField> optFields) throws WebClientResponseException {
        return importMsRunDataAsJobLocallyRequestCreation(projectId, requestBody, tag, alignRuns, allowMs1Only, filter, sigma, scale, window, noise, persistence, merge, optFields);
    }
    /**
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;ms-data-files&#39; instead.
     * <p><b>200</b> - OK
     * @param projectId Project to import into.
     * @param requestBody Local files to import into project
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @return ImportResult
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec importMsRunDataLocallyRequestCreation(String projectId, List<String> requestBody, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling importMsRunDataLocally", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'requestBody' is set
        if (requestBody == null) {
            throw new WebClientResponseException("Missing the required parameter 'requestBody' when calling importMsRunDataLocally", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "tag", tag));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "alignRuns", alignRuns));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "allowMs1Only", allowMs1Only));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "filter", filter));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "sigma", sigma));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "scale", scale));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "window", window));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "noise", noise));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "persistence", persistence));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "merge", merge));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/import/ms-local-data-files", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;ms-data-files&#39; instead.
     * <p><b>200</b> - OK
     * @param projectId Project to import into.
     * @param requestBody Local files to import into project
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @return ImportResult
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ImportResult importMsRunDataLocally(String projectId, List<String> requestBody, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge) throws WebClientResponseException {
        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return importMsRunDataLocallyRequestCreation(projectId, requestBody, tag, alignRuns, allowMs1Only, filter, sigma, scale, window, noise, persistence, merge).bodyToMono(localVarReturnType).block();
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;ms-data-files&#39; instead.
     * <p><b>200</b> - OK
     * @param projectId Project to import into.
     * @param requestBody Local files to import into project
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @return ResponseEntity&lt;ImportResult&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ImportResult> importMsRunDataLocallyWithHttpInfo(String projectId, List<String> requestBody, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge) throws WebClientResponseException {
        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return importMsRunDataLocallyRequestCreation(projectId, requestBody, tag, alignRuns, allowMs1Only, filter, sigma, scale, window, noise, persistence, merge).toEntity(localVarReturnType).block();
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;ms-data-files&#39; instead.
     * <p><b>200</b> - OK
     * @param projectId Project to import into.
     * @param requestBody Local files to import into project
     * @param tag The tag parameter
     * @param alignRuns Align LC/MS runs.
     * @param allowMs1Only Import data without MS/MS.
     * @param filter Filter algorithm to suppress noise.
     * @param sigma Sigma (kernel width) for Gaussian filter algorithm.
     * @param scale Number of coefficients for wavelet filter algorithm.
     * @param window Wavelet window size (%) for wavelet filter algorithm.
     * @param noise Features must be larger than &lt;value&gt; * detected noise level.
     * @param persistence Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity.
     * @param merge Merge neighboring features with valley less than &lt;value&gt; * intensity.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importMsRunDataLocallyWithResponseSpec(String projectId, List<String> requestBody, String tag, Boolean alignRuns, Boolean allowMs1Only, DataSmoothing filter, Double sigma, Integer scale, Double window, Double noise, Double persistence, Double merge) throws WebClientResponseException {
        return importMsRunDataLocallyRequestCreation(projectId, requestBody, tag, alignRuns, allowMs1Only, filter, sigma, scale, window, noise, persistence, merge);
    }
    /**
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     * <p><b>200</b> - OK
     * @param projectId project-space to import into.
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param inputFiles The inputFiles parameter
     * @return ImportResult
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec importPreprocessedDataRequestCreation(String projectId, Boolean ignoreFormulas, Boolean allowMs1Only, List<File> inputFiles) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling importPreprocessedData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "ignoreFormulas", ignoreFormulas));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "allowMs1Only", allowMs1Only));
        
        if (inputFiles != null)
            formParams.addAll("inputFiles", inputFiles.stream().map(FileSystemResource::new).collect(Collectors.toList()));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "multipart/form-data"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/import/preprocessed-data-files", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     * <p><b>200</b> - OK
     * @param projectId project-space to import into.
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param inputFiles The inputFiles parameter
     * @return ImportResult
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ImportResult importPreprocessedData(String projectId, Boolean ignoreFormulas, Boolean allowMs1Only, List<File> inputFiles) throws WebClientResponseException {
        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return importPreprocessedDataRequestCreation(projectId, ignoreFormulas, allowMs1Only, inputFiles).bodyToMono(localVarReturnType).block();
    }

    /**
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     * <p><b>200</b> - OK
     * @param projectId project-space to import into.
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param inputFiles The inputFiles parameter
     * @return ResponseEntity&lt;ImportResult&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ImportResult> importPreprocessedDataWithHttpInfo(String projectId, Boolean ignoreFormulas, Boolean allowMs1Only, List<File> inputFiles) throws WebClientResponseException {
        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return importPreprocessedDataRequestCreation(projectId, ignoreFormulas, allowMs1Only, inputFiles).toEntity(localVarReturnType).block();
    }

    /**
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     * <p><b>200</b> - OK
     * @param projectId project-space to import into.
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param inputFiles The inputFiles parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importPreprocessedDataWithResponseSpec(String projectId, Boolean ignoreFormulas, Boolean allowMs1Only, List<File> inputFiles) throws WebClientResponseException {
        return importPreprocessedDataRequestCreation(projectId, ignoreFormulas, allowMs1Only, inputFiles);
    }
    /**
     * Import ms/ms data from the given format into the specified project-space as background job.
     * Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)
     * <p><b>200</b> - the import job.
     * @param projectId project-space to import into.
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param inputFiles The inputFiles parameter
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec importPreprocessedDataAsJobRequestCreation(String projectId, Boolean ignoreFormulas, Boolean allowMs1Only, List<JobOptField> optFields, List<File> inputFiles) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling importPreprocessedDataAsJob", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "ignoreFormulas", ignoreFormulas));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "allowMs1Only", allowMs1Only));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        if (inputFiles != null)
            formParams.addAll("inputFiles", inputFiles.stream().map(FileSystemResource::new).collect(Collectors.toList()));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "multipart/form-data"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/import/preprocessed-data-files-job", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Import ms/ms data from the given format into the specified project-space as background job.
     * Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)
     * <p><b>200</b> - the import job.
     * @param projectId project-space to import into.
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param inputFiles The inputFiles parameter
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job importPreprocessedDataAsJob(String projectId, Boolean ignoreFormulas, Boolean allowMs1Only, List<JobOptField> optFields, List<File> inputFiles) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importPreprocessedDataAsJobRequestCreation(projectId, ignoreFormulas, allowMs1Only, optFields, inputFiles).bodyToMono(localVarReturnType).block();
    }

    /**
     * Import ms/ms data from the given format into the specified project-space as background job.
     * Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)
     * <p><b>200</b> - the import job.
     * @param projectId project-space to import into.
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param inputFiles The inputFiles parameter
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> importPreprocessedDataAsJobWithHttpInfo(String projectId, Boolean ignoreFormulas, Boolean allowMs1Only, List<JobOptField> optFields, List<File> inputFiles) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importPreprocessedDataAsJobRequestCreation(projectId, ignoreFormulas, allowMs1Only, optFields, inputFiles).toEntity(localVarReturnType).block();
    }

    /**
     * Import ms/ms data from the given format into the specified project-space as background job.
     * Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)
     * <p><b>200</b> - the import job.
     * @param projectId project-space to import into.
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param inputFiles The inputFiles parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importPreprocessedDataAsJobWithResponseSpec(String projectId, Boolean ignoreFormulas, Boolean allowMs1Only, List<JobOptField> optFields, List<File> inputFiles) throws WebClientResponseException {
        return importPreprocessedDataAsJobRequestCreation(projectId, ignoreFormulas, allowMs1Only, optFields, inputFiles);
    }
    /**
     * Import ms/ms data from the given format into the specified project-space as background job.
     * Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)   ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.   DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;preprocessed-data-files-job&#39; instead.
     * <p><b>200</b> - the import job.
     * @param projectId project-space to import into.
     * @param requestBody The requestBody parameter
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec importPreprocessedDataAsJobLocallyRequestCreation(String projectId, List<String> requestBody, Boolean ignoreFormulas, Boolean allowMs1Only, List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling importPreprocessedDataAsJobLocally", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'requestBody' is set
        if (requestBody == null) {
            throw new WebClientResponseException("Missing the required parameter 'requestBody' when calling importPreprocessedDataAsJobLocally", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "ignoreFormulas", ignoreFormulas));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "allowMs1Only", allowMs1Only));
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
        return apiClient.invokeAPI("/api/projects/{projectId}/import/preprocessed-local-data-files-job", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Import ms/ms data from the given format into the specified project-space as background job.
     * Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)   ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.   DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;preprocessed-data-files-job&#39; instead.
     * <p><b>200</b> - the import job.
     * @param projectId project-space to import into.
     * @param requestBody The requestBody parameter
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job importPreprocessedDataAsJobLocally(String projectId, List<String> requestBody, Boolean ignoreFormulas, Boolean allowMs1Only, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importPreprocessedDataAsJobLocallyRequestCreation(projectId, requestBody, ignoreFormulas, allowMs1Only, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Import ms/ms data from the given format into the specified project-space as background job.
     * Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)   ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.   DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;preprocessed-data-files-job&#39; instead.
     * <p><b>200</b> - the import job.
     * @param projectId project-space to import into.
     * @param requestBody The requestBody parameter
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> importPreprocessedDataAsJobLocallyWithHttpInfo(String projectId, List<String> requestBody, Boolean ignoreFormulas, Boolean allowMs1Only, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importPreprocessedDataAsJobLocallyRequestCreation(projectId, requestBody, ignoreFormulas, allowMs1Only, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Import ms/ms data from the given format into the specified project-space as background job.
     * Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)   ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.   DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;preprocessed-data-files-job&#39; instead.
     * <p><b>200</b> - the import job.
     * @param projectId project-space to import into.
     * @param requestBody The requestBody parameter
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importPreprocessedDataAsJobLocallyWithResponseSpec(String projectId, List<String> requestBody, Boolean ignoreFormulas, Boolean allowMs1Only, List<JobOptField> optFields) throws WebClientResponseException {
        return importPreprocessedDataAsJobLocallyRequestCreation(projectId, requestBody, ignoreFormulas, allowMs1Only, optFields);
    }
    /**
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)   ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)   ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.   DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;preprocessed-data-files&#39; instead.
     * <p><b>200</b> - OK
     * @param projectId project-space to import into.
     * @param requestBody files to import into project
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @return ImportResult
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec importPreprocessedDataLocallyRequestCreation(String projectId, List<String> requestBody, Boolean ignoreFormulas, Boolean allowMs1Only) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling importPreprocessedDataLocally", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'requestBody' is set
        if (requestBody == null) {
            throw new WebClientResponseException("Missing the required parameter 'requestBody' when calling importPreprocessedDataLocally", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "ignoreFormulas", ignoreFormulas));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "allowMs1Only", allowMs1Only));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/import/preprocessed-local-data-files", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)   ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)   ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.   DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;preprocessed-data-files&#39; instead.
     * <p><b>200</b> - OK
     * @param projectId project-space to import into.
     * @param requestBody files to import into project
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @return ImportResult
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ImportResult importPreprocessedDataLocally(String projectId, List<String> requestBody, Boolean ignoreFormulas, Boolean allowMs1Only) throws WebClientResponseException {
        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return importPreprocessedDataLocallyRequestCreation(projectId, requestBody, ignoreFormulas, allowMs1Only).bodyToMono(localVarReturnType).block();
    }

    /**
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)   ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)   ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.   DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;preprocessed-data-files&#39; instead.
     * <p><b>200</b> - OK
     * @param projectId project-space to import into.
     * @param requestBody files to import into project
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @return ResponseEntity&lt;ImportResult&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ImportResult> importPreprocessedDataLocallyWithHttpInfo(String projectId, List<String> requestBody, Boolean ignoreFormulas, Boolean allowMs1Only) throws WebClientResponseException {
        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return importPreprocessedDataLocallyRequestCreation(projectId, requestBody, ignoreFormulas, allowMs1Only).toEntity(localVarReturnType).block();
    }

    /**
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)   ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)   ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.   DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this  API to allow for more flexible use cases. Use &#39;preprocessed-data-files&#39; instead.
     * <p><b>200</b> - OK
     * @param projectId project-space to import into.
     * @param requestBody files to import into project
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importPreprocessedDataLocallyWithResponseSpec(String projectId, List<String> requestBody, Boolean ignoreFormulas, Boolean allowMs1Only) throws WebClientResponseException {
        return importPreprocessedDataLocallyRequestCreation(projectId, requestBody, ignoreFormulas, allowMs1Only);
    }
    /**
     * Open an existing project-space and make it accessible via the given projectId.
     * Open an existing project-space and make it accessible via the given projectId.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier that shall be used to access the opened project-space. Must consist only of [a-zA-Z0-9_-].
     * @param pathToProject local file path to open the project from. If NULL, project will be loaded by it projectId from default project location.  DEPRECATED: This parameter relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
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
     * @param pathToProject local file path to open the project from. If NULL, project will be loaded by it projectId from default project location.  DEPRECATED: This parameter relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
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
     * @param pathToProject local file path to open the project from. If NULL, project will be loaded by it projectId from default project location.  DEPRECATED: This parameter relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
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
     * @param pathToProject local file path to open the project from. If NULL, project will be loaded by it projectId from default project location.  DEPRECATED: This parameter relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
     * @param optFields The optFields parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec openProjectSpaceWithResponseSpec(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        return openProjectSpaceRequestCreation(projectId, pathToProject, optFields);
    }
}
