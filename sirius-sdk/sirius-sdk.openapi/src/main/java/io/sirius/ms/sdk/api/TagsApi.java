package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.TagDefinition;
import io.sirius.ms.sdk.model.TagDefinitionImport;
import io.sirius.ms.sdk.model.TagGroup;

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
public class TagsApi {
    private ApiClient apiClient;

    public TagsApi() {
        this(new ApiClient());
    }

    @Autowired
    public TagsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * [EXPERIMENTAL] Group tags in the project
     * [EXPERIMENTAL] Group tags in the project. The group name must not exist in the project.   &lt;p&gt;  See &lt;code&gt;/tagged&lt;/code&gt; for filter syntax.  &lt;/p&gt;   [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tag group that was added
     * @param projectId project-space to add to.
     * @param groupName name of the new group
     * @param filter filter query to create the group
     * @param type type of the group
     * @return TagGroup
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addGroupRequestCreation(String projectId, String groupName, String filter, String type) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addGroup", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'groupName' is set
        if (groupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'groupName' when calling addGroup", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'filter' is set
        if (filter == null) {
            throw new WebClientResponseException("Missing the required parameter 'filter' when calling addGroup", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'type' is set
        if (type == null) {
            throw new WebClientResponseException("Missing the required parameter 'type' when calling addGroup", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("groupName", groupName);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "filter", filter));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "type", type));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/groups/{groupName}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Group tags in the project
     * [EXPERIMENTAL] Group tags in the project. The group name must not exist in the project.   &lt;p&gt;  See &lt;code&gt;/tagged&lt;/code&gt; for filter syntax.  &lt;/p&gt;   [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tag group that was added
     * @param projectId project-space to add to.
     * @param groupName name of the new group
     * @param filter filter query to create the group
     * @param type type of the group
     * @return TagGroup
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TagGroup addGroup(String projectId, String groupName, String filter, String type) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return addGroupRequestCreation(projectId, groupName, filter, type).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Group tags in the project
     * [EXPERIMENTAL] Group tags in the project. The group name must not exist in the project.   &lt;p&gt;  See &lt;code&gt;/tagged&lt;/code&gt; for filter syntax.  &lt;/p&gt;   [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tag group that was added
     * @param projectId project-space to add to.
     * @param groupName name of the new group
     * @param filter filter query to create the group
     * @param type type of the group
     * @return ResponseEntity&lt;TagGroup&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TagGroup> addGroupWithHttpInfo(String projectId, String groupName, String filter, String type) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return addGroupRequestCreation(projectId, groupName, filter, type).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Group tags in the project
     * [EXPERIMENTAL] Group tags in the project. The group name must not exist in the project.   &lt;p&gt;  See &lt;code&gt;/tagged&lt;/code&gt; for filter syntax.  &lt;/p&gt;   [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tag group that was added
     * @param projectId project-space to add to.
     * @param groupName name of the new group
     * @param filter filter query to create the group
     * @param type type of the group
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addGroupWithResponseSpec(String projectId, String groupName, String filter, String type) throws WebClientResponseException {
        return addGroupRequestCreation(projectId, groupName, filter, type);
    }
    /**
     * [EXPERIMENTAL] Add a possible value to the tag definition in the project
     * [EXPERIMENTAL] Add a possible value to the tag definition in the project.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the definitions of the tags that have been added
     * @param projectId project-space to add to.
     * @param tagName the tag definition to add the values to
     * @param requestBody The requestBody parameter
     * @return TagDefinition
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addPossibleValuesToTagDefinitionRequestCreation(String projectId, String tagName, List<Object> requestBody) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addPossibleValuesToTagDefinition", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tagName' is set
        if (tagName == null) {
            throw new WebClientResponseException("Missing the required parameter 'tagName' when calling addPossibleValuesToTagDefinition", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'requestBody' is set
        if (requestBody == null) {
            throw new WebClientResponseException("Missing the required parameter 'requestBody' when calling addPossibleValuesToTagDefinition", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("tagName", tagName);

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

        ParameterizedTypeReference<TagDefinition> localVarReturnType = new ParameterizedTypeReference<TagDefinition>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/tags/{tagName}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Add a possible value to the tag definition in the project
     * [EXPERIMENTAL] Add a possible value to the tag definition in the project.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the definitions of the tags that have been added
     * @param projectId project-space to add to.
     * @param tagName the tag definition to add the values to
     * @param requestBody The requestBody parameter
     * @return TagDefinition
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TagDefinition addPossibleValuesToTagDefinition(String projectId, String tagName, List<Object> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<TagDefinition> localVarReturnType = new ParameterizedTypeReference<TagDefinition>() {};
        return addPossibleValuesToTagDefinitionRequestCreation(projectId, tagName, requestBody).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add a possible value to the tag definition in the project
     * [EXPERIMENTAL] Add a possible value to the tag definition in the project.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the definitions of the tags that have been added
     * @param projectId project-space to add to.
     * @param tagName the tag definition to add the values to
     * @param requestBody The requestBody parameter
     * @return ResponseEntity&lt;TagDefinition&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TagDefinition> addPossibleValuesToTagDefinitionWithHttpInfo(String projectId, String tagName, List<Object> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<TagDefinition> localVarReturnType = new ParameterizedTypeReference<TagDefinition>() {};
        return addPossibleValuesToTagDefinitionRequestCreation(projectId, tagName, requestBody).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add a possible value to the tag definition in the project
     * [EXPERIMENTAL] Add a possible value to the tag definition in the project.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the definitions of the tags that have been added
     * @param projectId project-space to add to.
     * @param tagName the tag definition to add the values to
     * @param requestBody The requestBody parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addPossibleValuesToTagDefinitionWithResponseSpec(String projectId, String tagName, List<Object> requestBody) throws WebClientResponseException {
        return addPossibleValuesToTagDefinitionRequestCreation(projectId, tagName, requestBody);
    }
    /**
     * [EXPERIMENTAL] Add tags to the project
     * [EXPERIMENTAL] Add tags to the project. Tag names must not exist in the project.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the definitions of the tags that have been created
     * @param projectId project-space to add to.
     * @param tagDefinitionImport the tag definitions to be created
     * @return List&lt;TagDefinition&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec createTagsRequestCreation(String projectId, List<TagDefinitionImport> tagDefinitionImport) throws WebClientResponseException {
        Object postBody = tagDefinitionImport;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling createTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tagDefinitionImport' is set
        if (tagDefinitionImport == null) {
            throw new WebClientResponseException("Missing the required parameter 'tagDefinitionImport' when calling createTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<TagDefinition> localVarReturnType = new ParameterizedTypeReference<TagDefinition>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/tags", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Add tags to the project
     * [EXPERIMENTAL] Add tags to the project. Tag names must not exist in the project.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the definitions of the tags that have been created
     * @param projectId project-space to add to.
     * @param tagDefinitionImport the tag definitions to be created
     * @return List&lt;TagDefinition&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<TagDefinition> createTags(String projectId, List<TagDefinitionImport> tagDefinitionImport) throws WebClientResponseException {
        ParameterizedTypeReference<TagDefinition> localVarReturnType = new ParameterizedTypeReference<TagDefinition>() {};
        return createTagsRequestCreation(projectId, tagDefinitionImport).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] Add tags to the project
     * [EXPERIMENTAL] Add tags to the project. Tag names must not exist in the project.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the definitions of the tags that have been created
     * @param projectId project-space to add to.
     * @param tagDefinitionImport the tag definitions to be created
     * @return ResponseEntity&lt;List&lt;TagDefinition&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<TagDefinition>> createTagsWithHttpInfo(String projectId, List<TagDefinitionImport> tagDefinitionImport) throws WebClientResponseException {
        ParameterizedTypeReference<TagDefinition> localVarReturnType = new ParameterizedTypeReference<TagDefinition>() {};
        return createTagsRequestCreation(projectId, tagDefinitionImport).toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add tags to the project
     * [EXPERIMENTAL] Add tags to the project. Tag names must not exist in the project.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the definitions of the tags that have been created
     * @param projectId project-space to add to.
     * @param tagDefinitionImport the tag definitions to be created
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec createTagsWithResponseSpec(String projectId, List<TagDefinitionImport> tagDefinitionImport) throws WebClientResponseException {
        return createTagsRequestCreation(projectId, tagDefinitionImport);
    }
    /**
     * [EXPERIMENTAL] Delete tag groups with the given name from the specified project-space
     * [EXPERIMENTAL] Delete tag groups with the given name from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param groupName name of group to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteGroupRequestCreation(String projectId, String groupName) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteGroup", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'groupName' is set
        if (groupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'groupName' when calling deleteGroup", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("groupName", groupName);

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
        return apiClient.invokeAPI("/api/projects/{projectId}/groups/{groupName}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Delete tag groups with the given name from the specified project-space
     * [EXPERIMENTAL] Delete tag groups with the given name from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param groupName name of group to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteGroup(String projectId, String groupName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteGroupRequestCreation(projectId, groupName).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete tag groups with the given name from the specified project-space
     * [EXPERIMENTAL] Delete tag groups with the given name from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param groupName name of group to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteGroupWithHttpInfo(String projectId, String groupName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteGroupRequestCreation(projectId, groupName).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete tag groups with the given name from the specified project-space
     * [EXPERIMENTAL] Delete tag groups with the given name from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param groupName name of group to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteGroupWithResponseSpec(String projectId, String groupName) throws WebClientResponseException {
        return deleteGroupRequestCreation(projectId, groupName);
    }
    /**
     * [EXPERIMENTAL] Delete tag definition with the given name from the specified project-space
     * [EXPERIMENTAL] Delete tag definition with the given name from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param tagName name of the tag definition to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteTagRequestCreation(String projectId, String tagName) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteTag", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tagName' is set
        if (tagName == null) {
            throw new WebClientResponseException("Missing the required parameter 'tagName' when calling deleteTag", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/tags/{tagName}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Delete tag definition with the given name from the specified project-space
     * [EXPERIMENTAL] Delete tag definition with the given name from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param tagName name of the tag definition to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteTag(String projectId, String tagName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteTagRequestCreation(projectId, tagName).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete tag definition with the given name from the specified project-space
     * [EXPERIMENTAL] Delete tag definition with the given name from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param tagName name of the tag definition to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteTagWithHttpInfo(String projectId, String tagName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteTagRequestCreation(projectId, tagName).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete tag definition with the given name from the specified project-space
     * [EXPERIMENTAL] Delete tag definition with the given name from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param tagName name of the tag definition to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteTagWithResponseSpec(String projectId, String tagName) throws WebClientResponseException {
        return deleteTagRequestCreation(projectId, tagName);
    }
    /**
     * [EXPERIMENTAL] Get tag group by name in the given project-space
     * [EXPERIMENTAL] Get tag group by name in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag group.
     * @param projectId project-space to read from.
     * @param groupName name of the group
     * @return TagGroup
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getGroupByNameRequestCreation(String projectId, String groupName) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getGroupByName", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'groupName' is set
        if (groupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'groupName' when calling getGroupByName", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("groupName", groupName);

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

        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/groups/{groupName}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get tag group by name in the given project-space
     * [EXPERIMENTAL] Get tag group by name in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag group.
     * @param projectId project-space to read from.
     * @param groupName name of the group
     * @return TagGroup
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TagGroup getGroupByName(String projectId, String groupName) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return getGroupByNameRequestCreation(projectId, groupName).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get tag group by name in the given project-space
     * [EXPERIMENTAL] Get tag group by name in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag group.
     * @param projectId project-space to read from.
     * @param groupName name of the group
     * @return ResponseEntity&lt;TagGroup&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TagGroup> getGroupByNameWithHttpInfo(String projectId, String groupName) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return getGroupByNameRequestCreation(projectId, groupName).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get tag group by name in the given project-space
     * [EXPERIMENTAL] Get tag group by name in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag group.
     * @param projectId project-space to read from.
     * @param groupName name of the group
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getGroupByNameWithResponseSpec(String projectId, String groupName) throws WebClientResponseException {
        return getGroupByNameRequestCreation(projectId, groupName);
    }
    /**
     * [EXPERIMENTAL] Get all tag based groups in the given project-space
     * [EXPERIMENTAL] Get all tag based groups in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Groups.
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return List&lt;TagGroup&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getGroupsRequestCreation(String projectId, String groupType) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getGroups", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "groupType", groupType));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/groups", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get all tag based groups in the given project-space
     * [EXPERIMENTAL] Get all tag based groups in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Groups.
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return List&lt;TagGroup&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<TagGroup> getGroups(String projectId, String groupType) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return getGroupsRequestCreation(projectId, groupType).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] Get all tag based groups in the given project-space
     * [EXPERIMENTAL] Get all tag based groups in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Groups.
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return ResponseEntity&lt;List&lt;TagGroup&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<TagGroup>> getGroupsWithHttpInfo(String projectId, String groupType) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return getGroupsRequestCreation(projectId, groupType).toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get all tag based groups in the given project-space
     * [EXPERIMENTAL] Get all tag based groups in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Groups.
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getGroupsWithResponseSpec(String projectId, String groupType) throws WebClientResponseException {
        return getGroupsRequestCreation(projectId, groupType);
    }
    /**
     * [EXPERIMENTAL] Get tag groups by type in the given project-space
     * [EXPERIMENTAL] Get tag groups by type in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag groups.
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return List&lt;TagGroup&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getGroupsByTypeRequestCreation(String projectId, String groupType) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getGroupsByType", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'groupType' is set
        if (groupType == null) {
            throw new WebClientResponseException("Missing the required parameter 'groupType' when calling getGroupsByType", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("groupType", groupType);

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

        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/groups/type/{groupType}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get tag groups by type in the given project-space
     * [EXPERIMENTAL] Get tag groups by type in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag groups.
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return List&lt;TagGroup&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<TagGroup> getGroupsByType(String projectId, String groupType) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return getGroupsByTypeRequestCreation(projectId, groupType).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] Get tag groups by type in the given project-space
     * [EXPERIMENTAL] Get tag groups by type in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag groups.
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return ResponseEntity&lt;List&lt;TagGroup&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<TagGroup>> getGroupsByTypeWithHttpInfo(String projectId, String groupType) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return getGroupsByTypeRequestCreation(projectId, groupType).toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get tag groups by type in the given project-space
     * [EXPERIMENTAL] Get tag groups by type in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag groups.
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getGroupsByTypeWithResponseSpec(String projectId, String groupType) throws WebClientResponseException {
        return getGroupsByTypeRequestCreation(projectId, groupType);
    }
    /**
     * [EXPERIMENTAL] Get tag definition by its name in the given project-space
     * [EXPERIMENTAL] Get tag definition by its name in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag definition.
     * @param projectId project-space to read from.
     * @param tagName name of the tag
     * @return TagDefinition
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getTagRequestCreation(String projectId, String tagName) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getTag", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tagName' is set
        if (tagName == null) {
            throw new WebClientResponseException("Missing the required parameter 'tagName' when calling getTag", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("tagName", tagName);

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

        ParameterizedTypeReference<TagDefinition> localVarReturnType = new ParameterizedTypeReference<TagDefinition>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/tags/{tagName}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get tag definition by its name in the given project-space
     * [EXPERIMENTAL] Get tag definition by its name in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag definition.
     * @param projectId project-space to read from.
     * @param tagName name of the tag
     * @return TagDefinition
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TagDefinition getTag(String projectId, String tagName) throws WebClientResponseException {
        ParameterizedTypeReference<TagDefinition> localVarReturnType = new ParameterizedTypeReference<TagDefinition>() {};
        return getTagRequestCreation(projectId, tagName).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get tag definition by its name in the given project-space
     * [EXPERIMENTAL] Get tag definition by its name in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag definition.
     * @param projectId project-space to read from.
     * @param tagName name of the tag
     * @return ResponseEntity&lt;TagDefinition&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TagDefinition> getTagWithHttpInfo(String projectId, String tagName) throws WebClientResponseException {
        ParameterizedTypeReference<TagDefinition> localVarReturnType = new ParameterizedTypeReference<TagDefinition>() {};
        return getTagRequestCreation(projectId, tagName).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get tag definition by its name in the given project-space
     * [EXPERIMENTAL] Get tag definition by its name in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag definition.
     * @param projectId project-space to read from.
     * @param tagName name of the tag
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getTagWithResponseSpec(String projectId, String tagName) throws WebClientResponseException {
        return getTagRequestCreation(projectId, tagName);
    }
    /**
     * [EXPERIMENTAL] Get all tag definitions in the given project-space
     * [EXPERIMENTAL] Get all tag definitions in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag definitions.
     * @param projectId project-space to read from.
     * @param tagScope scope of the tag (optional)
     * @return List&lt;TagDefinition&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getTagsRequestCreation(String projectId, String tagScope) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "tagScope", tagScope));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<TagDefinition> localVarReturnType = new ParameterizedTypeReference<TagDefinition>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/tags", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get all tag definitions in the given project-space
     * [EXPERIMENTAL] Get all tag definitions in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag definitions.
     * @param projectId project-space to read from.
     * @param tagScope scope of the tag (optional)
     * @return List&lt;TagDefinition&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<TagDefinition> getTags(String projectId, String tagScope) throws WebClientResponseException {
        ParameterizedTypeReference<TagDefinition> localVarReturnType = new ParameterizedTypeReference<TagDefinition>() {};
        return getTagsRequestCreation(projectId, tagScope).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] Get all tag definitions in the given project-space
     * [EXPERIMENTAL] Get all tag definitions in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag definitions.
     * @param projectId project-space to read from.
     * @param tagScope scope of the tag (optional)
     * @return ResponseEntity&lt;List&lt;TagDefinition&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<TagDefinition>> getTagsWithHttpInfo(String projectId, String tagScope) throws WebClientResponseException {
        ParameterizedTypeReference<TagDefinition> localVarReturnType = new ParameterizedTypeReference<TagDefinition>() {};
        return getTagsRequestCreation(projectId, tagScope).toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get all tag definitions in the given project-space
     * [EXPERIMENTAL] Get all tag definitions in the given project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Tag definitions.
     * @param projectId project-space to read from.
     * @param tagScope scope of the tag (optional)
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getTagsWithResponseSpec(String projectId, String tagScope) throws WebClientResponseException {
        return getTagsRequestCreation(projectId, tagScope);
    }
}
