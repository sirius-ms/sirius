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
     * [EXPERIMENTAL] Tags with the same name will be overwritten
     * [EXPERIMENTAL] Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param compoundId compound (group of ion identities) to add tags to.
     * @param tag tags to add.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addTagsToCompoundExperimentalRequestCreation(String projectId, String compoundId, List<Tag> tag) throws WebClientResponseException {
        Object postBody = tag;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addTagsToCompoundExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling addTagsToCompoundExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tag' is set
        if (tag == null) {
            throw new WebClientResponseException("Missing the required parameter 'tag' when calling addTagsToCompoundExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
     * [EXPERIMENTAL] Tags with the same name will be overwritten
     * [EXPERIMENTAL] Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param compoundId compound (group of ion identities) to add tags to.
     * @param tag tags to add.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Tag> addTagsToCompoundExperimental(String projectId, String compoundId, List<Tag> tag) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return addTagsToCompoundExperimentalRequestCreation(projectId, compoundId, tag).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] Tags with the same name will be overwritten
     * [EXPERIMENTAL] Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param compoundId compound (group of ion identities) to add tags to.
     * @param tag tags to add.
     * @return ResponseEntity&lt;List&lt;Tag&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Tag>> addTagsToCompoundExperimentalWithHttpInfo(String projectId, String compoundId, List<Tag> tag) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return addTagsToCompoundExperimentalRequestCreation(projectId, compoundId, tag).toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Tags with the same name will be overwritten
     * [EXPERIMENTAL] Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param compoundId compound (group of ion identities) to add tags to.
     * @param tag tags to add.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addTagsToCompoundExperimentalWithResponseSpec(String projectId, String compoundId, List<Tag> tag) throws WebClientResponseException {
        return addTagsToCompoundExperimentalRequestCreation(projectId, compoundId, tag);
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
     * [EXPERIMENTAL] Returns the full quantification table of compounds
     * [EXPERIMENTAL] Returns the full quantification table of compounds.  &lt;p&gt;  The quantification table contains a quantification of the features within all  runs they are contained in.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param type quantification type.
     * @return QuantTableExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundQuantTableExperimentalRequestCreation(String projectId, QuantMeasure type) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundQuantTableExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/quant-table", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Returns the full quantification table of compounds
     * [EXPERIMENTAL] Returns the full quantification table of compounds.  &lt;p&gt;  The quantification table contains a quantification of the features within all  runs they are contained in.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param type quantification type.
     * @return QuantTableExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public QuantTableExperimental getCompoundQuantTableExperimental(String projectId, QuantMeasure type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTableExperimental> localVarReturnType = new ParameterizedTypeReference<QuantTableExperimental>() {};
        return getCompoundQuantTableExperimentalRequestCreation(projectId, type).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns the full quantification table of compounds
     * [EXPERIMENTAL] Returns the full quantification table of compounds.  &lt;p&gt;  The quantification table contains a quantification of the features within all  runs they are contained in.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param type quantification type.
     * @return ResponseEntity&lt;QuantTableExperimental&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QuantTableExperimental> getCompoundQuantTableExperimentalWithHttpInfo(String projectId, QuantMeasure type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTableExperimental> localVarReturnType = new ParameterizedTypeReference<QuantTableExperimental>() {};
        return getCompoundQuantTableExperimentalRequestCreation(projectId, type).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns the full quantification table of compounds
     * [EXPERIMENTAL] Returns the full quantification table of compounds.  &lt;p&gt;  The quantification table contains a quantification of the features within all  runs they are contained in.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param type quantification type.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundQuantTableExperimentalWithResponseSpec(String projectId, QuantMeasure type) throws WebClientResponseException {
        return getCompoundQuantTableExperimentalRequestCreation(projectId, type);
    }
    /**
     * [EXPERIMENTAL] Returns a single quantification table row for the given compound
     * [EXPERIMENTAL] Returns a single quantification table row for the given compound.  &lt;p&gt;  The quantification table contains a quantification of the feature within all  samples it is contained in.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return QuantTableExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundQuantTableRowExperimentalRequestCreation(String projectId, String compoundId, QuantMeasure type) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundQuantTableRowExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling getCompoundQuantTableRowExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/{compoundId}/quant-table-row", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Returns a single quantification table row for the given compound
     * [EXPERIMENTAL] Returns a single quantification table row for the given compound.  &lt;p&gt;  The quantification table contains a quantification of the feature within all  samples it is contained in.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return QuantTableExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public QuantTableExperimental getCompoundQuantTableRowExperimental(String projectId, String compoundId, QuantMeasure type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTableExperimental> localVarReturnType = new ParameterizedTypeReference<QuantTableExperimental>() {};
        return getCompoundQuantTableRowExperimentalRequestCreation(projectId, compoundId, type).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns a single quantification table row for the given compound
     * [EXPERIMENTAL] Returns a single quantification table row for the given compound.  &lt;p&gt;  The quantification table contains a quantification of the feature within all  samples it is contained in.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return ResponseEntity&lt;QuantTableExperimental&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QuantTableExperimental> getCompoundQuantTableRowExperimentalWithHttpInfo(String projectId, String compoundId, QuantMeasure type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTableExperimental> localVarReturnType = new ParameterizedTypeReference<QuantTableExperimental>() {};
        return getCompoundQuantTableRowExperimentalRequestCreation(projectId, compoundId, type).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns a single quantification table row for the given compound
     * [EXPERIMENTAL] Returns a single quantification table row for the given compound.  &lt;p&gt;  The quantification table contains a quantification of the feature within all  samples it is contained in.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundQuantTableRowExperimentalWithResponseSpec(String projectId, String compoundId, QuantMeasure type) throws WebClientResponseException {
        return getCompoundQuantTableRowExperimentalRequestCreation(projectId, compoundId, type);
    }
    /**
     * [EXPERIMENTAL] Returns the traces of the given compound
     * [EXPERIMENTAL] Returns the traces of the given compound.  &lt;p&gt;  A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
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
     * [EXPERIMENTAL] Returns the traces of the given compound
     * [EXPERIMENTAL] Returns the traces of the given compound.  &lt;p&gt;  A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
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
     * [EXPERIMENTAL] Returns the traces of the given compound
     * [EXPERIMENTAL] Returns the traces of the given compound.  &lt;p&gt;  A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
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
     * [EXPERIMENTAL] Returns the traces of the given compound
     * [EXPERIMENTAL] Returns the traces of the given compound.  &lt;p&gt;  A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
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
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
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
    private ResponseSpec getCompoundsByGroupExperimentalRequestCreation(String projectId, String group, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundsByGroupExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'group' is set
        if (group == null) {
            throw new WebClientResponseException("Missing the required parameter 'group' when calling getCompoundsByGroupExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
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
    public PagedModelCompound getCompoundsByGroupExperimental(String projectId, String group, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return getCompoundsByGroupExperimentalRequestCreation(projectId, group, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
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
    public ResponseEntity<PagedModelCompound> getCompoundsByGroupExperimentalWithHttpInfo(String projectId, String group, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return getCompoundsByGroupExperimentalRequestCreation(projectId, group, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
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
    public ResponseSpec getCompoundsByGroupExperimentalWithResponseSpec(String projectId, String group, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        return getCompoundsByGroupExperimentalRequestCreation(projectId, group, page, size, sort, optFields);
    }
    /**
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name. Possible field names are:&lt;/p&gt;   &lt;ul&gt;    &lt;li&gt;&lt;strong&gt;tagName&lt;/strong&gt; - name of the tag&lt;/li&gt;    &lt;li&gt;&lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;tagName:my_name&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;text:&amp;quot;new york&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;text:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;integer&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;integer:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;(tagName:hello || tagName:world) &amp;amp;&amp;amp; text:&amp;quot;new york&amp;quot; AND text:/[mb]oat/ AND integer:[1 TO *] OR real&amp;lt;&#x3D;3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date&amp;lt;2022-01-01 OR time:12\\:00\\:00 OR time:[12\\:00\\:00 TO 14\\:00\\:00] OR time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
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
    private ResponseSpec getCompoundsByTagExperimentalRequestCreation(String projectId, String filter, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundsByTagExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name. Possible field names are:&lt;/p&gt;   &lt;ul&gt;    &lt;li&gt;&lt;strong&gt;tagName&lt;/strong&gt; - name of the tag&lt;/li&gt;    &lt;li&gt;&lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;tagName:my_name&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;text:&amp;quot;new york&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;text:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;integer&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;integer:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;(tagName:hello || tagName:world) &amp;amp;&amp;amp; text:&amp;quot;new york&amp;quot; AND text:/[mb]oat/ AND integer:[1 TO *] OR real&amp;lt;&#x3D;3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date&amp;lt;2022-01-01 OR time:12\\:00\\:00 OR time:[12\\:00\\:00 TO 14\\:00\\:00] OR time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
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
    public PagedModelCompound getCompoundsByTagExperimental(String projectId, String filter, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return getCompoundsByTagExperimentalRequestCreation(projectId, filter, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name. Possible field names are:&lt;/p&gt;   &lt;ul&gt;    &lt;li&gt;&lt;strong&gt;tagName&lt;/strong&gt; - name of the tag&lt;/li&gt;    &lt;li&gt;&lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;tagName:my_name&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;text:&amp;quot;new york&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;text:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;integer&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;integer:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;(tagName:hello || tagName:world) &amp;amp;&amp;amp; text:&amp;quot;new york&amp;quot; AND text:/[mb]oat/ AND integer:[1 TO *] OR real&amp;lt;&#x3D;3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date&amp;lt;2022-01-01 OR time:12\\:00\\:00 OR time:[12\\:00\\:00 TO 14\\:00\\:00] OR time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
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
    public ResponseEntity<PagedModelCompound> getCompoundsByTagExperimentalWithHttpInfo(String projectId, String filter, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return getCompoundsByTagExperimentalRequestCreation(projectId, filter, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name. Possible field names are:&lt;/p&gt;   &lt;ul&gt;    &lt;li&gt;&lt;strong&gt;tagName&lt;/strong&gt; - name of the tag&lt;/li&gt;    &lt;li&gt;&lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;tagName:my_name&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;text:&amp;quot;new york&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;text:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;integer&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;integer:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;(tagName:hello || tagName:world) &amp;amp;&amp;amp; text:&amp;quot;new york&amp;quot; AND text:/[mb]oat/ AND integer:[1 TO *] OR real&amp;lt;&#x3D;3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date&amp;lt;2022-01-01 OR time:12\\:00\\:00 OR time:[12\\:00\\:00 TO 14\\:00\\:00] OR time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;   [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
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
    public ResponseSpec getCompoundsByTagExperimentalWithResponseSpec(String projectId, String filter, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields) throws WebClientResponseException {
        return getCompoundsByTagExperimentalRequestCreation(projectId, filter, page, size, sort, optFields);
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
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId compound (group of ion identities) to delete tag from.
     * @param tagName name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec removeTagFromCompoundExperimentalRequestCreation(String projectId, String compoundId, String tagName) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling removeTagFromCompoundExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling removeTagFromCompoundExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tagName' is set
        if (tagName == null) {
            throw new WebClientResponseException("Missing the required parameter 'tagName' when calling removeTagFromCompoundExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/tags/{compoundId}/{tagName}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId compound (group of ion identities) to delete tag from.
     * @param tagName name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void removeTagFromCompoundExperimental(String projectId, String compoundId, String tagName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        removeTagFromCompoundExperimentalRequestCreation(projectId, compoundId, tagName).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId compound (group of ion identities) to delete tag from.
     * @param tagName name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> removeTagFromCompoundExperimentalWithHttpInfo(String projectId, String compoundId, String tagName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return removeTagFromCompoundExperimentalRequestCreation(projectId, compoundId, tagName).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId compound (group of ion identities) to delete tag from.
     * @param tagName name of the tag to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec removeTagFromCompoundExperimentalWithResponseSpec(String projectId, String compoundId, String tagName) throws WebClientResponseException {
        return removeTagFromCompoundExperimentalRequestCreation(projectId, compoundId, tagName);
    }
}
