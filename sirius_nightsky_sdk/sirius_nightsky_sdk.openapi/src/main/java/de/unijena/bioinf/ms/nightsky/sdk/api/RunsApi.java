package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;

import de.unijena.bioinf.ms.nightsky.sdk.model.PageAlignedFeature;
import de.unijena.bioinf.ms.nightsky.sdk.model.Run;
import de.unijena.bioinf.ms.nightsky.sdk.model.RunOptField;
import de.unijena.bioinf.ms.nightsky.sdk.model.TagCategory;

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
public class RunsApi {
    private ApiClient apiClient;

    public RunsApi() {
        this(new ApiClient());
    }

    @Autowired
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
     * Add tag category to the project.
     * Add tag category to the project. Category name must not exist in the project.
     * <p><b>200</b> - the tag categories that have been added
     * @param projectId project-space to add to.
     * @param tagCategory the tag categories to be added
     * @return List&lt;TagCategory&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addCategoriesRequestCreation(String projectId, List<TagCategory> tagCategory) throws WebClientResponseException {
        Object postBody = tagCategory;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addCategories", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tagCategory' is set
        if (tagCategory == null) {
            throw new WebClientResponseException("Missing the required parameter 'tagCategory' when calling addCategories", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/categories/add", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Add tag category to the project.
     * Add tag category to the project. Category name must not exist in the project.
     * <p><b>200</b> - the tag categories that have been added
     * @param projectId project-space to add to.
     * @param tagCategory the tag categories to be added
     * @return List&lt;TagCategory&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<TagCategory> addCategories(String projectId, List<TagCategory> tagCategory) throws WebClientResponseException {
        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return addCategoriesRequestCreation(projectId, tagCategory).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Add tag category to the project.
     * Add tag category to the project. Category name must not exist in the project.
     * <p><b>200</b> - the tag categories that have been added
     * @param projectId project-space to add to.
     * @param tagCategory the tag categories to be added
     * @return ResponseEntity&lt;List&lt;TagCategory&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<TagCategory>> addCategoriesWithHttpInfo(String projectId, List<TagCategory> tagCategory) throws WebClientResponseException {
        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return addCategoriesRequestCreation(projectId, tagCategory).toEntityList(localVarReturnType).block();
    }

    /**
     * Add tag category to the project.
     * Add tag category to the project. Category name must not exist in the project.
     * <p><b>200</b> - the tag categories that have been added
     * @param projectId project-space to add to.
     * @param tagCategory the tag categories to be added
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addCategoriesWithResponseSpec(String projectId, List<TagCategory> tagCategory) throws WebClientResponseException {
        return addCategoriesRequestCreation(projectId, tagCategory);
    }
    /**
     * Add tags to a run in the project.
     * Add tags to a run in the project. Tags with the same category name will be overwritten.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param objectId object to tag.
     * @param requestBody tags to add.
     * @return List&lt;Object&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addTagsRequestCreation(String projectId, String objectId, List<Object> requestBody) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'objectId' is set
        if (objectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'objectId' when calling addTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'requestBody' is set
        if (requestBody == null) {
            throw new WebClientResponseException("Missing the required parameter 'requestBody' when calling addTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Object> localVarReturnType = new ParameterizedTypeReference<Object>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/tags/add/{objectId}", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Add tags to a run in the project.
     * Add tags to a run in the project. Tags with the same category name will be overwritten.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param objectId object to tag.
     * @param requestBody tags to add.
     * @return List&lt;Object&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Object> addTags(String projectId, String objectId, List<Object> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<Object> localVarReturnType = new ParameterizedTypeReference<Object>() {};
        return addTagsRequestCreation(projectId, objectId, requestBody).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Add tags to a run in the project.
     * Add tags to a run in the project. Tags with the same category name will be overwritten.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param objectId object to tag.
     * @param requestBody tags to add.
     * @return ResponseEntity&lt;List&lt;Object&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Object>> addTagsWithHttpInfo(String projectId, String objectId, List<Object> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<Object> localVarReturnType = new ParameterizedTypeReference<Object>() {};
        return addTagsRequestCreation(projectId, objectId, requestBody).toEntityList(localVarReturnType).block();
    }

    /**
     * Add tags to a run in the project.
     * Add tags to a run in the project. Tags with the same category name will be overwritten.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param objectId object to tag.
     * @param requestBody tags to add.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addTagsWithResponseSpec(String projectId, String objectId, List<Object> requestBody) throws WebClientResponseException {
        return addTagsRequestCreation(projectId, objectId, requestBody);
    }
    /**
     * Delete tag categories with the given names from the specified project-space.
     * Delete tag categories with the given names from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param requestBody names of categories to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteCategoriesRequestCreation(String projectId, List<String> requestBody) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteCategories", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'requestBody' is set
        if (requestBody == null) {
            throw new WebClientResponseException("Missing the required parameter 'requestBody' when calling deleteCategories", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/categories/delete", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete tag categories with the given names from the specified project-space.
     * Delete tag categories with the given names from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param requestBody names of categories to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteCategories(String projectId, List<String> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteCategoriesRequestCreation(projectId, requestBody).bodyToMono(localVarReturnType).block();
    }

    /**
     * Delete tag categories with the given names from the specified project-space.
     * Delete tag categories with the given names from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param requestBody names of categories to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteCategoriesWithHttpInfo(String projectId, List<String> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteCategoriesRequestCreation(projectId, requestBody).toEntity(localVarReturnType).block();
    }

    /**
     * Delete tag categories with the given names from the specified project-space.
     * Delete tag categories with the given names from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param requestBody names of categories to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteCategoriesWithResponseSpec(String projectId, List<String> requestBody) throws WebClientResponseException {
        return deleteCategoriesRequestCreation(projectId, requestBody);
    }
    /**
     * Delete tags with the given IDs from the specified project-space.
     * Delete tags with the given IDs from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param objectId object to delete tags from.
     * @param requestBody Category names of the tags to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteTagsRequestCreation(String projectId, String objectId, List<String> requestBody) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'objectId' is set
        if (objectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'objectId' when calling deleteTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'requestBody' is set
        if (requestBody == null) {
            throw new WebClientResponseException("Missing the required parameter 'requestBody' when calling deleteTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("objectId", objectId);

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
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/tags/delete/{objectId}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete tags with the given IDs from the specified project-space.
     * Delete tags with the given IDs from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param objectId object to delete tags from.
     * @param requestBody Category names of the tags to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteTags(String projectId, String objectId, List<String> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteTagsRequestCreation(projectId, objectId, requestBody).bodyToMono(localVarReturnType).block();
    }

    /**
     * Delete tags with the given IDs from the specified project-space.
     * Delete tags with the given IDs from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param objectId object to delete tags from.
     * @param requestBody Category names of the tags to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteTagsWithHttpInfo(String projectId, String objectId, List<String> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteTagsRequestCreation(projectId, objectId, requestBody).toEntity(localVarReturnType).block();
    }

    /**
     * Delete tags with the given IDs from the specified project-space.
     * Delete tags with the given IDs from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param objectId object to delete tags from.
     * @param requestBody Category names of the tags to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteTagsWithResponseSpec(String projectId, String objectId, List<String> requestBody) throws WebClientResponseException {
        return deleteTagsRequestCreation(projectId, objectId, requestBody);
    }
    /**
     * Get all tag categories in the given project-space.
     * Get all tag categories in the given project-space.
     * <p><b>200</b> - Tag categories.
     * @param projectId project-space to read from.
     * @return List&lt;TagCategory&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCategoriesRequestCreation(String projectId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCategories", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/categories", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get all tag categories in the given project-space.
     * Get all tag categories in the given project-space.
     * <p><b>200</b> - Tag categories.
     * @param projectId project-space to read from.
     * @return List&lt;TagCategory&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<TagCategory> getCategories(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return getCategoriesRequestCreation(projectId).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Get all tag categories in the given project-space.
     * Get all tag categories in the given project-space.
     * <p><b>200</b> - Tag categories.
     * @param projectId project-space to read from.
     * @return ResponseEntity&lt;List&lt;TagCategory&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<TagCategory>> getCategoriesWithHttpInfo(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return getCategoriesRequestCreation(projectId).toEntityList(localVarReturnType).block();
    }

    /**
     * Get all tag categories in the given project-space.
     * Get all tag categories in the given project-space.
     * <p><b>200</b> - Tag categories.
     * @param projectId project-space to read from.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCategoriesWithResponseSpec(String projectId) throws WebClientResponseException {
        return getCategoriesRequestCreation(projectId);
    }
    /**
     * Get tag category by name in the given project-space.
     * Get tag category by name in the given project-space.
     * <p><b>200</b> - Tag categories.
     * @param projectId project-space to read from.
     * @param categoryName name of the category
     * @return TagCategory
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCategoryByNameRequestCreation(String projectId, String categoryName) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCategoryByName", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'categoryName' is set
        if (categoryName == null) {
            throw new WebClientResponseException("Missing the required parameter 'categoryName' when calling getCategoryByName", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("categoryName", categoryName);

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

        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/categories/{categoryName}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get tag category by name in the given project-space.
     * Get tag category by name in the given project-space.
     * <p><b>200</b> - Tag categories.
     * @param projectId project-space to read from.
     * @param categoryName name of the category
     * @return TagCategory
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TagCategory getCategoryByName(String projectId, String categoryName) throws WebClientResponseException {
        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return getCategoryByNameRequestCreation(projectId, categoryName).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get tag category by name in the given project-space.
     * Get tag category by name in the given project-space.
     * <p><b>200</b> - Tag categories.
     * @param projectId project-space to read from.
     * @param categoryName name of the category
     * @return ResponseEntity&lt;TagCategory&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TagCategory> getCategoryByNameWithHttpInfo(String projectId, String categoryName) throws WebClientResponseException {
        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return getCategoryByNameRequestCreation(projectId, categoryName).toEntity(localVarReturnType).block();
    }

    /**
     * Get tag category by name in the given project-space.
     * Get tag category by name in the given project-space.
     * <p><b>200</b> - Tag categories.
     * @param projectId project-space to read from.
     * @param categoryName name of the category
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCategoryByNameWithResponseSpec(String projectId, String categoryName) throws WebClientResponseException {
        return getCategoryByNameRequestCreation(projectId, categoryName);
    }
    /**
     * Get run with the given identifier from the specified project-space.
     * Get run with the given identifier from the specified project-space.
     * <p><b>200</b> - Run with tags (if specified).
     * @param projectId project-space to read from.
     * @param runId identifier of run to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Run
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getRunRequestCreation(String projectId, String runId, List<RunOptField> optFields) throws WebClientResponseException {
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
            "application/json"
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
     * @param projectId project-space to read from.
     * @param runId identifier of run to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return Run
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Run getRun(String projectId, String runId, List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Run> localVarReturnType = new ParameterizedTypeReference<Run>() {};
        return getRunRequestCreation(projectId, runId, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get run with the given identifier from the specified project-space.
     * Get run with the given identifier from the specified project-space.
     * <p><b>200</b> - Run with tags (if specified).
     * @param projectId project-space to read from.
     * @param runId identifier of run to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;Run&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Run> getRunWithHttpInfo(String projectId, String runId, List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Run> localVarReturnType = new ParameterizedTypeReference<Run>() {};
        return getRunRequestCreation(projectId, runId, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Get run with the given identifier from the specified project-space.
     * Get run with the given identifier from the specified project-space.
     * <p><b>200</b> - Run with tags (if specified).
     * @param projectId project-space to read from.
     * @param runId identifier of run to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getRunWithResponseSpec(String projectId, String runId, List<RunOptField> optFields) throws WebClientResponseException {
        return getRunRequestCreation(projectId, runId, optFields);
    }
    /**
     * Get all available runs in the given project-space.
     * Get all available runs in the given project-space.
     * <p><b>200</b> - Runs with tags (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PageAlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getRunsPagedRequestCreation(String projectId, Integer page, Integer size, List<String> sort, List<RunOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getRunsPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<PageAlignedFeature> localVarReturnType = new ParameterizedTypeReference<PageAlignedFeature>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get all available runs in the given project-space.
     * Get all available runs in the given project-space.
     * <p><b>200</b> - Runs with tags (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PageAlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageAlignedFeature getRunsPaged(String projectId, Integer page, Integer size, List<String> sort, List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageAlignedFeature> localVarReturnType = new ParameterizedTypeReference<PageAlignedFeature>() {};
        return getRunsPagedRequestCreation(projectId, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get all available runs in the given project-space.
     * Get all available runs in the given project-space.
     * <p><b>200</b> - Runs with tags (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PageAlignedFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageAlignedFeature> getRunsPagedWithHttpInfo(String projectId, Integer page, Integer size, List<String> sort, List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageAlignedFeature> localVarReturnType = new ParameterizedTypeReference<PageAlignedFeature>() {};
        return getRunsPagedRequestCreation(projectId, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Get all available runs in the given project-space.
     * Get all available runs in the given project-space.
     * <p><b>200</b> - Runs with tags (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getRunsPagedWithResponseSpec(String projectId, Integer page, Integer size, List<String> sort, List<RunOptField> optFields) throws WebClientResponseException {
        return getRunsPagedRequestCreation(projectId, page, size, sort, optFields);
    }
}
