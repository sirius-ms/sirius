package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.AlignedFeatureOptField;
import io.sirius.ms.sdk.model.Compound;
import io.sirius.ms.sdk.model.CompoundImport;
import io.sirius.ms.sdk.model.CompoundOptField;
import io.sirius.ms.sdk.model.InstrumentProfile;
import io.sirius.ms.sdk.model.PagedModelCompound;
import io.sirius.ms.sdk.model.QuantMeasure;
import io.sirius.ms.sdk.model.QuantTableExperimental;
import io.sirius.ms.sdk.model.Tag;
import io.sirius.ms.sdk.model.TagGroup;
import io.sirius.ms.sdk.model.TraceSetExperimental;

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
public class CompoundsApi {
    private ApiClient apiClient;

    public CompoundsApi() {
        this(new ApiClient());
    }

    @Autowired
    public CompoundsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Import Compounds and its contained features.
     * Import Compounds and its contained features. Compounds and Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Compounds that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param compoundImport the compound data to be imported
     * @param profile profile describing the instrument used to measure the data. Used to merge spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @param optFieldsFeatures set of optional fields of the nested features to be included. Use &#39;none&#39; to override defaults.
     * @return List&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addCompoundsRequestCreation(String projectId, List<CompoundImport> compoundImport, InstrumentProfile profile, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        Object postBody = compoundImport;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addCompounds", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundImport' is set
        if (compoundImport == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundImport' when calling addCompounds", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "profile", profile));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFieldsFeatures", optFieldsFeatures));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Import Compounds and its contained features.
     * Import Compounds and its contained features. Compounds and Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Compounds that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param compoundImport the compound data to be imported
     * @param profile profile describing the instrument used to measure the data. Used to merge spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @param optFieldsFeatures set of optional fields of the nested features to be included. Use &#39;none&#39; to override defaults.
     * @return List&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Compound> addCompounds(String projectId, List<CompoundImport> compoundImport, InstrumentProfile profile, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return addCompoundsRequestCreation(projectId, compoundImport, profile, optFields, optFieldsFeatures).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Import Compounds and its contained features.
     * Import Compounds and its contained features. Compounds and Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Compounds that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param compoundImport the compound data to be imported
     * @param profile profile describing the instrument used to measure the data. Used to merge spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @param optFieldsFeatures set of optional fields of the nested features to be included. Use &#39;none&#39; to override defaults.
     * @return ResponseEntity&lt;List&lt;Compound&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Compound>> addCompoundsWithHttpInfo(String projectId, List<CompoundImport> compoundImport, InstrumentProfile profile, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return addCompoundsRequestCreation(projectId, compoundImport, profile, optFields, optFieldsFeatures).toEntityList(localVarReturnType).block();
    }

    /**
     * Import Compounds and its contained features.
     * Import Compounds and its contained features. Compounds and Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Compounds that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param compoundImport the compound data to be imported
     * @param profile profile describing the instrument used to measure the data. Used to merge spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @param optFieldsFeatures set of optional fields of the nested features to be included. Use &#39;none&#39; to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addCompoundsWithResponseSpec(String projectId, List<CompoundImport> compoundImport, InstrumentProfile profile, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return addCompoundsRequestCreation(projectId, compoundImport, profile, optFields, optFieldsFeatures);
    }
    /**
     * **EXPERIMENTAL** Group tags in the project
     * **EXPERIMENTAL** Group tags in the project. The group name must not exist in the project.   &lt;p&gt;  See &lt;code&gt;/tagged&lt;/code&gt; for filter syntax.  &lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - the tag group that was added
     * @param projectId project-space to add to.
     * @param groupName name of the new group
     * @param filter filter query to create the group
     * @param type type of the group
     * @return TagGroup
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addGroup1RequestCreation(String projectId, String groupName, String filter, String type) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addGroup1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'groupName' is set
        if (groupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'groupName' when calling addGroup1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'filter' is set
        if (filter == null) {
            throw new WebClientResponseException("Missing the required parameter 'filter' when calling addGroup1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'type' is set
        if (type == null) {
            throw new WebClientResponseException("Missing the required parameter 'type' when calling addGroup1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/groups/{groupName}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * **EXPERIMENTAL** Group tags in the project
     * **EXPERIMENTAL** Group tags in the project. The group name must not exist in the project.   &lt;p&gt;  See &lt;code&gt;/tagged&lt;/code&gt; for filter syntax.  &lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - the tag group that was added
     * @param projectId project-space to add to.
     * @param groupName name of the new group
     * @param filter filter query to create the group
     * @param type type of the group
     * @return TagGroup
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TagGroup addGroup1(String projectId, String groupName, String filter, String type) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return addGroup1RequestCreation(projectId, groupName, filter, type).bodyToMono(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Group tags in the project
     * **EXPERIMENTAL** Group tags in the project. The group name must not exist in the project.   &lt;p&gt;  See &lt;code&gt;/tagged&lt;/code&gt; for filter syntax.  &lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - the tag group that was added
     * @param projectId project-space to add to.
     * @param groupName name of the new group
     * @param filter filter query to create the group
     * @param type type of the group
     * @return ResponseEntity&lt;TagGroup&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TagGroup> addGroup1WithHttpInfo(String projectId, String groupName, String filter, String type) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return addGroup1RequestCreation(projectId, groupName, filter, type).toEntity(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Group tags in the project
     * **EXPERIMENTAL** Group tags in the project. The group name must not exist in the project.   &lt;p&gt;  See &lt;code&gt;/tagged&lt;/code&gt; for filter syntax.  &lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - the tag group that was added
     * @param projectId project-space to add to.
     * @param groupName name of the new group
     * @param filter filter query to create the group
     * @param type type of the group
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addGroup1WithResponseSpec(String projectId, String groupName, String filter, String type) throws WebClientResponseException {
        return addGroup1RequestCreation(projectId, groupName, filter, type);
    }
    /**
     * Tags with the same category name will be overwritten
     * Tags with the same category name will be overwritten.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param compoundId compound (group of ion identities) to add tags to.
     * @param tag tags to add.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addTags1RequestCreation(String projectId, String compoundId, List<Tag> tag) throws WebClientResponseException {
        Object postBody = tag;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addTags1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling addTags1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tag' is set
        if (tag == null) {
            throw new WebClientResponseException("Missing the required parameter 'tag' when calling addTags1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);

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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/tags/{compoundId}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Tags with the same category name will be overwritten
     * Tags with the same category name will be overwritten.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param compoundId compound (group of ion identities) to add tags to.
     * @param tag tags to add.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Tag> addTags1(String projectId, String compoundId, List<Tag> tag) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return addTags1RequestCreation(projectId, compoundId, tag).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Tags with the same category name will be overwritten
     * Tags with the same category name will be overwritten.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param compoundId compound (group of ion identities) to add tags to.
     * @param tag tags to add.
     * @return ResponseEntity&lt;List&lt;Tag&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Tag>> addTags1WithHttpInfo(String projectId, String compoundId, List<Tag> tag) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return addTags1RequestCreation(projectId, compoundId, tag).toEntityList(localVarReturnType).block();
    }

    /**
     * Tags with the same category name will be overwritten
     * Tags with the same category name will be overwritten.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param compoundId compound (group of ion identities) to add tags to.
     * @param tag tags to add.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addTags1WithResponseSpec(String projectId, String compoundId, List<Tag> tag) throws WebClientResponseException {
        return addTags1RequestCreation(projectId, compoundId, tag);
    }
    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteCompoundRequestCreation(String projectId, String compoundId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteCompound", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling deleteCompound", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);

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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/{compoundId}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteCompound(String projectId, String compoundId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteCompoundRequestCreation(projectId, compoundId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteCompoundWithHttpInfo(String projectId, String compoundId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteCompoundRequestCreation(projectId, compoundId).toEntity(localVarReturnType).block();
    }

    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteCompoundWithResponseSpec(String projectId, String compoundId) throws WebClientResponseException {
        return deleteCompoundRequestCreation(projectId, compoundId);
    }
    /**
     * **EXPERIMENTAL** Delete tag groups with the given name from the specified project-space
     * **EXPERIMENTAL** Delete tag groups with the given name from the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param groupName name of group to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteGroup1RequestCreation(String projectId, String groupName) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteGroup1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'groupName' is set
        if (groupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'groupName' when calling deleteGroup1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/groups/{groupName}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * **EXPERIMENTAL** Delete tag groups with the given name from the specified project-space
     * **EXPERIMENTAL** Delete tag groups with the given name from the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param groupName name of group to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteGroup1(String projectId, String groupName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteGroup1RequestCreation(projectId, groupName).bodyToMono(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Delete tag groups with the given name from the specified project-space
     * **EXPERIMENTAL** Delete tag groups with the given name from the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param groupName name of group to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteGroup1WithHttpInfo(String projectId, String groupName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteGroup1RequestCreation(projectId, groupName).toEntity(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Delete tag groups with the given name from the specified project-space
     * **EXPERIMENTAL** Delete tag groups with the given name from the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param groupName name of group to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteGroup1WithResponseSpec(String projectId, String groupName) throws WebClientResponseException {
        return deleteGroup1RequestCreation(projectId, groupName);
    }
    /**
     * **EXPERIMENTAL** Delete tag with the given category from the compound (group of ion identities) with the specified ID in the specified project-space
     * **EXPERIMENTAL** Delete tag with the given category from the compound (group of ion identities) with the specified ID in the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId compound (group of ion identities) to delete tag from.
     * @param categoryName category name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteTags1RequestCreation(String projectId, String compoundId, String categoryName) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteTags1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling deleteTags1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'categoryName' is set
        if (categoryName == null) {
            throw new WebClientResponseException("Missing the required parameter 'categoryName' when calling deleteTags1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/tags/{compoundId}/{categoryName}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * **EXPERIMENTAL** Delete tag with the given category from the compound (group of ion identities) with the specified ID in the specified project-space
     * **EXPERIMENTAL** Delete tag with the given category from the compound (group of ion identities) with the specified ID in the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId compound (group of ion identities) to delete tag from.
     * @param categoryName category name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteTags1(String projectId, String compoundId, String categoryName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteTags1RequestCreation(projectId, compoundId, categoryName).bodyToMono(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Delete tag with the given category from the compound (group of ion identities) with the specified ID in the specified project-space
     * **EXPERIMENTAL** Delete tag with the given category from the compound (group of ion identities) with the specified ID in the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId compound (group of ion identities) to delete tag from.
     * @param categoryName category name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteTags1WithHttpInfo(String projectId, String compoundId, String categoryName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteTags1RequestCreation(projectId, compoundId, categoryName).toEntity(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Delete tag with the given category from the compound (group of ion identities) with the specified ID in the specified project-space
     * **EXPERIMENTAL** Delete tag with the given category from the compound (group of ion identities) with the specified ID in the specified project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId compound (group of ion identities) to delete tag from.
     * @param categoryName category name of the tag to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteTags1WithResponseSpec(String projectId, String compoundId, String categoryName) throws WebClientResponseException {
        return deleteTags1RequestCreation(projectId, compoundId, categoryName);
    }
    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return Compound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundRequestCreation(String projectId, String compoundId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompound", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling getCompound", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFieldsFeatures", optFieldsFeatures));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/{compoundId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return Compound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Compound getCompound(String projectId, String compoundId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundRequestCreation(projectId, compoundId, optFields, optFieldsFeatures).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseEntity&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Compound> getCompoundWithHttpInfo(String projectId, String compoundId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundRequestCreation(projectId, compoundId, optFields, optFieldsFeatures).toEntity(localVarReturnType).block();
    }

    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundWithResponseSpec(String projectId, String compoundId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return getCompoundRequestCreation(projectId, compoundId, optFields, optFieldsFeatures);
    }
    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Returns the traces of the given compound. A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.
     * <p><b>200</b> - Traces of the given compound.
     * @param projectId project-space to read from.
     * @param compoundId compound which intensities should be read out
     * @param featureId The featureId parameter
     * @return TraceSetExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundTracesExperimentalRequestCreation(String projectId, String compoundId, String featureId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundTracesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling getCompoundTracesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "featureId", featureId));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/{compoundId}/traces", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Returns the traces of the given compound. A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.
     * <p><b>200</b> - Traces of the given compound.
     * @param projectId project-space to read from.
     * @param compoundId compound which intensities should be read out
     * @param featureId The featureId parameter
     * @return TraceSetExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TraceSetExperimental getCompoundTracesExperimental(String projectId, String compoundId, String featureId) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return getCompoundTracesExperimentalRequestCreation(projectId, compoundId, featureId).bodyToMono(localVarReturnType).block();
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Returns the traces of the given compound. A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.
     * <p><b>200</b> - Traces of the given compound.
     * @param projectId project-space to read from.
     * @param compoundId compound which intensities should be read out
     * @param featureId The featureId parameter
     * @return ResponseEntity&lt;TraceSetExperimental&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TraceSetExperimental> getCompoundTracesExperimentalWithHttpInfo(String projectId, String compoundId, String featureId) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return getCompoundTracesExperimentalRequestCreation(projectId, compoundId, featureId).toEntity(localVarReturnType).block();
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Returns the traces of the given compound. A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.
     * <p><b>200</b> - Traces of the given compound.
     * @param projectId project-space to read from.
     * @param compoundId compound which intensities should be read out
     * @param featureId The featureId parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundTracesExperimentalWithResponseSpec(String projectId, String compoundId, String featureId) throws WebClientResponseException {
        return getCompoundTracesExperimentalRequestCreation(projectId, compoundId, featureId);
    }
    /**
     * List of all available compounds (group of ion identities) in the given project-space.
     * List of all available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return List&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundsRequestCreation(String projectId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompounds", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFieldsFeatures", optFieldsFeatures));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List of all available compounds (group of ion identities) in the given project-space.
     * List of all available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return List&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Compound> getCompounds(String projectId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundsRequestCreation(projectId, optFields, optFieldsFeatures).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List of all available compounds (group of ion identities) in the given project-space.
     * List of all available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseEntity&lt;List&lt;Compound&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Compound>> getCompoundsWithHttpInfo(String projectId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundsRequestCreation(projectId, optFields, optFieldsFeatures).toEntityList(localVarReturnType).block();
    }

    /**
     * List of all available compounds (group of ion identities) in the given project-space.
     * List of all available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundsWithResponseSpec(String projectId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return getCompoundsRequestCreation(projectId, optFields, optFieldsFeatures);
    }
    /**
     * Page of available compounds (group of ion identities) in the given project-space.
     * Page of available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return PagedModelCompound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundsPagedRequestCreation(String projectId, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundsPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFieldsFeatures", optFieldsFeatures));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Page of available compounds (group of ion identities) in the given project-space.
     * Page of available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return PagedModelCompound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelCompound getCompoundsPaged(String projectId, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return getCompoundsPagedRequestCreation(projectId, page, size, sort, optFields, optFieldsFeatures).bodyToMono(localVarReturnType).block();
    }

    /**
     * Page of available compounds (group of ion identities) in the given project-space.
     * Page of available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseEntity&lt;PagedModelCompound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelCompound> getCompoundsPagedWithHttpInfo(String projectId, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return getCompoundsPagedRequestCreation(projectId, page, size, sort, optFields, optFieldsFeatures).toEntity(localVarReturnType).block();
    }

    /**
     * Page of available compounds (group of ion identities) in the given project-space.
     * Page of available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundsPagedWithResponseSpec(String projectId, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return getCompoundsPagedRequestCreation(projectId, page, size, sort, optFields, optFieldsFeatures);
    }
    /**
     * **EXPERIMENTAL** Get tag group by name in the given project-space
     * **EXPERIMENTAL** Get tag group by name in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag group.
     * @param projectId project-space to read from.
     * @param groupName name of the group
     * @return TagGroup
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getGroupByName1RequestCreation(String projectId, String groupName) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getGroupByName1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'groupName' is set
        if (groupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'groupName' when calling getGroupByName1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/groups/{groupName}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * **EXPERIMENTAL** Get tag group by name in the given project-space
     * **EXPERIMENTAL** Get tag group by name in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag group.
     * @param projectId project-space to read from.
     * @param groupName name of the group
     * @return TagGroup
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TagGroup getGroupByName1(String projectId, String groupName) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return getGroupByName1RequestCreation(projectId, groupName).bodyToMono(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Get tag group by name in the given project-space
     * **EXPERIMENTAL** Get tag group by name in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag group.
     * @param projectId project-space to read from.
     * @param groupName name of the group
     * @return ResponseEntity&lt;TagGroup&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TagGroup> getGroupByName1WithHttpInfo(String projectId, String groupName) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return getGroupByName1RequestCreation(projectId, groupName).toEntity(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Get tag group by name in the given project-space
     * **EXPERIMENTAL** Get tag group by name in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag group.
     * @param projectId project-space to read from.
     * @param groupName name of the group
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getGroupByName1WithResponseSpec(String projectId, String groupName) throws WebClientResponseException {
        return getGroupByName1RequestCreation(projectId, groupName);
    }
    /**
     * **EXPERIMENTAL** Get all tag category groups in the given project-space
     * **EXPERIMENTAL** Get all tag category groups in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag category groups.
     * @param projectId project-space to read from.
     * @return List&lt;TagGroup&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getGroups1RequestCreation(String projectId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getGroups1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/groups", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * **EXPERIMENTAL** Get all tag category groups in the given project-space
     * **EXPERIMENTAL** Get all tag category groups in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag category groups.
     * @param projectId project-space to read from.
     * @return List&lt;TagGroup&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<TagGroup> getGroups1(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return getGroups1RequestCreation(projectId).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * **EXPERIMENTAL** Get all tag category groups in the given project-space
     * **EXPERIMENTAL** Get all tag category groups in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag category groups.
     * @param projectId project-space to read from.
     * @return ResponseEntity&lt;List&lt;TagGroup&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<TagGroup>> getGroups1WithHttpInfo(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return getGroups1RequestCreation(projectId).toEntityList(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Get all tag category groups in the given project-space
     * **EXPERIMENTAL** Get all tag category groups in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag category groups.
     * @param projectId project-space to read from.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getGroups1WithResponseSpec(String projectId) throws WebClientResponseException {
        return getGroups1RequestCreation(projectId);
    }
    /**
     * **EXPERIMENTAL** Get tag groups by type in the given project-space
     * **EXPERIMENTAL** Get tag groups by type in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag groups.
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return List&lt;TagGroup&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getGroupsByType1RequestCreation(String projectId, String groupType) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getGroupsByType1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'groupType' is set
        if (groupType == null) {
            throw new WebClientResponseException("Missing the required parameter 'groupType' when calling getGroupsByType1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/groups/type/{groupType}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * **EXPERIMENTAL** Get tag groups by type in the given project-space
     * **EXPERIMENTAL** Get tag groups by type in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag groups.
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return List&lt;TagGroup&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<TagGroup> getGroupsByType1(String projectId, String groupType) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return getGroupsByType1RequestCreation(projectId, groupType).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * **EXPERIMENTAL** Get tag groups by type in the given project-space
     * **EXPERIMENTAL** Get tag groups by type in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag groups.
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return ResponseEntity&lt;List&lt;TagGroup&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<TagGroup>> getGroupsByType1WithHttpInfo(String projectId, String groupType) throws WebClientResponseException {
        ParameterizedTypeReference<TagGroup> localVarReturnType = new ParameterizedTypeReference<TagGroup>() {};
        return getGroupsByType1RequestCreation(projectId, groupType).toEntityList(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Get tag groups by type in the given project-space
     * **EXPERIMENTAL** Get tag groups by type in the given project-space.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - Tag groups.
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getGroupsByType1WithResponseSpec(String projectId, String groupType) throws WebClientResponseException {
        return getGroupsByType1RequestCreation(projectId, groupType);
    }
    /**
     * Returns the full quantification table.
     * Returns the full quantification table. The quantification table contains a quantification of the features within all  runs they are contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param type quantification type.
     * @return QuantTableExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getQuantificationRequestCreation(String projectId, QuantMeasure type) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getQuantification", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "type", type));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<QuantTableExperimental> localVarReturnType = new ParameterizedTypeReference<QuantTableExperimental>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/quantification", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Returns the full quantification table.
     * Returns the full quantification table. The quantification table contains a quantification of the features within all  runs they are contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param type quantification type.
     * @return QuantTableExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public QuantTableExperimental getQuantification(String projectId, QuantMeasure type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTableExperimental> localVarReturnType = new ParameterizedTypeReference<QuantTableExperimental>() {};
        return getQuantificationRequestCreation(projectId, type).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns the full quantification table.
     * Returns the full quantification table. The quantification table contains a quantification of the features within all  runs they are contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param type quantification type.
     * @return ResponseEntity&lt;QuantTableExperimental&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QuantTableExperimental> getQuantificationWithHttpInfo(String projectId, QuantMeasure type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTableExperimental> localVarReturnType = new ParameterizedTypeReference<QuantTableExperimental>() {};
        return getQuantificationRequestCreation(projectId, type).toEntity(localVarReturnType).block();
    }

    /**
     * Returns the full quantification table.
     * Returns the full quantification table. The quantification table contains a quantification of the features within all  runs they are contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param type quantification type.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getQuantificationWithResponseSpec(String projectId, QuantMeasure type) throws WebClientResponseException {
        return getQuantificationRequestCreation(projectId, type);
    }
    /**
     * Returns a single quantification table row for the given feature.
     * Returns a single quantification table row for the given feature. The quantification table contains a quantification of the feature within all  samples it is contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return QuantTableExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getQuantificationRowRequestCreation(String projectId, String compoundId, QuantMeasure type) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getQuantificationRow", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling getQuantificationRow", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "type", type));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<QuantTableExperimental> localVarReturnType = new ParameterizedTypeReference<QuantTableExperimental>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/{compoundId}/quantification", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Returns a single quantification table row for the given feature.
     * Returns a single quantification table row for the given feature. The quantification table contains a quantification of the feature within all  samples it is contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return QuantTableExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public QuantTableExperimental getQuantificationRow(String projectId, String compoundId, QuantMeasure type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTableExperimental> localVarReturnType = new ParameterizedTypeReference<QuantTableExperimental>() {};
        return getQuantificationRowRequestCreation(projectId, compoundId, type).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns a single quantification table row for the given feature.
     * Returns a single quantification table row for the given feature. The quantification table contains a quantification of the feature within all  samples it is contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return ResponseEntity&lt;QuantTableExperimental&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QuantTableExperimental> getQuantificationRowWithHttpInfo(String projectId, String compoundId, QuantMeasure type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTableExperimental> localVarReturnType = new ParameterizedTypeReference<QuantTableExperimental>() {};
        return getQuantificationRowRequestCreation(projectId, compoundId, type).toEntity(localVarReturnType).block();
    }

    /**
     * Returns a single quantification table row for the given feature.
     * Returns a single quantification table row for the given feature. The quantification table contains a quantification of the feature within all  samples it is contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getQuantificationRowWithResponseSpec(String projectId, String compoundId, QuantMeasure type) throws WebClientResponseException {
        return getQuantificationRowRequestCreation(projectId, compoundId, type);
    }
    /**
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag group
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag group.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project-space to delete from.
     * @param group tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelCompound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec objectsByGroup1RequestCreation(String projectId, String group, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling objectsByGroup1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'group' is set
        if (group == null) {
            throw new WebClientResponseException("Missing the required parameter 'group' when calling objectsByGroup1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/grouped", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag group
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag group.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project-space to delete from.
     * @param group tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelCompound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelCompound objectsByGroup1(String projectId, String group, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return objectsByGroup1RequestCreation(projectId, group, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag group
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag group.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project-space to delete from.
     * @param group tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PagedModelCompound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelCompound> objectsByGroup1WithHttpInfo(String projectId, String group, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return objectsByGroup1RequestCreation(projectId, group, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag group
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag group.   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project-space to delete from.
     * @param group tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec objectsByGroup1WithResponseSpec(String projectId, String group, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        return objectsByGroup1RequestCreation(projectId, group, page, size, sort, optFields);
    }
    /**
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name. Possible field names are:&lt;/p&gt;   &lt;ul&gt;    &lt;li&gt;&lt;strong&gt;category&lt;/strong&gt; - category name&lt;/li&gt;    &lt;li&gt;&lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;category:my_category&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;text:&amp;quot;new york&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;text:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;integer&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;integer:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;(category:hello || category:world) &amp;amp;&amp;amp; text:&amp;quot;new york&amp;quot; AND text:/[mb]oat/ AND integer:[1 TO *] OR real&amp;lt;&#x3D;3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date&amp;lt;2022-01-01 OR time:12\\:00\\:00 OR time:[12\\:00\\:00 TO 14\\:00\\:00] OR time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project space to get compounds (group of ion identities) from.
     * @param filter tag filter.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelCompound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec objectsByTag1RequestCreation(String projectId, String filter, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling objectsByTag1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/tagged", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name. Possible field names are:&lt;/p&gt;   &lt;ul&gt;    &lt;li&gt;&lt;strong&gt;category&lt;/strong&gt; - category name&lt;/li&gt;    &lt;li&gt;&lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;category:my_category&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;text:&amp;quot;new york&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;text:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;integer&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;integer:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;(category:hello || category:world) &amp;amp;&amp;amp; text:&amp;quot;new york&amp;quot; AND text:/[mb]oat/ AND integer:[1 TO *] OR real&amp;lt;&#x3D;3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date&amp;lt;2022-01-01 OR time:12\\:00\\:00 OR time:[12\\:00\\:00 TO 14\\:00\\:00] OR time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project space to get compounds (group of ion identities) from.
     * @param filter tag filter.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelCompound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelCompound objectsByTag1(String projectId, String filter, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return objectsByTag1RequestCreation(projectId, filter, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name. Possible field names are:&lt;/p&gt;   &lt;ul&gt;    &lt;li&gt;&lt;strong&gt;category&lt;/strong&gt; - category name&lt;/li&gt;    &lt;li&gt;&lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;category:my_category&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;text:&amp;quot;new york&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;text:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;integer&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;integer:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;(category:hello || category:world) &amp;amp;&amp;amp; text:&amp;quot;new york&amp;quot; AND text:/[mb]oat/ AND integer:[1 TO *] OR real&amp;lt;&#x3D;3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date&amp;lt;2022-01-01 OR time:12\\:00\\:00 OR time:[12\\:00\\:00 TO 14\\:00\\:00] OR time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project space to get compounds (group of ion identities) from.
     * @param filter tag filter.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PagedModelCompound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelCompound> objectsByTag1WithHttpInfo(String projectId, String filter, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return objectsByTag1RequestCreation(projectId, filter, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag
     * **EXPERIMENTAL** Get compounds (group of ion identities) by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name. Possible field names are:&lt;/p&gt;   &lt;ul&gt;    &lt;li&gt;&lt;strong&gt;category&lt;/strong&gt; - category name&lt;/li&gt;    &lt;li&gt;&lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;category:my_category&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;text:&amp;quot;new york&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;text:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;integer&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;integer:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;(category:hello || category:world) &amp;amp;&amp;amp; text:&amp;quot;new york&amp;quot; AND text:/[mb]oat/ AND integer:[1 TO *] OR real&amp;lt;&#x3D;3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date&amp;lt;2022-01-01 OR time:12\\:00\\:00 OR time:[12\\:00\\:00 TO 14\\:00\\:00] OR time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   &lt;p&gt;This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.&lt;/p&gt;
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project space to get compounds (group of ion identities) from.
     * @param filter tag filter.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec objectsByTag1WithResponseSpec(String projectId, String filter, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        return objectsByTag1RequestCreation(projectId, filter, page, size, sort, optFields);
    }
}
