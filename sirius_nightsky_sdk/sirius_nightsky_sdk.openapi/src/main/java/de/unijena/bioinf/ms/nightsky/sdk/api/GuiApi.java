package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;

import de.unijena.bioinf.ms.nightsky.sdk.model.PageGuiInfo;

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
public class GuiApi {
    private ApiClient apiClient;

    public GuiApi() {
        this(new ApiClient());
    }

    @Autowired
    public GuiApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Close GUI instance of given project-space if available.
     * Close GUI instance of given project-space if available.
     * <p><b>200</b> - OK
     * @param projectId if project-space the GUI instance is connected to.
     * @param closeProject The closeProject parameter
     * @return Boolean
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec closeGuiRequestCreation(String projectId, Boolean closeProject) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling closeGui", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "closeProject", closeProject));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Boolean> localVarReturnType = new ParameterizedTypeReference<Boolean>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/gui", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Close GUI instance of given project-space if available.
     * Close GUI instance of given project-space if available.
     * <p><b>200</b> - OK
     * @param projectId if project-space the GUI instance is connected to.
     * @param closeProject The closeProject parameter
     * @return Boolean
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Boolean closeGui(String projectId, Boolean closeProject) throws WebClientResponseException {
        ParameterizedTypeReference<Boolean> localVarReturnType = new ParameterizedTypeReference<Boolean>() {};
        return closeGuiRequestCreation(projectId, closeProject).bodyToMono(localVarReturnType).block();
    }

    /**
     * Close GUI instance of given project-space if available.
     * Close GUI instance of given project-space if available.
     * <p><b>200</b> - OK
     * @param projectId if project-space the GUI instance is connected to.
     * @param closeProject The closeProject parameter
     * @return ResponseEntity&lt;Boolean&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Boolean> closeGuiWithHttpInfo(String projectId, Boolean closeProject) throws WebClientResponseException {
        ParameterizedTypeReference<Boolean> localVarReturnType = new ParameterizedTypeReference<Boolean>() {};
        return closeGuiRequestCreation(projectId, closeProject).toEntity(localVarReturnType).block();
    }

    /**
     * Close GUI instance of given project-space if available.
     * Close GUI instance of given project-space if available.
     * <p><b>200</b> - OK
     * @param projectId if project-space the GUI instance is connected to.
     * @param closeProject The closeProject parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec closeGuiWithResponseSpec(String projectId, Boolean closeProject) throws WebClientResponseException {
        return closeGuiRequestCreation(projectId, closeProject);
    }
    /**
     * Get list of currently running gui windows, managed by this SIRIUS instance.
     * Get list of currently running gui windows, managed by this SIRIUS instance.  Note this will not show any Clients that are connected from a separate process!
     * <p><b>200</b> - List of GUI windows that are currently managed by this SIRIUS instance.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return PageGuiInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getGuisRequestCreation(Integer page, Integer size, List<String> sort) throws WebClientResponseException {
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

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PageGuiInfo> localVarReturnType = new ParameterizedTypeReference<PageGuiInfo>() {};
        return apiClient.invokeAPI("/api/guis", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get list of currently running gui windows, managed by this SIRIUS instance.
     * Get list of currently running gui windows, managed by this SIRIUS instance.  Note this will not show any Clients that are connected from a separate process!
     * <p><b>200</b> - List of GUI windows that are currently managed by this SIRIUS instance.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return PageGuiInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageGuiInfo getGuis(Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        ParameterizedTypeReference<PageGuiInfo> localVarReturnType = new ParameterizedTypeReference<PageGuiInfo>() {};
        return getGuisRequestCreation(page, size, sort).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get list of currently running gui windows, managed by this SIRIUS instance.
     * Get list of currently running gui windows, managed by this SIRIUS instance.  Note this will not show any Clients that are connected from a separate process!
     * <p><b>200</b> - List of GUI windows that are currently managed by this SIRIUS instance.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return ResponseEntity&lt;PageGuiInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageGuiInfo> getGuisWithHttpInfo(Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        ParameterizedTypeReference<PageGuiInfo> localVarReturnType = new ParameterizedTypeReference<PageGuiInfo>() {};
        return getGuisRequestCreation(page, size, sort).toEntity(localVarReturnType).block();
    }

    /**
     * Get list of currently running gui windows, managed by this SIRIUS instance.
     * Get list of currently running gui windows, managed by this SIRIUS instance.  Note this will not show any Clients that are connected from a separate process!
     * <p><b>200</b> - List of GUI windows that are currently managed by this SIRIUS instance.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getGuisWithResponseSpec(Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        return getGuisRequestCreation(page, size, sort);
    }
    /**
     * Open GUI instance on specified project-space and bring the GUI window to foreground.
     * Open GUI instance on specified project-space and bring the GUI window to foreground.
     * <p><b>201</b> - Created
     * @param projectId of project-space the GUI instance will connect to.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec openGuiRequestCreation(String projectId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling openGui", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/gui", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Open GUI instance on specified project-space and bring the GUI window to foreground.
     * Open GUI instance on specified project-space and bring the GUI window to foreground.
     * <p><b>201</b> - Created
     * @param projectId of project-space the GUI instance will connect to.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void openGui(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        openGuiRequestCreation(projectId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Open GUI instance on specified project-space and bring the GUI window to foreground.
     * Open GUI instance on specified project-space and bring the GUI window to foreground.
     * <p><b>201</b> - Created
     * @param projectId of project-space the GUI instance will connect to.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> openGuiWithHttpInfo(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return openGuiRequestCreation(projectId).toEntity(localVarReturnType).block();
    }

    /**
     * Open GUI instance on specified project-space and bring the GUI window to foreground.
     * Open GUI instance on specified project-space and bring the GUI window to foreground.
     * <p><b>201</b> - Created
     * @param projectId of project-space the GUI instance will connect to.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec openGuiWithResponseSpec(String projectId) throws WebClientResponseException {
        return openGuiRequestCreation(projectId);
    }
}
