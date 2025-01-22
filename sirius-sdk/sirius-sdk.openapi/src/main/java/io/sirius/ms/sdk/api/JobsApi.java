package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.CommandSubmission;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobOptField;
import io.sirius.ms.sdk.model.JobSubmission;
import io.sirius.ms.sdk.model.PagedModelJob;
import io.sirius.ms.sdk.model.StoredJobSubmission;

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
public class JobsApi {
    private ApiClient apiClient;

    public JobsApi() {
        this(new ApiClient());
    }

    @Autowired
    public JobsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Delete job.
     * Delete job. Specify how to behave for running jobs.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to delete job from
     * @param jobId of the job to be deleted
     * @param cancelIfRunning If true, job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished.
     * @param awaitDeletion If true, request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteJobRequestCreation(String projectId, String jobId, Boolean cancelIfRunning, Boolean awaitDeletion) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteJob", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'jobId' is set
        if (jobId == null) {
            throw new WebClientResponseException("Missing the required parameter 'jobId' when calling deleteJob", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("jobId", jobId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "cancelIfRunning", cancelIfRunning));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "awaitDeletion", awaitDeletion));
        
        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/jobs/{jobId}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete job.
     * Delete job. Specify how to behave for running jobs.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to delete job from
     * @param jobId of the job to be deleted
     * @param cancelIfRunning If true, job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished.
     * @param awaitDeletion If true, request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteJob(String projectId, String jobId, Boolean cancelIfRunning, Boolean awaitDeletion) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteJobRequestCreation(projectId, jobId, cancelIfRunning, awaitDeletion).bodyToMono(localVarReturnType).block();
    }

    /**
     * Delete job.
     * Delete job. Specify how to behave for running jobs.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to delete job from
     * @param jobId of the job to be deleted
     * @param cancelIfRunning If true, job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished.
     * @param awaitDeletion If true, request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteJobWithHttpInfo(String projectId, String jobId, Boolean cancelIfRunning, Boolean awaitDeletion) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteJobRequestCreation(projectId, jobId, cancelIfRunning, awaitDeletion).toEntity(localVarReturnType).block();
    }

    /**
     * Delete job.
     * Delete job. Specify how to behave for running jobs.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to delete job from
     * @param jobId of the job to be deleted
     * @param cancelIfRunning If true, job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished.
     * @param awaitDeletion If true, request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteJobWithResponseSpec(String projectId, String jobId, Boolean cancelIfRunning, Boolean awaitDeletion) throws WebClientResponseException {
        return deleteJobRequestCreation(projectId, jobId, cancelIfRunning, awaitDeletion);
    }
    /**
     * Delete job configuration with given name.
     * Delete job configuration with given name.
     * <p><b>202</b> - Accepted
     * @param name name of the job-config to delete
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteJobConfigRequestCreation(String name) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling deleteJobConfig", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("name", name);

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
        return apiClient.invokeAPI("/api/job-configs/{name}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete job configuration with given name.
     * Delete job configuration with given name.
     * <p><b>202</b> - Accepted
     * @param name name of the job-config to delete
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteJobConfig(String name) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteJobConfigRequestCreation(name).bodyToMono(localVarReturnType).block();
    }

    /**
     * Delete job configuration with given name.
     * Delete job configuration with given name.
     * <p><b>202</b> - Accepted
     * @param name name of the job-config to delete
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteJobConfigWithHttpInfo(String name) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteJobConfigRequestCreation(name).toEntity(localVarReturnType).block();
    }

    /**
     * Delete job configuration with given name.
     * Delete job configuration with given name.
     * <p><b>202</b> - Accepted
     * @param name name of the job-config to delete
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteJobConfigWithResponseSpec(String name) throws WebClientResponseException {
        return deleteJobConfigRequestCreation(name);
    }
    /**
     * * Delete ALL jobs.
     * * Delete ALL jobs. Specify how to behave for running jobs.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to delete jobs from
     * @param cancelIfRunning If true, job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished.
     * @param awaitDeletion If true, request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteJobsRequestCreation(String projectId, Boolean cancelIfRunning, Boolean awaitDeletion) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteJobs", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "cancelIfRunning", cancelIfRunning));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "awaitDeletion", awaitDeletion));
        
        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/jobs", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * * Delete ALL jobs.
     * * Delete ALL jobs. Specify how to behave for running jobs.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to delete jobs from
     * @param cancelIfRunning If true, job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished.
     * @param awaitDeletion If true, request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteJobs(String projectId, Boolean cancelIfRunning, Boolean awaitDeletion) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteJobsRequestCreation(projectId, cancelIfRunning, awaitDeletion).bodyToMono(localVarReturnType).block();
    }

    /**
     * * Delete ALL jobs.
     * * Delete ALL jobs. Specify how to behave for running jobs.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to delete jobs from
     * @param cancelIfRunning If true, job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished.
     * @param awaitDeletion If true, request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteJobsWithHttpInfo(String projectId, Boolean cancelIfRunning, Boolean awaitDeletion) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteJobsRequestCreation(projectId, cancelIfRunning, awaitDeletion).toEntity(localVarReturnType).block();
    }

    /**
     * * Delete ALL jobs.
     * * Delete ALL jobs. Specify how to behave for running jobs.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to delete jobs from
     * @param cancelIfRunning If true, job will be canceled if it is not finished. Otherwise,                         deletion will fail for running jobs or request will block until job has finished.
     * @param awaitDeletion If true, request will block until deletion succeeded or failed.                         If the job is still running the request will wait until the job has finished.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteJobsWithResponseSpec(String projectId, Boolean cancelIfRunning, Boolean awaitDeletion) throws WebClientResponseException {
        return deleteJobsRequestCreation(projectId, cancelIfRunning, awaitDeletion);
    }
    /**
     * Request default job configuration
     * Request default job configuration
     * <p><b>200</b> - {@link JobSubmission JobSubmission} with all parameters set to default values.
     * @param includeConfigMap if true, generic configmap with-defaults will be included
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters
     * @param includeCustomDbsForStructureSearch if true, default database selection of structure db search contains also all available custom DB.
     * @return JobSubmission
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getDefaultJobConfigRequestCreation(Boolean includeConfigMap, Boolean moveParametersToConfigMap, Boolean includeCustomDbsForStructureSearch) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeConfigMap", includeConfigMap));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "moveParametersToConfigMap", moveParametersToConfigMap));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeCustomDbsForStructureSearch", includeCustomDbsForStructureSearch));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<JobSubmission> localVarReturnType = new ParameterizedTypeReference<JobSubmission>() {};
        return apiClient.invokeAPI("/api/default-job-config", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Request default job configuration
     * Request default job configuration
     * <p><b>200</b> - {@link JobSubmission JobSubmission} with all parameters set to default values.
     * @param includeConfigMap if true, generic configmap with-defaults will be included
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters
     * @param includeCustomDbsForStructureSearch if true, default database selection of structure db search contains also all available custom DB.
     * @return JobSubmission
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public JobSubmission getDefaultJobConfig(Boolean includeConfigMap, Boolean moveParametersToConfigMap, Boolean includeCustomDbsForStructureSearch) throws WebClientResponseException {
        ParameterizedTypeReference<JobSubmission> localVarReturnType = new ParameterizedTypeReference<JobSubmission>() {};
        return getDefaultJobConfigRequestCreation(includeConfigMap, moveParametersToConfigMap, includeCustomDbsForStructureSearch).bodyToMono(localVarReturnType).block();
    }

    /**
     * Request default job configuration
     * Request default job configuration
     * <p><b>200</b> - {@link JobSubmission JobSubmission} with all parameters set to default values.
     * @param includeConfigMap if true, generic configmap with-defaults will be included
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters
     * @param includeCustomDbsForStructureSearch if true, default database selection of structure db search contains also all available custom DB.
     * @return ResponseEntity&lt;JobSubmission&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<JobSubmission> getDefaultJobConfigWithHttpInfo(Boolean includeConfigMap, Boolean moveParametersToConfigMap, Boolean includeCustomDbsForStructureSearch) throws WebClientResponseException {
        ParameterizedTypeReference<JobSubmission> localVarReturnType = new ParameterizedTypeReference<JobSubmission>() {};
        return getDefaultJobConfigRequestCreation(includeConfigMap, moveParametersToConfigMap, includeCustomDbsForStructureSearch).toEntity(localVarReturnType).block();
    }

    /**
     * Request default job configuration
     * Request default job configuration
     * <p><b>200</b> - {@link JobSubmission JobSubmission} with all parameters set to default values.
     * @param includeConfigMap if true, generic configmap with-defaults will be included
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters
     * @param includeCustomDbsForStructureSearch if true, default database selection of structure db search contains also all available custom DB.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getDefaultJobConfigWithResponseSpec(Boolean includeConfigMap, Boolean moveParametersToConfigMap, Boolean includeCustomDbsForStructureSearch) throws WebClientResponseException {
        return getDefaultJobConfigRequestCreation(includeConfigMap, moveParametersToConfigMap, includeCustomDbsForStructureSearch);
    }
    /**
     * Get job information and its current state and progress (if available).
     * Get job information and its current state and progress (if available).
     * <p><b>200</b> - OK
     * @param projectId project-space to run jobs on
     * @param jobId of the job to be returned
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getJobRequestCreation(String projectId, String jobId, List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getJob", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'jobId' is set
        if (jobId == null) {
            throw new WebClientResponseException("Missing the required parameter 'jobId' when calling getJob", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("jobId", jobId);

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

        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/jobs/{jobId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get job information and its current state and progress (if available).
     * Get job information and its current state and progress (if available).
     * <p><b>200</b> - OK
     * @param projectId project-space to run jobs on
     * @param jobId of the job to be returned
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job getJob(String projectId, String jobId, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return getJobRequestCreation(projectId, jobId, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get job information and its current state and progress (if available).
     * Get job information and its current state and progress (if available).
     * <p><b>200</b> - OK
     * @param projectId project-space to run jobs on
     * @param jobId of the job to be returned
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> getJobWithHttpInfo(String projectId, String jobId, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return getJobRequestCreation(projectId, jobId, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Get job information and its current state and progress (if available).
     * Get job information and its current state and progress (if available).
     * <p><b>200</b> - OK
     * @param projectId project-space to run jobs on
     * @param jobId of the job to be returned
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getJobWithResponseSpec(String projectId, String jobId, List<JobOptField> optFields) throws WebClientResponseException {
        return getJobRequestCreation(projectId, jobId, optFields);
    }
    /**
     * Request job configuration with given name.
     * Request job configuration with given name.
     * <p><b>200</b> - {@link JobSubmission JobSubmission} for given name.
     * @param name name of the job-config to return
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters
     * @return StoredJobSubmission
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getJobConfigRequestCreation(String name, Boolean moveParametersToConfigMap) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling getJobConfig", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("name", name);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "moveParametersToConfigMap", moveParametersToConfigMap));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<StoredJobSubmission> localVarReturnType = new ParameterizedTypeReference<StoredJobSubmission>() {};
        return apiClient.invokeAPI("/api/job-configs/{name}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Request job configuration with given name.
     * Request job configuration with given name.
     * <p><b>200</b> - {@link JobSubmission JobSubmission} for given name.
     * @param name name of the job-config to return
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters
     * @return StoredJobSubmission
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public StoredJobSubmission getJobConfig(String name, Boolean moveParametersToConfigMap) throws WebClientResponseException {
        ParameterizedTypeReference<StoredJobSubmission> localVarReturnType = new ParameterizedTypeReference<StoredJobSubmission>() {};
        return getJobConfigRequestCreation(name, moveParametersToConfigMap).bodyToMono(localVarReturnType).block();
    }

    /**
     * Request job configuration with given name.
     * Request job configuration with given name.
     * <p><b>200</b> - {@link JobSubmission JobSubmission} for given name.
     * @param name name of the job-config to return
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters
     * @return ResponseEntity&lt;StoredJobSubmission&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<StoredJobSubmission> getJobConfigWithHttpInfo(String name, Boolean moveParametersToConfigMap) throws WebClientResponseException {
        ParameterizedTypeReference<StoredJobSubmission> localVarReturnType = new ParameterizedTypeReference<StoredJobSubmission>() {};
        return getJobConfigRequestCreation(name, moveParametersToConfigMap).toEntity(localVarReturnType).block();
    }

    /**
     * Request job configuration with given name.
     * Request job configuration with given name.
     * <p><b>200</b> - {@link JobSubmission JobSubmission} for given name.
     * @param name name of the job-config to return
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getJobConfigWithResponseSpec(String name, Boolean moveParametersToConfigMap) throws WebClientResponseException {
        return getJobConfigRequestCreation(name, moveParametersToConfigMap);
    }
    /**
     * DEPRECATED: use /job-configs to get all configs with names.
     * Get all (non-default) job configuration names
     * <p><b>200</b> - OK
     * @return List&lt;String&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec getJobConfigNamesRequestCreation() throws WebClientResponseException {
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

        ParameterizedTypeReference<List<String>> localVarReturnType = new ParameterizedTypeReference<List<String>>() {};
        return apiClient.invokeAPI("/api/job-config-names", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * DEPRECATED: use /job-configs to get all configs with names.
     * Get all (non-default) job configuration names
     * <p><b>200</b> - OK
     * @return List&lt;String&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<String> getJobConfigNames() throws WebClientResponseException {
        ParameterizedTypeReference<List<String>> localVarReturnType = new ParameterizedTypeReference<List<String>>() {};
        return getJobConfigNamesRequestCreation().bodyToMono(localVarReturnType).block();
    }

    /**
     * DEPRECATED: use /job-configs to get all configs with names.
     * Get all (non-default) job configuration names
     * <p><b>200</b> - OK
     * @return ResponseEntity&lt;List&lt;String&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<String>> getJobConfigNamesWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<List<String>> localVarReturnType = new ParameterizedTypeReference<List<String>>() {};
        return getJobConfigNamesRequestCreation().toEntity(localVarReturnType).block();
    }

    /**
     * DEPRECATED: use /job-configs to get all configs with names.
     * Get all (non-default) job configuration names
     * <p><b>200</b> - OK
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getJobConfigNamesWithResponseSpec() throws WebClientResponseException {
        return getJobConfigNamesRequestCreation();
    }
    /**
     * Request all available job configurations
     * Request all available job configurations
     * <p><b>200</b> - list of available {@link JobSubmission JobSubmission}s
     * @return List&lt;StoredJobSubmission&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getJobConfigsRequestCreation() throws WebClientResponseException {
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

        ParameterizedTypeReference<StoredJobSubmission> localVarReturnType = new ParameterizedTypeReference<StoredJobSubmission>() {};
        return apiClient.invokeAPI("/api/job-configs", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Request all available job configurations
     * Request all available job configurations
     * <p><b>200</b> - list of available {@link JobSubmission JobSubmission}s
     * @return List&lt;StoredJobSubmission&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<StoredJobSubmission> getJobConfigs() throws WebClientResponseException {
        ParameterizedTypeReference<StoredJobSubmission> localVarReturnType = new ParameterizedTypeReference<StoredJobSubmission>() {};
        return getJobConfigsRequestCreation().bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Request all available job configurations
     * Request all available job configurations
     * <p><b>200</b> - list of available {@link JobSubmission JobSubmission}s
     * @return ResponseEntity&lt;List&lt;StoredJobSubmission&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<StoredJobSubmission>> getJobConfigsWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<StoredJobSubmission> localVarReturnType = new ParameterizedTypeReference<StoredJobSubmission>() {};
        return getJobConfigsRequestCreation().toEntityList(localVarReturnType).block();
    }

    /**
     * Request all available job configurations
     * Request all available job configurations
     * <p><b>200</b> - list of available {@link JobSubmission JobSubmission}s
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getJobConfigsWithResponseSpec() throws WebClientResponseException {
        return getJobConfigsRequestCreation();
    }
    /**
     * Get List of all available jobs with information such as current state and progress (if available).
     * Get List of all available jobs with information such as current state and progress (if available).
     * <p><b>200</b> - OK
     * @param projectId project-space to run jobs on
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getJobsRequestCreation(String projectId, List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getJobs", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/jobs", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get List of all available jobs with information such as current state and progress (if available).
     * Get List of all available jobs with information such as current state and progress (if available).
     * <p><b>200</b> - OK
     * @param projectId project-space to run jobs on
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Job> getJobs(String projectId, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return getJobsRequestCreation(projectId, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Get List of all available jobs with information such as current state and progress (if available).
     * Get List of all available jobs with information such as current state and progress (if available).
     * <p><b>200</b> - OK
     * @param projectId project-space to run jobs on
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;List&lt;Job&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Job>> getJobsWithHttpInfo(String projectId, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return getJobsRequestCreation(projectId, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * Get List of all available jobs with information such as current state and progress (if available).
     * Get List of all available jobs with information such as current state and progress (if available).
     * <p><b>200</b> - OK
     * @param projectId project-space to run jobs on
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getJobsWithResponseSpec(String projectId, List<JobOptField> optFields) throws WebClientResponseException {
        return getJobsRequestCreation(projectId, optFields);
    }
    /**
     * Get Page of jobs with information such as current state and progress (if available).
     * Get Page of jobs with information such as current state and progress (if available).
     * <p><b>200</b> - OK
     * @param projectId project-space to run jobs on
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelJob
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getJobsPagedRequestCreation(String projectId, Integer page, Integer size, List<String> sort, List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getJobsPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelJob> localVarReturnType = new ParameterizedTypeReference<PagedModelJob>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/jobs/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get Page of jobs with information such as current state and progress (if available).
     * Get Page of jobs with information such as current state and progress (if available).
     * <p><b>200</b> - OK
     * @param projectId project-space to run jobs on
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelJob
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelJob getJobsPaged(String projectId, Integer page, Integer size, List<String> sort, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelJob> localVarReturnType = new ParameterizedTypeReference<PagedModelJob>() {};
        return getJobsPagedRequestCreation(projectId, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get Page of jobs with information such as current state and progress (if available).
     * Get Page of jobs with information such as current state and progress (if available).
     * <p><b>200</b> - OK
     * @param projectId project-space to run jobs on
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PagedModelJob&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelJob> getJobsPagedWithHttpInfo(String projectId, Integer page, Integer size, List<String> sort, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelJob> localVarReturnType = new ParameterizedTypeReference<PagedModelJob>() {};
        return getJobsPagedRequestCreation(projectId, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Get Page of jobs with information such as current state and progress (if available).
     * Get Page of jobs with information such as current state and progress (if available).
     * <p><b>200</b> - OK
     * @param projectId project-space to run jobs on
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getJobsPagedWithResponseSpec(String projectId, Integer page, Integer size, List<String> sort, List<JobOptField> optFields) throws WebClientResponseException {
        return getJobsPagedRequestCreation(projectId, page, size, sort, optFields);
    }
    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param includeFinished The includeFinished parameter
     * @return Boolean
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec hasJobsRequestCreation(String projectId, Boolean includeFinished) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling hasJobs", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeFinished", includeFinished));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Boolean> localVarReturnType = new ParameterizedTypeReference<Boolean>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/has-jobs", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param includeFinished The includeFinished parameter
     * @return Boolean
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Boolean hasJobs(String projectId, Boolean includeFinished) throws WebClientResponseException {
        ParameterizedTypeReference<Boolean> localVarReturnType = new ParameterizedTypeReference<Boolean>() {};
        return hasJobsRequestCreation(projectId, includeFinished).bodyToMono(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param includeFinished The includeFinished parameter
     * @return ResponseEntity&lt;Boolean&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Boolean> hasJobsWithHttpInfo(String projectId, Boolean includeFinished) throws WebClientResponseException {
        ParameterizedTypeReference<Boolean> localVarReturnType = new ParameterizedTypeReference<Boolean>() {};
        return hasJobsRequestCreation(projectId, includeFinished).toEntity(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param includeFinished The includeFinished parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec hasJobsWithResponseSpec(String projectId, Boolean includeFinished) throws WebClientResponseException {
        return hasJobsRequestCreation(projectId, includeFinished);
    }
    /**
     * Add new job configuration with given name.
     * Add new job configuration with given name.
     * <p><b>200</b> - StoredJobSubmission that contains the JobSubmission and the probably modified name of the config (to ensure path compatibility).
     * @param name name of the job-config to add
     * @param jobSubmission to add
     * @param overrideExisting The overrideExisting parameter
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters in the return object
     * @return StoredJobSubmission
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec saveJobConfigRequestCreation(String name, JobSubmission jobSubmission, Boolean overrideExisting, Boolean moveParametersToConfigMap) throws WebClientResponseException {
        Object postBody = jobSubmission;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling saveJobConfig", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'jobSubmission' is set
        if (jobSubmission == null) {
            throw new WebClientResponseException("Missing the required parameter 'jobSubmission' when calling saveJobConfig", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("name", name);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "overrideExisting", overrideExisting));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "moveParametersToConfigMap", moveParametersToConfigMap));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<StoredJobSubmission> localVarReturnType = new ParameterizedTypeReference<StoredJobSubmission>() {};
        return apiClient.invokeAPI("/api/job-configs/{name}", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Add new job configuration with given name.
     * Add new job configuration with given name.
     * <p><b>200</b> - StoredJobSubmission that contains the JobSubmission and the probably modified name of the config (to ensure path compatibility).
     * @param name name of the job-config to add
     * @param jobSubmission to add
     * @param overrideExisting The overrideExisting parameter
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters in the return object
     * @return StoredJobSubmission
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public StoredJobSubmission saveJobConfig(String name, JobSubmission jobSubmission, Boolean overrideExisting, Boolean moveParametersToConfigMap) throws WebClientResponseException {
        ParameterizedTypeReference<StoredJobSubmission> localVarReturnType = new ParameterizedTypeReference<StoredJobSubmission>() {};
        return saveJobConfigRequestCreation(name, jobSubmission, overrideExisting, moveParametersToConfigMap).bodyToMono(localVarReturnType).block();
    }

    /**
     * Add new job configuration with given name.
     * Add new job configuration with given name.
     * <p><b>200</b> - StoredJobSubmission that contains the JobSubmission and the probably modified name of the config (to ensure path compatibility).
     * @param name name of the job-config to add
     * @param jobSubmission to add
     * @param overrideExisting The overrideExisting parameter
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters in the return object
     * @return ResponseEntity&lt;StoredJobSubmission&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<StoredJobSubmission> saveJobConfigWithHttpInfo(String name, JobSubmission jobSubmission, Boolean overrideExisting, Boolean moveParametersToConfigMap) throws WebClientResponseException {
        ParameterizedTypeReference<StoredJobSubmission> localVarReturnType = new ParameterizedTypeReference<StoredJobSubmission>() {};
        return saveJobConfigRequestCreation(name, jobSubmission, overrideExisting, moveParametersToConfigMap).toEntity(localVarReturnType).block();
    }

    /**
     * Add new job configuration with given name.
     * Add new job configuration with given name.
     * <p><b>200</b> - StoredJobSubmission that contains the JobSubmission and the probably modified name of the config (to ensure path compatibility).
     * @param name name of the job-config to add
     * @param jobSubmission to add
     * @param overrideExisting The overrideExisting parameter
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters in the return object
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec saveJobConfigWithResponseSpec(String name, JobSubmission jobSubmission, Boolean overrideExisting, Boolean moveParametersToConfigMap) throws WebClientResponseException {
        return saveJobConfigRequestCreation(name, jobSubmission, overrideExisting, moveParametersToConfigMap);
    }
    /**
     * DEPRECATED: this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * Start computation for given command and input.
     * <p><b>200</b> - Job of the command to be executed.
     * @param projectId project-space to perform the command for.
     * @param commandSubmission the command and the input to be executed
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec startCommandRequestCreation(String projectId, CommandSubmission commandSubmission, List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = commandSubmission;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling startCommand", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'commandSubmission' is set
        if (commandSubmission == null) {
            throw new WebClientResponseException("Missing the required parameter 'commandSubmission' when calling startCommand", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/jobs/run-command", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * DEPRECATED: this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * Start computation for given command and input.
     * <p><b>200</b> - Job of the command to be executed.
     * @param projectId project-space to perform the command for.
     * @param commandSubmission the command and the input to be executed
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job startCommand(String projectId, CommandSubmission commandSubmission, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return startCommandRequestCreation(projectId, commandSubmission, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * DEPRECATED: this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * Start computation for given command and input.
     * <p><b>200</b> - Job of the command to be executed.
     * @param projectId project-space to perform the command for.
     * @param commandSubmission the command and the input to be executed
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> startCommandWithHttpInfo(String projectId, CommandSubmission commandSubmission, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return startCommandRequestCreation(projectId, commandSubmission, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * DEPRECATED: this endpoint is based on local file paths and will likely be removed in future versions of this API.
     * Start computation for given command and input.
     * <p><b>200</b> - Job of the command to be executed.
     * @param projectId project-space to perform the command for.
     * @param commandSubmission the command and the input to be executed
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec startCommandWithResponseSpec(String projectId, CommandSubmission commandSubmission, List<JobOptField> optFields) throws WebClientResponseException {
        return startCommandRequestCreation(projectId, commandSubmission, optFields);
    }
    /**
     * Start computation for given compounds and with given parameters.
     * Start computation for given compounds and with given parameters.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to run jobs on
     * @param jobSubmission configuration of the job that will be submitted of the job to be returned
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec startJobRequestCreation(String projectId, JobSubmission jobSubmission, List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = jobSubmission;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling startJob", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'jobSubmission' is set
        if (jobSubmission == null) {
            throw new WebClientResponseException("Missing the required parameter 'jobSubmission' when calling startJob", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/jobs", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Start computation for given compounds and with given parameters.
     * Start computation for given compounds and with given parameters.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to run jobs on
     * @param jobSubmission configuration of the job that will be submitted of the job to be returned
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job startJob(String projectId, JobSubmission jobSubmission, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return startJobRequestCreation(projectId, jobSubmission, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Start computation for given compounds and with given parameters.
     * Start computation for given compounds and with given parameters.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to run jobs on
     * @param jobSubmission configuration of the job that will be submitted of the job to be returned
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> startJobWithHttpInfo(String projectId, JobSubmission jobSubmission, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return startJobRequestCreation(projectId, jobSubmission, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Start computation for given compounds and with given parameters.
     * Start computation for given compounds and with given parameters.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to run jobs on
     * @param jobSubmission configuration of the job that will be submitted of the job to be returned
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec startJobWithResponseSpec(String projectId, JobSubmission jobSubmission, List<JobOptField> optFields) throws WebClientResponseException {
        return startJobRequestCreation(projectId, jobSubmission, optFields);
    }
    /**
     * Start computation for given compounds and with parameters from a stored job-config.
     * Start computation for given compounds and with parameters from a stored job-config.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to run jobs on
     * @param jobConfigName name if the config to be used
     * @param requestBody List of alignedFeatureIds to be computed
     * @param recompute enable or disable recompute. If null the stored value will be used.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec startJobFromConfigRequestCreation(String projectId, String jobConfigName, List<String> requestBody, Boolean recompute, List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling startJobFromConfig", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'jobConfigName' is set
        if (jobConfigName == null) {
            throw new WebClientResponseException("Missing the required parameter 'jobConfigName' when calling startJobFromConfig", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'requestBody' is set
        if (requestBody == null) {
            throw new WebClientResponseException("Missing the required parameter 'requestBody' when calling startJobFromConfig", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "jobConfigName", jobConfigName));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "recompute", recompute));
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
        return apiClient.invokeAPI("/api/projects/{projectId}/jobs/from-config", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Start computation for given compounds and with parameters from a stored job-config.
     * Start computation for given compounds and with parameters from a stored job-config.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to run jobs on
     * @param jobConfigName name if the config to be used
     * @param requestBody List of alignedFeatureIds to be computed
     * @param recompute enable or disable recompute. If null the stored value will be used.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job startJobFromConfig(String projectId, String jobConfigName, List<String> requestBody, Boolean recompute, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return startJobFromConfigRequestCreation(projectId, jobConfigName, requestBody, recompute, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Start computation for given compounds and with parameters from a stored job-config.
     * Start computation for given compounds and with parameters from a stored job-config.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to run jobs on
     * @param jobConfigName name if the config to be used
     * @param requestBody List of alignedFeatureIds to be computed
     * @param recompute enable or disable recompute. If null the stored value will be used.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> startJobFromConfigWithHttpInfo(String projectId, String jobConfigName, List<String> requestBody, Boolean recompute, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return startJobFromConfigRequestCreation(projectId, jobConfigName, requestBody, recompute, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Start computation for given compounds and with parameters from a stored job-config.
     * Start computation for given compounds and with parameters from a stored job-config.
     * <p><b>202</b> - Accepted
     * @param projectId project-space to run jobs on
     * @param jobConfigName name if the config to be used
     * @param requestBody List of alignedFeatureIds to be computed
     * @param recompute enable or disable recompute. If null the stored value will be used.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec startJobFromConfigWithResponseSpec(String projectId, String jobConfigName, List<String> requestBody, Boolean recompute, List<JobOptField> optFields) throws WebClientResponseException {
        return startJobFromConfigRequestCreation(projectId, jobConfigName, requestBody, recompute, optFields);
    }
}
