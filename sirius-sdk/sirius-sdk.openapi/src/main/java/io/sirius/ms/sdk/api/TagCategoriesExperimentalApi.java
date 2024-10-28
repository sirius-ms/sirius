package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.TagCategory;
import io.sirius.ms.sdk.model.TagCategoryImport;

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
public class TagCategoriesExperimentalApi {
    private ApiClient apiClient;

    public TagCategoriesExperimentalApi() {
        this(new ApiClient());
    }

    @Autowired
    public TagCategoriesExperimentalApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Add tag categories to the project.
     * Add tag categories to the project. Category names must not exist in the project.
     * <p><b>200</b> - the tag categories that have been added
     * @param projectId project-space to add to.
     * @param tagCategoryImport the tag categories to be added
     * @return List&lt;TagCategory&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addCategoriesRequestCreation(String projectId, List<TagCategoryImport> tagCategoryImport) throws WebClientResponseException {
        Object postBody = tagCategoryImport;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addCategories", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tagCategoryImport' is set
        if (tagCategoryImport == null) {
            throw new WebClientResponseException("Missing the required parameter 'tagCategoryImport' when calling addCategories", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/categories", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Add tag categories to the project.
     * Add tag categories to the project. Category names must not exist in the project.
     * <p><b>200</b> - the tag categories that have been added
     * @param projectId project-space to add to.
     * @param tagCategoryImport the tag categories to be added
     * @return List&lt;TagCategory&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<TagCategory> addCategories(String projectId, List<TagCategoryImport> tagCategoryImport) throws WebClientResponseException {
        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return addCategoriesRequestCreation(projectId, tagCategoryImport).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Add tag categories to the project.
     * Add tag categories to the project. Category names must not exist in the project.
     * <p><b>200</b> - the tag categories that have been added
     * @param projectId project-space to add to.
     * @param tagCategoryImport the tag categories to be added
     * @return ResponseEntity&lt;List&lt;TagCategory&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<TagCategory>> addCategoriesWithHttpInfo(String projectId, List<TagCategoryImport> tagCategoryImport) throws WebClientResponseException {
        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return addCategoriesRequestCreation(projectId, tagCategoryImport).toEntityList(localVarReturnType).block();
    }

    /**
     * Add tag categories to the project.
     * Add tag categories to the project. Category names must not exist in the project.
     * <p><b>200</b> - the tag categories that have been added
     * @param projectId project-space to add to.
     * @param tagCategoryImport the tag categories to be added
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addCategoriesWithResponseSpec(String projectId, List<TagCategoryImport> tagCategoryImport) throws WebClientResponseException {
        return addCategoriesRequestCreation(projectId, tagCategoryImport);
    }
    /**
     * Add a possible value to the tag category in the project.
     * Add a possible value to the tag category in the project.
     * <p><b>200</b> - the tag categories that have been added
     * @param projectId project-space to add to.
     * @param categoryName the tag category to add to
     * @param requestBody The requestBody parameter
     * @return TagCategory
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addPossibleValuesToCategoryRequestCreation(String projectId, String categoryName, List<Object> requestBody) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addPossibleValuesToCategory", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'categoryName' is set
        if (categoryName == null) {
            throw new WebClientResponseException("Missing the required parameter 'categoryName' when calling addPossibleValuesToCategory", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'requestBody' is set
        if (requestBody == null) {
            throw new WebClientResponseException("Missing the required parameter 'requestBody' when calling addPossibleValuesToCategory", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/categories/{categoryName}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Add a possible value to the tag category in the project.
     * Add a possible value to the tag category in the project.
     * <p><b>200</b> - the tag categories that have been added
     * @param projectId project-space to add to.
     * @param categoryName the tag category to add to
     * @param requestBody The requestBody parameter
     * @return TagCategory
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TagCategory addPossibleValuesToCategory(String projectId, String categoryName, List<Object> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return addPossibleValuesToCategoryRequestCreation(projectId, categoryName, requestBody).bodyToMono(localVarReturnType).block();
    }

    /**
     * Add a possible value to the tag category in the project.
     * Add a possible value to the tag category in the project.
     * <p><b>200</b> - the tag categories that have been added
     * @param projectId project-space to add to.
     * @param categoryName the tag category to add to
     * @param requestBody The requestBody parameter
     * @return ResponseEntity&lt;TagCategory&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TagCategory> addPossibleValuesToCategoryWithHttpInfo(String projectId, String categoryName, List<Object> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return addPossibleValuesToCategoryRequestCreation(projectId, categoryName, requestBody).toEntity(localVarReturnType).block();
    }

    /**
     * Add a possible value to the tag category in the project.
     * Add a possible value to the tag category in the project.
     * <p><b>200</b> - the tag categories that have been added
     * @param projectId project-space to add to.
     * @param categoryName the tag category to add to
     * @param requestBody The requestBody parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addPossibleValuesToCategoryWithResponseSpec(String projectId, String categoryName, List<Object> requestBody) throws WebClientResponseException {
        return addPossibleValuesToCategoryRequestCreation(projectId, categoryName, requestBody);
    }
    /**
     * Delete tag categories with the given names from the specified project-space.
     * Delete tag categories with the given names from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param categoryName name of category to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteCategoriesRequestCreation(String projectId, String categoryName) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteCategories", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'categoryName' is set
        if (categoryName == null) {
            throw new WebClientResponseException("Missing the required parameter 'categoryName' when calling deleteCategories", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("categoryName", categoryName);

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
        return apiClient.invokeAPI("/api/projects/{projectId}/categories/{categoryName}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete tag categories with the given names from the specified project-space.
     * Delete tag categories with the given names from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param categoryName name of category to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteCategories(String projectId, String categoryName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteCategoriesRequestCreation(projectId, categoryName).bodyToMono(localVarReturnType).block();
    }

    /**
     * Delete tag categories with the given names from the specified project-space.
     * Delete tag categories with the given names from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param categoryName name of category to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteCategoriesWithHttpInfo(String projectId, String categoryName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteCategoriesRequestCreation(projectId, categoryName).toEntity(localVarReturnType).block();
    }

    /**
     * Delete tag categories with the given names from the specified project-space.
     * Delete tag categories with the given names from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param categoryName name of category to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteCategoriesWithResponseSpec(String projectId, String categoryName) throws WebClientResponseException {
        return deleteCategoriesRequestCreation(projectId, categoryName);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/categories", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
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
     * Get tag categories by type in the given project-space.
     * Get tag categories by type in the given project-space.
     * <p><b>200</b> - Tag categories.
     * @param projectId project-space to read from.
     * @param categoryType name of the category
     * @return List&lt;TagCategory&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCategoriesByTypeRequestCreation(String projectId, String categoryType) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCategoriesByType", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'categoryType' is set
        if (categoryType == null) {
            throw new WebClientResponseException("Missing the required parameter 'categoryType' when calling getCategoriesByType", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("categoryType", categoryType);

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
        return apiClient.invokeAPI("/api/projects/{projectId}/categories/type/{categoryType}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get tag categories by type in the given project-space.
     * Get tag categories by type in the given project-space.
     * <p><b>200</b> - Tag categories.
     * @param projectId project-space to read from.
     * @param categoryType name of the category
     * @return List&lt;TagCategory&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<TagCategory> getCategoriesByType(String projectId, String categoryType) throws WebClientResponseException {
        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return getCategoriesByTypeRequestCreation(projectId, categoryType).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Get tag categories by type in the given project-space.
     * Get tag categories by type in the given project-space.
     * <p><b>200</b> - Tag categories.
     * @param projectId project-space to read from.
     * @param categoryType name of the category
     * @return ResponseEntity&lt;List&lt;TagCategory&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<TagCategory>> getCategoriesByTypeWithHttpInfo(String projectId, String categoryType) throws WebClientResponseException {
        ParameterizedTypeReference<TagCategory> localVarReturnType = new ParameterizedTypeReference<TagCategory>() {};
        return getCategoriesByTypeRequestCreation(projectId, categoryType).toEntityList(localVarReturnType).block();
    }

    /**
     * Get tag categories by type in the given project-space.
     * Get tag categories by type in the given project-space.
     * <p><b>200</b> - Tag categories.
     * @param projectId project-space to read from.
     * @param categoryType name of the category
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCategoriesByTypeWithResponseSpec(String projectId, String categoryType) throws WebClientResponseException {
        return getCategoriesByTypeRequestCreation(projectId, categoryType);
    }
    /**
     * Get tag category by name in the given project-space.
     * Get tag category by name in the given project-space.
     * <p><b>200</b> - Tag category.
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
        return apiClient.invokeAPI("/api/projects/{projectId}/categories/{categoryName}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get tag category by name in the given project-space.
     * Get tag category by name in the given project-space.
     * <p><b>200</b> - Tag category.
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
     * <p><b>200</b> - Tag category.
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
     * <p><b>200</b> - Tag category.
     * @param projectId project-space to read from.
     * @param categoryName name of the category
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCategoryByNameWithResponseSpec(String projectId, String categoryName) throws WebClientResponseException {
        return getCategoryByNameRequestCreation(projectId, categoryName);
    }
}
