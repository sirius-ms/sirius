package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import java.io.File;
import io.sirius.ms.sdk.model.ImportResult;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobOptField;
import io.sirius.ms.sdk.model.LcmsSubmissionParameters;
import io.sirius.ms.sdk.model.ProjectInfo;
import io.sirius.ms.sdk.model.ProjectInfoOptField;

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
     * @param compact if true, compact project storage after closing. DEPRECATED: Compacting acts on the local filesystem and will likely be removed in a later version.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec closeProjectRequestCreation(String projectId, Boolean compact) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling closeProject", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "compact", compact));
        
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
     * @param compact if true, compact project storage after closing. DEPRECATED: Compacting acts on the local filesystem and will likely be removed in a later version.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void closeProject(String projectId, Boolean compact) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        closeProjectRequestCreation(projectId, compact).bodyToMono(localVarReturnType).block();
    }

    /**
     * Close project-space and remove it from application
     * Close project-space and remove it from application. Project will NOT be deleted from disk.  &lt;p&gt;  ATTENTION: This will cancel and remove all jobs running on this Project before closing it.  If there are many jobs, this might take some time.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier of the  project-space to be closed.
     * @param compact if true, compact project storage after closing. DEPRECATED: Compacting acts on the local filesystem and will likely be removed in a later version.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> closeProjectWithHttpInfo(String projectId, Boolean compact) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return closeProjectRequestCreation(projectId, compact).toEntity(localVarReturnType).block();
    }

    /**
     * Close project-space and remove it from application
     * Close project-space and remove it from application. Project will NOT be deleted from disk.  &lt;p&gt;  ATTENTION: This will cancel and remove all jobs running on this Project before closing it.  If there are many jobs, this might take some time.
     * <p><b>200</b> - OK
     * @param projectId unique name/identifier of the  project-space to be closed.
     * @param compact if true, compact project storage after closing. DEPRECATED: Compacting acts on the local filesystem and will likely be removed in a later version.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec closeProjectWithResponseSpec(String projectId, Boolean compact) throws WebClientResponseException {
        return closeProjectRequestCreation(projectId, compact);
    }
    /**
     * DEPRECATED: this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * [DEPRECATED] Move an existing (opened) project-space to another location.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * <p><b>200</b> - ProjectInfo of the newly created project if opened (copyProjectId !&#x3D; null) or the project info of  the source project otherwise
     * @param projectId unique name/identifier of the project-space that shall be copied.
     * @param pathToCopiedProject target location where the source project will be copied to.
     * @param copyProjectId optional id/mame of the newly created project (copy). If given the project will be opened.
     * @param optFields The optFields parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec copyProjectRequestCreation(String projectId, String pathToCopiedProject, String copyProjectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling copyProject", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'pathToCopiedProject' is set
        if (pathToCopiedProject == null) {
            throw new WebClientResponseException("Missing the required parameter 'pathToCopiedProject' when calling copyProject", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
     * DEPRECATED: this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * [DEPRECATED] Move an existing (opened) project-space to another location.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * <p><b>200</b> - ProjectInfo of the newly created project if opened (copyProjectId !&#x3D; null) or the project info of  the source project otherwise
     * @param projectId unique name/identifier of the project-space that shall be copied.
     * @param pathToCopiedProject target location where the source project will be copied to.
     * @param copyProjectId optional id/mame of the newly created project (copy). If given the project will be opened.
     * @param optFields The optFields parameter
     * @return ProjectInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ProjectInfo copyProject(String projectId, String pathToCopiedProject, String copyProjectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return copyProjectRequestCreation(projectId, pathToCopiedProject, copyProjectId, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * DEPRECATED: this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * [DEPRECATED] Move an existing (opened) project-space to another location.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * <p><b>200</b> - ProjectInfo of the newly created project if opened (copyProjectId !&#x3D; null) or the project info of  the source project otherwise
     * @param projectId unique name/identifier of the project-space that shall be copied.
     * @param pathToCopiedProject target location where the source project will be copied to.
     * @param copyProjectId optional id/mame of the newly created project (copy). If given the project will be opened.
     * @param optFields The optFields parameter
     * @return ResponseEntity&lt;ProjectInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ProjectInfo> copyProjectWithHttpInfo(String projectId, String pathToCopiedProject, String copyProjectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return copyProjectRequestCreation(projectId, pathToCopiedProject, copyProjectId, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * DEPRECATED: this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * [DEPRECATED] Move an existing (opened) project-space to another location.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * <p><b>200</b> - ProjectInfo of the newly created project if opened (copyProjectId !&#x3D; null) or the project info of  the source project otherwise
     * @param projectId unique name/identifier of the project-space that shall be copied.
     * @param pathToCopiedProject target location where the source project will be copied to.
     * @param copyProjectId optional id/mame of the newly created project (copy). If given the project will be opened.
     * @param optFields The optFields parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec copyProjectWithResponseSpec(String projectId, String pathToCopiedProject, String copyProjectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        return copyProjectRequestCreation(projectId, pathToCopiedProject, copyProjectId, optFields);
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
    private ResponseSpec createProjectRequestCreation(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling createProject", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
    public ProjectInfo createProject(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return createProjectRequestCreation(projectId, pathToProject, optFields).bodyToMono(localVarReturnType).block();
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
    public ResponseEntity<ProjectInfo> createProjectWithHttpInfo(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return createProjectRequestCreation(projectId, pathToProject, optFields).toEntity(localVarReturnType).block();
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
    public ResponseSpec createProjectWithResponseSpec(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        return createProjectRequestCreation(projectId, pathToProject, optFields);
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
            "application/csv", "application/CSV"
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
            "application/csv", "application/CSV"
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
            "application/csv", "application/CSV"
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
    private ResponseSpec getProjectRequestCreation(String projectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getProject", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
    public ProjectInfo getProject(String projectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return getProjectRequestCreation(projectId, optFields).bodyToMono(localVarReturnType).block();
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
    public ResponseEntity<ProjectInfo> getProjectWithHttpInfo(String projectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return getProjectRequestCreation(projectId, optFields).toEntity(localVarReturnType).block();
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
    public ResponseSpec getProjectWithResponseSpec(String projectId, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        return getProjectRequestCreation(projectId, optFields);
    }
    /**
     * List opened project spaces.
     * List opened project spaces.
     * <p><b>200</b> - OK
     * @return List&lt;ProjectInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getProjectsRequestCreation() throws WebClientResponseException {
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
    public List<ProjectInfo> getProjects() throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return getProjectsRequestCreation().bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List opened project spaces.
     * List opened project spaces.
     * <p><b>200</b> - OK
     * @return ResponseEntity&lt;List&lt;ProjectInfo&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<ProjectInfo>> getProjectsWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return getProjectsRequestCreation().toEntityList(localVarReturnType).block();
    }

    /**
     * List opened project spaces.
     * List opened project spaces.
     * <p><b>200</b> - OK
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getProjectsWithResponseSpec() throws WebClientResponseException {
        return getProjectsRequestCreation();
    }
    /**
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     * <p><b>200</b> - OK
     * @param projectId Project-space to import into.
     * @param inputFiles Files to import into project.
     * @param parameters The parameters parameter
     * @return ImportResult
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec importMsRunDataRequestCreation(String projectId, List<File> inputFiles, LcmsSubmissionParameters parameters) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling importMsRunData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'inputFiles' is set
        if (inputFiles == null) {
            throw new WebClientResponseException("Missing the required parameter 'inputFiles' when calling importMsRunData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'parameters' is set
        if (parameters == null) {
            throw new WebClientResponseException("Missing the required parameter 'parameters' when calling importMsRunData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        if (inputFiles != null)
            formParams.addAll("inputFiles", inputFiles.stream().map(FileSystemResource::new).collect(Collectors.toList()));
        if (parameters != null)
            formParams.add("parameters", parameters);

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
     * @param inputFiles Files to import into project.
     * @param parameters The parameters parameter
     * @return ImportResult
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ImportResult importMsRunData(String projectId, List<File> inputFiles, LcmsSubmissionParameters parameters) throws WebClientResponseException {
        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return importMsRunDataRequestCreation(projectId, inputFiles, parameters).bodyToMono(localVarReturnType).block();
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     * <p><b>200</b> - OK
     * @param projectId Project-space to import into.
     * @param inputFiles Files to import into project.
     * @param parameters The parameters parameter
     * @return ResponseEntity&lt;ImportResult&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ImportResult> importMsRunDataWithHttpInfo(String projectId, List<File> inputFiles, LcmsSubmissionParameters parameters) throws WebClientResponseException {
        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return importMsRunDataRequestCreation(projectId, inputFiles, parameters).toEntity(localVarReturnType).block();
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     * <p><b>200</b> - OK
     * @param projectId Project-space to import into.
     * @param inputFiles Files to import into project.
     * @param parameters The parameters parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importMsRunDataWithResponseSpec(String projectId, List<File> inputFiles, LcmsSubmissionParameters parameters) throws WebClientResponseException {
        return importMsRunDataRequestCreation(projectId, inputFiles, parameters);
    }
    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job.
     * Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param inputFiles Files to import into project.
     * @param parameters The parameters parameter
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec importMsRunDataAsJobRequestCreation(String projectId, List<File> inputFiles, LcmsSubmissionParameters parameters, List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling importMsRunDataAsJob", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'inputFiles' is set
        if (inputFiles == null) {
            throw new WebClientResponseException("Missing the required parameter 'inputFiles' when calling importMsRunDataAsJob", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'parameters' is set
        if (parameters == null) {
            throw new WebClientResponseException("Missing the required parameter 'parameters' when calling importMsRunDataAsJob", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        if (inputFiles != null)
            formParams.addAll("inputFiles", inputFiles.stream().map(FileSystemResource::new).collect(Collectors.toList()));
        if (parameters != null)
            formParams.add("parameters", parameters);

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
        return apiClient.invokeAPI("/api/projects/{projectId}/import/ms-data-files-job", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job.
     * Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param inputFiles Files to import into project.
     * @param parameters The parameters parameter
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job importMsRunDataAsJob(String projectId, List<File> inputFiles, LcmsSubmissionParameters parameters, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importMsRunDataAsJobRequestCreation(projectId, inputFiles, parameters, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job.
     * Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param inputFiles Files to import into project.
     * @param parameters The parameters parameter
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> importMsRunDataAsJobWithHttpInfo(String projectId, List<File> inputFiles, LcmsSubmissionParameters parameters, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importMsRunDataAsJobRequestCreation(projectId, inputFiles, parameters, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job.
     * Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param inputFiles Files to import into project.
     * @param parameters The parameters parameter
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importMsRunDataAsJobWithResponseSpec(String projectId, List<File> inputFiles, LcmsSubmissionParameters parameters, List<JobOptField> optFields) throws WebClientResponseException {
        return importMsRunDataAsJobRequestCreation(projectId, inputFiles, parameters, optFields);
    }
    /**
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project as background job
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;ms-data-files-job&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param parameters Parameters for feature alignment and feature finding.
     * @param requestBody Local files to import into project.
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec importMsRunDataAsJobLocallyRequestCreation(String projectId, LcmsSubmissionParameters parameters, List<String> requestBody, List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling importMsRunDataAsJobLocally", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'parameters' is set
        if (parameters == null) {
            throw new WebClientResponseException("Missing the required parameter 'parameters' when calling importMsRunDataAsJobLocally", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "alignLCMSRuns", parameters.isAlignLCMSRuns()));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "noiseIntensity", parameters.getNoiseIntensity()));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "traceMaxMassDeviation", parameters.getTraceMaxMassDeviation()));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "alignMaxMassDeviation", parameters.getAlignMaxMassDeviation()));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "alignMaxRetentionTimeDeviation", parameters.getAlignMaxRetentionTimeDeviation()));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "minSNR", parameters.getMinSNR()));
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
        return apiClient.invokeAPI("/api/projects/{projectId}/import/ms-data-local-files-job", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project as background job
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;ms-data-files-job&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param parameters Parameters for feature alignment and feature finding.
     * @param requestBody Local files to import into project.
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job importMsRunDataAsJobLocally(String projectId, LcmsSubmissionParameters parameters, List<String> requestBody, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importMsRunDataAsJobLocallyRequestCreation(projectId, parameters, requestBody, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project as background job
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;ms-data-files-job&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param parameters Parameters for feature alignment and feature finding.
     * @param requestBody Local files to import into project.
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> importMsRunDataAsJobLocallyWithHttpInfo(String projectId, LcmsSubmissionParameters parameters, List<String> requestBody, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importMsRunDataAsJobLocallyRequestCreation(projectId, parameters, requestBody, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project as background job
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;ms-data-files-job&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * <p><b>200</b> - the import job.
     * @param projectId Project-space to import into.
     * @param parameters Parameters for feature alignment and feature finding.
     * @param requestBody Local files to import into project.
     * @param optFields Set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importMsRunDataAsJobLocallyWithResponseSpec(String projectId, LcmsSubmissionParameters parameters, List<String> requestBody, List<JobOptField> optFields) throws WebClientResponseException {
        return importMsRunDataAsJobLocallyRequestCreation(projectId, parameters, requestBody, optFields);
    }
    /**
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;ms-data-files&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * <p><b>200</b> - OK
     * @param projectId Project to import into.
     * @param parameters Parameters for feature alignment and feature finding.
     * @param requestBody Local files to import into project.
     * @return ImportResult
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec importMsRunDataLocallyRequestCreation(String projectId, LcmsSubmissionParameters parameters, List<String> requestBody) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling importMsRunDataLocally", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'parameters' is set
        if (parameters == null) {
            throw new WebClientResponseException("Missing the required parameter 'parameters' when calling importMsRunDataLocally", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "alignLCMSRuns", parameters.isAlignLCMSRuns()));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "noiseIntensity", parameters.getNoiseIntensity()));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "traceMaxMassDeviation", parameters.getTraceMaxMassDeviation()));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "alignMaxMassDeviation", parameters.getAlignMaxMassDeviation()));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "alignMaxRetentionTimeDeviation", parameters.getAlignMaxRetentionTimeDeviation()));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "minSNR", parameters.getMinSNR()));
        
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
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;ms-data-files&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * <p><b>200</b> - OK
     * @param projectId Project to import into.
     * @param parameters Parameters for feature alignment and feature finding.
     * @param requestBody Local files to import into project.
     * @return ImportResult
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ImportResult importMsRunDataLocally(String projectId, LcmsSubmissionParameters parameters, List<String> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return importMsRunDataLocallyRequestCreation(projectId, parameters, requestBody).bodyToMono(localVarReturnType).block();
    }

    /**
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;ms-data-files&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * <p><b>200</b> - OK
     * @param projectId Project to import into.
     * @param parameters Parameters for feature alignment and feature finding.
     * @param requestBody Local files to import into project.
     * @return ResponseEntity&lt;ImportResult&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ImportResult> importMsRunDataLocallyWithHttpInfo(String projectId, LcmsSubmissionParameters parameters, List<String> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return importMsRunDataLocallyRequestCreation(projectId, parameters, requestBody).toEntity(localVarReturnType).block();
    }

    /**
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  
     * [DEPRECATED] Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;ms-data-files&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * <p><b>200</b> - OK
     * @param projectId Project to import into.
     * @param parameters Parameters for feature alignment and feature finding.
     * @param requestBody Local files to import into project.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importMsRunDataLocallyWithResponseSpec(String projectId, LcmsSubmissionParameters parameters, List<String> requestBody) throws WebClientResponseException {
        return importMsRunDataLocallyRequestCreation(projectId, parameters, requestBody);
    }
    /**
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     * <p><b>200</b> - OK
     * @param projectId project-space to import into.
     * @param inputFiles files to import into project
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @return ImportResult
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec importPreprocessedDataRequestCreation(String projectId, List<File> inputFiles, Boolean ignoreFormulas, Boolean allowMs1Only) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling importPreprocessedData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'inputFiles' is set
        if (inputFiles == null) {
            throw new WebClientResponseException("Missing the required parameter 'inputFiles' when calling importPreprocessedData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
     * @param inputFiles files to import into project
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @return ImportResult
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ImportResult importPreprocessedData(String projectId, List<File> inputFiles, Boolean ignoreFormulas, Boolean allowMs1Only) throws WebClientResponseException {
        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return importPreprocessedDataRequestCreation(projectId, inputFiles, ignoreFormulas, allowMs1Only).bodyToMono(localVarReturnType).block();
    }

    /**
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     * <p><b>200</b> - OK
     * @param projectId project-space to import into.
     * @param inputFiles files to import into project
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @return ResponseEntity&lt;ImportResult&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ImportResult> importPreprocessedDataWithHttpInfo(String projectId, List<File> inputFiles, Boolean ignoreFormulas, Boolean allowMs1Only) throws WebClientResponseException {
        ParameterizedTypeReference<ImportResult> localVarReturnType = new ParameterizedTypeReference<ImportResult>() {};
        return importPreprocessedDataRequestCreation(projectId, inputFiles, ignoreFormulas, allowMs1Only).toEntity(localVarReturnType).block();
    }

    /**
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     * <p><b>200</b> - OK
     * @param projectId project-space to import into.
     * @param inputFiles files to import into project
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importPreprocessedDataWithResponseSpec(String projectId, List<File> inputFiles, Boolean ignoreFormulas, Boolean allowMs1Only) throws WebClientResponseException {
        return importPreprocessedDataRequestCreation(projectId, inputFiles, ignoreFormulas, allowMs1Only);
    }
    /**
     * Import ms/ms data from the given format into the specified project-space as background job.
     * Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)
     * <p><b>200</b> - the import job.
     * @param projectId project-space to import into.
     * @param inputFiles The inputFiles parameter
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec importPreprocessedDataAsJobRequestCreation(String projectId, List<File> inputFiles, Boolean ignoreFormulas, Boolean allowMs1Only, List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling importPreprocessedDataAsJob", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'inputFiles' is set
        if (inputFiles == null) {
            throw new WebClientResponseException("Missing the required parameter 'inputFiles' when calling importPreprocessedDataAsJob", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
     * @param inputFiles The inputFiles parameter
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job importPreprocessedDataAsJob(String projectId, List<File> inputFiles, Boolean ignoreFormulas, Boolean allowMs1Only, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importPreprocessedDataAsJobRequestCreation(projectId, inputFiles, ignoreFormulas, allowMs1Only, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Import ms/ms data from the given format into the specified project-space as background job.
     * Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)
     * <p><b>200</b> - the import job.
     * @param projectId project-space to import into.
     * @param inputFiles The inputFiles parameter
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> importPreprocessedDataAsJobWithHttpInfo(String projectId, List<File> inputFiles, Boolean ignoreFormulas, Boolean allowMs1Only, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importPreprocessedDataAsJobRequestCreation(projectId, inputFiles, ignoreFormulas, allowMs1Only, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Import ms/ms data from the given format into the specified project-space as background job.
     * Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)
     * <p><b>200</b> - the import job.
     * @param projectId project-space to import into.
     * @param inputFiles The inputFiles parameter
     * @param ignoreFormulas The ignoreFormulas parameter
     * @param allowMs1Only The allowMs1Only parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importPreprocessedDataAsJobWithResponseSpec(String projectId, List<File> inputFiles, Boolean ignoreFormulas, Boolean allowMs1Only, List<JobOptField> optFields) throws WebClientResponseException {
        return importPreprocessedDataAsJobRequestCreation(projectId, inputFiles, ignoreFormulas, allowMs1Only, optFields);
    }
    /**
     * [DEPRECATED] Import ms/ms data from the given format into the specified project-space as background job
     * [DEPRECATED] Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;preprocessed-data-files-job&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
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
     * [DEPRECATED] Import ms/ms data from the given format into the specified project-space as background job
     * [DEPRECATED] Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;preprocessed-data-files-job&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
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
     * [DEPRECATED] Import ms/ms data from the given format into the specified project-space as background job
     * [DEPRECATED] Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;preprocessed-data-files-job&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
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
     * [DEPRECATED] Import ms/ms data from the given format into the specified project-space as background job
     * [DEPRECATED] Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;preprocessed-data-files-job&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
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
     * [DEPRECATED] Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)  
     * [DEPRECATED] Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;preprocessed-data-files&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
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
     * [DEPRECATED] Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)  
     * [DEPRECATED] Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;preprocessed-data-files&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
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
     * [DEPRECATED] Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)  
     * [DEPRECATED] Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;preprocessed-data-files&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
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
     * [DEPRECATED] Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)  
     * [DEPRECATED] Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)  &lt;p&gt;  ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,  not on the system where the client SDK is running.  Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)  are running on the same host.  &lt;p&gt;  API to allow for more flexible use cases. Use &#39;preprocessed-data-files&#39; instead.  &lt;p&gt;  [DEPRECATED] this endpoint is based on local file paths and will likely be removed in future versions of this API.
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
    private ResponseSpec openProjectRequestCreation(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling openProject", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
    public ProjectInfo openProject(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return openProjectRequestCreation(projectId, pathToProject, optFields).bodyToMono(localVarReturnType).block();
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
    public ResponseEntity<ProjectInfo> openProjectWithHttpInfo(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<ProjectInfo> localVarReturnType = new ParameterizedTypeReference<ProjectInfo>() {};
        return openProjectRequestCreation(projectId, pathToProject, optFields).toEntity(localVarReturnType).block();
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
    public ResponseSpec openProjectWithResponseSpec(String projectId, String pathToProject, List<ProjectInfoOptField> optFields) throws WebClientResponseException {
        return openProjectRequestCreation(projectId, pathToProject, optFields);
    }
}
