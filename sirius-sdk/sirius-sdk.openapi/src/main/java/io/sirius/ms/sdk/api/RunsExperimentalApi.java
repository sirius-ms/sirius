package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.PageRun;
import io.sirius.ms.sdk.model.Run;
import io.sirius.ms.sdk.model.RunOptField;
import io.sirius.ms.sdk.model.Tag;
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
public class RunsExperimentalApi {
    private ApiClient apiClient;

    public RunsExperimentalApi() {
        this(new ApiClient());
    }

    @Autowired
    public RunsExperimentalApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Group tags in the project
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Group tags in the project. The group name must not exist in the project.   &lt;p&gt;  See &lt;code&gt;/tagged&lt;/code&gt; for filter syntax.  &lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/groups/{groupName}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Group tags in the project
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Group tags in the project. The group name must not exist in the project.   &lt;p&gt;  See &lt;code&gt;/tagged&lt;/code&gt; for filter syntax.  &lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Group tags in the project
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Group tags in the project. The group name must not exist in the project.   &lt;p&gt;  See &lt;code&gt;/tagged&lt;/code&gt; for filter syntax.  &lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Group tags in the project
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Group tags in the project. The group name must not exist in the project.   &lt;p&gt;  See &lt;code&gt;/tagged&lt;/code&gt; for filter syntax.  &lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Add tags to an object in the project
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Add tags to an object in the project. Tags with the same category name will be overwritten.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param objectId object to tag.
     * @param tag tags to add.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addTagsRequestCreation(String projectId, String objectId, List<Tag> tag) throws WebClientResponseException {
        Object postBody = tag;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'objectId' is set
        if (objectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'objectId' when calling addTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tag' is set
        if (tag == null) {
            throw new WebClientResponseException("Missing the required parameter 'tag' when calling addTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/tags/{objectId}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Add tags to an object in the project
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Add tags to an object in the project. Tags with the same category name will be overwritten.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param objectId object to tag.
     * @param tag tags to add.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Tag> addTags(String projectId, String objectId, List<Tag> tag) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return addTagsRequestCreation(projectId, objectId, tag).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Add tags to an object in the project
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Add tags to an object in the project. Tags with the same category name will be overwritten.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param objectId object to tag.
     * @param tag tags to add.
     * @return ResponseEntity&lt;List&lt;Tag&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Tag>> addTagsWithHttpInfo(String projectId, String objectId, List<Tag> tag) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return addTagsRequestCreation(projectId, objectId, tag).toEntityList(localVarReturnType).block();
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Add tags to an object in the project
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Add tags to an object in the project. Tags with the same category name will be overwritten.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param objectId object to tag.
     * @param tag tags to add.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addTagsWithResponseSpec(String projectId, String objectId, List<Tag> tag) throws WebClientResponseException {
        return addTagsRequestCreation(projectId, objectId, tag);
    }
    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag groups with the given name from the specified project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag groups with the given name from the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/groups/{groupName}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag groups with the given name from the specified project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag groups with the given name from the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag groups with the given name from the specified project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag groups with the given name from the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag groups with the given name from the specified project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag groups with the given name from the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag with the given category from the object with the specified ID in the specified project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag with the given category from the object with the specified ID in the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param objectId object to delete tag from.
     * @param categoryName category name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteTagsRequestCreation(String projectId, String objectId, String categoryName) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'objectId' is set
        if (objectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'objectId' when calling deleteTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'categoryName' is set
        if (categoryName == null) {
            throw new WebClientResponseException("Missing the required parameter 'categoryName' when calling deleteTags", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("objectId", objectId);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/tags/{objectId}/{categoryName}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag with the given category from the object with the specified ID in the specified project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag with the given category from the object with the specified ID in the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param objectId object to delete tag from.
     * @param categoryName category name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteTags(String projectId, String objectId, String categoryName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteTagsRequestCreation(projectId, objectId, categoryName).bodyToMono(localVarReturnType).block();
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag with the given category from the object with the specified ID in the specified project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag with the given category from the object with the specified ID in the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param objectId object to delete tag from.
     * @param categoryName category name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteTagsWithHttpInfo(String projectId, String objectId, String categoryName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteTagsRequestCreation(projectId, objectId, categoryName).toEntity(localVarReturnType).block();
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag with the given category from the object with the specified ID in the specified project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Delete tag with the given category from the object with the specified ID in the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param objectId object to delete tag from.
     * @param categoryName category name of the tag to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteTagsWithResponseSpec(String projectId, String objectId, String categoryName) throws WebClientResponseException {
        return deleteTagsRequestCreation(projectId, objectId, categoryName);
    }
    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag group by name in the given project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag group by name in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/groups/{groupName}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag group by name in the given project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag group by name in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag group by name in the given project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag group by name in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag group by name in the given project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag group by name in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get all tag category groups in the given project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get all tag category groups in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag category groups.
     * @param projectId project-space to read from.
     * @return List&lt;TagGroup&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getGroupsRequestCreation(String projectId) throws WebClientResponseException {
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

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/groups", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get all tag category groups in the given project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get all tag category groups in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag category groups.
     * @param projectId project-space to read from.
     * @return List&lt;TagGroup&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<TagGroup> getGroups(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return getGroupsRequestCreation(projectId).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get all tag category groups in the given project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get all tag category groups in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag category groups.
     * @param projectId project-space to read from.
     * @return ResponseEntity&lt;List&lt;TagGroup&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<TagGroup>> getGroupsWithHttpInfo(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return getGroupsRequestCreation(projectId).toEntityList(localVarReturnType).block();
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get all tag category groups in the given project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get all tag category groups in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag category groups.
     * @param projectId project-space to read from.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getGroupsWithResponseSpec(String projectId) throws WebClientResponseException {
        return getGroupsRequestCreation(projectId);
    }
    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag groups by type in the given project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag groups by type in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/groups/type/{groupType}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag groups by type in the given project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag groups by type in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag groups by type in the given project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag groups by type in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag groups by type in the given project-space
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get tag groups by type in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
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
     * @return PageRun
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

        ParameterizedTypeReference<PageRun> localVarReturnType = new ParameterizedTypeReference<PageRun>() {};
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
     * @return PageRun
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageRun getRunsPaged(String projectId, Integer page, Integer size, List<String> sort, List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageRun> localVarReturnType = new ParameterizedTypeReference<PageRun>() {};
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
     * @return ResponseEntity&lt;PageRun&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageRun> getRunsPagedWithHttpInfo(String projectId, Integer page, Integer size, List<String> sort, List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageRun> localVarReturnType = new ParameterizedTypeReference<PageRun>() {};
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
    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag group
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag group.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged objects
     * @param projectId project-space to delete from.
     * @param group tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PageRun
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec objectsByGroupRequestCreation(String projectId, String group, Integer page, Integer size, List<String> sort, List<RunOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling objectsByGroup", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'group' is set
        if (group == null) {
            throw new WebClientResponseException("Missing the required parameter 'group' when calling objectsByGroup", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "group", group));
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

        ParameterizedTypeReference<PageRun> localVarReturnType = new ParameterizedTypeReference<PageRun>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/grouped", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag group
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag group.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged objects
     * @param projectId project-space to delete from.
     * @param group tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PageRun
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageRun objectsByGroup(String projectId, String group, Integer page, Integer size, List<String> sort, List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageRun> localVarReturnType = new ParameterizedTypeReference<PageRun>() {};
        return objectsByGroupRequestCreation(projectId, group, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag group
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag group.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged objects
     * @param projectId project-space to delete from.
     * @param group tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PageRun&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageRun> objectsByGroupWithHttpInfo(String projectId, String group, Integer page, Integer size, List<String> sort, List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageRun> localVarReturnType = new ParameterizedTypeReference<PageRun>() {};
        return objectsByGroupRequestCreation(projectId, group, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag group
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag group.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged objects
     * @param projectId project-space to delete from.
     * @param group tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec objectsByGroupWithResponseSpec(String projectId, String group, Integer page, Integer size, List<String> sort, List<RunOptField> optFields) throws WebClientResponseException {
        return objectsByGroupRequestCreation(projectId, group, page, size, sort, optFields);
    }
    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name. Possible field names are:&lt;/p&gt;   &lt;ul&gt;    &lt;li&gt;&lt;strong&gt;category&lt;/strong&gt; - category name&lt;/li&gt;    &lt;li&gt;&lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;category:my_category&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;text:&amp;quot;new york&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;text:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;integer&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;integer:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;(category:hello || category:world) &amp;amp;&amp;amp; text:&amp;quot;new york&amp;quot; AND text:/[mb]oat/ AND integer:[1 TO *] OR real&amp;lt;&#x3D;3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date&amp;lt;2022-01-01 OR time:12\\:00\\:00 OR time:[12\\:00\\:00 TO 14\\:00\\:00] OR time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged objects
     * @param projectId project space to get objects from.
     * @param filter tag filter.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PageRun
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec objectsByTagRequestCreation(String projectId, String filter, Integer page, Integer size, List<String> sort, List<RunOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling objectsByTag", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "filter", filter));
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

        ParameterizedTypeReference<PageRun> localVarReturnType = new ParameterizedTypeReference<PageRun>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/runs/tagged", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name. Possible field names are:&lt;/p&gt;   &lt;ul&gt;    &lt;li&gt;&lt;strong&gt;category&lt;/strong&gt; - category name&lt;/li&gt;    &lt;li&gt;&lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;category:my_category&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;text:&amp;quot;new york&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;text:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;integer&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;integer:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;(category:hello || category:world) &amp;amp;&amp;amp; text:&amp;quot;new york&amp;quot; AND text:/[mb]oat/ AND integer:[1 TO *] OR real&amp;lt;&#x3D;3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date&amp;lt;2022-01-01 OR time:12\\:00\\:00 OR time:[12\\:00\\:00 TO 14\\:00\\:00] OR time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged objects
     * @param projectId project space to get objects from.
     * @param filter tag filter.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PageRun
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageRun objectsByTag(String projectId, String filter, Integer page, Integer size, List<String> sort, List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageRun> localVarReturnType = new ParameterizedTypeReference<PageRun>() {};
        return objectsByTagRequestCreation(projectId, filter, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name. Possible field names are:&lt;/p&gt;   &lt;ul&gt;    &lt;li&gt;&lt;strong&gt;category&lt;/strong&gt; - category name&lt;/li&gt;    &lt;li&gt;&lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;category:my_category&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;text:&amp;quot;new york&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;text:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;integer&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;integer:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;(category:hello || category:world) &amp;amp;&amp;amp; text:&amp;quot;new york&amp;quot; AND text:/[mb]oat/ AND integer:[1 TO *] OR real&amp;lt;&#x3D;3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date&amp;lt;2022-01-01 OR time:12\\:00\\:00 OR time:[12\\:00\\:00 TO 14\\:00\\:00] OR time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged objects
     * @param projectId project space to get objects from.
     * @param filter tag filter.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PageRun&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageRun> objectsByTagWithHttpInfo(String projectId, String filter, Integer page, Integer size, List<String> sort, List<RunOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PageRun> localVarReturnType = new ParameterizedTypeReference<PageRun>() {};
        return objectsByTagRequestCreation(projectId, filter, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag
     * &lt;strong&gt;(EXPERIMENTAL)&lt;/strong&gt; Get objects by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name. Possible field names are:&lt;/p&gt;   &lt;ul&gt;    &lt;li&gt;&lt;strong&gt;category&lt;/strong&gt; - category name&lt;/li&gt;    &lt;li&gt;&lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;category:my_category&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;text:&amp;quot;new york&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;text:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;integer&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;integer:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;(category:hello || category:world) &amp;amp;&amp;amp; text:&amp;quot;new york&amp;quot; AND text:/[mb]oat/ AND integer:[1 TO *] OR real&amp;lt;&#x3D;3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date&amp;lt;2022-01-01 OR time:12\\:00\\:00 OR time:[12\\:00\\:00 TO 14\\:00\\:00] OR time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged objects
     * @param projectId project space to get objects from.
     * @param filter tag filter.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec objectsByTagWithResponseSpec(String projectId, String filter, Integer page, Integer size, List<String> sort, List<RunOptField> optFields) throws WebClientResponseException {
        return objectsByTagRequestCreation(projectId, filter, page, size, sort, optFields);
    }
}
